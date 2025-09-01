package org.client_server.client.gui.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.client_server.client.ClientService;
import org.client_server.model.Sex;
import org.client_server.model.Student;

import java.io.IOException;
import java.util.function.Consumer;

public class StudentFormController {

    @FXML private TextField nameField;
    @FXML private DatePicker dobPicker;
    @FXML private TextField gpaField;
    @FXML private ComboBox<String> sexComboBox;
    @FXML private TextField majorField;

    private Stage stage;
    private Student student;
    private boolean isUpdate;
    private ClientService clientService;
    private Consumer<Void> onSaveCallback;

    public static void showForm(Student student, ClientService clientService, Consumer<Void> refreshCallback) throws IOException {
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(StudentFormController.class.getResource("/view/studentForm.fxml"));
        javafx.scene.Parent root = loader.load();
        StudentFormController controller = loader.getController();
        controller.init(student, clientService, refreshCallback);

        Stage stage = new Stage();
        stage.setScene(new javafx.scene.Scene(root));
        stage.setTitle(student == null ? "Add Student" : "Update Student");
        stage.initModality(Modality.APPLICATION_MODAL);
        controller.stage = stage;
        stage.showAndWait();
    }

    public void init(Student student, ClientService clientService, Consumer<Void> refreshCallback) {
        this.student = student;
        this.clientService = clientService;
        this.onSaveCallback = refreshCallback;
        this.isUpdate = student != null;

        sexComboBox.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));

        if (isUpdate) {
            // fill existing values
            nameField.setText(student.getName());
            dobPicker.setValue(student.getDob());
            gpaField.setText(String.format("%.2f", student.getGpa()));
            sexComboBox.setValue(switch (student.getSex()) {
                case MALE -> "Male";
                case FEMALE -> "Female";
                case OTHER -> "Other";
            });
            majorField.setText(student.getMajor());
        } else {
            sexComboBox.setValue("Male");
        }
    }

    @FXML
    private void handleSave() {
        try {
            String name = nameField.getText().trim();
            if (name.isEmpty()) throw new IllegalArgumentException("Name cannot be empty");

            if (dobPicker.getValue() == null) throw new IllegalArgumentException("DOB cannot be empty");

            double gpa;
            try {
                gpa = Double.parseDouble(gpaField.getText().trim());
                if (gpa < 0 || gpa > 4.0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("GPA must be between 0.0 and 4.0");
            }

            String sexText = sexComboBox.getValue();
            Sex sex = switch (sexText) {
                case "Male" -> Sex.MALE;
                case "Female" -> Sex.FEMALE;
                case "Other" -> Sex.OTHER;
                default -> throw new IllegalArgumentException("Invalid sex");
            };

            String major = majorField.getText().trim();
            if (major.isEmpty()) throw new IllegalArgumentException("Major cannot be empty");

            Student s = isUpdate ? student : new Student();
            s.setName(name);
            s.setDob(dobPicker.getValue());
            s.setGpa(gpa);
            s.setSex(sex);
            s.setMajor(major);

            if (isUpdate) clientService.update(s);
            else clientService.insert(s);

            onSaveCallback.accept(null); // refresh main table
            stage.close();
        } catch (IllegalArgumentException | IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }

    @FXML
    private void handleCancel() {
        stage.close();
    }
}
