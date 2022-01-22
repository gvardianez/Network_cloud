package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent parent = FXMLLoader.load(getClass().getResource("/client.fxml"));
        primaryStage.setScene(new Scene(parent, 665, 610));
        primaryStage.setResizable(false);
        primaryStage.show();
        primaryStage.setTitle("Network Cloud");
        primaryStage.setOnCloseRequest(windowEvent -> System.exit(0));
    }
}