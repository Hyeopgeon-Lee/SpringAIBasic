package kopo.poly;

import kopo.poly.dto.StudentDTO;
import kopo.poly.mapper.IStudentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Runs the actual MyBatis XML statements against an in-memory H2 database.
 * Oracle is NOT required to execute this test.
 */
@SpringBootTest(properties = {
        "demo.mode=none",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:spring_ai_basic;MODE=Oracle;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
class SpringAiBasicApplicationTests {

    @Autowired
    private IStudentMapper studentMapper;

    @Test
    void studentCrudWorksWithXmlMapper() throws Exception {
        StudentDTO student = new StudentDTO();
        student.setUserId("test_student_01");
        student.setUserName("테스트학생");
        student.setEmail("test@example.com");
        student.setAddr("서울");

        studentMapper.insertStudent(student);
        StudentDTO saved = studentMapper.getStudent(student);
        assertNotNull(saved);
        assertEquals("테스트학생", saved.getUserName());
        assertEquals("test@example.com", saved.getEmail());

        student.setUserName("수정된학생");
        studentMapper.updateStudent(student);
        assertEquals("수정된학생", studentMapper.getStudent(student).getUserName());

        studentMapper.deleteStudent(student);
        assertNull(studentMapper.getStudent(student));
    }
}
