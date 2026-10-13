package org.nithyakarma.pipeline.base;

import org.nithyakarma.pipeline.auth.Sessions;
import org.nithyakarma.pipeline.auth.UserSession;
import org.nithyakarma.pipeline.pages.AppHeader;
import org.testng.annotations.BeforeClass;

// UiTestBase opens Chrome first (TestNG runs the parent's @BeforeClass first), then this logs in as A.
// Each class mints its own session, so no test depends on another class having run.
public abstract class LoggedInUiTestBase extends UiTestBase {
    protected UserSession userA;

    @BeforeClass(alwaysRun = true)
    public void logInAsA() {
        userA = Sessions.userA();
        Sessions.injectInto(driver, userA);
        new AppHeader(driver).waitUntilLoggedIn();
    }
}
