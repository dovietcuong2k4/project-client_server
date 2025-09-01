package org.client_server.client.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.client_server.client.ClientService;
import org.client_server.client.gui.controller.MainController;

public class ClientGUI extends Application {

    private static final String HOST = "localhost"; // đổi theo server của bạn
    private static final int PORT = 12345;          // đổi theo server của bạn

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load FXML Main
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main.fxml"));
            Parent root = loader.load();

            // Lấy controller và gắn ClientService
            MainController controller = loader.getController();
            ClientService clientService = new ClientService(HOST, PORT);
            controller.setClientService(clientService);

            // Scene + Stage
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Student Manager GUI");
            primaryStage.show();

            // Đóng ClientService khi tắt chương trình
            primaryStage.setOnCloseRequest(e -> {
                try {
                    clientService.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
