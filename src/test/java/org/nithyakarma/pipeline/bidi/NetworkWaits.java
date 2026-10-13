package org.nithyakarma.pipeline.bidi;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.module.Network;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// "Click, then wait for the save to reach the server" without sleeps.
// The app updates the screen first and saves in the background, so a visible change
// does not mean the data is saved. BiDi lets us wait for the actual 2xx response.
// A CountDownLatch, not WebDriverWait: the latch wakes the moment the event arrives,
// WebDriverWait only polls every 500ms.
public final class NetworkWaits {

    private NetworkWaits() {
    }

    public static void clickAndWaitFor(WebDriver driver, String method, String urlPart, Runnable click) {
        CountDownLatch done = new CountDownLatch(1);
        try (Network network = new Network(driver)) {
            network.onResponseCompleted(response -> {
                if (method.equals(response.getRequest().getMethod())
                        && response.getRequest().getUrl().contains(urlPart)
                        && response.getResponseData().getStatus() / 100 == 2) {
                    done.countDown();
                }
            });
            click.run();
            if (!done.await(10, TimeUnit.SECONDS)) {
                throw new TimeoutException("no 2xx " + method + " " + urlPart + " within 10s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
