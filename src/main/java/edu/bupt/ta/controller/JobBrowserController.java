package edu.bupt.ta.controller;

import edu.bupt.ta.dto.JobSearchCriteria;
import edu.bupt.ta.dto.MatchExplanationDTO;
import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.ValidationResult;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

public class JobBrowserController {

    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    private final TextField keywordField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final TableView<Job> jobTable = new TableView<>();
    private final JobDetailController jobDetailController = new JobDetailController();

    public JobBrowserController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
        loadJobs();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.setTop(buildFilters());

        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.65);
        splitPane.getItems().addAll(buildTableArea(), jobDetailController.getView());
        view.setCenter(splitPane);

        jobTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            jobDetailController.setJob(newValue);
            updateMatchExplanation(newValue);
        });

        jobDetailController.setOnApply(statement -> {
            Job selected = jobTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            applyToJob(selected, statement);
        });

        if (user.getRole() != Role.TA) {
            jobDetailController.setOnApply(statement -> {
            });
            jobDetailController.setMatchExplanation(null);
        }
    }

    private Parent buildFilters() {
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(20));
        wrapper.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

        Label heading = new Label("Browse Opportunities");
        heading.setStyle("-fx-font-size: 30px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        keywordField.setPromptText("Search by keyword (module, skill, title)");
        keywordField.setPrefHeight(40);
        keywordField.setPrefWidth(560);

        statusFilter.getItems().addAll("ALL", "OPEN", "CLOSED", "EXPIRED", "DRAFT");
        statusFilter.setValue("ALL");

        Button searchButton = new Button("SEARCH");
        searchButton.getStyleClass().add("primary-button");
        searchButton.setOnAction(event -> loadJobs());

        HBox row = new HBox(12, keywordField, statusFilter, searchButton);
        wrapper.getChildren().addAll(heading, row);
        return wrapper;
    }

    private Parent buildTableArea() {
        TableColumn<Job, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(280);

        TableColumn<Job, String> moduleCol = new TableColumn<>("Module");
        moduleCol.setCellValueFactory(new PropertyValueFactory<>("moduleCode"));
        moduleCol.setPrefWidth(120);

        TableColumn<Job, Integer> hoursCol = new TableColumn<>("Hours/Week");
        hoursCol.setCellValueFactory(new PropertyValueFactory<>("weeklyHours"));
        hoursCol.setPrefWidth(100);

        TableColumn<Job, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(110);

        TableColumn<Job, String> deadlineCol = new TableColumn<>("Deadline");
        deadlineCol.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        deadlineCol.setPrefWidth(150);

        jobTable.getColumns().setAll(titleCol, moduleCol, hoursCol, statusCol, deadlineCol);
        jobTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox box = new VBox(jobTable);
        box.setPadding(new Insets(16));
        return box;
    }

    private void loadJobs() {
        JobSearchCriteria criteria = new JobSearchCriteria();
        criteria.setKeyword(keywordField.getText());
        if (!"ALL".equals(statusFilter.getValue())) {
            criteria.setStatus(Enum.valueOf(edu.bupt.ta.enums.JobStatus.class, statusFilter.getValue()));
        }

        List<Job> jobs = services.jobService().searchJobs(criteria);
        if (user.getRole() == Role.MO) {
            jobs = jobs.stream().filter(j -> user.getUserId().equals(j.getOrganiserId())).toList();
        }

        jobTable.setItems(FXCollections.observableArrayList(jobs));
        if (!jobs.isEmpty()) {
            jobTable.getSelectionModel().selectFirst();
        } else {
            jobDetailController.setJob(null);
            jobDetailController.setMatchExplanation(null);
        }
    }

    private void applyToJob(Job job, String statement) {
        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        if (applicantIdOpt.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "Profile not found for current TA account. Complete profile in Phase 3.");
            alert.setHeaderText("Cannot apply");
            alert.showAndWait();
            return;
        }

        ValidationResult result = services.applicationService().apply(applicantIdOpt.get(), job.getJobId(), statement);
        if (!result.isValid()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, String.join("\n", result.getErrors()));
            alert.setHeaderText("Apply failed");
            alert.showAndWait();
            return;
        }

        Alert ok = new Alert(Alert.AlertType.INFORMATION, "Application submitted successfully.");
        ok.setHeaderText("Apply success");
        ok.showAndWait();
        loadJobs();
    }

    private void updateMatchExplanation(Job selectedJob) {
        if (selectedJob == null || user.getRole() != Role.TA) {
            jobDetailController.setMatchExplanation(null);
            return;
        }

        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());
        if (applicantIdOpt.isEmpty()) {
            jobDetailController.setMatchExplanation(null);
            return;
        }

        MatchExplanationDTO explanation = services.matchingService().evaluateMatch(applicantIdOpt.get(), selectedJob.getJobId());
        jobDetailController.setMatchExplanation(explanation);
    }
}
