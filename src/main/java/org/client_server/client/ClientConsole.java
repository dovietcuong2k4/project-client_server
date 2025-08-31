package org.client_server.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.client_server.model.Sex;
import org.client_server.model.Student;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class ClientConsole {
    private final String HOST;
    private final int PORT;

    public ClientConsole(String host, int port) {
        HOST = host;
        PORT = port;
    }

    public void start(){
        try (ClientService service = new ClientService(HOST,PORT);
        Scanner scanner = new Scanner(System.in)){
            System.out.println("Kết nối thành công vào server " + HOST + " : " + PORT);

            while (true){
                System.out.println("Vui lòng nhập các từ khóa để thực hiện chức năng tương ứng :"
                        + "\nINSERT : Ghi thêm sinh viên vào dữ liệu"
                        + "\nFIND : Tìm kiếm và hiển thị một sinh viên"
                        + "\nLIST : Hiển thị danh sách tất cả các sinh viên"
                        + "\nQUIT : Thoát khỏi chương trình"
                        + "\nHiện tại chương trình chỉ hỗ trợ các chức năng trên.");

                String command = scanner.nextLine().trim().toUpperCase();

                switch (command){
                    case "INSERT":
                        Student student = inputStudent(scanner);
                        printResponse(service.insert(student));
                        break;
                    case "FIND":
                        long id = inputId(scanner);
                        printResponse(service.find(id));
                        deleteOrUpdate(service,scanner, id);
                        break;
                    case "LIST":
                        printResponse(service.list());
                        break;
                    case "QUIT":
                        printResponse(service.quit());
                        return;
                    default:
                        System.out.println("Từ khóa không đúng. Vui lòng nhập lại \n");
                }
            }

        } catch (IOException e) {
            System.err.println("Lỗi không thể kết nối vào máy chủ - server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteOrUpdate(ClientService service, Scanner scanner, long id) throws IOException {
        while (true){
            System.out.println("UPDATE: Cập nhật thông tin sinh viên này"
                    + "\nDELETE: Xóa sinh viên này"
                    + "\nCANCEL: Trở về");
            String command = scanner.nextLine().trim().toUpperCase();
            switch (command) {
                case "DELETE":
                    printResponse(service.delete(id));
                    return;
                case "UPDATE":
                    Student student = inputUpdateStudent(scanner, id);
                    printResponse(service.update(student));
                    return;
                case "CANCEL":
                    return;
                default:
                    System.out.println("Từ khóa không đúng. Vui lòng nhập lại \n");
            }
        }
    }


    private void printResponse(ObjectNode response) throws IOException {
        String status = response.path("status").asText();

        if ("OK".equals(status)){
            if(response.has("message")){
                System.out.println("[SUCCESS] " + response.path("message").asText());
            }

            if (response.has("data")) {
                if (response.path("data").isArray()) {
                    System.out.println("[LIST]");
                    for (JsonNode node : response.path("data")) {
                        System.out.println(" - " + node);
                    }
                } else {
                    System.out.println("[STUDENT]");
                    System.out.println(response.path("data"));
                }
            }
        }

        else if ("ERROR".equals(status)){
            System.out.println("[ERROR] code = " + response.path("code").asText()
            + ", message = " + response.path("message").asText());
        }

        else {
            System.out.println("[UNKNOWN RESPONSE] Không có phản hồi: " + response.toPrettyString());
        }

    }

    private Student inputUpdateStudent(Scanner scanner, long id) throws IOException {
        System.out.println("Các trường không muốn thấy đổi thì ĐỂ TRỐNG");

        System.out.println("Tên đầy đủ:");
        String name = scanner.nextLine().trim();
        name = name.isBlank() ? null : name;

        System.out.println("Ngày sinh (YYYY-MM-DD) : ");
        String dobStr = scanner.nextLine().trim();
        LocalDate dob = parseDob(dobStr);

        System.out.println("Gpa (0.0 - 4.0) : ");
        String gpaStr = scanner.nextLine().trim();
        double gpa = parseGpa(gpaStr);

        System.out.println("Giới tính (MALE/FEMALE/OTHER) : ");
        String sexStr = scanner.nextLine().trim();
        Sex sex = parseSex(sexStr);

        System.out.println("Ngành học:");
        String major = scanner.nextLine().trim();
        major = major.isBlank() ? null : major;

        return Student.builder()
                .id(id)
                .name(name)
                .dob(dob)
                .gpa(gpa)
                .sex(sex)
                .major(major)
                .build();

    }

    // ==== Helper  =======
    private LocalDate parseDob(String s) {
        if (s.isBlank()) return null;
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e){
            return null;
        }
    }

    private double parseGpa(String s){
        if (s.isBlank()) return -1;
        try {
            double gpa = Double.parseDouble(s);
            return (gpa < 0 || gpa > 4) ? -1 : gpa;
        } catch (NumberFormatException e){
            return -1;
        }
    }

    private Sex parseSex(String s){
        if (s.isBlank()) return null;
        try {
            return Sex.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e){
            return null;
        }
    }

    private long inputId(Scanner scanner) {
        System.out.println("Nhập id của sinh viên mong muốn: ");

        long id;
        while (true){
            try {
                id = Long.parseLong(scanner.nextLine().trim());
                if (id < 0){
                    throw new IllegalStateException();
                }
                break;
            } catch (Exception e) {
                System.out.println("Id hợp lệ phải là số nguyên dương. Vui lòng nhập lại");
            }
        }
        return id;
    }

    private Student inputStudent(Scanner scanner) {
        System.out.println("Nhập các trường thông tin sau: ");

        String name;
        while (true){
            System.out.println("Tên đầy đủ: ");
            name = scanner.nextLine().trim();
            if(!name.isBlank())
                break;
            System.out.println("Tên không được để trống. Vui lòng nhập lại");
        }

        LocalDate dob;
        while (true){
            System.out.println("Ngày sinh (YYYY-MM-DD) : ");
            String dobStr = scanner.nextLine().trim();
            try {
                dob = LocalDate.parse(dobStr);
                break;
            } catch (Exception e){
                System.out.println("Ngày sinh không hợp lệ. Vui lòng nhập lại");
            }
        }

        double gpa;
        while (true){
            System.out.println("Gpa (0.0 - 4.0) : ");
            String gpaStr = scanner.nextLine().trim().replace(',','.');
            try {
                gpa = Double.parseDouble(gpaStr);
                if (gpa >= 0 && gpa <= 4)
                    break;
                System.out.println("GPA phải hợp lệ từ 0.0 - 4.0 .");
            } catch (Exception e){
                System.out.println("GPA không hợp lệ. Vui lòng nhập lại");
            }
        }

        Sex sex;
        while (true){
            System.out.println("Giới tính (MALE/FEMALE/OTHER) : ");
            String sexStr = scanner.nextLine().trim().toUpperCase();
            try {
                sex = Sex.valueOf(sexStr);
                break;
            } catch (Exception e){
                System.out.println("Giới tính chỉ được nhập theo yêu cầu. Vui lòng nhập lại");
            }
        }

        String major;
        while (true){
            System.out.println("Ngành học: ");
            major = scanner.nextLine().trim();
            if (!major.isBlank())
                break;
            System.out.println("Major không được để trống. Vui lòng nhập lại");
        }

        return Student.builder()
                .name(name)
                .dob(dob)
                .gpa(gpa)
                .sex(sex)
                .major(major)
                .build();
    }

    public static void main(String[] args) {
        ClientConsole client = new ClientConsole("localhost",12345);
        client.start();
    }
}
