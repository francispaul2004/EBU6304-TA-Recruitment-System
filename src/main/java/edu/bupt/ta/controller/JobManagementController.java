package edu.bupt.ta.controller;

import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.ValidationResult;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class JobManagementController {

    private final ServiceRegistry services;
    private final User user;
    private final Consumer<Job> onViewApplicants;
    private final BorderPane view = new BorderPane();
    private final VBox page = new VBox(16);
    private final TableView<Job> table = new TableView<>();
    private HBox kpiRow;
    private JobStatus currentFilterStatus;

    private final Label detailTitle = new Label("No Job Selected");
    private final Label detailSubtitle = new Label("-");
    private final Label detailJobId = new Label("-");
    private final Label detailModuleCode = new Label("-");
    private final Label detailModuleName = new Label("-");
    private final Label detailType = new Label("-");
    private final Label detailStatus = new Label("-");
    private final Label detailWeeklyHours = new Label("-");
    private final Label detailPositions = new Label("-");
    private final Label detailApplicants = new Label("-");
    private final Label detailDeadline = new Label("-");
    private final Label detailCreated = new Label("-");
    private final Label detailOrganiserId = new Label("-");
    private final Label detailRequiredSkills = new Label("-");
    private final Label detailPreferredSkills = new Label("-");
    private final Label detailDescription = new Label("-");

    public JobManagementController(ServiceRegistry services, User user, Consumer<Job> onViewApplicants) {
        this.services = services;
        this.user = user;
        this.onViewApplicants = onViewApplicants;
        initialize();
        refresh();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");

        page.setPadding(new Insets(24));

        kpiRow = buildKpiRow();
        page.getChildren().add(buildHeader());
        page.getChildren().add(kpiRow);
        page.getChildren().add(buildMainArea());

        view.setCenter(page);
    }

    private HBox buildHeader() {
        Label title = new Label("My Jobs");
        title.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button filter = new Button("Filter");
        filter.getStyleClass().add("secondary-button");
        filter.setOnAction(event -> onFilter());

        Button create = new Button("+ Create New Job");
        create.getStyleClass().add("primary-button");
        create.setOnAction(event -> onCreate());

        HBox row = new HBox(10, title, spacer, filter, create);
        return row;
    }

    private HBox buildKpiRow() {
        List<Job> jobs = services.jobService().getJobsByOrganiser(user.getUserId());
        long open = jobs.stream().filter(job -> job.getStatus() == JobStatus.OPEN).count();
        long closed = jobs.stream().filter(job -> job.getStatus() == JobStatus.CLOSED).count();
        long draft = jobs.stream().filter(job -> job.getStatus() == JobStatus.DRAFT).count();

        HBox row = new HBox(16,
                kpiCard("Total Jobs", String.valueOf(jobs.size())),
                kpiCard("Active Jobs", String.valueOf(open)),
                kpiCard("Completed", String.valueOf(closed)),
                kpiCard("Draft", String.valueOf(draft))
        );
        return row;
    }

    private VBox kpiCard(String title, String value) {
        VBox card = new VBox(4);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(14));
        card.setMinWidth(230);

        Label titleNode = new Label(title);
        titleNode.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #64748b;");

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-font-size: 40px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");

        card.getChildren().addAll(titleNode, valueNode);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox buildMainArea() {
        VBox listPanel = new VBox(12);
        listPanel.getStyleClass().add("panel-card");
        listPanel.setPadding(new Insets(14));

        HBox actions = new HBox(8);
        Button edit = new Button("Edit");
        edit.getStyleClass().add("secondary-button");
        edit.setOnAction(event -> onEdit());

        Button close = new Button("Close Job");
        close.getStyleClass().add("secondary-button");
        close.setOnAction(event -> onClose());

        Button reload = new Button("Refresh");
        reload.getStyleClass().add("secondary-button");
        reload.setOnAction(event -> refresh());

        actions.getChildren().addAll(edit, close, reload);

        TableColumn<Job, String> titleCol = new TableColumn<>("JOB TITLE");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Job, String> idCol = new TableColumn<>("JOB ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("jobId"));

        TableColumn<Job, String> moduleCol = new TableColumn<>("MODULE");
        moduleCol.setCellValueFactory(new PropertyValueFactory<>("moduleCode"));

        TableColumn<Job, Integer> positionsCol = new TableColumn<>("POSITIONS");
        positionsCol.setCellValueFactory(new PropertyValueFactory<>("positions"));

        TableColumn<Job, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Job, String> createdCol = new TableColumn<>("CREATED");
        createdCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        table.getColumns().setAll(titleCol, idCol, moduleCol, positionsCol, statusCol, createdCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(620);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldJob, newJob) -> updateDetail(newJob));

        listPanel.getChildren().addAll(actions, table);

        VBox detail = new VBox(12);
        detail.getStyleClass().add("panel-card");
        detail.setPadding(new Insets(16));
        detail.setPrefWidth(340);

        detailTitle.setStyle("-fx-font-size: 34px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");
        detailTitle.setWrapText(true);
        detailSubtitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #64748b;");
        detailSubtitle.setWrapText(true);

        Button viewApplicants = new Button("View All Applicants");
        viewApplicants.getStyleClass().add("primary-button");
        viewApplicants.setMaxWidth(Double.MAX_VALUE);
        viewApplicants.setOnAction(event -> onViewApplicants());

        Button editJob = new Button("Edit Job Details");
        editJob.getStyleClass().add("secondary-button");
        editJob.setMaxWidth(Double.MAX_VALUE);
        editJob.setOnAction(event -> onEdit());

        detail.getChildren().addAll(
                detailTitle,
                detailSubtitle,
                detailField("Job ID", detailJobId),
                detailField("Module Code", detailModuleCode),
                detailField("Module Name", detailModuleName),
                detailField("Job Type", detailType),
                detailField("Status", detailStatus),
                detailField("Weekly Hours", detailWeeklyHours),
                detailField("Positions", detailPositions),
                detailField("Applicants", detailApplicants),
                detailField("Deadline", detailDeadline),
                detailField("Created", detailCreated),
                detailField("Organiser ID", detailOrganiserId),
                detailField("Required Skills", detailRequiredSkills),
                detailField("Preferred Skills", detailPreferredSkills),
                detailField("Description", detailDescription),
                viewApplicants,
                editJob
        );

        ScrollPane detailScroll = new ScrollPane(detail);
        detailScroll.setFitToWidth(true);
        detailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScroll.setPrefWidth(340);
        detailScroll.getStyleClass().add("panel-card");

        HBox body = new HBox(16, listPanel, detailScroll);
        HBox.setHgrow(listPanel, Priority.ALWAYS);
        return body;
    }

    private VBox detailField(String title, Label value) {
        VBox box = new VBox(4);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
        value.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
        value.setWrapText(true);
        box.getChildren().addAll(t, value);
        return box;
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
        boolean confirmed = DialogControllerFactory.confirmAction(
                "Close Job",
                "Close \"" + selected.getTitle() + "\" now? Closed jobs cannot receive new applications.",
                view.getScene() == null ? null : view.getScene().getWindow());
        if (!confirmed) {
            return;
        }
        services.jobService().closeJob(selected.getJobId(), user.getUserId());
        DialogControllerFactory.success("Job Closed", "The job was set to CLOSED successfully.",
                view.getScene() == null ? null : view.getScene().getWindow());
        refresh();
    }

    private void onFilter() {
        List<String> options = List.of("All Statuses", "OPEN", "DRAFT", "CLOSED", "EXPIRED");
        String currentSelection = currentFilterStatus == null ? "All Statuses" : currentFilterStatus.name();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(currentSelection, options);
        dialog.setTitle("Filter Jobs");
        dialog.setHeaderText("Filter jobs by status");
        dialog.setContentText("Status:");
        if (view.getScene() != null && view.getScene().getWindow() != null) {
            dialog.initOwner(view.getScene().getWindow());
        }

        Optional<String> selected = dialog.showAndWait();
        if (selected.isEmpty()) {
            return;
        }

        currentFilterStatus = "All Statuses".equals(selected.get()) ? null : JobStatus.valueOf(selected.get());
        refresh();
    }

    private void onViewApplicants() {
        Job selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select one job first.");
            return;
        }
        if (onViewApplicants != null) {
            onViewApplicants.accept(selected);
        }
    }

    private void refresh() {
        Job previouslySelected = table.getSelectionModel().getSelectedItem();
        String selectedJobId = previouslySelected == null ? null : previouslySelected.getJobId();

        kpiRow = buildKpiRow();
        page.getChildren().set(1, kpiRow);
        List<Job> jobs = services.jobService().getJobsByOrganiser(user.getUserId());
        if (currentFilterStatus != null) {
            jobs = jobs.stream().filter(job -> job.getStatus() == currentFilterStatus).toList();
        }
        table.setItems(FXCollections.observableArrayList(jobs));
        if (jobs.isEmpty()) {
            updateDetail(null);
            return;
        }

        if (selectedJobId != null) {
            for (Job job : jobs) {
                if (selectedJobId.equals(job.getJobId())) {
                    table.getSelectionModel().select(job);
                    return;
                }
            }
        }

        table.getSelectionModel().selectFirst();
    }

    private void updateDetail(Job job) {
        if (job == null) {
            detailTitle.setText("No Job Selected");
            detailSubtitle.setText("-");
            detailJobId.setText("-");
            detailModuleCode.setText("-");
            detailModuleName.setText("-");
            detailType.setText("-");
            detailStatus.setText("-");
            detailWeeklyHours.setText("-");
            detailPositions.setText("-");
            detailApplicants.setText("-");
            detailDeadline.setText("-");
            detailCreated.setText("-");
            detailOrganiserId.setText("-");
            detailRequiredSkills.setText("-");
            detailPreferredSkills.setText("-");
            detailDescription.setText("-");
            return;
        }

        int applicantCount = services.applicationService().getApplicationsByJob(job.getJobId()).size();
        detailTitle.setText(job.getTitle() == null || job.getTitle().isBlank() ? "Untitled Job" : job.getTitle());
        detailSubtitle.setText(buildDetailSubtitle(job));
        detailJobId.setText(job.getJobId() == null || job.getJobId().isBlank() ? "-" : job.getJobId());
        detailModuleCode.setText(job.getModuleCode() == null || job.getModuleCode().isBlank() ? "-" : job.getModuleCode());
        detailModuleName.setText(job.getModuleName() == null || job.getModuleName().isBlank() ? "-" : job.getModuleName());
        detailType.setText(job.getType() == null ? "-" : job.getType().name());
        detailStatus.setText(job.getStatus().name());
        detailWeeklyHours.setText(String.valueOf(job.getWeeklyHours()));
        detailPositions.setText(String.valueOf(job.getPositions()));
        detailApplicants.setText(String.valueOf(applicantCount));
        detailDeadline.setText(job.getDeadline() == null ? "-" : job.getDeadline().toString());
        detailCreated.setText(job.getCreatedAt() == null ? "-" : job.getCreatedAt().toString());
        detailOrganiserId.setText(job.getOrganiserId() == null || job.getOrganiserId().isBlank() ? "-" : job.getOrganiserId());
        detailRequiredSkills.setText(formatList(job.getRequiredSkills()));
        detailPreferredSkills.setText(formatList(job.getPreferredSkills()));
        detailDescription.setText(job.getDescription() == null || job.getDescription().isBlank() ? "-" : job.getDescription());
    }

    private String buildDetailSubtitle(Job job) {
        String moduleCode = job.getModuleCode() == null || job.getModuleCode().isBlank() ? "No Module Code" : job.getModuleCode();
        String status = job.getStatus() == null ? "UNKNOWN" : job.getStatus().name();
        return moduleCode + " | " + status;
    }

    private String formatList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "-";
        }
        return String.join(", ", values);
    }

    private void showError(String message) {
        DialogControllerFactory.validationError(message, view.getScene() == null ? null : view.getScene().getWindow());
    }
}
