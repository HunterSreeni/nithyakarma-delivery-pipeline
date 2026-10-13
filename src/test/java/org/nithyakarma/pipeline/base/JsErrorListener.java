package org.nithyakarma.pipeline.base;

import org.nithyakarma.pipeline.bidi.Scrubber;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import java.util.List;

// Runs right after every UI test method, before TestNG records the result:
// - a passing test fails if the page threw an uncaught JavaScript exception
// - a failing test gets its scrubbed browser console saved to reports/console/
public class JsErrorListener implements IInvokedMethodListener {

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (!method.isTestMethod() || !(result.getInstance() instanceof UiTestBase test) || test.guard == null) {
            return;
        }
        List<String> jsErrors = test.guard.jsErrors();
        if (result.getStatus() == ITestResult.SUCCESS && !jsErrors.isEmpty()) {
            result.setStatus(ITestResult.FAILURE);
            result.setThrowable(new AssertionError(
                    "page threw " + jsErrors.size() + " uncaught JS error(s): " + Scrubber.scrub(jsErrors.get(0))));
        }
        if (result.getStatus() == ITestResult.FAILURE) {
            String name = result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
            test.guard.saveConsole(name);
        }
    }
}
