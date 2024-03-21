package me.ex4ltado.jareditor.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class JarEditorApp extends Application {

    private static final String TITLE = "JarEditor";

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/views/FXMLJarEditor.fxml")));
        root.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/darcula.css")).toExternalForm());
        primaryStage.setTitle(TITLE);
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

}
