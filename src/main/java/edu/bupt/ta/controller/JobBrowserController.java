package edu.bupt.ta.controller;

import edu.bupt.ta.dto.JobSearchCriteria;
import edu.bupt.ta.dto.MatchExplanationDTO;
import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.enums.JobType;
import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.ui.IconFactory;
import edu.bupt.ta.util.ValidationResult;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class JobBrowserController {
    private static final DateTimeFormatter CARD_DEADLINE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final double OPEN_POSITIONS_PANEL_WIDTH = 392;


    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    private final TextField keywordField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ComboBox<String> moduleCodeFilter = new ComboBox<>();
    private final ComboBox<String> jobTypeFilter = new ComboBox<>();
    private final ComboBox<String> skillsFilter = new ComboBox<>();
    private final ComboBox<String> deadlineFilter = new ComboBox<>();
    private final ListView<JobWithApplication> jobList = new ListView<>();
    private final JobDetailController jobDetailController;

    public JobBrowserController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        this.jobDetailController = new JobDetailController(services);
        initialize();
        loadJobs();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");
        view.setTop(buildFilters());

        HBox content = new HBox(0);
        content.setPadding(Insets.EMPTY);

        VBox listPanel = new VBox(16);
        listPanel.setPrefWidth(OPEN_POSITIONS_PANEL_WIDTH);
        listPanel.setMinWidth(OPEN_POSITIONS_PANEL_WIDTH);
        listPanel.setMaxWidth(OPEN_POSITIONS_PANEL_WIDTH);
        listPanel.setPadding(new Insets(20, 16, 20, 20));
        listPanel.getStyleClass().add("open-positions-panel");

        Label listTitle = new Label("Open Positions");
        listTitle.getStyleClass().add("section-title");

        jobList.getStyleClass().add("job-list");
        jobList.setCellFactory(param -> new JobCardCell());
        jobList.setPrefHeight(760);
        VBox.setVgrow(jobList, Priority.ALWAYS);

        listPanel.getChildren().addAll(listTitle, jobList);

        HBox.setHgrow(jobDetailController.getView(), Priority.ALWAYS);
        content.getChildren().addAll(listPanel, jobDetailController.getView());
        view.setCenter(content);

        jobList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                jobDetailController.setJobWithApplicationStatus(newValue.job, getCurrentApplicantId(), newValue.appStatus);
                updateMatchExplanation(newValue.job);
            } else {
                jobDetailController.setJob(null);
                updateMatchExplanation(null);
            }
        });

        jobDetailController.setOnApply(() -> {
            JobWithApplication selected = jobList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            Optional<String> applicantIdOpt = services.applicantProfileRepository()
                    .findByUserId(user.getUserId())
                    .map(profile -> profile.getApplicantId());
            if (applicantIdOpt.isEmpty()) {
                DialogControllerFactory.permissionDenied(
                        "Profile not found for current TA account. Please complete your profile first.",
                        view.getScene() == null ? null : view.getScene().getWindow());
                return;
            }
            Optional<ApplicantProfile> profileOpt = services.applicantProfileRepository()
                    .findById(applicantIdOpt.get());
            if (profileOpt.isEmpty()) {
                DialogControllerFactory.permissionDenied(
                        "Profile not found for current TA account. Please complete your profile first.",
                        view.getScene() == null ? null : view.getScene().getWindow());
                return;
            }
            Optional<String> statementOpt = JobApplyDialog.showAndWait(
                    selected.job,
                    profileOpt.get(),
                    services.resumeService(),
                    applicantIdOpt.get(),
                    view.getScene() == null ? null : view.getScene().getWindow());
            statementOpt.ifPresent(statement -> applyToJob(selected.job, statement));
        });

        jobDetailController.setOnCancel(applicantId -> {
            JobWithApplication selected = jobList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                cancelApplication(selected.job);
            }
        });

        if (user.getRole() != Role.TA) {
            jobDetailController.setOnApply(() -> {
            });
            jobDetailController.setOnCancel(applicantId -> {
            });
            jobDetailController.setMatchExplanation(null);
        }
    }

    // 获取当前申请者ID
    private String getCurrentApplicantId() {
        if (user.getRole() != Role.TA) return null;

        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        return applicantIdOpt.orElse(null);
    }

    private Parent buildFilters() {
        VBox wrapper = new VBox();
        wrapper.getStyleClass().add("job-browser-top");

        VBox headerSection = new VBox(14);
        headerSection.getStyleClass().add("job-browser-header");

        Label heading = new Label("Browse Opportunities");
        heading.getStyleClass().add("job-browser-title");

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        HBox keywordShell = new HBox(12);
        keywordShell.setAlignment(Pos.CENTER_LEFT);
        keywordShell.getStyleClass().add("job-browser-search-shell");

        keywordField.setPromptText("Search by Keyword (e.g. CS101, Python, Web Dev)");
        keywordField.getStyleClass().add("job-browser-keyword-field");
        keywordField.setOnAction(event -> loadJobs());
        HBox.setHgrow(keywordField, Priority.ALWAYS);
        keywordShell.getChildren().addAll(
                IconFactory.glyph(IconFactory.IconType.SEARCH, 18, Color.web("#94a3b8")),
                keywordField);

        Button searchButton = new Button("SEARCH");
        searchButton.getStyleClass().add("job-browser-search-button");
        searchButton.setOnAction(event -> loadJobs());

        HBox.setHgrow(keywordShell, Priority.ALWAYS);
        searchRow.getChildren().addAll(keywordShell, searchButton);

        // --- Status Filter ---
        statusFilter.getItems().setAll("Status", "OPEN", "CLOSED", "EXPIRED", "ACCEPTED");
        statusFilter.setValue("Status");
        statusFilter.getStyleClass().add("job-browser-filter-pill");
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) loadJobs();
        });

        // --- Job Type Filter ---
        jobTypeFilter.getItems().add("Job Type");
        for (JobType type : JobType.values()) {
            jobTypeFilter.getItems().add(type.name());
        }
        jobTypeFilter.setValue("Job Type");
        jobTypeFilter.getStyleClass().add("job-browser-filter-pill");
        jobTypeFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) loadJobs();
        });

        // --- Module Code Filter (dynamic) ---
        moduleCodeFilter.getItems().add("Module Code");
        services.jobService().searchJobs(new JobSearchCriteria()).stream()
                .map(Job::getModuleCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .sorted()
                .forEach(code -> moduleCodeFilter.getItems().add(code));
        moduleCodeFilter.setValue("Module Code");
        moduleCodeFilter.getStyleClass().add("job-browser-filter-pill");
        moduleCodeFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) loadJobs();
        });

        // --- Skills Filter (dynamic) ---
        skillsFilter.getItems().add("Skills");
        services.jobService().searchJobs(new JobSearchCriteria()).stream()
                .flatMap(job -> {
                    List<String> combined = new ArrayList<>();
                    if (job.getRequiredSkills() != null) combined.addAll(job.getRequiredSkills());
                    if (job.getPreferredSkills() != null) combined.addAll(job.getPreferredSkills());
                    return combined.stream();
                })
                .filter(skill -> skill != null && !skill.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .forEach(skill -> skillsFilter.getItems().add(skill));
        skillsFilter.setValue("Skills");
        skillsFilter.getStyleClass().add("job-browser-filter-pill");
        skillsFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) loadJobs();
        });

        // --- Deadline Filter ---
        deadlineFilter.getItems().setAll("Deadline", "Within 7 days", "Within 30 days", "Within 90 days");
        deadlineFilter.setValue("Deadline");
        deadlineFilter.getStyleClass().add("job-browser-filter-pill");
        deadlineFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) loadJobs();
        });

        // --- CLEAR FILTERS ---
        Button clear = new Button("CLEAR FILTERS");
        clear.getStyleClass().add("job-browser-clear-filters");
        clear.setGraphic(IconFactory.glyph(IconFactory.IconType.FILTER, 11, Color.web("#ef4444")));
        clear.setContentDisplay(ContentDisplay.LEFT);
        clear.setGraphicTextGap(6);
        clear.setOnAction(event -> clearFilters());

        Region divider = new Region();
        divider.getStyleClass().add("job-browser-filter-divider");
        divider.setMinSize(1, 20);
        divider.setPrefSize(1, 20);
        divider.setMaxSize(1, 20);

        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("job-browser-filter-row");
        filters.getChildren().addAll(
                moduleCodeFilter, jobTypeFilter,
                skillsFilter, deadlineFilter, statusFilter, divider, clear);

        headerSection.getChildren().addAll(heading, searchRow);
        wrapper.getChildren().addAll(headerSection, filters);
        return wrapper;
    }

    private void loadJobs() {
        JobSearchCriteria criteria = new JobSearchCriteria();
        criteria.setKeyword(keywordField.getText());

        // --- Module Code ---
        String selectedModuleCode = moduleCodeFilter.getValue();
        if (selectedModuleCode != null && !"Module Code".equals(selectedModuleCode)) {
            criteria.setModuleCode(selectedModuleCode);
        }

        // --- Job Type ---
        String selectedJobType = jobTypeFilter.getValue();
        if (selectedJobType != null && !"Job Type".equals(selectedJobType)) {
            criteria.setType(JobType.valueOf(selectedJobType));
        }

        // --- Skills ---
        String selectedSkill = skillsFilter.getValue();
        if (selectedSkill != null && !"Skills".equals(selectedSkill)) {
            criteria.setRequiredSkill(selectedSkill);
        }

        // --- Status ---
        String selectedStatus = statusFilter.getValue();
        if (selectedStatus != null && !"Status".equals(selectedStatus)) {
            if ("ACCEPTED".equals(selectedStatus)) {
                // ACCEPTED 是申请状态，不走 JobStatus 精确匹配，在后置过滤中处理
            } else {
                criteria.setStatus(JobStatus.valueOf(selectedStatus));
            }
        }

        // 基础搜索
        List<Job> jobs = services.jobService().searchJobs(criteria);

        // MO 角色只显示自己发布的岗位
        if (user.getRole() == Role.MO) {
            jobs = jobs.stream().filter(job -> user.getUserId().equals(job.getOrganiserId())).toList();
        }

        // 始终过滤掉 DRAFT 状态
        jobs = jobs.stream()
                .filter(job -> job.getStatus() != JobStatus.DRAFT)
                .collect(java.util.stream.Collectors.toList());

        // --- Deadline 后置过滤 ---
        String selectedDeadline = deadlineFilter.getValue();
        if (selectedDeadline != null && !"Deadline".equals(selectedDeadline)) {
            LocalDateTime now = LocalDateTime.now();
            jobs = jobs.stream().filter(job -> {
                LocalDateTime dl = job.getDeadline();
                if (dl == null) return false;
                long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(now, dl);
                return switch (selectedDeadline) {
                    case "Within 7 days" -> daysUntil >= 0 && daysUntil <= 7;
                    case "Within 30 days" -> daysUntil >= 0 && daysUntil <= 30;
                    case "Within 90 days" -> daysUntil >= 0 && daysUntil <= 90;
                    default -> true;
                };
            }).collect(java.util.stream.Collectors.toList());
        }

        // --- ACCEPTED 后置过滤：仅保留当前 TA 申请状态为 ACCEPTED 的岗位 ---
        if ("ACCEPTED".equals(selectedStatus)) {
            jobs = filterJobsByAcceptedApplication(jobs);
        }

        // 获取申请状态进行排序
        List<JobWithApplication> jobsWithApp = new ArrayList<>();
        for (Job job : jobs) {
            ApplicationStatus appStatus = getApplicationStatusForJob(job);
            jobsWithApp.add(new JobWithApplication(job, appStatus));
        }

        // 按优先级排序：Accepted > Apply > Rejected > Expired > Closed，同档内按标题
        jobsWithApp.sort(
                Comparator.comparingInt((JobWithApplication jwa) -> getSortOrder(jwa.job(), jwa.appStatus()))
                        .thenComparing(jwa -> jwa.job().getTitle(), String.CASE_INSENSITIVE_ORDER));

        jobList.setItems(FXCollections.observableArrayList(jobsWithApp));

        if (!jobs.isEmpty()) {
            // Delay selection to let cells finish updateItem first, avoiding
            // padding jump when previously selected card becomes unselected.
            Platform.runLater(() -> {
                jobList.getSelectionModel().selectFirst();
                JobWithApplication first = jobList.getSelectionModel().getSelectedItem();
                if (first != null) {
                    jobDetailController.setJobWithApplicationStatus(first.job, getCurrentApplicantId(), first.appStatus);
                    updateMatchExplanation(first.job);
                }
            });
        } else {
            jobDetailController.setJob(null);
            jobDetailController.setMatchExplanation(null);
        }
    }

    /**
     * 仅保留当前 TA 申请状态为 ACCEPTED 的岗位；非 TA 或无档案时返回空列表。
     */
    private List<Job> filterJobsByAcceptedApplication(List<Job> jobs) {
        if (user.getRole() != Role.TA) return new ArrayList<>();
        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());
        if (applicantIdOpt.isEmpty()) return new ArrayList<>();
        String applicantId = applicantIdOpt.get();
        return jobs.stream()
                .filter(job -> services.applicationService()
                        .getApplicationStatus(applicantId, job.getJobId())
                        .map(s -> s == ApplicationStatus.ACCEPTED)
                        .orElse(false))
                .collect(java.util.stream.Collectors.toList());
    }

    private void clearFilters() {
        keywordField.clear();
        moduleCodeFilter.setValue("Module Code");
        jobTypeFilter.setValue("Job Type");
        skillsFilter.setValue("Skills");
        deadlineFilter.setValue("Deadline");
        statusFilter.setValue("Status");
        loadJobs();
    }

    // 辅助类：包含岗位和申请状态
    private record JobWithApplication(Job job, ApplicationStatus appStatus) {}

    // 获取岗位的申请状态
    private ApplicationStatus getApplicationStatusForJob(Job job) {
        if (user.getRole() != Role.TA) return null;

        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        if (applicantIdOpt.isEmpty()) return null;

        return services.applicationService().getApplicationStatus(applicantIdOpt.get(), job.getJobId())
                .orElse(null);
    }

    /**
     * 列表排序档位：Accepted → Apply → Rejected → Expired → Closed（数字 0–4）。
     * Apply：已投递、审核中、已取消申请，或可申请（无申请且岗位为 OPEN）。
     */
    private int getSortOrder(Job job, ApplicationStatus appStatus) {
        if (appStatus == ApplicationStatus.ACCEPTED) {
            return 0;
        }
        if (appStatus == ApplicationStatus.REJECTED) {
            return 2;
        }
        if (appStatus == ApplicationStatus.SUBMITTED
                || appStatus == ApplicationStatus.UNDER_REVIEW
                || appStatus == ApplicationStatus.CANCELLED) {
            return 1;
        }
        // 无申请记录：按岗位状态分档
        return switch (job.getStatus()) {
            case OPEN -> 1;
            case EXPIRED -> 3;
            case CLOSED -> 4;
            case DRAFT -> 5;
        };
    }

    private void applyToJob(Job job, String statement) {
        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        if (applicantIdOpt.isEmpty()) {
            DialogControllerFactory.permissionDenied(
                    "Profile not found for current TA account. Please complete your profile first.",
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        ValidationResult result = services.applicationService().apply(applicantIdOpt.get(), job.getJobId(), statement);
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed("Apply Failed", String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        DialogControllerFactory.success("Apply Success", "Application submitted successfully.",
                view.getScene() == null ? null : view.getScene().getWindow());
        loadJobs();
    }

    private void cancelApplication(Job job) {
        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        if (applicantIdOpt.isEmpty()) {
            return;
        }

        ValidationResult result = services.applicationService().cancelApplication(applicantIdOpt.get(), job.getJobId());
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed("Cancel Failed", String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        DialogControllerFactory.success("Cancel Success", "Application cancelled successfully.",
                view.getScene() == null ? null : view.getScene().getWindow());
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

    private static class JobCardCell extends ListCell<JobWithApplication> {
        private static final String TITLE_SELECTED_STYLE = "-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #0f172a;";
        private static final String TITLE_DEFAULT_STYLE = "-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #0f172a;";
        private static final String MODULE_SELECTED_STYLE = "-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #00c29f;";
        private static final String MODULE_DEFAULT_STYLE = "-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #64748b;";
        private static final String FOOTER_TEXT_STYLE = "-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;";

        private final VBox card = new VBox(0);
        private final HBox heading = new HBox(10);
        private final Label title = new Label();
        private final Label status = new Label();
        private final Label module = new Label();
        private final HBox chipRow = new HBox(8);
        private final Region footerDivider = new Region();
        private final HBox footerRow = new HBox(8);
        private final HBox postsGroup = new HBox(8);
        private final HBox deadlineGroup = new HBox(8);
        private final Label posts = new Label();
        private final Label deadline = new Label();
        private final Region footerSpacer = new Region();

        private String fullTitle = "";
        private String statusText = "";

        private JobCardCell() {
            setText(null);
            setPrefWidth(0);
            setMinWidth(0);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setStyle("-fx-background-color: transparent; -fx-padding: 0 0 16 0; -fx-background-insets: 0; -fx-border-color: transparent; -fx-border-width: 0;");

            card.getStyleClass().add("open-position-card");
            card.setPadding(new Insets(14, 18, 14, 18));
            card.setMinWidth(0);
            card.prefWidthProperty().bind(Bindings.max(0, widthProperty().subtract(14)));
            card.minWidthProperty().bind(card.prefWidthProperty());
            card.maxWidthProperty().bind(card.prefWidthProperty());

            title.setWrapText(true);
            title.setTextOverrun(OverrunStyle.CLIP);
            title.setEllipsisString("");
            title.setMinWidth(0);
            title.setMaxWidth(Double.MAX_VALUE);
            title.setMaxHeight(Double.MAX_VALUE);

            status.setVisible(true);
            status.setManaged(true);
            status.setMinWidth(Region.USE_PREF_SIZE);
            status.setMaxWidth(Region.USE_PREF_SIZE);

            heading.setMaxWidth(Double.MAX_VALUE);
            heading.setAlignment(Pos.TOP_LEFT);
            HBox.setHgrow(title, Priority.ALWAYS);
            heading.getChildren().setAll(title, status);

            module.setWrapText(true);
            module.setTextOverrun(OverrunStyle.CLIP);
            module.setEllipsisString("");
            module.setMinWidth(0);
            module.setMaxWidth(Double.MAX_VALUE);
            module.setMaxHeight(Double.MAX_VALUE);
            VBox.setMargin(module, new Insets(4, 0, 0, 0));

            chipRow.setAlignment(Pos.CENTER_LEFT);
            chipRow.setPadding(new Insets(6, 0, 0, 0));

            footerDivider.setPrefHeight(1);
            footerDivider.setMinHeight(1);
            footerDivider.setMaxHeight(1);
            footerDivider.setStyle("-fx-background-color: #f8fafc;");
            footerDivider.setVisible(true);
            footerDivider.setManaged(true);

            posts.setStyle(FOOTER_TEXT_STYLE);
            deadline.setStyle(FOOTER_TEXT_STYLE);

            postsGroup.setAlignment(Pos.CENTER_LEFT);
            deadlineGroup.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(footerSpacer, Priority.ALWAYS);

            footerRow.setPadding(new Insets(8, 0, 0, 0));
            footerRow.setAlignment(Pos.CENTER_LEFT);
            footerRow.getChildren().addAll(postsGroup, footerSpacer, deadlineGroup);

            card.getChildren().addAll(heading, module, chipRow, footerDivider, footerRow);
        }

        @Override
        protected void updateItem(JobWithApplication item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                fullTitle = "";
                statusText = "";
                return;
            }

            Job job = item.job();
            ApplicationStatus appStatus = item.appStatus();

            statusText = getDynamicStatusText(job, appStatus);
            status.setText(statusText == null ? "" : statusText.toUpperCase(Locale.ENGLISH));
            status.setStyle(getDynamicStatusStyle(statusText));
            status.setVisible(true);
            status.setManaged(true);

            fullTitle = job.getTitle() == null ? "-" : job.getTitle();
            title.setText(fullTitle);

            boolean selected = isSelected();
            title.setStyle(selected ? TITLE_SELECTED_STYLE : TITLE_DEFAULT_STYLE);
            module.setStyle(selected ? MODULE_SELECTED_STYLE : MODULE_DEFAULT_STYLE);
            module.setText(moduleText(job));

            refreshSkillChips(job, selected);
            refreshFooter(job, selected);
            applyCardSelection(selected);

            setGraphic(card);
            setText(null);
        }

        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            if (getItem() == null) {
                return;
            }
            status.setVisible(true);
            status.setManaged(true);
            title.setStyle(selected ? TITLE_SELECTED_STYLE : TITLE_DEFAULT_STYLE);
            module.setStyle(selected ? MODULE_SELECTED_STYLE : MODULE_DEFAULT_STYLE);
            module.setText(moduleText(getItem().job()));
            refreshSkillChips(getItem().job(), selected);
            refreshFooter(getItem().job(), selected);
            applyCardSelection(selected);
        }

        private void applyCardSelection(boolean selected) {
            card.getStyleClass().setAll("open-position-card");
            if (selected) {
                card.getStyleClass().add("open-position-card-selected");
                card.setPadding(new Insets(13, 17, 13, 17));
            } else {
                card.setPadding(new Insets(14, 18, 14, 18));
            }
        }

        private void refreshSkillChips(Job job, boolean selected) {
            chipRow.getChildren().clear();
            List<String> skillTags = collectSkillTags(job);
            if (skillTags.isEmpty()) {
                chipRow.setVisible(false);
                chipRow.setManaged(false);
                return;
            }
            chipRow.setVisible(true);
            chipRow.setManaged(true);
            for (String skill : skillTags) {
                Label chip = new Label(skill.toUpperCase(Locale.ENGLISH));
                chip.setStyle(selected
                        ? "-fx-background-color: #f1f5f9; -fx-border-color: #f1f5f9; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5 11 5 11; -fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #475569;"
                        : "-fx-background-color: #f8fafc; -fx-border-color: #f1f5f9; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5 11 5 11; -fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #64748b;");
                chipRow.getChildren().add(chip);
            }
        }

        private void refreshFooter(Job job, boolean selected) {
            posts.setText(job.getPositions() == 1 ? "1 Post" : job.getPositions() + " Posts");
            deadline.setText(formatCardDeadline(job.getDeadline()));

            Color postsIconColor = selected ? Color.web("#00c29f") : Color.web("#94a3b8");
            Color dateIconColor = Color.web("#94a3b8");

            postsGroup.getChildren().setAll(
                    IconFactory.glyph(IconFactory.IconType.USERS, 13, postsIconColor),
                    posts
            );
            deadlineGroup.getChildren().setAll(
                    IconFactory.glyph(IconFactory.IconType.CALENDAR, 13, dateIconColor),
                    deadline
            );
        }

        private static String moduleText(Job job) {
            return fallback(job.getModuleCode()) + " • " + fallback(job.getModuleName());
        }

        private static String fallback(String value) {
            return value == null || value.isBlank() ? "-" : value;
        }

        private static List<String> collectSkillTags(Job job) {
            Set<String> skills = new LinkedHashSet<>();
            if (job.getRequiredSkills() != null) {
                for (String skill : job.getRequiredSkills()) {
                    if (skill != null && !skill.isBlank()) {
                        skills.add(skill.trim());
                    }
                }
            }
            if (job.getPreferredSkills() != null) {
                for (String skill : job.getPreferredSkills()) {
                    if (skill != null && !skill.isBlank()) {
                        skills.add(skill.trim());
                    }
                }
            }
            return skills.stream().limit(2).toList();
        }

        private String getDynamicStatusText(Job job, ApplicationStatus appStatus) {
            if (appStatus != null) {
                return switch (appStatus) {
                    case ACCEPTED -> "Accepted";
                    case SUBMITTED -> "Applied";
                    case CANCELLED -> "Cancelled";
                    case UNDER_REVIEW -> "Under Review";
                    case REJECTED -> "Rejected";
                };
            }
            // 无申请时显示岗位状态
            return switch (job.getStatus()) {
                case OPEN -> "Open";
                case CLOSED -> "Closed";
                case EXPIRED -> "Expired";
                case DRAFT -> "Draft";
            };
        }

        private String getDynamicStatusStyle(String statusText) {
            String color = switch (statusText) {
                case "Accepted" -> "#047857;#ecfdf5";
                case "Applied", "Under Review" -> "#1d4ed8;#eff6ff";
                case "Rejected" -> "#b91c1c;#fef2f2";
                case "Cancelled" -> "#b45309;#fffbeb";
                case "Open" -> "#ffffff;#00c29f";
                case "Closed", "Expired" -> "#64748b;#f1f5f9";
                default -> "#64748b;#f1f5f9";
            };
            String[] parts = color.split(";");
            return String.format("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: %s; -fx-background-color: %s; -fx-background-radius: 4; -fx-padding: 2 8 2 8; -fx-letter-spacing: 0.4px;", parts[0], parts[1]);
        }
    }

    private static String formatCardDeadline(LocalDateTime deadline) {
        return deadline == null ? "-" : deadline.format(CARD_DEADLINE_FORMAT);
    }
}
