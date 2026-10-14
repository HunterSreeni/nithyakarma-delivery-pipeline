package org.nithyakarma.pipeline.tests;

import org.nithyakarma.pipeline.config.Config;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Pipeline canary, no browser, no data. Passes normally; with DEMO_EXPECTED_FAILURE=true it fails
// on purpose so the red path of the pipeline (reports, labelling) can be shown without breaking a real test.
// Own group with no dependencies, so in the DAG suite it never skips anything else.
public class CanaryTest {

    @Test(groups = "canary")
    public void pipelineCanary() {
        assertThat(Config.demoExpectedFailure())
                .as("DEMO_EXPECTED_FAILURE is on: this failure is intentional (demo of a red build)")
                .isFalse();
    }
}
