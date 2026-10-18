package kopo.poly;

import kopo.poly.dto.OcrDTO;
import kopo.poly.dto.StudentDTO;
import kopo.poly.service.IOcrService;
import kopo.poly.service.IStudentService;
import kopo.poly.service.impl.KoSentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@SpringBootApplication
public class SpringAiBasicApplication implements CommandLineRunner {

    // The Keras model is loaded by the independent Python koSent server, not this JVM.
    private final KoSentClient koSentClient;
    private final IOcrService ocrService;
    private final IStudentService studentService;

    @Value("${demo.mode:none}")
    private String demoMode;

    @Value("${sentiment.text:정말 재미있고 감동적인 영화였습니다}")
    private String sentimentText;

    public static void main(String[] args) {
        SpringApplication.run(SpringAiBasicApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        switch (demoMode.toLowerCase(Locale.ROOT)) {
            case "none" -> log.info("실습 모드: --demo.mode=sentiment, sentiment-batch, ocr 또는 student");
            case "sentiment" -> runSentimentDemo();
            case "sentiment-batch" -> runSentimentBatchDemo();
            case "ocr" -> runOcrDemo();
            case "student" -> runStudentDemo();
            default -> throw new IllegalArgumentException(
                    "잘못된 demo.mode: " + demoMode
                            + " (none, sentiment, sentiment-batch, ocr, student)");
        }
    }

    private void runSentimentDemo() {
        log.info("감성분석 원문: {}", sentimentText);
        var prediction = koSentClient.predict(sentimentText);
        logPrediction(prediction);
    }

    private void runSentimentBatchDemo() {
        List<String> examples = List.of(
                "정말 재미있고 감동적인 영화였습니다",
                "스토리가 지루하고 연기도 아쉬웠습니다",
                "생각보다 괜찮았지만 결말은 별로였어요");
        var predictions = koSentClient.predictBatch(examples);

        for (int i = 0; i < examples.size(); i++) {
            log.info("문장 {}: {}", i + 1, examples.get(i));
            logPrediction(predictions.get(i));
        }
    }

    private void logPrediction(KoSentClient.Prediction prediction) {
        String sentiment = "pos".equals(prediction.label()) ? "긍정" : "부정";
        log.info("분류 결과={} / 긍정 점수={} / 응답 라벨={}",
                sentiment, String.format(Locale.ROOT, "%.4f", prediction.score()),
                prediction.label());
        log.info("참고: score는 긍정 클래스의 모델 점수이며 정확도나 신뢰도 보증 값이 아닙니다.");
    }

    private void runOcrDemo() throws Exception {
        OcrDTO request = new OcrDTO();
        request.setFilePath("image");
        request.setFileName("sample01.jpg");
        log.info("OCR 인식 결과:\n{}", ocrService.getReadforImageText(request).getResult());
    }

    private void runStudentDemo() throws Exception {
        String demoId = "demo_" + UUID.randomUUID().toString().substring(0, 8);
        StudentDTO student = new StudentDTO();
        student.setUserId(demoId);
        student.setUserName("실습학생");
        student.setEmail("student@example.com");
        student.setAddr("서울");

        log.info("1. 학생 등록");
        printStudents(studentService.insertStudent(student));

        student.setUserName("실습학생_수정");
        student.setAddr("부천");
        log.info("2. 학생 수정");
        printStudents(studentService.updateStudent(student));

        StudentDTO toDelete = new StudentDTO();
        toDelete.setUserId(demoId);
        log.info("3. 학생 삭제");
        printStudents(studentService.deleteStudent(toDelete));
    }

    private void printStudents(List<StudentDTO> students) {
        for (StudentDTO student : students) {
            log.info("아이디={}, 이름={}, 이메일={}, 주소={}",
                    student.getUserId(), student.getUserName(),
                    student.getEmail(), student.getAddr());
        }
    }
}
