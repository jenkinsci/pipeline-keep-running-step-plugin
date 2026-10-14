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

    /** Starts a long-lived background process and records its pid in the workspace. */
    private static final String START_BACKGROUND_PROCESS = "sh 'sleep 120 & echo $! > pid.txt'";

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
                "node {\n    keepRunning {\n        " + START_BACKGROUND_PROCESS + "\n    }\n}\n", true));

        j.buildAndAssertSuccess(p);

        long pid = Long.parseLong(readFromWorkspace(j, p, "pid.txt"));
        try {
            assertTrue(
                    isAliveAfter(pid, 5_000),
                    "pid " + pid + " was started inside keepRunning and must outlive the build");
        } finally {
            ProcessHandle.of(pid).ifPresent(ProcessHandle::destroyForcibly);
        }
    }

    private static String readFromWorkspace(JenkinsRule j, WorkflowJob p, String file) throws Exception {
        FilePath ws = j.jenkins.getWorkspaceFor(p);
        String content = ws.child(file).readToString().trim();
        assertFalse(content.isEmpty(), file + " was not written by the pipeline");
        return content;
    }

    /** Polls for the given duration; reports whether the process is still there at the end. */
    private static boolean isAliveAfter(long pid, long millis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + millis;
        while (true) {
            java.util.Optional<ProcessHandle> handle = ProcessHandle.of(pid);
            if (handle.isEmpty() || !handle.get().isAlive()) {
                return false;
            }
            if (System.currentTimeMillis() >= deadline) {
                return true;
            }
            Thread.sleep(250);
        }
    }
}
