package org.nithyakarma.pipeline.tests;

import org.nithyakarma.pipeline.base.LoggedInUiTestBase;
import org.nithyakarma.pipeline.pages.AppHeader;
import org.nithyakarma.pipeline.pages.LoginPage;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

// T14: uses its own session (LoggedInUiTestBase), so logging out cannot break another class
public class LogoutTest extends LoggedInUiTestBase {

    @Test(groups = "logout")
    public void logoutReturnsToLoginPage() {
        AppHeader header = new AppHeader(driver);
        header.logout();

        assertThat(new LoginPage(driver).isShown()).as("login page").isTrue();
        assertThat(header.isLogoutVisible()).as("logout button gone").isFalse();
    }
}
