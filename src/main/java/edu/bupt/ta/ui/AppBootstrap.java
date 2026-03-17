package edu.bupt.ta.ui;

import edu.bupt.ta.controller.LoginController;
import edu.bupt.ta.controller.MainShellController;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

public class AppBootstrap {

    private final ServiceRegistry services = new ServiceRegistry();
    private final StackPane root = new StackPane();

    public Scene createInitialScene() {
        showLogin();
        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        return scene;
    }

    private void showLogin() {
        LoginController loginController = new LoginController(services, this::showMainShell);
        root.getChildren().setAll(loginController.getView());
    }

    private void showMainShell(User user) {
        MainShellController shellController = new MainShellController(services, user, () -> {
            services.authenticationService().logout();
            showLogin();
        });
        root.getChildren().setAll(shellController.getView());
    }
}
