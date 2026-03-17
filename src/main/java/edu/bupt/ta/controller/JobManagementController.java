package edu.bupt.ta.controller;

import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.ValidationResult;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class JobManagementController {

    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();
    private final TableView<Job> table = new TableView<>();

    public JobManagementController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
        refresh();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        TableColumn<Job, String> idCol = new TableColumn<>("Job ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("jobId"));

        TableColumn<Job, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Job, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Job, Integer> positionsCol = new TableColumn<>("Positions");
        positionsCol.setCellValueFactory(new PropertyValueFactory<>("positions"));

        table.getColumns().setAll(idCol, titleCol, statusCol, positionsCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button create = new Button("New");
        create.getStyleClass().add("primary-button");
        create.setOnAction(event -> onCreate());

        Button edit = new Button("Edit");
        edit.getStyleClass().add("secondary-button");
        edit.setOnAction(event -> onEdit());

        Button close = new Button("Close Job");
        close.getStyleClass().add("secondary-button");
        close.setOnAction(event -> onClose());

        Button reload = new Button("Refresh");
        reload.getStyleClass().add("secondary-button");
        reload.setOnAction(event -> refresh());

        HBox actions = new HBox(8, create, edit, close, reload);
        VBox center = new VBox(12, actions, table);
        center.setPadding(new Insets(20));

        view.setCenter(center);
    }

    private void onCreate() {
        JobEditorController editor = new JobEditorController();
        editor.show(null, user.getUserId()).ifPresent(job -> {
            ValidationResult result = services.jobService().createJob(job);
            if (!result.isValid()) {
                showError(String.join("\n", result.getErrors()));
                return;
            }
            refresh();
        });
    }

    private void onEdit() {
        Job selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select one job first.");
            return;
        }

        JobEditorController editor = new JobEditorController();
        editor.show(selected, user.getUserId()).ifPresent(job -> {
            ValidationResult result = services.jobService().updateJob(job);
            if (!result.isValid()) {
                showError(String.join("\n", result.getErrors()));
                return;
            }
            refresh();
        });
    }

    private void onClose() {
        Job selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select one job first.");
            return;
        }
        services.jobService().closeJob(selected.getJobId(), user.getUserId());
        refresh();
    }

    private void refresh() {
        table.setItems(FXCollections.observableArrayList(services.jobService().getJobsByOrganiser(user.getUserId())));
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Validation error");
        alert.showAndWait();
    }
}
