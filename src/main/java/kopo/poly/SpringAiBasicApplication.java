package kopo.poly;

import kopo.poly.dto.OcrDTO;
import kopo.poly.dto.StudentDTO;
import kopo.poly.service.IOcrService;
import kopo.poly.service.IStudentService;
import kopo.poly.service.impl.IrisInferenceService;
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

    private final IrisInferenceService irisInferenceService;
    private final IOcrService ocrService;
    private final IStudentService studentService;

    @Value("${demo.mode:none}")
    private String demoMode;

    @Value("${iris.sepal-length:5.1}")
    private float sepalLength;
    @Value("${iris.sepal-width:3.5}")
    private float sepalWidth;
    @Value("${iris.petal-length:1.4}")
    private float petalLength;
    @Value("${iris.petal-width:0.2}")
    private float petalWidth;

    public static void main(String[] args) {
        SpringApplication.run(SpringAiBasicApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        switch (demoMode.toLowerCase(Locale.ROOT)) {
            case "none" -> log.info("실습 선택: --demo.mode=iris, ocr 또는 student");
            case "iris" -> runIrisDemo();
            case "ocr" -> runOcrDemo();
            case "student" -> runStudentDemo();
            default -> throw new IllegalArgumentException(
                    "알 수 없는 demo.mode: " + demoMode + " (none, iris, ocr, student)");
        }
    }

    private void runIrisDemo() throws Exception {
        // Supervised learning example:
        // four measurements -> pre-trained ONNX model -> three class probabilities.
        var result = irisInferenceService.predict(sepalLength, sepalWidth, petalLength, petalWidth);
        log.info("꽃받침 길이={}cm, 너비={}cm / 꽃잎 길이={}cm, 너비={}cm",
                sepalLength, sepalWidth, petalLength, petalWidth);
        result.probabilities().forEach((species, probability) ->
                log.info("품종 {} : {}%", species, String.format(Locale.ROOT, "%.2f", probability * 100)));
        log.info("최종 예측: {} (확률 약 {}%)", result.species(),
                String.format(Locale.ROOT, "%.2f", result.confidence() * 100));
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
