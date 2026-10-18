package kopo.poly;

import kopo.poly.service.impl.IrisInferenceService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Loads and executes the actual .onnx binary with the JVM CPU runtime.
 */
class IrisInferenceServiceTests {

    @Test
    void predictsThreeTypicalIrisSpecies() throws Exception {
        try (var model = new CloseableIrisModel()) {
            var service = model.service;
            assertEquals("setosa", service.predict(5.1f, 3.5f, 1.4f, 0.2f).species());
            assertEquals("versicolor", service.predict(6.0f, 2.7f, 4.5f, 1.5f).species());
            assertEquals("virginica", service.predict(6.9f, 3.1f, 5.4f, 2.1f).species());

            var prediction = service.predict(5.1f, 3.5f, 1.4f, 0.2f);
            float sum = 0;
            for (float value : prediction.probabilities().values()) {
                assertTrue(value >= 0f && value <= 1f);
                sum += value;
            }
            assertEquals(1.0f, sum, 1e-5f);
        }
    }

    @Test
    void rejectsInvalidMeasurements() throws Exception {
        try (var model = new CloseableIrisModel()) {
            assertThrows(IllegalArgumentException.class, () ->
                    model.service.predict(-1f, 3.5f, 1.4f, 0.2f));
            assertThrows(IllegalArgumentException.class, () ->
                    model.service.predict(Float.NaN, 3.5f, 1.4f, 0.2f));
        }
    }

    private static class CloseableIrisModel implements AutoCloseable {
        final IrisInferenceService service;

        CloseableIrisModel() throws Exception {
            service = new IrisInferenceService();
        }

        @Override
        public void close() throws Exception {
            service.close();
        }
    }
}
