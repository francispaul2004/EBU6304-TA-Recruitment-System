package edu.bupt.ta.controller;

import edu.bupt.ta.dto.AdminApplicationRowDTO;
import edu.bupt.ta.dto.ApplicantReviewDTO;
import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.DisplayPlaceholders;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class AdminApplicationsController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ServiceRegistry services;
    private final User user;
    private final String initialJobFilter;
    private final VBox view = new VBox(16);

    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ComboBox<String> riskFilter = new ComboBox<>();
    private final TableView<AdminApplicationRowDTO> table = new TableView<>();

    private final Label submittedValue = new Label("0");
    private final Label underReviewValue = new Label("0");
    private final Label acceptedValue = new Label("0");
    private final Label rejectedValue = new Label("0");

    private final Label heroTitle = new Label("Select an Application");
    private final Label heroSubtitle = new Label("-");
    private final Label heroStatus = new Label("QUEUE VIEW");

    private final Label fullNameValue = new Label("-");
    private final Label studentIdValue = new Label("-");
    private final Label programmeValue = new Label("-");
    private final Label emailValue = new Label("-");
    private final Label phoneValue = new Label("-");
    private final Label matchScoreValue = new Label("-");
    private final Label workloadValue = new Label("-");
    private final Label statementValue = new Label("-");
    private final Label attachmentNameValue = new Label("-");
    private final Label attachmentMetaValue = new Label("-");

    private final FlowPane skillsPane = new FlowPane(8, 8);
    private final FlowPane availabilityPane = new FlowPane(8, 8);
    private final FlowPane missingSkillsPane = new FlowPane(8, 8);

    private final TextArea decisionNoteInput = new TextArea();
    private final Button openReviewButton = new Button("Open Review Workspace");
    private final Button acceptButton = new Button("Accept Candidate");
    private final Button rejectButton = new Button("Reject Candidate");

    private List<AdminApplicationRowDTO> allApplications = List.of();

    public AdminApplicationsController(ServiceRegistry services, User user) {
        this(services, user, null);
    }

    public AdminApplicationsController(ServiceRegistry services, User user, String initialJobFilter) {
        this.services = services;
        this.user = user;
        this.initialJobFilter = initialJobFilter;
        initialize();
        if (initialJobFilter != null && !initialJobFilter.isBlank()) {
            searchField.setText(initialJobFilter);
        }
        refresh();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");
        view.setPadding(new Insets(24));
        view.getChildren().addAll(buildHeader(), buildKpiRow(), buildBody());

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        riskFilter.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        updateActionButtons(null);
    }

    private VBox buildHeader() {
        Label title = new Label("Applications");
        title.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(event -> refresh());

        Button exportButton = new Button("Export Applications");
        exportButton.getStyleClass().add("secondary-button");
        exportButton.setOnAction(event -> exportApplications());

        HBox topRow = new HBox(10, title, spacer, refreshButton, exportButton);
        topRow.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Search by applicant, application ID, job, organiser, or student ID...");
        searchField.setPrefWidth(420);

        statusFilter.getItems().setAll("ALL STATUS", "SUBMITTED", "UNDER_REVIEW", "ACCEPTED", "REJECTED");
        statusFilter.setValue("ALL STATUS");
        statusFilter.setPrefWidth(150);

        riskFilter.getItems().setAll("ALL RISK", "HIGH", "MEDIUM", "LOW");
        riskFilter.setValue("ALL RISK");
        riskFilter.setPrefWidth(140);

        HBox toolbar = new HBox(12, searchField, statusFilter, riskFilter);
        toolbar.getStyleClass().add("surface-toolbar");
        return new VBox(12, topRow, toolbar);
    }

    private HBox buildKpiRow() {
        return new HBox(16,
                kpiCard("SUBMITTED", submittedValue, "#2563eb"),
                kpiCard("UNDER REVIEW", underReviewValue, "#8b5cf6"),
                kpiCard("ACCEPTED", acceptedValue, "#10b981"),
                kpiCard("REJECTED", rejectedValue, "#ef4444")
        );
    }

    private VBox kpiCard(String title, Label valueLabel, String accent) {
        VBox card = new VBox(6);
        card.getStyleClass().add("metric-card");
        card.setMinWidth(200);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label titleNode = new Label(title);
        titleNode.getStyleClass().add("metric-kicker");

        valueLabel.getStyleClass().add("metric-value");
        valueLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: 900; -fx-text-fill: " + accent + ";");

        card.getChildren().addAll(titleNode, valueLabel);
        return card;
    }

    private HBox buildBody() {
        VBox queuePanel = new VBox(12);
        queuePanel.getStyleClass().add("panel-card");
        queuePanel.setPadding(new Insets(18));

        Label queueTitle = new Label("Application Queue");
        queueTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #1e293b;");

        Label queueSubtitle = new Label("Filtered list of all candidate submissions");
        queueSubtitle.getStyleClass().add("body-muted");

        TableColumn<AdminApplicationRowDTO, String> applicantCol = new TableColumn<>("APPLICANT");
        applicantCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().applicantName()));
        applicantCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                AdminApplicationRowDTO row = getTableView().getItems().get(getIndex());

                Label name = new Label(row.applicantName());
                name.setWrapText(true);
                name.setMaxWidth(520);
                name.setAlignment(Pos.CENTER);
                name.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                name.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #334155;");

                Label meta = new Label(row.jobTitle() + "  |  " + row.applicationId());
                meta.setWrapText(true);
                meta.setMaxWidth(520);
                meta.setAlignment(Pos.CENTER);
                meta.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

                VBox box = new VBox(3, name, meta);
                box.setAlignment(Pos.CENTER);
                setAlignment(Pos.CENTER);
                setGraphic(box);
                setText(null);
            }
        });

        TableColumn<AdminApplicationRowDTO, String> statusCol = new TableColumn<>("STATUS");
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
                Label chip = new Label(item.replace('_', ' '));
                chip.setStyle(statusChipStyle(item));
                setAlignment(Pos.CENTER);
                setGraphic(chip);
                setText(null);
            }
        });

        TableColumn<AdminApplicationRowDTO, Number> matchCol = new TableColumn<>("MATCH");
        matchCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().matchScore()));
        matchCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DisplayPlaceholders.MATCH_VALUE);
                if (!empty) {
                    setAlignment(Pos.CENTER);
                    setStyle("-fx-font-weight: 900; -fx-text-fill: #334155;");
                } else {
                    setStyle("");
                }
                setGraphic(null);
            }
        });

        TableColumn<AdminApplicationRowDTO, String> riskCol = new TableColumn<>("RISK");
        riskCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().riskLevel()));
        riskCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label chip = new Label(item);
                chip.setStyle(riskChipStyle(item));
                setAlignment(Pos.CENTER);
                setGraphic(chip);
                setText(null);
            }
        });

        TableColumn<AdminApplicationRowDTO, String> detailCol = new TableColumn<>("DETAIL");
        detailCol.setCellValueFactory(cell -> new SimpleStringProperty("Detail"));
        detailCol.setCellFactory(column -> new TableCell<>() {
            private final Button detailButton = new Button("Detail");

            {
                detailButton.getStyleClass().add("secondary-button");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                AdminApplicationRowDTO row = getTableView().getItems().get(getIndex());
                detailButton.setOnAction(event -> showApplicationDetailWindow(row));
                setAlignment(Pos.CENTER);
                setGraphic(detailButton);
                setText(null);
            }
        });

        applicantCol.setPrefWidth(520);
        statusCol.setPrefWidth(120);
        matchCol.setPrefWidth(90);
        riskCol.setPrefWidth(100);
        detailCol.setPrefWidth(120);
        table.getColumns().setAll(applicantCol, statusCol, matchCol, riskCol, detailCol);
        table.getStyleClass().add("job-table-spaced");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(78);
        table.setPrefHeight(680);
        table.setPlaceholder(new Label("No applications match the current filters."));

        queuePanel.getChildren().addAll(queueTitle, queueSubtitle, table);
        HBox.setHgrow(queuePanel, Priority.ALWAYS);
        HBox body = new HBox(queuePanel);
        return body;
    }

    private VBox buildReviewWorkspace() {
        VBox content = new VBox(18);
        content.setPadding(new Insets(12));

        HBox heroCard = new HBox();
        heroCard.getStyleClass().add("surface-toolbar");
        heroCard.setAlignment(Pos.CENTER_LEFT);

        VBox heroText = new VBox(6, heroTitle, heroSubtitle);
        heroTitle.setStyle("-fx-font-size: 34px; -fx-font-weight: 900; -fx-text-fill: #1e293b;");
        heroTitle.setWrapText(true);
        heroSubtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b; -fx-font-weight: 600;");
        heroSubtitle.setWrapText(true);

        Region heroSpacer = new Region();
        HBox.setHgrow(heroSpacer, Priority.ALWAYS);

        heroStatus.setStyle("-fx-background-color: #eef2f7; -fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: 900; -fx-background-radius: 999; -fx-padding: 6 12 6 12;");
        heroCard.getChildren().addAll(heroText, heroSpacer, heroStatus);

        VBox basicInfoCard = new VBox(14);
        basicInfoCard.getStyleClass().add("surface-toolbar");
        basicInfoCard.getChildren().addAll(
                sectionTitle("Basic Information"),
                infoGridRow("Full Name", fullNameValue, "Student ID", studentIdValue, "Degree Program", programmeValue),
                infoGridRow("Email", emailValue, "Match Score", matchScoreValue, "Phone", phoneValue),
                infoGridRow("Workload", workloadValue, "Missing Skills", placeholderLabel("See below"), "Statement", placeholderLabel("See below"))
        );

        VBox skillsCard = new VBox(12);
        skillsCard.getStyleClass().add("surface-toolbar");
        skillsCard.getChildren().addAll(sectionTitle("Skills & Competencies"), skillsPane, sectionTitle("Availability"), availabilityPane, sectionTitle("Missing Skills"), missingSkillsPane);

        VBox attachmentCard = new VBox(12);
        attachmentCard.getStyleClass().add("surface-toolbar");
        attachmentCard.setPrefWidth(250);
        attachmentCard.getChildren().addAll(
                sectionTitle("Attachments"),
                attachmentNameValue,
                attachmentMetaValue
        );
        attachmentNameValue.setWrapText(true);
        attachmentNameValue.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: #334155;");
        attachmentMetaValue.setWrapText(true);
        attachmentMetaValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        HBox middleRow = new HBox(16, skillsCard, attachmentCard);
        HBox.setHgrow(skillsCard, Priority.ALWAYS);

        VBox statementCard = new VBox(12);
        statementCard.getStyleClass().add("surface-toolbar");
        statementCard.getChildren().addAll(sectionTitle("Candidate Statement"), statementValue);
        statementValue.setWrapText(true);
        statementValue.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        VBox noteCard = new VBox(12);
        noteCard.getStyleClass().add("surface-toolbar");
        Label noteLabel = new Label("Decision Note");
        noteLabel.getStyleClass().add("field-label");
        decisionNoteInput.setPromptText("Add observations or justification for the recruitment decision...");
        decisionNoteInput.setPrefRowCount(4);
        noteCard.getChildren().addAll(noteLabel, decisionNoteInput);

        openReviewButton.getStyleClass().add("secondary-button");
        openReviewButton.setOnAction(event -> openReview());

        acceptButton.getStyleClass().add("primary-button");
        acceptButton.setStyle("-fx-background-color: #14c7b1; -fx-text-fill: white; -fx-font-weight: 900; -fx-background-radius: 10; -fx-padding: 12 24 12 24;");
        acceptButton.setOnAction(event -> quickAccept());

        rejectButton.getStyleClass().add("danger-outline");
        rejectButton.setStyle("-fx-background-color: white; -fx-border-color: #fca5a5; -fx-text-fill: #ef4444; -fx-font-weight: 900; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 12 24 12 24;");
        rejectButton.setOnAction(event -> quickReject());

        HBox actions = new HBox(14, openReviewButton, acceptButton, rejectButton);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(8, 0, 0, 0));

        content.getChildren().addAll(heroCard, basicInfoCard, middleRow, statementCard, noteCard, actions);
        return content;
    }

    private void refresh() {
        allApplications = services.adminMonitoringService().getApplicationRows();
        submittedValue.setText(String.valueOf(allApplications.stream().filter(app -> "SUBMITTED".equals(app.statusLabel())).count()));
        underReviewValue.setText(String.valueOf(allApplications.stream().filter(app -> "UNDER_REVIEW".equals(app.statusLabel())).count()));
        acceptedValue.setText(String.valueOf(allApplications.stream().filter(app -> "ACCEPTED".equals(app.statusLabel())).count()));
        rejectedValue.setText(String.valueOf(allApplications.stream().filter(app -> "REJECTED".equals(app.statusLabel())).count()));
        applyFilters();
    }

    private void applyFilters() {
        String keyword = normalize(searchField.getText());
        String status = statusFilter.getValue();
        String risk = riskFilter.getValue();

        List<AdminApplicationRowDTO> filtered = allApplications.stream()
                .filter(app -> keyword.isEmpty()
                        || contains(app.applicationId(), keyword)
                        || contains(app.applicantName(), keyword)
                        || contains(app.applicantId(), keyword)
                        || contains(app.studentId(), keyword)
                        || contains(app.jobTitle(), keyword)
                        || contains(app.jobId(), keyword)
                        || contains(app.organiserName(), keyword)
                        || contains(app.organiserId(), keyword))
                .filter(app -> status == null || "ALL STATUS".equals(status) || status.equals(app.statusLabel()))
                .filter(app -> risk == null || "ALL RISK".equals(risk) || risk.equals(app.riskLevel()))
                .toList();

        table.setItems(FXCollections.observableArrayList(filtered));
        if (!filtered.isEmpty()) {
            table.getSelectionModel().selectFirst();
        } else {
            updateActionButtons(null);
            updateDetail(null);
        }
    }

    private void updateActionButtons(AdminApplicationRowDTO application) {
        boolean hasSelection = application != null;
        openReviewButton.setDisable(!hasSelection);
        acceptButton.setDisable(!hasSelection);
        rejectButton.setDisable(!hasSelection);
        decisionNoteInput.setDisable(!hasSelection);
    }

    private void updateDetail(AdminApplicationRowDTO application) {
        if (application == null) {
            heroTitle.setText("Select an Application");
            heroSubtitle.setText("-");
            heroStatus.setText("QUEUE VIEW");
            fullNameValue.setText("-");
            studentIdValue.setText("-");
            programmeValue.setText("-");
            emailValue.setText("-");
            phoneValue.setText("-");
            matchScoreValue.setText("-");
            workloadValue.setText("-");
            statementValue.setText("-");
            attachmentNameValue.setText("-");
            attachmentMetaValue.setText("-");
            decisionNoteInput.clear();
            resetTagPane(skillsPane, List.of("No skills loaded"), true);
            resetTagPane(availabilityPane, List.of("No availability loaded"), true);
            resetTagPane(missingSkillsPane, List.of("No missing skills"), true);
            return;
        }

        ApplicantProfile profile = services.applicantProfileRepository().findById(application.applicantId()).orElse(new ApplicantProfile());
        ResumeInfo resume = services.resumeInfoRepository().findByApplicantId(application.applicantId()).orElse(new ResumeInfo());
        ApplicantReviewDTO reviewData = services.reviewService().getApplicantReviewData(application.applicationId(), user.getUserId(), true);

        heroTitle.setText(application.applicantName());
        heroSubtitle.setText(buildSubtitle(profile, application));
        heroStatus.setText(heroStatusText(application.statusLabel()));
        heroStatus.setStyle(statusChipStyle(application.statusLabel()));

        fullNameValue.setText(blankToDash(profile.getFullName()));
        studentIdValue.setText(blankToDash(profile.getStudentId()));
        programmeValue.setText(blankToDash(profile.getProgramme()) + (profile.getYear() > 0 ? " (Year " + profile.getYear() + ")" : ""));
        emailValue.setText(blankToDash(profile.getEmail()));
        phoneValue.setText(blankToDash(profile.getPhone()));
        matchScoreValue.setText(DisplayPlaceholders.MATCH_VALUE);
        workloadValue.setText("Current " + application.currentWeeklyHours() + "h/week -> Projected "
                + application.projectedWeeklyHours() + "h/week (" + application.riskLevel() + ")");
        statementValue.setText(blankToDash(reviewData.statement()));
        attachmentNameValue.setText(blankToDash(resume.getCvFileName()));
        attachmentMetaValue.setText(buildAttachmentMeta(resume));
        decisionNoteInput.setText("-".equals(application.decisionNote()) ? "" : application.decisionNote());

        resetTagPane(skillsPane, combineSkills(resume), false);
        resetTagPane(availabilityPane, safeList(resume.getAvailability()), true);
        resetTagPane(missingSkillsPane, safeList(reviewData.missingSkills()), true);
    }

    private void resetTagPane(FlowPane pane, List<String> values, boolean mutedIfEmpty) {
        pane.getChildren().clear();
        List<String> safe = safeList(values);
        if (safe.isEmpty()) {
            pane.getChildren().add(chip(mutedIfEmpty ? "Not provided" : "No data", true));
            return;
        }
        for (String value : safe) {
            pane.getChildren().add(chip(value, false));
        }
    }

    private Label chip(String text, boolean muted) {
        Label label = new Label(text);
        label.getStyleClass().add(muted ? "tag-chip-muted" : "tag-chip");
        return label;
    }

    private void openReview() {
        AdminApplicationRowDTO selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select one application first.");
            return;
        }
        openReview(selected);
    }

    private void openReview(AdminApplicationRowDTO selected) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (view.getScene() != null) {
            stage.initOwner(view.getScene().getWindow());
        }
        stage.setTitle("Applicant Review");

        Parent reviewView = new ApplicantReviewController(services, user, selected.applicationId()).getView();
        Scene scene = new Scene(reviewView, 920, 760);
        if (AdminApplicationsController.class.getResource("/styles/app.css") != null) {
            scene.getStylesheets().add(AdminApplicationsController.class.getResource("/styles/app.css").toExternalForm());
        }
        stage.setScene(scene);
        stage.showAndWait();
        refresh();
    }

    private void quickAccept() {
        AdminApplicationRowDTO selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select one application first.");
            return;
        }
        quickAccept(selected, decisionNoteInput.getText(), null);
    }

    private boolean quickAccept(AdminApplicationRowDTO selected, String decisionNote, Stage detailStage) {
        if ("HIGH".equalsIgnoreCase(selected.riskLevel())) {
            DialogControllerFactory.workloadWarning(
                    "Projected hours: " + selected.projectedWeeklyHours() + "h/week.",
                    view.getScene() == null ? null : view.getScene().getWindow());
        }

        boolean confirmed = DialogControllerFactory.confirmAction(
                "Accept Candidate",
                "Accept this applicant and update workload records?",
                view.getScene() == null ? null : view.getScene().getWindow());
        if (!confirmed) {
            return false;
        }

        ValidationResult result = services.reviewService()
                .acceptApplication(selected.applicationId(), user.getUserId(), decisionNote, true);
        boolean success = showActionResult("Accept Application", result);
        if (success && detailStage != null) {
            detailStage.close();
        }
        return success;
    }

    private void quickReject() {
        AdminApplicationRowDTO selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select one application first.");
            return;
        }
        quickReject(selected, decisionNoteInput.getText(), null);
    }

    private boolean quickReject(AdminApplicationRowDTO selected, String decisionNote, Stage detailStage) {
        boolean confirmed = DialogControllerFactory.confirmAction(
                "Reject Candidate",
                "Reject this applicant for the selected job?",
                view.getScene() == null ? null : view.getScene().getWindow());
        if (!confirmed) {
            return false;
        }

        ValidationResult result = services.reviewService()
                .rejectApplication(selected.applicationId(), user.getUserId(), decisionNote, true);
        boolean success = showActionResult("Reject Application", result);
        if (success && detailStage != null) {
            detailStage.close();
        }
        return success;
    }

    private boolean showActionResult(String header, ValidationResult result) {
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed(header, String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return false;
        }
        DialogControllerFactory.success(header, "Operation completed.",
                view.getScene() == null ? null : view.getScene().getWindow());
        refresh();
        return true;
    }

    private Parent infoGridRow(String titleA, Label valueA,
                               String titleB, Label valueB,
                               String titleC, Label valueC) {
        HBox row = new HBox(24,
                infoBlock(titleA, valueA),
                infoBlock(titleB, valueB),
                infoBlock(titleC, valueC)
        );
        return row;
    }

    private VBox infoBlock(String title, Label value) {
        VBox block = new VBox(4);
        Label titleNode = new Label(title);
        titleNode.getStyleClass().add("section-kicker");
        value.setWrapText(true);
        value.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #334155;");
        block.getChildren().addAll(titleNode, value);
        HBox.setHgrow(block, Priority.ALWAYS);
        block.setPrefWidth(220);
        return block;
    }

    private Label placeholderLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        return label;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #334155;");
        return label;
    }

    private String buildSubtitle(ApplicantProfile profile, AdminApplicationRowDTO application) {
        String programme = blankToDash(profile.getProgramme());
        if (!"-".equals(programme) && profile.getYear() > 0) {
            return programme + " (Year " + profile.getYear() + ")";
        }
        return application.jobTitle();
    }

    private String heroStatusText(String status) {
        return switch (status == null ? "" : status) {
            case "ACCEPTED" -> "ACCEPTED CANDIDATE";
            case "REJECTED" -> "REJECTED CANDIDATE";
            default -> "ACTIVE CANDIDATE";
        };
    }

    private String buildAttachmentMeta(ResumeInfo resume) {
        if (resume.getCvFileName() == null || resume.getCvFileName().isBlank()) {
            return "No CV uploaded";
        }
        String size = resume.getCvFileSizeBytes() > 0 ? formatBytes(resume.getCvFileSizeBytes()) : "size unavailable";
        String updated = resume.getCvUploadedAt() == null ? "" : "  |  Uploaded " + resume.getCvUploadedAt().format(TIME_FORMAT);
        return size + updated;
    }

    private List<String> combineSkills(ResumeInfo resume) {
        return safeList(resume.getTechnicalSkills()).stream().limit(6).toList();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(value -> value != null && !value.isBlank()).toList();
    }

    private String formatBytes(long bytes) {
        double mb = bytes / 1024.0 / 1024.0;
        return String.format(Locale.ROOT, "%.1f MB", mb);
    }

    private String statusChipStyle(String status) {
        return switch (status == null ? "" : status) {
            case "ACCEPTED" -> "-fx-background-color: #ecfdf3; -fx-text-fill: #10b981; -fx-font-size: 11px; -fx-font-weight: 900; -fx-background-radius: 999; -fx-padding: 5 10 5 10;";
            case "REJECTED" -> "-fx-background-color: #fff1f2; -fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-font-weight: 900; -fx-background-radius: 999; -fx-padding: 5 10 5 10;";
            case "UNDER_REVIEW" -> "-fx-background-color: #f5f3ff; -fx-text-fill: #8b5cf6; -fx-font-size: 11px; -fx-font-weight: 900; -fx-background-radius: 999; -fx-padding: 5 10 5 10;";
            default -> "-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-font-size: 11px; -fx-font-weight: 900; -fx-background-radius: 999; -fx-padding: 5 10 5 10;";
        };
    }

    private String riskChipStyle(String risk) {
        return switch (risk == null ? "" : risk) {
            case "HIGH" -> "-fx-background-color: #fff1f2; -fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-font-weight: 900; -fx-background-radius: 999; -fx-padding: 4 10 4 10;";
            case "MEDIUM" -> "-fx-background-color: #fff7ed; -fx-text-fill: #f59e0b; -fx-font-size: 11px; -fx-font-weight: 900; -fx-background-radius: 999; -fx-padding: 4 10 4 10;";
            default -> "-fx-background-color: #ecfdf3; -fx-text-fill: #10b981; -fx-font-size: 11px; -fx-font-weight: 900; -fx-background-radius: 999; -fx-padding: 4 10 4 10;";
        };
    }

    private void exportApplications() {
        Path path = services.exportService().exportApplicationReport();
        DialogControllerFactory.info("Application CSV Generated", "Exported: " + path.toAbsolutePath(),
                view.getScene() == null ? null : view.getScene().getWindow());
    }

    private void showError(String message) {
        DialogControllerFactory.validationError(message,
                view.getScene() == null ? null : view.getScene().getWindow());
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

    private void showApplicationDetailWindow(AdminApplicationRowDTO application) {
        if (application == null) {
            showError("Please select one application first.");
            return;
        }

        table.getSelectionModel().select(application);
        updateDetail(application);
        updateActionButtons(application);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (view.getScene() != null) {
            stage.initOwner(view.getScene().getWindow());
        }
        stage.setTitle("Application Detail");

        VBox workspace = buildReviewWorkspace();

        openReviewButton.getStyleClass().add("secondary-button");
        openReviewButton.setOnAction(event -> openReview(application));

        acceptButton.getStyleClass().add("primary-button");
        acceptButton.setStyle("-fx-background-color: #14c7b1; -fx-text-fill: white; -fx-font-weight: 900; -fx-background-radius: 10; -fx-padding: 12 24 12 24;");
        acceptButton.setOnAction(event -> quickAccept(application, decisionNoteInput.getText(), stage));

        rejectButton.getStyleClass().add("danger-outline");
        rejectButton.setStyle("-fx-background-color: white; -fx-border-color: #fca5a5; -fx-text-fill: #ef4444; -fx-font-weight: 900; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 12 24 12 24;");
        rejectButton.setOnAction(event -> quickReject(application, decisionNoteInput.getText(), stage));

        ScrollPane reviewScroll = new ScrollPane(workspace);
        reviewScroll.setFitToWidth(true);
        reviewScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        reviewScroll.getStyleClass().add("detail-scroll");

        VBox reviewPanel = new VBox(reviewScroll);
        reviewPanel.getStyleClass().addAll("app-surface", "panel-card");
        reviewPanel.setPadding(new Insets(16));
        VBox.setVgrow(reviewScroll, Priority.ALWAYS);

        Scene scene = new Scene(reviewPanel, 1120, 840);
        if (AdminApplicationsController.class.getResource("/styles/app.css") != null) {
            scene.getStylesheets().add(AdminApplicationsController.class.getResource("/styles/app.css").toExternalForm());
        }
        stage.setScene(scene);
        stage.showAndWait();
        refresh();
    }
}
