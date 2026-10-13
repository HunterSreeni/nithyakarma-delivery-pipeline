package org.nithyakarma.pipeline.bidi;

import org.nithyakarma.pipeline.config.Config;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.module.LogInspector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Listens to the browser through WebDriver BiDi while one test runs:
// - uncaught JavaScript exceptions (a UI test fails if the page threw)
// - console output (saved, scrubbed, when a test fails)
// One guard per test method; close() removes the listeners so they never leak into the next test.
public class BrowserLogGuard implements AutoCloseable {

    private final LogInspector inspector;
    private final List<String> jsErrors = Collections.synchronizedList(new ArrayList<>());
    private final List<String> console = Collections.synchronizedList(new ArrayList<>());

    public BrowserLogGuard(WebDriver driver) {
        inspector = new LogInspector(driver);
        // BiDi events arrive on another thread, hence the synchronized lists
        inspector.onJavaScriptException(entry -> jsErrors.add(entry.getText()));
        inspector.onConsoleEntry(entry -> console.add(entry.getLevel() + " " + entry.getText()));
    }

    public List<String> jsErrors() {
        synchronized (jsErrors) {
            return List.copyOf(jsErrors);
        }
    }

    // writes the scrubbed console to <reports>/console/<name>.log and returns the path
    public Path saveConsole(String name) {
        List<String> lines;
        synchronized (console) {
            lines = new ArrayList<>(console);
        }
        synchronized (jsErrors) {
            jsErrors.forEach(e -> lines.add("UNCAUGHT " + e));
        }
        Path file = Path.of(Config.reportsDir(), "console", name + ".log");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, Scrubber.scrub(String.join(System.lineSeparator(), lines)));
        } catch (IOException e) {
            throw new IllegalStateException("could not write " + file, e);
        }
        return file;
    }

    @Override
    public void close() {
        inspector.close();
    }
}
