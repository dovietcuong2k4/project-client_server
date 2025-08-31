import lombok.extern.slf4j.Slf4j;
import org.client_server.model.Sex;
import org.client_server.model.Student;
import org.client_server.server.StudentRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class StudentRepositoryTest {
    private StudentRepository repository = new StudentRepository();

    @Test
    void testInsertAndFindAll() {
       // Student student = new Student("Marry", LocalDate.parse("2001-10-01"), 3.6, Sex.FEMALE, "IT");
        Student student = Student.builder()
                .name("Test")
                .dob(LocalDate.parse("2001-10-01"))
                .gpa(3.6)
                .sex(Sex.FEMALE)
                .major("IT")
                .build();

        repository.insert(student);

        List<Student> studentList = repository.findAll();

        assertEquals("Test", studentList.get(studentList.size() - 1).getName());
        assertEquals(3.6, studentList.get(studentList.size() - 1).getGpa());

        log.info("Test thành công");

    }
}
