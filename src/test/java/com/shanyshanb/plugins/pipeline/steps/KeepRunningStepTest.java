package com.shanyshanb.plugins.pipeline.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.FilePath;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Runtime tests for the {@code keepRunning} step.
 *
 * <p>Every test boots a real Jenkins instance and runs a real Pipeline build on the built-in node;
 * nothing is mocked. Jenkins kills the processes a build leaves behind by looking for the
 * build-scoped {@code JENKINS_NODE_COOKIE} value in their environment, and this step works by
 * replacing that value with the constant {@code keepRunning} for everything inside its body. The
 * tests assert both the mechanism (the cookie the child process really sees) and the outcome (the
 * process is still working after the build finished).
 *
 * <p>The step requires a {@link FilePath} and {@code TaskListener} context, so it has to be used
 * inside a {@code node} block - the same requirement as the {@code sh} step it normally wraps.
 */
@WithJenkins
class KeepRunningStepTest {

    /**
     * Rewrites {@code heartbeat.txt} with the current time once a second, then exits on its own after
     * about 40 seconds so that a surviving process cannot outlive the test run.
     *
     * <p>The file is produced through a temporary file and a rename so a reader never observes a
     * half-written file; liveness is observed through the file rather than through a pid because the
     * {@code sh} step runs under Git Bash on Windows, where pids are MSYS pids that the test JVM
     * cannot resolve.
     */
    private static final String START_HEARTBEAT_PROCESS = "sh 'i=0; while [ $i -lt 40 ]; do date > heartbeat.tmp;"
            + " mv -f heartbeat.tmp heartbeat.txt; i=$((i+1)); sleep 1; done > /dev/null 2>&1 &'";

    /** Records the JENKINS_NODE_COOKIE the child process actually runs with. */
    private static final String RECORD_COOKIE = "sh 'printf %s \"$JENKINS_NODE_COOKIE\" > cookie.txt'";

    @Test
    void keepRunningReplacesTheNodeCookieSeenByTheBody(JenkinsRule j) throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "keepRunningCookie");
        p.setDefinition(
                new CpsFlowDefinition("node {\n    keepRunning {\n        " + RECORD_COOKIE + "\n    }\n}\n", true));

        j.buildAndAssertSuccess(p);

        assertEquals(
                "keepRunning",
                readFromWorkspace(j, p, "cookie.txt"),
                "inside keepRunning the build-scoped node cookie must be replaced, otherwise Jenkins"
                        + " still finds and kills the process when the build finishes");
    }

    @Test
    void withoutKeepRunningTheNodeCookieIsBuildScoped(JenkinsRule j) throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "plainCookie");
        p.setDefinition(new CpsFlowDefinition("node {\n    " + RECORD_COOKIE + "\n}\n", true));

        j.buildAndAssertSuccess(p);

        String cookie = readFromWorkspace(j, p, "cookie.txt");
        assertNotEquals("keepRunning", cookie, "the plain pipeline must not already use the keepRunning cookie");
        assertTrue(
                cookie.matches("[0-9a-fA-F-]{36}"),
                "the plain pipeline is expected to carry the build-scoped JENKINS_NODE_COOKIE uuid but was <" + cookie
                        + ">");
    }

    @Test
    void processStartedInsideKeepRunningSurvivesTheBuild(JenkinsRule j) throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "keepRunningSurvives");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n    keepRunning {\n        " + START_HEARTBEAT_PROCESS + "\n    }\n}\n", true));

        j.buildAndAssertSuccess(p);

        String first = awaitHeartbeat(j, p);
        assertNotNull(
                first,
                "the background process never wrote heartbeat.txt, so it either did not start or was killed"
                        + " right away");
        Thread.sleep(4_000);
        String second = awaitHeartbeat(j, p);

        // The loop rewrites the file every second; a killed process would leave it frozen or missing.
        assertNotEquals(
                first,
                second,
                "the process started inside keepRunning stopped working after the build finished"
                        + " (heartbeat.txt is still <" + first + ">)");
    }

    /** Polls for a non-empty heartbeat value; returns {@code null} if it never appears. */
    private static String awaitHeartbeat(JenkinsRule j, WorkflowJob p) throws Exception {
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            String content = readQuietly(j, p, "heartbeat.txt");
            if (content != null && !content.isEmpty()) {
                return content;
            }
            Thread.sleep(500);
        }
        return null;
    }

    private static String readFromWorkspace(JenkinsRule j, WorkflowJob p, String file) throws Exception {
        String content = readQuietly(j, p, file);
        assertNotNull(content, file + " does not exist in " + j.jenkins.getWorkspaceFor(p));
        assertTrue(!content.isEmpty(), file + " was not written by the pipeline");
        return content;
    }

    /** Reads a workspace file, returning {@code null} when it is missing, empty or being rewritten. */
    private static String readQuietly(JenkinsRule j, WorkflowJob p, String file) {
        try {
            FilePath child = j.jenkins.getWorkspaceFor(p).child(file);
            if (!child.exists()) {
                return null;
            }
            return child.readToString().trim();
        } catch (Exception e) {
            return null; // raced with the writer
        }
    }
}
