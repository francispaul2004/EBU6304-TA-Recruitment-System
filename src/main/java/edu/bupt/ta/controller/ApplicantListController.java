package edu.bupt.ta.controller;

import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class ApplicantListController {

    private final ServiceRegistry services;
    private final User user;
    private final String preferredJobId;
    private final BorderPane view = new BorderPane();

    private final ComboBox<Job> jobSelector = new ComboBox<>();
    private final ListView<Row> listView = new ListView<>();

    private final Label detailSkills = new Label("-");
    private final Label detailAcademic = new Label("-");
    private Row selectedRow;

    public ApplicantListController(ServiceRegistry services, User user) {
        this(services, user, null);
    }

    public ApplicantListController(ServiceRegistry services, User user, String preferredJobId) {
        this.services = services;
        this.user = user;
        this.preferredJobId = preferredJobId;
        initialize();
        refreshJobs();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");

        VBox page = new VBox(16);
        page.setPadding(new Insets(24));

        page.getChildren().add(buildHeader());
        page.getChildren().add(buildBody());

        view.setCenter(page);
    }

    private HBox buildHeader() {
        Label title = new Label("Applicant List");
        title.getStyleClass().add("page-title");

        jobSelector.setPrefWidth(320);
        jobSelector.setPromptText("Select your job");
        jobSelector.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Job item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
        jobSelector.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Job item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
        jobSelector.valueProperty().addListener((obs, oldV, newV) -> refreshApplications());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button filter = new Button("Filter");
        filter.getStyleClass().add("secondary-button");

        Button export = new Button("Export Report");
        export.getStyleClass().add("secondary-button");

        HBox row = new HBox(10, title, jobSelector, spacer, filter, export);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildBody() {
        VBox left = new VBox(10);
        left.getStyleClass().add("panel-card");
        left.setPadding(new Insets(12));

        listView.setCellFactory(param -> new ApplicantRowCell());
        listView.setPrefHeight(700);
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateDetail(newV));
        VBox.setVgrow(listView, Priority.ALWAYS);

        left.getChildren().add(listView);

        VBox right = new VBox(12);
        right.getStyleClass().add("panel-card");
        right.setPadding(new Insets(16));
        right.setPrefWidth(360);

        Label title = new Label("Applicant Details");
        title.setStyle("-fx-font-size: 34px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Button accept = new Button("Accept");
        accept.getStyleClass().add("primary-button");
        accept.setMaxWidth(Double.MAX_VALUE);
        accept.setOnAction(event -> openReview());

        Button viewFull = new Button("View Full Application");
        viewFull.getStyleClass().add("secondary-button");
        viewFull.setMaxWidth(Double.MAX_VALUE);
        viewFull.setOnAction(event -> openReview());

        right.getChildren().addAll(
                title,
                detailField("Skills Overlap", detailSkills),
                detailField("Academic Record", detailAcademic),
                accept,
                viewFull
        );

        HBox body = new HBox(16, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        return body;
    }

    private VBox detailField(String title, Label value) {
        VBox box = new VBox(4);
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
        value.setWrapText(true);
        value.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
        box.getChildren().addAll(label, value);
        return box;
    }

    private void refreshJobs() {
        List<Job> jobs = services.jobService().getJobsByOrganiser(user.getUserId());
        jobSelector.setItems(FXCollections.observableArrayList(jobs));
        if (jobs.isEmpty()) {
            return;
        }

        if (preferredJobId != null) {
            for (Job job : jobs) {
                if (preferredJobId.equals(job.getJobId())) {
                    jobSelector.setValue(job);
                    return;
                }
            }
        }

        jobSelector.setValue(jobs.get(0));
    }

    private void refreshApplications() {
        Job selectedJob = jobSelector.getValue();
        if (selectedJob == null) {
            listView.setItems(FXCollections.observableArrayList());
            updateDetail(null);
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

        listView.setItems(FXCollections.observableArrayList(rows));
        if (!rows.isEmpty()) {
            listView.getSelectionModel().selectFirst();
        } else {
            updateDetail(null);
        }
    }

    private void updateDetail(Row row) {
        selectedRow = row;
        if (row == null) {
            detailSkills.setText("-");
            detailAcademic.setText("-");
            return;
        }

        detailSkills.setText("Match score " + row.matchScore + "% based on skills and availability.");
        detailAcademic.setText("Applicant: " + row.applicantName + "\nCurrent status: " + row.status);
    }

    private void openReview() {
        if (selectedRow == null) {
            DialogControllerFactory.validationError("Please select one applicant first.",
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (view.getScene() != null) {
            stage.initOwner(view.getScene().getWindow());
        }
        stage.setTitle("Applicant Review");

        Parent reviewView = new ApplicantReviewController(services, user, selectedRow.applicationId).getView();
        Scene scene = new Scene(reviewView, 920, 760);
        if (ApplicantListController.class.getResource("/styles/app.css") != null) {
            String stylesheet = ApplicantListController.class.getResource("/styles/app.css").toExternalForm();
            scene.getStylesheets().add(stylesheet);
        }
        stage.setScene(scene);
        stage.showAndWait();
        refreshApplications();
    }

    private static class ApplicantRowCell extends ListCell<Row> {
        @Override
        protected void updateItem(Row item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            VBox row = new VBox(6);
            row.setPadding(new Insets(12));
            row.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-background-radius: 10;");

            HBox top = new HBox();
            Label name = new Label(item.applicantName);
            name.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label status = new Label(item.status.replace('_', ' '));
            status.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #1d4ed8; -fx-background-color: #eff6ff; -fx-background-radius: 999; -fx-padding: 2 8 2 8;");

            top.getChildren().addAll(name, spacer, status);

            Label meta = new Label("Application " + item.applicationId + "   |   Score " + item.matchScore + "%");
            meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            row.getChildren().addAll(top, meta);
            setGraphic(row);
        }
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
