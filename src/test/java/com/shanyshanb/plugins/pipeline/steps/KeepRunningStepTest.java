package com.shanyshanb.plugins.pipeline.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
 * process is still running after the build finished).
 *
 * <p>The step requires a {@link FilePath} and {@code TaskListener} context, so it has to be used
 * inside a {@code node} block - the same requirement as the {@code sh} step it normally wraps.
 */
@WithJenkins
class KeepRunningStepTest {

    /**
     * Writes the current time to {@code heartbeat.txt} once a second, then exits on its own after
     * about 40 seconds so that a surviving process cannot outlive the test run.
     *
     * <p>Liveness is deliberately observed through the file rather than through a pid: on Windows the
     * {@code sh} step runs under Git Bash, whose pids are MSYS pids that {@code ProcessHandle} in the
     * test JVM cannot resolve (this test used to fail on the windows-21 CI leg for that reason).
     */
    private static final String START_HEARTBEAT_PROCESS =
            "sh 'i=0; while [ $i -lt 40 ]; do date > heartbeat.txt; i=$((i+1)); sleep 1; done > /dev/null 2>&1 &'";

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

        String first = readFromWorkspace(j, p, "heartbeat.txt");
        Thread.sleep(4_000);
        String second = readFromWorkspace(j, p, "heartbeat.txt");

        // The loop keeps rewriting the file every second; if the build had killed it, it would be frozen.
        assertNotEquals(
                first,
                second,
                "the process started inside keepRunning stopped working after the build finished"
                        + " (heartbeat.txt is still <" + first + ">)");
    }

    private static String readFromWorkspace(JenkinsRule j, WorkflowJob p, String file) throws Exception {
        FilePath ws = j.jenkins.getWorkspaceFor(p);
        String content = ws.child(file).readToString().trim();
        assertFalse(content.isEmpty(), file + " was not written by the pipeline");
        return content;
    }
}
