package edu.bupt.ta.controller;

import edu.bupt.ta.dto.AdminJobRowDTO;
import edu.bupt.ta.dto.AuditLogItemDTO;
import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.ValidationResult;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AdminJobsController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DETAIL_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ServiceRegistry services;
    private final User user;

    private final VBox view = new VBox(16);
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ComboBox<String> typeFilter = new ComboBox<>();
    private final TableView<AdminJobRowDTO> table = new TableView<>();

    private final Label totalJobsValue = new Label("0");
    private final Label totalApplicantsValue = new Label("0");
    private final Label activeJobsValue = new Label("0");

    private final Label detailTitle = new Label("Select a Job");
    private final Label detailSubline = new Label("-");
    private final Label detailModuleTitle = new Label("-");
    private final Label detailModuleMeta = new Label("-");
    private final Label detailModuleDescription = new Label("-");
    private final Label detailOrganiser = new Label("-");
    private final Label detailWeeklyHours = new Label("-");
    private final Label detailPositions = new Label("-");
    private final Label detailDeadline = new Label("-");
    private final Label detailCreated = new Label("-");
    private final Label detailRequiredSkills = new Label("-");
    private final Label detailPreferredSkills = new Label("-");
    private final Label detailAppliedCount = new Label("0");
    private final Label detailReviewCount = new Label("0");
    private final Label detailAcceptedCount = new Label("0");
    private final HBox applicantAvatarStrip = new HBox(6);
    private final VBox activityLogBox = new VBox(10);

    private final Button editButton = new Button("Edit Job");
    private final Button closeButton = new Button("Close Job");
    private final Button viewApplicationsButton = new Button("View All Applications");
    private final Button detailEditButton = new Button("Edit Job Details");
    private final Button detailCloseButton = new Button("Close Job");

    private List<AdminJobRowDTO> allJobs = List.of();

    public AdminJobsController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
        refresh();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");
        view.setPadding(new Insets(24));
        view.getChildren().addAll(buildHeader(), buildKpiRow(), buildMainArea());

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        typeFilter.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        updateActionButtons(null);
    }

    private VBox buildHeader() {
        Label title = new Label("Jobs");
        title.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button filterButton = new Button("Filter");
        filterButton.getStyleClass().add("secondary-button");
        filterButton.setOnAction(event -> statusFilter.show());

        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(event -> refresh());

        Button createButton = new Button("+ Create New Job");
        createButton.getStyleClass().add("primary-button");
        createButton.setOnAction(event -> onCreate());

        HBox topRow = new HBox(10, title, spacer, filterButton, refreshButton, createButton);
        topRow.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Search by title, job ID, module, organiser...");
        searchField.setPrefWidth(360);

        statusFilter.getItems().setAll("ALL STATUS", "OPEN", "CLOSED", "EXPIRED", "DRAFT");
        statusFilter.setValue("ALL STATUS");
        statusFilter.setPrefWidth(150);

        typeFilter.getItems().setAll("ALL TYPES", "MODULE_TA", "INVIGILATION", "ACTIVITY_SUPPORT", "OTHER");
        typeFilter.setValue("ALL TYPES");
        typeFilter.setPrefWidth(170);

        editButton.getStyleClass().add("secondary-button");
        editButton.setOnAction(event -> onEdit());

        closeButton.getStyleClass().add("secondary-button");
        closeButton.setOnAction(event -> onClose());

        HBox toolbar = new HBox(12, searchField, statusFilter, typeFilter, spacerNode(), editButton, closeButton);
        toolbar.getStyleClass().add("surface-toolbar");

        return new VBox(12, topRow, toolbar);
    }

    private HBox buildKpiRow() {
        return new HBox(16,
                kpiCard("Total Openings", totalJobsValue, "Across all organisers"),
                kpiCard("Total Applicants", totalApplicantsValue, "Applications linked to current postings"),
                kpiCard("Active Jobs", activeJobsValue, "OPEN jobs currently receiving candidates")
        );
    }

    private VBox kpiCard(String title, Label valueLabel, String note) {
        VBox card = new VBox(6);
        card.getStyleClass().add("metric-card");
        card.setMinWidth(220);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label titleNode = new Label(title);
        titleNode.getStyleClass().add("metric-kicker");

        valueLabel.getStyleClass().add("metric-value");
        valueLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Label noteNode = new Label(note);
        noteNode.setWrapText(true);
        noteNode.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        card.getChildren().addAll(titleNode, valueLabel, noteNode);
        return card;
    }

    private HBox buildMainArea() {
        VBox listPanel = new VBox(12);
        listPanel.getStyleClass().add("panel-card");
        listPanel.setPadding(new Insets(18));

        TableColumn<AdminJobRowDTO, String> titleCol = new TableColumn<>("JOB TITLE & ID");
        titleCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().title()));
        titleCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                AdminJobRowDTO row = getTableView().getItems().get(getIndex());

                Label title = new Label(row.title());
                title.setWrapText(false);
                title.setTextOverrun(OverrunStyle.ELLIPSIS);
                title.setMaxWidth(250);
                title.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #334155;");

                Label id = new Label("ID: " + row.jobId());
                id.setTextOverrun(OverrunStyle.ELLIPSIS);
                id.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: 700;");

                VBox box = new VBox(3, title, id);
                setGraphic(box);
                setText(null);
            }
        });

        TableColumn<AdminJobRowDTO, String> departmentCol = new TableColumn<>("DEPARTMENT");
        departmentCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().moduleName()));
        departmentCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                AdminJobRowDTO row = getTableView().getItems().get(getIndex());

                Label title = new Label(row.moduleName());
                title.setWrapText(false);
                title.setTextOverrun(OverrunStyle.ELLIPSIS);
                title.setMaxWidth(220);
                title.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #475569;");

                Label meta = new Label(row.moduleCode());
                meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

                setGraphic(new VBox(4, title, meta));
                setText(null);
            }
        });

        TableColumn<AdminJobRowDTO, Number> applicantsCol = new TableColumn<>("APPLICANTS");
        applicantsCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().applicantCount()));
        applicantsCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(String.valueOf(item.intValue()));
                badge.setStyle("-fx-background-color: #eef2f7; -fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-padding: 4 10 4 10;");
                setGraphic(badge);
                setText(null);
            }
        });

        TableColumn<AdminJobRowDTO, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().statusLabel()));
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label chip = new Label(item);
                chip.setStyle(statusChipStyle(item));
                setGraphic(chip);
                setText(null);
            }
        });

        TableColumn<AdminJobRowDTO, String> createdCol = new TableColumn<>("CREATED");
        createdCol.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().createdAt())));
        createdCol.setCellFactory(column -> mutedStringCell());

        TableColumn<AdminJobRowDTO, String> actionCol = new TableColumn<>("ACTIONS");
        actionCol.setCellValueFactory(cell -> new SimpleStringProperty("..."));
        actionCol.setCellFactory(column -> centeredEllipsisCell());

        titleCol.setPrefWidth(290);
        departmentCol.setPrefWidth(240);
        applicantsCol.setPrefWidth(100);
        statusCol.setPrefWidth(120);
        createdCol.setPrefWidth(130);
        actionCol.setPrefWidth(80);
        table.getColumns().setAll(titleCol, departmentCol, applicantsCol, statusCol, createdCol, actionCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(72);
        table.setPrefHeight(560);
        table.setPlaceholder(new Label("No jobs match the current filters."));
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldJob, newJob) -> {
            updateActionButtons(newJob);
            updateDetail(newJob);
        });

        listPanel.getChildren().add(table);
        HBox.setHgrow(listPanel, Priority.ALWAYS);

        VBox detailContent = new VBox(18);
        detailContent.setPadding(new Insets(18));

        Label kicker = new Label("JOB DETAILS");
        kicker.getStyleClass().add("section-kicker");

        detailTitle.setWrapText(true);
        detailTitle.setStyle("-fx-font-size: 32px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");

        detailSubline.setWrapText(true);
        detailSubline.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-weight: 700;");

        VBox moduleCard = new VBox(8,
                sectionTitle("ASSOCIATED MODULE"),
                detailModuleTitle,
                detailModuleMeta,
                detailModuleDescription);
        moduleCard.getStyleClass().add("surface-toolbar");
        detailModuleTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #334155;");
        detailModuleMeta.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        detailModuleDescription.setWrapText(true);
        detailModuleDescription.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        VBox recruitmentCard = new VBox(10,
                sectionTitle("JOB SUMMARY"),
                detailLine("Organiser", detailOrganiser),
                detailLine("Weekly Hours", detailWeeklyHours),
                detailLine("Positions", detailPositions),
                detailLine("Deadline", detailDeadline),
                detailLine("Created", detailCreated),
                detailLine("Required Skills", detailRequiredSkills),
                detailLine("Preferred Skills", detailPreferredSkills)
        );
        recruitmentCard.getStyleClass().add("surface-toolbar");

        VBox applicantStatusCard = new VBox(10,
                sectionTitle("APPLICANT STATUS"),
                statusRow("Applied", detailAppliedCount, "#2563eb"),
                statusRow("In Review", detailReviewCount, "#8b5cf6"),
                statusRow("Hired", detailAcceptedCount, "#10b981"),
                applicantAvatarStrip
        );
        applicantStatusCard.getStyleClass().add("surface-toolbar");

        VBox activityCard = new VBox(10, sectionTitle("ACTIVITY LOG"), activityLogBox);
        activityCard.getStyleClass().add("surface-toolbar");

        detailContent.getChildren().addAll(kicker, detailTitle, detailSubline, moduleCard, recruitmentCard, applicantStatusCard, activityCard);

        ScrollPane detailScroll = new ScrollPane(detailContent);
        detailScroll.setFitToWidth(true);
        detailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScroll.getStyleClass().add("detail-scroll");
        VBox.setVgrow(detailScroll, Priority.ALWAYS);

        viewApplicationsButton.getStyleClass().add("primary-button");
        viewApplicationsButton.setMaxWidth(Double.MAX_VALUE);
        viewApplicationsButton.setOnAction(event -> openApplicationsModal());

        detailEditButton.getStyleClass().add("secondary-button");
        detailEditButton.setMaxWidth(Double.MAX_VALUE);
        detailEditButton.setOnAction(event -> onEdit());

        detailCloseButton.getStyleClass().add("secondary-button");
        detailCloseButton.setMaxWidth(Double.MAX_VALUE);
        detailCloseButton.setOnAction(event -> onClose());

        VBox detailPanel = new VBox(12, detailScroll, viewApplicationsButton, detailEditButton, detailCloseButton);
        detailPanel.getStyleClass().add("panel-card");
        detailPanel.setPadding(new Insets(16));
        detailPanel.setPrefWidth(360);

        return new HBox(16, listPanel, detailPanel);
    }

    private void refresh() {
        allJobs = services.adminMonitoringService().getJobRows();
        totalJobsValue.setText(String.valueOf(allJobs.size()));
        totalApplicantsValue.setText(String.valueOf(allJobs.stream().mapToInt(AdminJobRowDTO::applicantCount).sum()));
        activeJobsValue.setText(String.valueOf(allJobs.stream().filter(job -> "OPEN".equals(job.statusLabel())).count()));
        applyFilters();
    }

    private void applyFilters() {
        String keyword = normalize(searchField.getText());
        String status = statusFilter.getValue();
        String type = typeFilter.getValue();

        List<AdminJobRowDTO> filtered = allJobs.stream()
                .filter(job -> keyword.isEmpty()
                        || contains(job.title(), keyword)
                        || contains(job.moduleCode(), keyword)
                        || contains(job.moduleName(), keyword)
                        || contains(job.organiserName(), keyword)
                        || contains(job.organiserId(), keyword)
                        || contains(job.jobId(), keyword)
                        || contains(job.description(), keyword))
                .filter(job -> status == null || "ALL STATUS".equals(status) || status.equals(job.statusLabel()))
                .filter(job -> type == null || "ALL TYPES".equals(type) || type.equals(job.typeLabel()))
                .toList();

        table.setItems(FXCollections.observableArrayList(filtered));
        if (!filtered.isEmpty()) {
            table.getSelectionModel().selectFirst();
        } else {
            updateActionButtons(null);
            updateDetail(null);
        }
    }

    private void updateActionButtons(AdminJobRowDTO job) {
        boolean hasSelection = job != null;
        boolean canClose = hasSelection && "OPEN".equals(job.statusLabel());

        editButton.setDisable(!hasSelection);
        detailEditButton.setDisable(!hasSelection);
        closeButton.setDisable(!canClose);
        detailCloseButton.setDisable(!canClose);
        viewApplicationsButton.setDisable(!hasSelection);
    }

    private void updateDetail(AdminJobRowDTO job) {
        if (job == null) {
            detailTitle.setText("Select a Job");
            detailSubline.setText("-");
            detailModuleTitle.setText("-");
            detailModuleMeta.setText("-");
            detailModuleDescription.setText("-");
            detailOrganiser.setText("-");
            detailWeeklyHours.setText("-");
            detailPositions.setText("-");
            detailDeadline.setText("-");
            detailCreated.setText("-");
            detailRequiredSkills.setText("-");
            detailPreferredSkills.setText("-");
            detailAppliedCount.setText("0");
            detailReviewCount.setText("0");
            detailAcceptedCount.setText("0");
            applicantAvatarStrip.getChildren().setAll(new Label("-"));
            activityLogBox.getChildren().setAll(mutedText("No recent activity"));
            return;
        }

        detailTitle.setText(job.title());
        detailSubline.setText(job.jobId() + "    " + job.statusLabel());
        detailModuleTitle.setText(job.moduleName());
        detailModuleMeta.setText(job.moduleCode() + "  |  " + job.typeLabel());
        detailModuleDescription.setText(blankToDash(job.description()));
        detailOrganiser.setText(job.organiserName() + " (" + job.organiserId() + ")");
        detailWeeklyHours.setText(job.weeklyHours() + " h/week");
        detailPositions.setText(String.valueOf(job.positions()));
        detailDeadline.setText(formatDeadline(job.deadline()));
        detailCreated.setText(formatDateTime(job.createdAt()));
        detailRequiredSkills.setText(joinList(job.requiredSkills()));
        detailPreferredSkills.setText(joinList(job.preferredSkills()));

        List<Application> applications = services.applicationRepository().findByJobId(job.jobId());
        long applied = applications.stream().filter(application -> application.getStatus() == ApplicationStatus.SUBMITTED).count();
        long inReview = applications.stream().filter(application -> application.getStatus() == ApplicationStatus.UNDER_REVIEW).count();
        long accepted = applications.stream().filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED).count();
        detailAppliedCount.setText(String.valueOf(applied));
        detailReviewCount.setText(String.valueOf(inReview));
        detailAcceptedCount.setText(String.valueOf(accepted));

        updateAvatarStrip(applications);
        updateActivityLog(job, applications);
    }

    private void updateAvatarStrip(List<Application> applications) {
        applicantAvatarStrip.getChildren().clear();
        List<Label> avatars = applications.stream()
                .limit(4)
                .map(application -> services.applicantProfileRepository().findById(application.getApplicantId()).orElse(null))
                .map(profile -> buildAvatar(profile == null ? null : profile.getFullName()))
                .toList();

        if (avatars.isEmpty()) {
            applicantAvatarStrip.getChildren().add(mutedText("No applicants yet"));
            return;
        }
        applicantAvatarStrip.getChildren().addAll(avatars);
        if (applications.size() > 4) {
            applicantAvatarStrip.getChildren().add(buildAvatar("+" + (applications.size() - 4)));
        }
    }

    private void updateActivityLog(AdminJobRowDTO job, List<Application> applications) {
        List<String> applicationIds = applications.stream().map(Application::getApplicationId).toList();
        List<AuditLogItemDTO> logs = services.adminMonitoringService().getAuditLogs().stream()
                .filter(item -> contains(item.detail(), normalize(job.jobId()))
                        || applicationIds.stream().anyMatch(appId -> contains(item.detail(), normalize(appId))))
                .limit(4)
                .toList();

        activityLogBox.getChildren().clear();
        if (logs.isEmpty()) {
            activityLogBox.getChildren().add(mutedText("No recent activity"));
            return;
        }

        for (AuditLogItemDTO log : logs) {
            VBox row = new VBox(4);
            Label detail = new Label(log.actorName() + " " + log.action().replace('_', ' ').toLowerCase(Locale.ROOT));
            detail.setWrapText(true);
            detail.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #334155;");

            Label meta = new Label(formatDateTime(log.timestamp()));
            meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            row.getChildren().addAll(detail, meta);
            activityLogBox.getChildren().add(row);
        }
    }

    private void onCreate() {
        List<User> organisers = loadOrganisers();
        if (organisers.isEmpty()) {
            showError("No active MO organiser accounts are available for assignment.");
            return;
        }

        JobEditorController editor = new JobEditorController();
        editor.show(null, user.getUserId(), organisers).ifPresent(job -> {
            ValidationResult result = services.jobService().createJob(job, user.getUserId());
            if (!result.isValid()) {
                showError(String.join("\n", result.getErrors()));
                return;
            }
            refresh();
        });
    }

    private void onEdit() {
        AdminJobRowDTO selectedRow = table.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            showError("Please select one job first.");
            return;
        }

        Job source = services.jobService().getJob(selectedRow.jobId()).orElse(null);
        if (source == null) {
            showError("The selected job could not be loaded.");
            refresh();
            return;
        }

        JobEditorController editor = new JobEditorController();
        editor.show(source, source.getOrganiserId(), loadOrganisers()).ifPresent(job -> {
            ValidationResult result = services.jobService().updateJob(job, user.getUserId(), true);
            if (!result.isValid()) {
                showError(String.join("\n", result.getErrors()));
                return;
            }
            refresh();
        });
    }

    private void onClose() {
        AdminJobRowDTO selectedRow = table.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            showError("Please select one job first.");
            return;
        }

        boolean confirmed = DialogControllerFactory.confirmAction(
                "Close Job",
                "Close \"" + selectedRow.title() + "\" now? Closed jobs cannot receive new applications.",
                view.getScene() == null ? null : view.getScene().getWindow());
        if (!confirmed) {
            return;
        }

        ValidationResult result = services.jobService().closeJobWithValidation(selectedRow.jobId(), user.getUserId(), true);
        if (!result.isValid()) {
            showError(String.join("\n", result.getErrors()));
            refresh();
            return;
        }

        DialogControllerFactory.success("Job Closed", "The job was set to CLOSED successfully.",
                view.getScene() == null ? null : view.getScene().getWindow());
        refresh();
    }

    private void openApplicationsModal() {
        AdminJobRowDTO selectedRow = table.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            showError("Please select one job first.");
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (view.getScene() != null) {
            stage.initOwner(view.getScene().getWindow());
        }
        stage.setTitle("Applications for " + selectedRow.jobId());

        Parent content = new AdminApplicationsController(services, user, selectedRow.jobId()).getView();
        Scene scene = new Scene(content, 1440, 900);
        if (AdminJobsController.class.getResource("/styles/app.css") != null) {
            scene.getStylesheets().add(AdminJobsController.class.getResource("/styles/app.css").toExternalForm());
        }
        stage.setScene(scene);
        stage.showAndWait();
        refresh();
    }

    private List<User> loadOrganisers() {
        return services.userRepository().findAll().stream()
                .filter(candidate -> candidate.getRole() == Role.MO && candidate.isActive())
                .sorted(Comparator.comparing(candidate -> normalize(candidate.getDisplayName())))
                .toList();
    }

    private HBox detailLine(String label, Label value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #94a3b8;");
        value.setWrapText(true);
        value.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #334155;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, labelNode, spacer, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox statusRow(String label, Label valueLabel, String accent) {
        Label name = new Label(label);
        name.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569; -fx-font-weight: 700;");
        valueLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: " + accent + ";");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, name, spacer, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Label buildAvatar(String text) {
        String label = text == null || text.isBlank() ? "?" : (text.startsWith("+") ? text : initials(text));
        Label avatar = new Label(label);
        avatar.setAlignment(Pos.CENTER);
        avatar.setMinSize(30, 30);
        avatar.setStyle("-fx-background-color: #eef2f7; -fx-text-fill: #475569; -fx-background-radius: 999; -fx-font-size: 11px; -fx-font-weight: 800;");
        return avatar;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-kicker");
        return label;
    }

    private Region spacerNode() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private TableCell<AdminJobRowDTO, String> mutedStringCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                if (!empty) {
                    setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: 600;");
                } else {
                    setStyle("");
                }
                setGraphic(null);
            }
        };
    }

    private TableCell<AdminJobRowDTO, String> centeredEllipsisCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label dots = new Label("...");
                dots.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #94a3b8;");
                setGraphic(dots);
                setText(null);
            }
        };
    }

    private String statusChipStyle(String status) {
        return switch (status == null ? "" : status) {
            case "OPEN" -> "-fx-background-color: #ecfdf3; -fx-text-fill: #10b981; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-padding: 4 10 4 10;";
            case "DRAFT" -> "-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-padding: 4 10 4 10;";
            case "EXPIRED" -> "-fx-background-color: #fff7ed; -fx-text-fill: #f59e0b; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-padding: 4 10 4 10;";
            default -> "-fx-background-color: #f8fafc; -fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-padding: 4 10 4 10;";
        };
    }

    private Label mutedText(String value) {
        Label label = new Label(value);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        return label;
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DATE_FORMAT);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DETAIL_TIME_FORMAT);
    }

    private String formatDeadline(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DETAIL_TIME_FORMAT);
    }

    private String joinList(List<String> values) {
        return values == null || values.isEmpty() ? "-" : String.join(", ", values);
    }

    private String initials(String text) {
        if (text == null || text.isBlank()) {
            return "?";
        }
        String[] parts = text.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean contains(String text, String keyword) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void showError(String message) {
        DialogControllerFactory.validationError(message,
                view.getScene() == null ? null : view.getScene().getWindow());
    }
}
