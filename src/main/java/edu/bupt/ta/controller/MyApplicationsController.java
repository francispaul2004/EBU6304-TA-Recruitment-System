package edu.bupt.ta.controller;

import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.model.Application;
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

public class MyApplicationsController {

    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final TableView<ApplicationRow> table = new TableView<>();

    public MyApplicationsController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
        refresh();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        Label heading = new Label("My Applications");
        heading.setStyle("-fx-font-size: 30px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        statusFilter.getItems().addAll("ALL", "SUBMITTED", "UNDER_REVIEW", "ACCEPTED", "REJECTED");
        statusFilter.setValue("ALL");

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(event -> refresh());

        HBox top = new HBox(12, heading, statusFilter, refresh);
        top.setPadding(new Insets(20));

        TableColumn<ApplicationRow, String> appId = new TableColumn<>("Application ID");
        appId.setCellValueFactory(new PropertyValueFactory<>("applicationId"));

        TableColumn<ApplicationRow, String> jobTitle = new TableColumn<>("Job");
        jobTitle.setCellValueFactory(new PropertyValueFactory<>("jobTitle"));

        TableColumn<ApplicationRow, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<ApplicationRow, String> date = new TableColumn<>("Applied At");
        date.setCellValueFactory(new PropertyValueFactory<>("applyDate"));

        TableColumn<ApplicationRow, Integer> score = new TableColumn<>("Match Score");
        score.setCellValueFactory(new PropertyValueFactory<>("matchScore"));

        table.getColumns().setAll(appId, jobTitle, status, date, score);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox container = new VBox(top, table);
        view.setCenter(container);
    }

    private void refresh() {
        String applicantId = services.applicantProfileService().getOrCreateProfile(user.getUserId()).getApplicantId();
        List<Application> applications = services.applicationService().getApplicationsByApplicant(applicantId);

        if (!"ALL".equals(statusFilter.getValue())) {
            ApplicationStatus status = ApplicationStatus.valueOf(statusFilter.getValue());
            applications = applications.stream().filter(a -> a.getStatus() == status).toList();
        }

        List<ApplicationRow> rows = new ArrayList<>();
        for (Application application : applications) {
            String title = services.jobService().getJob(application.getJobId())
                    .map(job -> job.getTitle())
                    .orElse(application.getJobId());
            rows.add(new ApplicationRow(
                    application.getApplicationId(),
                    title,
                    String.valueOf(application.getStatus()),
                    String.valueOf(application.getApplyDate()),
                    application.getMatchScore()
            ));
        }

        table.setItems(FXCollections.observableArrayList(rows));
    }

    public static class ApplicationRow {
        private final String applicationId;
        private final String jobTitle;
        private final String status;
        private final String applyDate;
        private final int matchScore;

        public ApplicationRow(String applicationId, String jobTitle, String status, String applyDate, int matchScore) {
            this.applicationId = applicationId;
            this.jobTitle = jobTitle;
            this.status = status;
            this.applyDate = applyDate;
            this.matchScore = matchScore;
        }

        public String getApplicationId() {
            return applicationId;
        }

        public String getJobTitle() {
            return jobTitle;
        }

        public String getStatus() {
            return status;
        }

        public String getApplyDate() {
            return applyDate;
        }

        public int getMatchScore() {
            return matchScore;
        }
    }
}
