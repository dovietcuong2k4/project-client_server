package org.client_server.client.gui.controller;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.client_server.client.ClientService;
import org.client_server.model.Sex;
import org.client_server.model.Student;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainController {

    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, Long> idColumn;
    @FXML private TableColumn<Student, String> nameColumn;
    @FXML private TableColumn<Student, String> dobColumn;
    @FXML private TableColumn<Student, Double> gpaColumn;
    @FXML private TableColumn<Student, Sex> sexColumn;
    @FXML private TableColumn<Student, String> majorColumn;

    @FXML private TextField searchField;

    private ObservableList<Student> studentList;
    private ClientService clientService;
    private final DateTimeFormatter dobFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setClientService(ClientService service) {
        this.clientService = service;
        initTable();
        refreshTable();
    }

    private void initTable() {
        studentList = FXCollections.observableArrayList();

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        dobColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getDob().format(dobFormatter)
                )
        );
        gpaColumn.setCellValueFactory(new PropertyValueFactory<>("gpa"));
        sexColumn.setCellValueFactory(new PropertyValueFactory<>("sex"));
        majorColumn.setCellValueFactory(new PropertyValueFactory<>("major"));

        studentTable.setItems(studentList);
    }

    @FXML
    private void handleRefresh() {
        refreshTable();
    }

    private void refreshTable() {
        try {
            JsonNode dataNode = clientService.list().path("data"); // Lấy "data" từ server
            if (!dataNode.isArray()) {
                studentList.clear(); // không có dữ liệu → xóa table
                return;
            }

            List<Student> list = new ArrayList<>();
            for (JsonNode node : dataNode) {
                try {
                    list.add(clientService.toStudent(node)); // parse node thành Student
                } catch (IOException e) {
                    e.printStackTrace(); // log lỗi riêng từng node
                }
            }

            studentList.setAll(list);

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Cannot fetch student list: " + e.getMessage());
        }
    }


    @FXML
    private void handleAdd() {
        openStudentForm(null);
    }

    @FXML
    private void handleUpdate() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a student to update!");
            return;
        }
        openStudentForm(selected);
    }

    @FXML
    private void handleDelete() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a student to delete!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete student " + selected.getName() + " ID "  + selected.getId() +"?", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                clientService.delete(selected.getId());
                showAlert(Alert.AlertType.INFORMATION, "Deleted", "Student deleted successfully!");
                refreshTable();
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Cannot delete student: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSearch() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            refreshTable();
            return;
        }
        try {
            long id = Long.parseLong(text);
            Student s = clientService.toStudent(clientService.find(id).path("data"));
            if (s != null) {
                studentList.setAll(s);
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Not Found", "No student with ID " + id);
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Warning", "ID must be a number!");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Cannot search student: " + e.getMessage());
        }
    }

    private void openStudentForm(Student student) {
        try {
            StudentFormController.showForm(student, clientService, v -> refreshTable());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Cannot open form: " + e.getMessage());
        }
    }


    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
