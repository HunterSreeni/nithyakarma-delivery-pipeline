package org.nithyakarma.pipeline.tests;

import org.nithyakarma.pipeline.base.LoggedInUiTestBase;
import org.nithyakarma.pipeline.config.Config;
import org.nithyakarma.pipeline.pages.AppHeader;
import org.nithyakarma.pipeline.pages.TodayPage;
import org.openqa.selenium.bidi.module.Network;
import org.openqa.selenium.bidi.network.AddInterceptParameters;
import org.openqa.selenium.bidi.network.InterceptPhase;
import org.openqa.selenium.bidi.network.UrlPattern;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// T10: kill the app's request for tracked practices (BiDi network intercept) and check the
// app survives: no uncaught JS error (JsErrorListener), header and navigation still work.
public class ResilienceTest extends LoggedInUiTestBase {
    private static final String BLOCKED_PATH = "/rest/v1/user_practices";

    @Test(groups = "features")
    public void appStaysUsableWhenPracticesRequestFails() {
        AtomicInteger blocked = new AtomicInteger();
        // BiDi events arrive on the connection's own thread. Sending a command (failRequest) from
        // that thread deadlocks: it waits for a reply only that same thread can read. So hand it off.
        ExecutorService commands = Executors.newSingleThreadExecutor();
        try (Network network = new Network(driver)) {
            // pause only requests to this one endpoint (any query string)
            UrlPattern target = new UrlPattern()
                    .protocol("https")
                    .hostname(URI.create(Config.supabaseUrl()).getHost())
                    .pathname(BLOCKED_PATH);
            String intercept = network.addIntercept(
                    new AddInterceptParameters(InterceptPhase.BEFORE_REQUEST_SENT).urlPattern(target));
            network.onBeforeRequestSent(event -> {
                if (event.isBlocked()) {
                    blocked.incrementAndGet();
                    String requestId = event.getRequest().getRequestId();
                    commands.submit(() -> network.failRequest(requestId));
                }
            });
            try {
                TodayPage today = new TodayPage(driver);
                today.open(baseUrl);

                AppHeader header = new AppHeader(driver);
                header.waitUntilLoggedIn();
                assertThat(today.greeting()).matches("Namaskaram, \\S+.*");
                assertThat(blocked.get()).as("requests to " + BLOCKED_PATH + " that were failed").isPositive();

                header.goTo("Learning");
                header.goTo("Today");
            } finally {
                // close() alone does not remove the intercept; a leftover one would hang those requests
                network.removeIntercept(intercept);
            }
        } finally {
            commands.shutdownNow();
        }
    }
}
