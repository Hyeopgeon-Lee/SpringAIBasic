package kopo.poly.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Spring Boot 4 → HTTP/JSON → koSent FastAPI → pre-trained .keras model.
 *
 * koSent performs its original Korean text normalization and TextVectorization
 * on the Python side. Trying to reproduce them differently in Java could
 * change model predictions.
 */
@Service
public class KoSentClient {

    private final RestClient client;

    public KoSentClient(RestClient.Builder builder,
                        @Value("${kosent.api.base-url:http://127.0.0.1:8000}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    /** Calls POST /predict. The score returned by koSent means POSITIVE score. */
    public Prediction predict(String text) {
        String validText = validateText(text);
        try {
            Prediction raw = client.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", validText))
                    .retrieve()
                    .body(Prediction.class);
            return validateResponse(raw);
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "koSent API 호출에 실패했습니다. koSent 서버가 실행 중인지, "
                            + "KOSENT_API_URL 설정이 올바른지 확인하세요.", ex);
        }
    }

    /** Calls POST /predict/batch and returns results in input order. */
    public List<Prediction> predictBatch(List<String> texts) {
        if (texts == null || texts.isEmpty() || texts.size() > 256) {
            throw new IllegalArgumentException("문장은 1~256개까지 요청할 수 있습니다.");
        }
        List<String> validated = new ArrayList<>(texts.size());
        for (String text : texts) {
            validated.add(validateText(text));
        }

        try {
            List<Prediction> raw = client.post()
                    .uri("/predict/batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("texts", validated))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Prediction>>() {});
            if (raw == null || raw.size() != validated.size()) {
                throw new IllegalStateException("koSent API 응답의 결과 개수가 입력과 다릅니다.");
            }
            for (int i = 0; i < raw.size(); i++) {
                validateResponse(raw.get(i));
                if (raw.get(i).index() == null || raw.get(i).index() != i) {
                    throw new IllegalStateException("koSent API 배치 결과의 index 순서가 올바르지 않습니다.");
                }
            }
            return List.copyOf(raw);
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "koSent 배치 API 호출에 실패했습니다. koSent 서버 실행 상태를 확인하세요.", ex);
        }
    }

    private static String validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("분석할 문장을 입력해야 합니다.");
        }
        String trimmed = text.trim();
        if (trimmed.length() > 5000) {
            throw new IllegalArgumentException("한 문장은 5,000자를 초과할 수 없습니다.");
        }
        return trimmed;
    }

    private static Prediction validateResponse(Prediction p) {
        if (p == null || (!"pos".equals(p.label()) && !"neg".equals(p.label()))
                || p.score() == null || !Double.isFinite(p.score())
                || p.score() < 0.0 || p.score() > 1.0) {
            throw new IllegalStateException("koSent API 응답 형식이 올바르지 않습니다.");
        }
        return p;
    }

    /**
     * Both /predict and /predict/batch share label, score, score_str.
     * Only the batch endpoint returns 'index' (null for a single prediction).
     * JSON field names follow the Python response contract exactly.
     */
    public record Prediction(String label, Double score, String score_str, Integer index) {
    }
}
