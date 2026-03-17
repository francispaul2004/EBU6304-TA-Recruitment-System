package edu.bupt.ta.controller;

import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class ApplicantListController {

    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    private final ComboBox<Job> jobSelector = new ComboBox<>();
    private final TableView<Row> table = new TableView<>();

    public ApplicantListController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
        refreshJobs();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        Label heading = new Label("Applicant List");
        heading.setStyle("-fx-font-size: 30px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        jobSelector.setPromptText("Select your job");
        jobSelector.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Job item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getJobId() + " - " + item.getTitle());
            }
        });
        jobSelector.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Job item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getJobId() + " - " + item.getTitle());
            }
        });
        jobSelector.valueProperty().addListener((obs, oldV, newV) -> refreshApplications());

        Button review = new Button("Review Selected");
        review.getStyleClass().add("primary-button");
        review.setOnAction(event -> openReview());

        HBox top = new HBox(12, heading, jobSelector, review);
        top.setPadding(new Insets(20));

        TableColumn<Row, String> appCol = new TableColumn<>("Application");
        appCol.setCellValueFactory(new PropertyValueFactory<>("applicationId"));

        TableColumn<Row, String> applicantCol = new TableColumn<>("Applicant");
        applicantCol.setCellValueFactory(new PropertyValueFactory<>("applicantName"));

        TableColumn<Row, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Row, Integer> scoreCol = new TableColumn<>("Match Score");
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("matchScore"));

        table.getColumns().setAll(appCol, applicantCol, statusCol, scoreCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox container = new VBox(top, table);
        view.setCenter(container);
    }

    private void refreshJobs() {
        List<Job> jobs = services.jobService().getJobsByOrganiser(user.getUserId());
        jobSelector.setItems(FXCollections.observableArrayList(jobs));
        if (!jobs.isEmpty()) {
            jobSelector.setValue(jobs.get(0));
        }
    }

    private void refreshApplications() {
        Job selectedJob = jobSelector.getValue();
        if (selectedJob == null) {
            table.setItems(FXCollections.observableArrayList());
            return;
        }

        List<Application> apps = services.applicationService().getApplicationsByJob(selectedJob.getJobId());
        List<Row> rows = new ArrayList<>();
        for (Application app : apps) {
            String applicantName = services.applicantProfileRepository().findById(app.getApplicantId())
                    .map(profile -> profile.getFullName())
                    .orElse(app.getApplicantId());
            rows.add(new Row(app.getApplicationId(), applicantName, app.getStatus().name(), app.getMatchScore()));
        }
        table.setItems(FXCollections.observableArrayList(rows));
    }

    private void openReview() {
        Row row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            return;
        }

        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Applicant Review");
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.getDialogPane().setContent(new ApplicantReviewController(services, user, row.getApplicationId()).getView());
        dialog.showAndWait();
        refreshApplications();
    }

    public static class Row {
        private final String applicationId;
        private final String applicantName;
        private final String status;
        private final int matchScore;

        public Row(String applicationId, String applicantName, String status, int matchScore) {
            this.applicationId = applicationId;
            this.applicantName = applicantName;
            this.status = status;
            this.matchScore = matchScore;
        }

        public String getApplicationId() {
            return applicationId;
        }

        public String getApplicantName() {
            return applicantName;
        }

        public String getStatus() {
            return status;
        }

        public int getMatchScore() {
            return matchScore;
        }
    }
}
