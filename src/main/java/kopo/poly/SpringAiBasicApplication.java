package kopo.poly;

import kopo.poly.dto.NlpDTO;
import kopo.poly.dto.OcrDTO;
import kopo.poly.dto.StudentDTO;
import kopo.poly.service.INlpService;
import kopo.poly.service.IOcrService;
import kopo.poly.service.IStudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@SpringBootApplication
public class SpringAiBasicApplication implements CommandLineRunner {

    // Spring creates the @Service implementations and injects them here.
    private final IOcrService ocrService;
    private final INlpService nlpService;
    private final IStudentService studentService;

    @Value("${demo.mode:none}")
    private String demoMode;

    public static void main(String[] args) {
        SpringApplication.run(SpringAiBasicApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Students explicitly select the example they want to run.
        switch (demoMode.toLowerCase(Locale.ROOT)) {
            case "none" -> log.info("실습 모드가 선택되지 않았습니다. --demo.mode=nlp 등을 지정하세요.");
            case "nlp" -> runNlpDemo("한국폴리텍대학에서 인공지능과 빅데이터를 공부합니다.");
            case "ocr" -> runOcrDemo();
            case "ocr-nlp" -> {
                String recognizedText = runOcrDemo();
                runNlpDemo(recognizedText);
            }
            case "student" -> runStudentDemo();
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 demo.mode: " + demoMode + " (none, student, ocr, nlp, ocr-nlp)");
        }
    }

    private String runOcrDemo() throws Exception {
        // The example image is bundled in src/main/resources/image.
        OcrDTO request = new OcrDTO();
        request.setFilePath("image");
        request.setFileName("sample01.jpg");

        OcrDTO response = ocrService.getReadforImageText(request);
        String recognizedText = response.getResult();
        log.info("OCR 인식 결과:\n{}", recognizedText);
        return recognizedText;
    }

    private void runNlpDemo(String text) {
        // Part-of-speech tagging: nouns, verbs, particles and other word classes.
        NlpDTO tagged = nlpService.getPlainText(text);
        log.info("형태소/품사 분석 결과: {}", tagged.getResult());

        // Extract nouns, then count and sort occurrences.
        NlpDTO analyzed = nlpService.getNouns(text);
        List<String> nouns = analyzed.getNouns();
        Map<String, Integer> frequencies = new HashMap<>();

        for (String noun : nouns) {
            // merge increments the count when the same noun appears again.
            frequencies.merge(noun, 1, Integer::sum);
        }

        List<Map.Entry<String, Integer>> ranked = new ArrayList<>(frequencies.entrySet());
        ranked.sort(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()));

        log.info("명사 전체 개수: {}, 중복 제외: {}", nouns.size(), frequencies.size());
        log.info("명사 출현 빈도(내림차순): {}", ranked);
    }

    private void runStudentDemo() throws Exception {
        // Use a unique demo ID so the example never edits an existing student's row.
        String demoId = "demo_" + UUID.randomUUID().toString().substring(0, 8);
        StudentDTO student = new StudentDTO();
        student.setUserId(demoId);
        student.setUserName("실습학생");
        student.setEmail("student@example.com");
        student.setAddr("서울");

        log.info("1. 학생 INSERT 후 전체 조회");
        printStudentList(studentService.insertStudent(student));

        student.setUserName("실습학생_수정");
        student.setAddr("부천");
        log.info("2. 학생 UPDATE 후 전체 조회");
        printStudentList(studentService.updateStudent(student));

        StudentDTO toDelete = new StudentDTO();
        toDelete.setUserId(demoId);
        log.info("3. 학생 DELETE 후 전체 조회");
        printStudentList(studentService.deleteStudent(toDelete));
    }

    private void printStudentList(List<StudentDTO> students) {
        for (StudentDTO student : students) {
            log.info("아이디={}, 이름={}, 이메일={}, 주소={}",
                    student.getUserId(), student.getUserName(),
                    student.getEmail(), student.getAddr());
        }
    }
}
