/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Tang Ming
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.shanyshanb.plugins.pipeline.steps;

import org.jenkinsci.plugins.workflow.steps.*;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Execution of {@link KeepRunningStep}.
 *
 * @author Tang Ming
 */
public class KeepRunningStepExecution extends AbstractStepExecutionImpl {
    private static final long serialVersionUID = 1L;

    protected KeepRunningStepExecution(@Nonnull StepContext context) {
        super(context);
    }

    @Override
    public boolean start() throws Exception {
        Map<String, String> keepRunning = new HashMap<>();
        keepRunning.put("JENKINS_NODE_COOKIE", "keepRunning");
        getContext().newBodyInvoker().
                withContext(EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class), EnvironmentExpander.constant(keepRunning))).
                withCallback(BodyExecutionCallback.wrap(getContext())).
                start();
        return false;
    }

    @Override public void onResume() {}
}