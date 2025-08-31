package org.client_server.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.client_server.exception.ProgramException;
import org.client_server.model.Sex;
import org.client_server.model.Student;


@Slf4j
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final StudentRepository studentRepository = new StudentRepository();//có thể viết vào constructor thay vì trực tiếp
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        // có thể thay đổi viết repository vào constructor để dễ test với db ảo
    }

    @Override
    public void run() {
        String clientAddress = String.valueOf(clientSocket.getRemoteSocketAddress());
        Thread.currentThread().setName("client - " + clientAddress);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8))) {

            log.info("Handler bắt đầu cho {}", clientAddress);

            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    ObjectNode request = (ObjectNode) mapper.readTree(line);
                    String action = request.path("action").asText("");

                    switch (action == null ? "" : action.toUpperCase().trim()) {
                        case "INSERT":
                            handleInsert(request, writer);
                            break;
                        case "LIST":
                            handleList(writer);
                            break;
                        case "UPDATE":
                            handleUpdate(request, writer);
                            break;
                        case "DELETE":
                            handleDelete(request, writer);
                            break;
                        case "FIND":
                            handleFind(request, writer);
                            break;
                        case "QUIT":
                            handlQuit(writer, clientAddress);
                            return;
                        default:
                            sendError(writer,"UNKNOWN_ACTION","Chỉ hỗ trợ: INSERT,FIND,LIST,UPDATE,DELETE,QUIT");
                    }
                } catch (Exception e) {
                    if (e instanceof SocketTimeoutException) {
                        log.warn("Client {} không có dữ liệu phản hồi, đã vượt timeout ", clientAddress);
                        sendError(writer,"TIMEOUT","Không có data phản hồi quá thời gian quy định");

                        break;
                    } else {
                        log.error("Có lỗi trong yêu cầu từ client {}", clientAddress, e);
                        sendError(writer,"SERVER_ERROR",e.getMessage());
                        break;
                    }
                }

            }
        } catch (IOException e) {
            log.error("I/O của client {} có lỗi ", clientAddress);
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                log.warn("Lỗi khi đóng sockket ", e);
            }
            log.info("Đã đóng thành công handler client {}", clientAddress);
        }
    }

    private void handleInsert(ObjectNode request, BufferedWriter writer) throws IOException {
        if (!validatePayload(request.path("payload"),writer,"INSERT")) return;
        ObjectNode payload = (ObjectNode) request.path("payload");

        Student student = mappingPayloadToStudent(payload);
        if (student.getName() == null || student.getName().isBlank()){
            sendError(writer,"INVALID_NAME","Name không thể để trống");
            return;
        }
        if (student.getDob() == null){
            sendError(writer,"INVALID_DOB","Dob không để trống và chỉ được để ở dạng YYYY-MM-DD");
            return;
        }
        if (student.getGpa() == -1 ){
            sendError(writer,"INVALID_GPA","Gpa không để trống và chỉ nằm trong 0 và 4");
            return;
        }
        if (student.getSex() == null){
            sendError(writer,"INVALID_SEX","Sex không để trống và phải điền đúng yêu cầu");
            return;
        }
        if (student.getMajor() == null || student.getMajor().isBlank()){
            sendError(writer,"INVALID_MAJOR","Major không thể để trống");
            return;
        }

        try {
            long id = studentRepository.insert(student);
            student.setId(id);
            sendSuccess(writer, "Student " + id +" được insert thành công");
            log.info("Inserted Student {}", student);
        } catch (ProgramException pe) {
            sendError(writer, "DB_ERROR", pe.getMessage());
            log.error("Lỗi khi insert", pe);
        }
    }

    private void handleFind(ObjectNode request, BufferedWriter writer) throws IOException {
        //không nên ép kiểu trực tiếp bởi vì nếu ép kiểu objectnode cua missingnode sẽ ném ra ClassCastException
        if (!validatePayload(request.path("payload"),writer,"FIND")) return;
        ObjectNode payload = (ObjectNode) request.path("payload");

        try {
            Optional<Student> optionalStudent = idStudentExistsed(payload,writer);
            if (optionalStudent.isPresent()) {
                Student student = optionalStudent.get();
                ObjectNode response = mapper.createObjectNode();

                response.put("status", "OK");
                response.set("data", mapper.valueToTree(student));

                writer.write(mapper.writeValueAsString(response));
                writer.write("\n");
                writer.flush();
            }
        } catch (ProgramException e){
            sendError(writer,"DB_ERROR", e.getMessage());
        }
    }

    private void handleList(BufferedWriter writer) throws IOException{
        try{
            List<Student> studentList = studentRepository.findAll();
            ObjectNode response = mapper.createObjectNode();
            response.put("status", "OK");
            response.set("data",mapper.valueToTree(studentList));//cần đăng kí java 8 Date/Time để k bị lỗi khi chuyển đổi
            writer.write(mapper.writeValueAsString(response));//viết response thành dạng json string
            writer.write("\n");
            writer.flush();
            log.info("Gửi {} students", studentList.size());

        } catch (ProgramException e) {
            sendError(writer,"DB_ERROR", e.getMessage());
        }
    }

    private void handleUpdate(ObjectNode request, BufferedWriter writer) throws IOException {
        if (!validatePayload(request.path("payload"),writer,"UPDATE")) return;
        ObjectNode payload = (ObjectNode) request.path("payload");

        try {
            Optional<Student> optionalStudentById = idStudentExistsed(payload,writer);
            if (optionalStudentById.isPresent()){
                Student dbStudent = optionalStudentById.get();

                Student studentPayload = mappingPayloadToStudent(payload);
                if (studentPayload.getName() != null && !studentPayload.getName().isBlank()){
                    dbStudent.setName(studentPayload.getName());
                }
                if (studentPayload.getDob() != null){
                    dbStudent.setDob(studentPayload.getDob());
                }
                if (studentPayload.getGpa() != -1 ){
                    dbStudent.setGpa(studentPayload.getGpa());
                }
                if (studentPayload.getSex() != null){
                    dbStudent.setSex(studentPayload.getSex());
                }
                if (studentPayload.getMajor() != null && !studentPayload.getMajor().isBlank()){
                    dbStudent.setMajor(studentPayload.getMajor());
                }

                if (studentRepository.update(dbStudent)){
                    sendSuccess(writer, "Student id : " + dbStudent.getId()
                            +" và name : " + dbStudent.getName()+ " đã được update");
                    log.info("Đã update thành công student {}", dbStudent);
                } else {
                    sendError(writer, "UPDATE_FAIL", "Không thể update");
                    log.error("Đã tìm thấy id, id đã tồn tại nhưng không thể update student id = {}", dbStudent.getId());
                }
            }
        } catch (ProgramException e){
            sendError(writer,"DB_ERROR",e.getMessage());
            log.error("Không thể cập nhật, có lỗi cơ sở dữ liệu",e);
        }
    }

    private void handleDelete(ObjectNode request, BufferedWriter writer) throws IOException {
        if(!validatePayload(request.path("payload"),writer,"DELETE")) return;
        ObjectNode payload = (ObjectNode) request.path("payload");

        try {
            if (idStudentExistsed(payload, writer).isPresent()){
                long idExisted = payload.path("id").asLong();
                if(studentRepository.delete(idExisted)) {
                    sendSuccess(writer, "Student id : " + idExisted + " đã được xóa");
                    log.info("Đã xóa thành công student có id = {}", idExisted);
                } else {
                    sendError(writer, "DELETE_FAIL", "Không thể xóa");
                    log.error("Đã tìm thấy id, id đã tồn tại nhưng không thể xóa student id = {}", idExisted);
                }
            }
        } catch (ProgramException e){
            sendError(writer,"DB_ERROR",e.getMessage());
            log.error("Không thể xóa, có lỗi cơ sở dữ liệu",e);
        }

    }

    private void handlQuit(BufferedWriter writer, String clientAddress) throws IOException {
        sendSuccess(writer,"Client " + clientAddress + " đã đóng");
        log.info("Client {} yêu cầu đã từ bỏ", clientAddress);
    }

    private boolean validatePayload(JsonNode payload, BufferedWriter writer, String action) throws  IOException{
        if (payload.isMissingNode() || payload.isEmpty()){
            sendError(writer, action.toUpperCase(), "Payload phải có để yêu cầu " + action.toUpperCase());
            return false;
        }
        return true;
    }


    private Student mappingPayloadToStudent(ObjectNode payload){
        //validate từng trường thông tin
        String name = payload.path("name").asText(null);
        String dobStr = payload.path("dob").asText(null);
        double gpa = payload.path("gpa").asDouble(-1);
        String sexStr = payload.path("sex").asText(null);
        String major = payload.path("major").asText(null);


        LocalDate dob = null;
        if (dobStr != null){ // cần check null nếu không khi parse sẽ ném ra NullPointException
            try {
                dob = LocalDate.parse(dobStr);
            } catch (DateTimeParseException e){
            }
        }
        if (gpa < 0 || gpa > 4 ){
            gpa = -1;
        }

        Sex sex = null;
        if (sexStr != null){
            try {
                sex = Sex.valueOf(sexStr.toUpperCase());
            } catch (IllegalArgumentException e ) {
            }
        }


         return Student.builder()
                .name(name)
                .dob(dob)
                .gpa(gpa)
                .sex(sex)
                .major(major)
                .build();

    }

    private Optional<Student> idStudentExistsed(ObjectNode payload, BufferedWriter writer) throws IOException {
        long id_student_exist = payload.path("id").asLong(-1);
        if (id_student_exist < 0){
            sendError(writer, "INVALID_ID","Id hợp lệ phải là số nguyên dương");
            return Optional.empty();
        }
        try {
            Optional<Student> optionalStudent = studentRepository.findById(id_student_exist);
            if (optionalStudent.isPresent()){
                return optionalStudent;
            }
            sendError(writer,"ID_NOT_EXIST","Id không tồn tại");
        } catch (ProgramException e){
            sendError(writer,"DB_ERROR", e.getMessage());
        }
        return Optional.empty();
    }

    private void sendSuccess(BufferedWriter writer, String message) throws IOException{
        ObjectNode response = mapper.createObjectNode();
        response.put("status","OK");
        response.put("message", message);
        writer.write(mapper.writeValueAsString(response));
        writer.write("\n");
        writer.flush();
    }

    private void sendError(BufferedWriter writer, String code, String message) throws IOException{
        ObjectNode response = mapper.createObjectNode();
        response.put("status", "ERROR");
        response.put("code", code);
        response.put("message",message);
        writer.write(mapper.writeValueAsString(response));
        writer.write("\n");
        writer.flush();
    }
}
