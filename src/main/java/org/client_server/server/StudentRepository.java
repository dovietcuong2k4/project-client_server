package org.client_server.server;

import lombok.extern.slf4j.Slf4j;
import org.client_server.exception.ProgramException;
import org.client_server.model.Sex;
import org.client_server.model.Student;
import org.client_server.util.DBConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class StudentRepository {


    public long insert(Student student) {
        String sql = "INSERT INTO Students (FullName, Dob, Gpa, Sex, Major) VALUES (?,?,?,?,?)";

        try (Connection connection = DBConnector.getConnector();
             PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, student.getName());
            preparedStatement.setDate(2, Date.valueOf(student.getDob()));
            preparedStatement.setDouble(3, student.getGpa());
            preparedStatement.setString(4, student.getSex().name());
            preparedStatement.setString(5, student.getMajor());

            int row = preparedStatement.executeUpdate();
            if (row > 0){
                try (ResultSet rs = preparedStatement.getGeneratedKeys()){
                    if (rs.next()){
                        long id = rs.getLong("Id_student");
                        student.setId(id);
                        log.info("Insert thành công Student : {}", student);
                        return id;
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            log.error("Lỗi tại student : {}", student, e);
            throw new ProgramException("Lỗi không thể insert dữ liệu", e);
        }
    }

    //lấy ra tất cả trong danh sách
    public List<Student> findAll(){
        List<Student> studentList = new ArrayList<>();
        String sql = "SELECT Id_student, FullName, Dob, Gpa, Sex, Major FROM Students";

        try (Connection connection = DBConnector.getConnector();
        PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()){

            while (rs.next()){
                studentList.add(mapResultStudent(rs));
            }
            return studentList;
        } catch (SQLException e) {
            log.error("Lỗi không lấy được dữ liệu từ table Students",e);
            throw new ProgramException("Lỗi không lấy được dữ liệu từ table Students",e);
        }
    }

    public boolean update(Student student){
        String sql = "UPDATE Students SET FullName=?, Dob=?, Gpa=?, Sex=?, Major=? WHERE Id_student=?";
        try (Connection connection = DBConnector.getConnector();
        PreparedStatement statement = connection.prepareStatement(sql)){

            statement.setString(1, student.getName());
            statement.setDate(2, Date.valueOf(student.getDob()));
            statement.setDouble(3, student.getGpa());
            statement.setString(4, student.getSex().name());
            statement.setString(5, student.getMajor());
            statement.setLong(6, student.getId());

            int row = statement.executeUpdate();
            if (row > 0){
                log.info("Update thành công student : {}",student);
                return true;
            }
        } catch (SQLException e) {
            log.error("Đã có lỗi update tại {}", student,e);
            throw new ProgramException("Lỗi không thể update",e);
        }
        return false;
    }

    public boolean delete(long id){
        String sql = "DELETE FROM Students WHERE Id_student=?";
        try (Connection connection = DBConnector.getConnector();
        PreparedStatement statement = connection.prepareStatement(sql)){

            statement.setLong(1,id);
            int row = statement.executeUpdate();
            if (row > 0){
                log.info("Delete thành công student có Id = {}",id);
                return true;
            }

        } catch (SQLException e) {
            log.error("Có lỗi delete tại student có Id = {}",id,e);
            throw new ProgramException("Lỗi không thể delete",e);
        }
        return false;
    }

    public Optional<Student> findById(long id){
        String sql = "SELECT Id_student, FullName, Dob, Gpa, Sex, Major FROM Students WHERE Id_student = ?";
        try (Connection connection = DBConnector.getConnector();
        PreparedStatement statement = connection.prepareStatement(sql)){

            statement.setLong(1,id);
            try (ResultSet rs = statement.executeQuery()){
                if (rs.next()){
                    log.info("Đã tìm thấy student có Id = {}", id);
                    return Optional.of(mapResultStudent(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Có lỗi xảy ra khi tim student có Id = {}", id, e);
            throw new ProgramException("Lỗi khi tìm kiếm",e);
        }
        return Optional.empty();
    }

    //mapping dữ liệu
    private Student mapResultStudent (ResultSet rs) throws SQLException {
        return Student.builder()
                .id(rs.getLong("Id_student"))
                .name(rs.getString("FullName"))
                .dob(rs.getDate("Dob").toLocalDate())
                .gpa(rs.getDouble("Gpa"))
                .sex(Sex.valueOf(rs.getString("Sex")))
                .major(rs.getString("Major"))
                .build();
    }
}
