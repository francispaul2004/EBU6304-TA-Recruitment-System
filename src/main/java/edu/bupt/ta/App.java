package edu.bupt.ta;

import edu.bupt.ta.ui.AppBootstrap;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        AppBootstrap bootstrap = new AppBootstrap();
        Scene scene = bootstrap.createInitialScene();
        primaryStage.setTitle("BUPT IS TA Recruitment System");
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(760);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
