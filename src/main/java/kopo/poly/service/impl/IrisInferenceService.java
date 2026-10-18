package kopo.poly.service.impl;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PreDestroy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Example of real ML inference:
 * features -> ONNX Logistic Regression model -> class probabilities.
 *
 * The model was trained on the Iris dataset and exported to the ONNX format.
 * No model training, online API or GPU is needed to call predict().
 */
@Service
public class IrisInferenceService {

    private static final String[] SPECIES = {"setosa", "versicolor", "virginica"};
    private static final String INPUT_NAME = "features";

    private final OrtEnvironment environment;
    private final OrtSession session;

    public IrisInferenceService() throws IOException, OrtException {
        environment = OrtEnvironment.getEnvironment();

        // ClassPathResource.getInputStream() also works when packed into a JAR.
        var resource = new ClassPathResource("models/iris-logreg.onnx");
        byte[] model;
        try (InputStream input = resource.getInputStream()) {
            model = input.readAllBytes();
        }

        try (var options = new OrtSession.SessionOptions()) {
            session = environment.createSession(model, options);
        }
    }

    public Prediction predict(float sepalLength, float sepalWidth,
                              float petalLength, float petalWidth) throws OrtException {
        float[] measurements = {sepalLength, sepalWidth, petalLength, petalWidth};
        for (float value : measurements) {
            if (!Float.isFinite(value) || value <= 0) {
                throw new IllegalArgumentException("붓꽃의 길이·너비는 유한한 양수(cm)여야 합니다.");
            }
        }

        // Input tensor: one iris example, four floating-point measurements [1, 4].
        float[][] features = {measurements};

        try (OnnxTensor input = OnnxTensor.createTensor(environment, features);
             OrtSession.Result outputs = session.run(Collections.singletonMap(INPUT_NAME, input))) {
            float[][] matrix = (float[][]) ((OnnxTensor) outputs.get(0)).getValue();
            float[] probabilities = matrix[0];

            int best = 0;
            Map<String, Float> probabilitiesBySpecies = new LinkedHashMap<>();
            for (int i = 0; i < SPECIES.length; i++) {
                probabilitiesBySpecies.put(SPECIES[i], probabilities[i]);
                if (probabilities[i] > probabilities[best]) {
                    best = i;
                }
            }
            return new Prediction(SPECIES[best], probabilities[best],
                    Collections.unmodifiableMap(probabilitiesBySpecies));
        }
    }

    /** Three model outputs are estimates, not a guarantee of classification correctness. */
    public record Prediction(String species, float confidence, Map<String, Float> probabilities) {
    }

    @PreDestroy
    public void close() throws OrtException {
        session.close();
    }
}
