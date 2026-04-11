package edu.bupt.ta.controller;

import edu.bupt.ta.dto.ApplicantReviewDTO;
import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.DisplayPlaceholders;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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
    private final HBox jobModeTabs = new HBox(14);
    private final HBox tabBar = new HBox(10);
    private final Label goalCount = new Label("0 / 0");
    private final StackPane goalProgress = new StackPane();

    private final VBox leftContent = new VBox(14);
    private final VBox emptyState = new VBox(14);

    private final Label detailName = new Label("Applicant Details");
    private final Label detailMeta = new Label("Select an applicant to review key information.");
    private final VBox skillsBox = new VBox(10);
    private final VBox academicBox = new VBox(10);
    private final VBox insightBox = new VBox(10);
    private final Button acceptButton = new Button("Accept");
    private final Button viewFullButton = new Button("View Full Application");

    private Row selectedRow;
    private String activeTab = "ALL";

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
        page.getChildren().addAll(buildHeader(), buildBody());

        view.setCenter(page);
    }

    private VBox buildHeader() {
        VBox shell = new VBox(12);

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBlock = new VBox(4);
        Label title = new Label("Applicant List");
        title.getStyleClass().add("page-title");

        Label caption = new Label("RECRUITMENT GOAL");
        caption.getStyleClass().add("tiny-kicker");

        RectangleBar bar = new RectangleBar(170, 6);
        goalProgress.getChildren().add(bar.track());
        goalProgress.setAlignment(Pos.CENTER_LEFT);
        goalProgress.setMinWidth(170);

        HBox goalRow = new HBox(10, caption, goalProgress, goalCount);
        goalRow.setAlignment(Pos.CENTER_LEFT);
        titleBlock.getChildren().addAll(title, goalRow);

        jobSelector.setPrefWidth(260);
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
        filter.setOnAction(event -> cycleTab());

        Button export = new Button("Export Report");
        export.getStyleClass().add("secondary-button");
        export.setOnAction(event -> services.exportService().exportApplicationReport());

        topRow.getChildren().addAll(titleBlock, jobSelector, spacer, filter, export);

        shell.getChildren().addAll(topRow, jobModeTabs, tabBar);
        return shell;
    }

    private HBox buildBody() {
        VBox leftPanel = new VBox(12);
        leftPanel.getStyleClass().add("panel-card");
        leftPanel.setPadding(new Insets(16));
        HBox.setHgrow(leftPanel, Priority.ALWAYS);

        listView.setCellFactory(param -> new ApplicantRowCell());
        listView.setPrefHeight(660);
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateDetail(newV));
        VBox.setVgrow(listView, Priority.ALWAYS);

        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(40, 24, 40, 24));
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        leftContent.getChildren().addAll(listView, emptyState);
        VBox.setVgrow(listView, Priority.ALWAYS);
        leftPanel.getChildren().add(leftContent);

        VBox rightPanel = buildDetailPanel();

        HBox body = new HBox(18, leftPanel, rightPanel);
        HBox.setHgrow(leftPanel, Priority.ALWAYS);
        return body;
    }

    private VBox buildDetailPanel() {
        VBox panel = new VBox(16);
        panel.getStyleClass().add("panel-card");
        panel.setPadding(new Insets(18));
        panel.setPrefWidth(340);
        panel.setMinWidth(340);

        detailName.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");
        detailName.setWrapText(true);

        detailMeta.setStyle("-fx-font-size: 13px; -fx-font-weight: 400; -fx-text-fill: #64748b;");
        detailMeta.setWrapText(true);

        Label skillKicker = new Label("SKILLS OVERLAP");
        skillKicker.getStyleClass().add("tiny-kicker");

        Label academicKicker = new Label("ACADEMIC RECORD");
        academicKicker.getStyleClass().add("tiny-kicker");

        Label insightKicker = new Label("INSIGHTS");
        insightKicker.getStyleClass().add("tiny-kicker");

        skillsBox.getChildren().add(detailCard("No applicant selected", "Select an applicant to view overlap information."));
        academicBox.getChildren().add(detailCard("Awaiting selection", "Matched and missing skills will appear here."));
        insightBox.getChildren().add(detailCard("Review summary", "Current workload, risk level and statement preview will appear here."));

        acceptButton.getStyleClass().add("primary-button");
        acceptButton.setMaxWidth(Double.MAX_VALUE);
        acceptButton.setOnAction(event -> openReview());

        viewFullButton.getStyleClass().add("secondary-button");
        viewFullButton.setMaxWidth(Double.MAX_VALUE);
        viewFullButton.setOnAction(event -> openReview());

        ScrollPane scrollPane = new ScrollPane(new VBox(16,
                detailName,
                detailMeta,
                skillKicker,
                skillsBox,
                academicKicker,
                academicBox,
                insightKicker,
                insightBox
        ));
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("detail-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        panel.getChildren().addAll(scrollPane, acceptButton, viewFullButton);
        return panel;
    }

    private void refreshJobs() {
        List<Job> jobs = services.jobService().getJobsByOrganiser(user.getUserId());
        jobSelector.setItems(FXCollections.observableArrayList(jobs));
        if (jobs.isEmpty()) {
            jobSelector.setValue(null);
            refreshApplications();
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
        updateJobModeTabs(selectedJob, 0);
        buildTabs(List.of());
        updateGoal(selectedJob, List.of());

        if (selectedJob == null) {
            listView.setItems(FXCollections.observableArrayList());
            showEmptyState("No job selected", "Select a managed job to review its applicants.");
            updateDetail(null);
            return;
        }

        List<Application> applications = services.applicationService().getApplicationsByJob(selectedJob.getJobId());
        updateJobModeTabs(selectedJob, applications.size());
        List<Row> rows = new ArrayList<>();
        for (Application app : applications) {
            String applicantName = services.applicantProfileRepository().findById(app.getApplicantId())
                    .map(profile -> profile.getFullName())
                    .orElse(app.getApplicantId());
            String programme = services.applicantProfileRepository().findById(app.getApplicantId())
                    .map(profile -> safe(profile.getProgramme()))
                    .orElse("Programme pending");
            int year = services.applicantProfileRepository().findById(app.getApplicantId())
                    .map(profile -> profile.getYear())
                    .orElse(0);
            rows.add(new Row(
                    app.getApplicationId(),
                    applicantName,
                    app.getStatus(),
                    app.getMatchScore(),
                    programme,
                    year,
                    selectedJob.getTitle()
            ));
        }

        buildTabs(rows);
        updateGoal(selectedJob, rows);

        List<Row> filtered = rows.stream().filter(this::matchesActiveTab).toList();
        listView.setItems(FXCollections.observableArrayList(filtered));

        if (filtered.isEmpty()) {
            showEmptyState("No applicants for this job yet", "Candidates will appear here once they start submitting their resumes and supporting documents.");
            updateDetail(null);
            return;
        }

        listView.setVisible(true);
        listView.setManaged(true);
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        listView.getSelectionModel().selectFirst();
    }

    private void buildTabs(List<Row> rows) {
        long underReview = rows.stream()
                .filter(row -> row.status == ApplicationStatus.SUBMITTED || row.status == ApplicationStatus.UNDER_REVIEW)
                .count();
        long accepted = rows.stream().filter(row -> row.status == ApplicationStatus.ACCEPTED).count();

        tabBar.getChildren().setAll(
                tabButton("ALL", "All (" + rows.size() + ")"),
                tabButton("UNDER_REVIEW", "Under Review (" + underReview + ")"),
                tabButton("ACCEPTED", "Accepted (" + accepted + ")")
        );
    }

    private Button tabButton(String key, String label) {
        Button button = new Button(label);
        if (key.equals(activeTab)) {
            button.setStyle("-fx-background-color: transparent; -fx-text-fill: #334155; -fx-font-size: 14px; -fx-font-weight: 900; -fx-border-color: transparent transparent #334155 transparent; -fx-border-width: 0 0 2 0; -fx-padding: 4 2 8 2;");
        } else {
            button.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 4 2 8 2;");
        }
        button.setOnAction(event -> {
            activeTab = key;
            refreshApplications();
        });
        return button;
    }

    private void updateJobModeTabs(Job job, int applicantCount) {
        Button applicantsTab = new Button("Applicants (" + applicantCount + ")");
        applicantsTab.setStyle("-fx-background-color: transparent; -fx-text-fill: #334155; -fx-font-size: 14px; -fx-font-weight: 900; -fx-border-color: transparent transparent #334155 transparent; -fx-border-width: 0 0 2 0; -fx-padding: 4 2 8 2;");
        applicantsTab.setDisable(true);

        Button detailsTab = new Button("Job Details");
        detailsTab.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 4 2 8 2;");
        detailsTab.setDisable(job == null);
        detailsTab.setOnAction(event -> openJobDetails(job));

        jobModeTabs.getChildren().setAll(applicantsTab, detailsTab);
    }

    private void updateGoal(Job job, List<Row> rows) {
        int target = job == null ? 0 : Math.max(job.getPositions(), 0);
        long accepted = rows.stream().filter(row -> row.status == ApplicationStatus.ACCEPTED).count();
        goalCount.setText(accepted + " / " + target);
        goalCount.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #334155;");

        goalProgress.getChildren().clear();
        RectangleBar bar = new RectangleBar(170, 6);
        goalProgress.getChildren().add(bar.track());
        double ratio = target <= 0 ? 0 : Math.min(1.0, accepted / (double) target);
        goalProgress.getChildren().add(bar.fill(ratio, "#354a5f"));
    }

    private boolean matchesActiveTab(Row row) {
        return switch (activeTab) {
            case "UNDER_REVIEW" -> row.status == ApplicationStatus.SUBMITTED || row.status == ApplicationStatus.UNDER_REVIEW;
            case "ACCEPTED" -> row.status == ApplicationStatus.ACCEPTED;
            default -> true;
        };
    }

    private void showEmptyState(String titleText, String bodyText) {
        listView.setVisible(false);
        listView.setManaged(false);
        emptyState.setVisible(true);
        emptyState.setManaged(true);
        emptyState.getChildren().clear();

        StackPane ghost = new StackPane();
        ghost.getStyleClass().add("ghost-empty-shell");
        ghost.setMinSize(170, 170);
        ghost.setPrefSize(170, 170);
        ghost.getChildren().add(styledLabel("APPS", "-fx-font-size: 34px; -fx-font-weight: 900; -fx-text-fill: #d7e0ea;"));

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Label body = new Label(bodyText);
        body.setWrapText(true);
        body.setStyle("-fx-font-size: 14px; -fx-font-weight: 400; -fx-text-fill: #64748b; -fx-text-alignment: center;");

        VBox internalCard = new VBox(4);
        internalCard.getStyleClass().add("soft-info-card");
        internalCard.setPadding(new Insets(14));
        internalCard.setMaxWidth(360);

        Label internalTitle = new Label("Add internal candidate");
        internalTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #334155;");
        Label internalMeta = new Label("Manually upload information for a known student candidate for internal tracking.");
        internalMeta.setWrapText(true);
        internalMeta.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #64748b;");
        internalCard.getChildren().addAll(internalTitle, internalMeta);

        emptyState.getChildren().addAll(ghost, title, body, internalCard);
    }

    private void updateDetail(Row row) {
        selectedRow = row;
        skillsBox.getChildren().clear();
        academicBox.getChildren().clear();
        insightBox.getChildren().clear();

        if (row == null) {
            detailName.setText("Applicant Details");
            detailMeta.setText("Select an applicant to review key information.");
            skillsBox.getChildren().add(detailCard("No applicant selected", "Select an applicant to view overlap information."));
            academicBox.getChildren().add(detailCard("Awaiting selection", "Matched and missing skills will appear here."));
            insightBox.getChildren().add(detailCard("Review summary", "Current workload, risk level and statement preview will appear here."));
            acceptButton.setDisable(true);
            viewFullButton.setDisable(true);
            return;
        }

        ApplicantReviewDTO dto = services.reviewService().getApplicantReviewData(row.applicationId, user.getUserId(), false);
        detailName.setText(row.applicantName);
        detailMeta.setText(row.programme + (row.year > 0 ? ", Year " + row.year : "") + "  •  Match " + DisplayPlaceholders.MATCH_VALUE + "  •  " + humanStatus(row.status));

        skillsBox.getChildren().add(detailCard(
                dto.matchedSkills().isEmpty() ? "Relevant skills pending" : dto.matchedSkills().get(0),
                DisplayPlaceholders.MATCH_DETAILS
        ));
        skillsBox.getChildren().add(detailCard(
                "Teaching Availability",
                dto.availability().isEmpty() ? "Availability not provided yet." : String.join(", ", dto.availability())
        ));

        academicBox.getChildren().add(detailCard(
                "Technical Skills",
                dto.technicalSkills().isEmpty() ? "No technical skills listed." : String.join(", ", dto.technicalSkills())
        ));
        academicBox.getChildren().add(detailCard(
                "Missing Skills",
                dto.missingSkills().isEmpty() ? "No obvious gaps detected." : String.join(", ", dto.missingSkills())
        ));

        insightBox.getChildren().add(detailCard(
                "Workload Check",
                "Current " + dto.currentHours() + "h • Projected " + dto.projectedHours() + "h / Max " + dto.maxWeeklyHours() + "h • Risk " + dto.riskLevel()
        ));
        insightBox.getChildren().add(detailCard(
                "Statement Preview",
                dto.statement() == null || dto.statement().isBlank() ? "No statement submitted." : dto.statement()
        ));

        acceptButton.setDisable(row.status == ApplicationStatus.ACCEPTED);
        viewFullButton.setDisable(false);
    }

    private VBox detailCard(String titleText, String bodyText) {
        VBox card = new VBox(4);
        card.getStyleClass().add("soft-info-card");
        card.setPadding(new Insets(12));

        Label title = new Label(titleText);
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #334155;");

        Label body = new Label(bodyText);
        body.setWrapText(true);
        body.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #64748b;");

        card.getChildren().addAll(title, body);
        return card;
    }

    private void cycleTab() {
        activeTab = switch (activeTab) {
            case "ALL" -> "UNDER_REVIEW";
            case "UNDER_REVIEW" -> "ACCEPTED";
            default -> "ALL";
        };
        refreshApplications();
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
        Scene scene = new Scene(reviewView, 980, 780);
        if (ApplicantListController.class.getResource("/styles/app.css") != null) {
            scene.getStylesheets().add(ApplicantListController.class.getResource("/styles/app.css").toExternalForm());
        }
        stage.setScene(scene);
        stage.showAndWait();
        refreshApplications();
    }

    private void openJobDetails(Job job) {
        if (job == null) {
            return;
        }
        JobEditorController editor = new JobEditorController();
        editor.show(job, user.getUserId());
    }

    private String humanStatus(ApplicationStatus status) {
        if (status == null) {
            return "Pending";
        }
        return switch (status) {
            case SUBMITTED -> "Submitted";
            case UNDER_REVIEW -> "Under Review";
            case ACCEPTED -> "Accepted";
            case REJECTED -> "Rejected";
            case CANCELLED -> "Cancelled";
        };
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Profile pending" : value;
    }

    private Label styledLabel(String text, String style) {
        Label label = new Label(text);
        label.setStyle(style);
        return label;
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

            VBox card = new VBox(8);
            card.setPadding(new Insets(16));
            card.getStyleClass().add(isSelected() ? "list-card-selected" : "list-card");

            HBox top = new HBox();
            top.setAlignment(Pos.CENTER_LEFT);

            Label name = new Label(item.applicantName);
            name.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label status = new Label(item.statusLabel());
            status.setStyle(item.statusStyle());

            top.getChildren().addAll(name, spacer, status);

            Label meta = new Label(item.programme + (item.year > 0 ? ", Year " + item.year : "")
                    + "   •   Score " + DisplayPlaceholders.MATCH_VALUE);
            meta.setWrapText(true);
            meta.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #64748b;");

            Label job = new Label(item.jobTitle + "  |  " + item.applicationId);
            job.setWrapText(true);
            job.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #94a3b8;");

            card.getChildren().addAll(top, meta, job);
            setGraphic(card);
            setText(null);
        }

        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            updateItem(getItem(), getItem() == null);
        }
    }

    private record Row(
            String applicationId,
            String applicantName,
            ApplicationStatus status,
            int matchScore,
            String programme,
            int year,
            String jobTitle
    ) {
        String statusLabel() {
            return switch (status) {
                case SUBMITTED -> "Submitted";
                case UNDER_REVIEW -> "Under Review";
                case ACCEPTED -> "Accepted";
                case REJECTED -> "Rejected";
                case CANCELLED -> "Cancelled";
            };
        }

        String statusStyle() {
            return switch (status) {
                case ACCEPTED -> "-fx-background-color: #ecfdf3; -fx-text-fill: #10b981; -fx-font-size: 10px; -fx-font-weight: 900; -fx-background-radius: 999; -fx-padding: 4 10 4 10;";
                case REJECTED -> "-fx-background-color: #fff1f2; -fx-text-fill: #ef4444; -fx-font-size: 10px; -fx-font-weight: 900; -fx-background-radius: 999; -fx-padding: 4 10 4 10;";
                case UNDER_REVIEW, SUBMITTED -> "-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-font-size: 10px; -fx-font-weight: 900; -fx-background-radius: 999; -fx-padding: 4 10 4 10;";
                case CANCELLED -> "-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-font-size: 10px; -fx-font-weight: 900; -fx-background-radius: 999; -fx-padding: 4 10 4 10;";
            };
        }
    }

    private record RectangleBar(double width, double height) {
        StackPane track() {
            StackPane pane = new StackPane();
            pane.setMinSize(width, height);
            pane.setPrefSize(width, height);
            pane.setMaxSize(width, height);
            pane.setStyle("-fx-background-color: #dfe7f1; -fx-background-radius: 999;");
            return pane;
        }

        StackPane fill(double ratio, String color) {
            StackPane pane = new StackPane();
            double actualWidth = ratio <= 0 ? 8 : Math.max(8, width * ratio);
            pane.setMinSize(actualWidth, height);
            pane.setPrefSize(actualWidth, height);
            pane.setMaxSize(actualWidth, height);
            pane.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 999;");
            StackPane.setAlignment(pane, Pos.CENTER_LEFT);
            return pane;
        }
    }
}
