package edu.bupt.ta.controller;

import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.enums.JobType;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class JobEditorController {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");


    public Optional<Job> show(Job source, String organiserId) {
        return show(source, organiserId, List.of());
    }

    public Optional<Job> show(Job source, String organiserId, List<User> organisers) {
        EditorFields fields = new EditorFields();
        fields.defaultOrganiserId = organiserId;
        populateForm(fields, source, organiserId, organisers);

        AtomicReference<Job> result = new AtomicReference<>();

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        Window owner = Window.getWindows().stream().filter(Window::isFocused).findFirst().orElse(null);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle(source == null ? "Create Job Post" : "Edit Job Post");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-surface");

        VBox leftPanel = new VBox(0);
        leftPanel.setPrefWidth(960);
        leftPanel.setMinWidth(900);
        leftPanel.getChildren().add(buildHeader(fields, source, stage, result));
        leftPanel.getChildren().add(buildFormContent(fields, !organisers.isEmpty()));
        HBox.setHgrow(leftPanel, Priority.ALWAYS);

        VBox previewPanel = buildPreviewPanel(fields);

        HBox body = new HBox(leftPanel, previewPanel);
        HBox.setHgrow(leftPanel, Priority.ALWAYS);
        root.setCenter(body);

        Scene scene = new Scene(root, 1280, 800);
        if (JobEditorController.class.getResource("/styles/app.css") != null) {
            String stylesheet = JobEditorController.class.getResource("/styles/app.css").toExternalForm();
            scene.getStylesheets().add(stylesheet);
        }
        stage.setScene(scene);
        stage.showAndWait();

        return Optional.ofNullable(result.get());
    }

    private Parent buildHeader(EditorFields fields, Job source, Stage stage, AtomicReference<Job> result) {
        HBox header = new HBox();
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        Label title = new Label(source == null ? "Create Job Post" : "Edit Job Post");
        title.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("secondary-button");
        cancel.setOnAction(event -> stage.close());

        Button saveDraft = new Button("Save Draft");
        saveDraft.getStyleClass().add("secondary-button");
        saveDraft.setOnAction(event -> submitWithStatus(fields, source, JobStatus.DRAFT, stage, result));

        Button update = new Button(source == null ? "Publish Job" : "Update Job");
        update.getStyleClass().add("primary-button");
        update.setOnAction(event -> submitWithStatus(fields, source, fields.status.getValue(), stage, result));

        header.getChildren().addAll(title, spacer, cancel, saveDraft, update);
        return header;
    }

private Parent buildFormContent(EditorFields fields, boolean showOrganiserField) {
        configureNumericField(fields.positions);
        
        VBox wrapper = new VBox(24);
        wrapper.setPadding(new Insets(24));
        wrapper.setStyle("-fx-background-color: #ffffff;");

        wrapper.getChildren().add(buildSectionTitle("Basic Job Information"));

        Label requiredHint = new Label("* indicates required fields.");
        requiredHint.getStyleClass().add("body-muted");
        wrapper.getChildren().add(requiredHint);

        GridPane basicGrid = new GridPane();
        basicGrid.setHgap(16);
        basicGrid.setVgap(14);
        ColumnConstraints leftColumn = new ColumnConstraints();
        leftColumn.setPercentWidth(50);
        leftColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints rightColumn = new ColumnConstraints();
        rightColumn.setPercentWidth(50);
        rightColumn.setHgrow(Priority.ALWAYS);
        basicGrid.getColumnConstraints().setAll(leftColumn, rightColumn);
        basicGrid.add(field("Job Title", fields.title, true), 0, 0);
        basicGrid.add(field("Module Code", fields.moduleCode, true), 1, 0);
        basicGrid.add(field("Module Name", fields.moduleName, true), 0, 1);
        basicGrid.add(field("Job Type", fields.type), 1, 1);
        basicGrid.add(field("Semester", fields.semester, true), 0, 2);
        basicGrid.add(field("Positions", fields.positions, true), 1, 2);
        basicGrid.add(field("Deadline", deadlineField(fields), true), 0, 3);
        basicGrid.add(field("Publication Status", fields.status), 1, 3);
        int descriptionRow = 4;
        if (showOrganiserField) {
            basicGrid.add(field("Organiser", fields.organiser, true), 0, 4, 2, 1);
            descriptionRow = 5;
        }
        basicGrid.add(areaField("Job Description / Key Responsibilities", fields.description, 6), 0, descriptionRow, 2, 1);

        wrapper.getChildren().add(basicGrid);

        wrapper.getChildren().add(buildSectionTitle("Skills & Requirements"));

        GridPane skillsGrid = new GridPane();
        skillsGrid.setHgap(16);
        skillsGrid.setVgap(14);
        skillsGrid.add(areaField("Required Skills (comma separated)", fields.requiredSkills, 2), 0, 0);
        skillsGrid.add(areaField("Preferred Skills (comma separated)", fields.preferredSkills, 2), 1, 0);

        Label note = new Label("Tip: Include at least one required skill and set the semester clearly.");
        note.getStyleClass().add("body-muted");

        wrapper.getChildren().addAll(skillsGrid, note);
        return wrapper;
    }

    private VBox buildPreviewPanel(EditorFields fields) {
        VBox preview = new VBox(18);
        preview.setPadding(new Insets(24));
        preview.setPrefWidth(320);
        preview.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 1;");

        Label heading = new Label("Live Preview Summary");
        heading.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

        VBox card = new VBox(4);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #354a5f; -fx-background-radius: 12;");

        Label cardCaption = new Label("JOB CARD PREVIEW");
        cardCaption.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #cbd5e1;");

        Label cardTitle = new Label();
        cardTitle.setWrapText(true);
        cardTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: white;");

        Label cardMeta = new Label();
        cardMeta.setWrapText(true);
        cardMeta.setStyle("-fx-font-size: 11px; -fx-text-fill: #e2e8f0;");

        card.getChildren().addAll(cardCaption, cardTitle, cardMeta);

        VBox metrics = new VBox(8);
        Label metricTitle = new Label("KEY METRICS");
        metricTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");

        Label semester = metricLine("Semester", "-");
        Label positions = metricLine("Positions", "-");
        Label deadline = metricLine("Deadline", "-");
        Label organiser = metricLine("Organiser", "-");

        metrics.getChildren().addAll(metricTitle, semester, positions, deadline, organiser);

        VBox requirementCard = new VBox(8);
        requirementCard.setPadding(new Insets(14));
        requirementCard.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-radius: 12;");

        Label requirementTitle = new Label("REQUIREMENTS CHECK");
        requirementTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #354a5f;");

        Label requiredLine = new Label();
        requiredLine.setWrapText(true);
        requiredLine.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        Label preferredLine = new Label();
        preferredLine.setWrapText(true);
        preferredLine.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        requirementCard.getChildren().addAll(requirementTitle, requiredLine, preferredLine);

        preview.getChildren().addAll(heading, card, metrics, requirementCard);

        Runnable updater = () -> updatePreview(fields, cardTitle, cardMeta, semester, positions, deadline, organiser, requiredLine, preferredLine);

        bindPreviewListeners(fields, updater);
        updater.run();

        return preview;
    }

    private VBox buildSectionTitle(String titleText) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(0, 0, 4, 0));
        box.setStyle("-fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;");
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        box.getChildren().add(title);
        return box;
    }

    private VBox field(String labelText, Parent control) {
        return field(labelText, control, false);
    }

    private VBox field(String labelText, Parent control, boolean required) {
        VBox box = new VBox(6);
        HBox labelRow = new HBox(4);
        labelRow.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");

        labelRow.getChildren().add(label);
        if (required) {
            Label requiredMark = new Label("*");
            requiredMark.getStyleClass().add("required-asterisk");
            labelRow.getChildren().add(requiredMark);
        }

        box.getChildren().addAll(labelRow, control);
        VBox.setVgrow(control, Priority.NEVER);
        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox areaField(String labelText, TextArea area, int rows) {
        area.setPrefRowCount(rows);
        area.setWrapText(true);
        return field(labelText, area);
    }


    private Parent deadlineField(EditorFields fields) {
        fields.deadlineHour.getItems().setAll(buildHourOptions());
        fields.deadlineMinute.getItems().setAll(buildMinuteOptions());
        fields.deadlineHour.setPromptText("HH");
        fields.deadlineMinute.setPromptText("MM");
        fields.deadline.setPrefWidth(320);
        fields.deadlineHour.setPrefWidth(140);
        fields.deadlineMinute.setPrefWidth(140);
        HBox row = new HBox(10, fields.deadline, fields.deadlineHour, fields.deadlineMinute);
        HBox.setHgrow(fields.deadline, Priority.ALWAYS);
        return row;
    }

  

    private void populateForm(EditorFields fields, Job source, String organiserId, List<User> organisers) {
        fields.type.getItems().setAll(JobType.values());
        configureTypeSelector(fields.type);
        fields.status.getItems().setAll(JobStatus.values());
        configureStatusSelector(fields.status);
        fields.organiser.getItems().setAll(organisers);
        configureOrganiserSelector(fields.organiser);
        if (source == null) {
            fields.type.setValue(JobType.MODULE_TA);
            fields.status.setValue(JobStatus.OPEN);

            LocalDateTime defaultDeadline = LocalDateTime.now().plusDays(7).withHour(23).withMinute(59).withSecond(0).withNano(0);
            fields.deadline.setValue(defaultDeadline.toLocalDate());
            fields.deadlineHour.setValue(String.format("%02d", defaultDeadline.getHour()));
            fields.deadlineMinute.setValue(String.format("%02d", defaultDeadline.getMinute()));
            fields.semester.setText("Spring Semester 2026");
           
            selectOrganiser(fields.organiser, organiserId);

            return;
        }

        fields.title.setText(source.getTitle());
        fields.moduleCode.setText(source.getModuleCode());
        fields.moduleName.setText(source.getModuleName());
        fields.type.setValue(source.getType());
        fields.semester.setText(source.getSemester() == null ? "" : source.getSemester());
        fields.positions.setText(source.getPositions() <= 0 ? "" : String.valueOf(source.getPositions()));
        if (source.getDeadline() != null) {
            fields.deadline.setValue(source.getDeadline().toLocalDate());
            fields.deadlineHour.setValue(String.format("%02d", source.getDeadline().getHour()));
            fields.deadlineMinute.setValue(String.format("%02d", source.getDeadline().getMinute()));
        }
        fields.status.setValue(source.getStatus());
        fields.description.setText(source.getDescription());
        fields.requiredSkills.setText(String.join(", ", source.getRequiredSkills()));
        fields.preferredSkills.setText(String.join(", ", source.getPreferredSkills()));
        fields.defaultOrganiserId = source.getOrganiserId();
        selectOrganiser(fields.organiser, source.getOrganiserId());
    }

    private void configureStatusSelector(ChoiceBox<JobStatus> statusBox) {
        if (!statusBox.getStyleClass().contains("status-selector")) {
            statusBox.getStyleClass().add("status-selector");
        }
        statusBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(JobStatus object) {
                return object == null ? "" : object.name();
            }

            @Override
            public JobStatus fromString(String string) {
                return string == null || string.isBlank() ? null : JobStatus.valueOf(string);
            }
        });
    }

    private void configureTypeSelector(ChoiceBox<JobType> typeBox) {
        if (!typeBox.getStyleClass().contains("status-selector")) {
            typeBox.getStyleClass().add("status-selector");
        }
        typeBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(JobType object) {
                return object == null ? "" : object.name();
            }

            @Override
            public JobType fromString(String string) {
                return string == null || string.isBlank() ? null : JobType.valueOf(string);
            }
        });
    }

    private void configureOrganiserSelector(ComboBox<User> organiserBox) {
        organiserBox.setPromptText("Select organiser");
        organiserBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatOrganiserLabel(item));
            }
        });
        organiserBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatOrganiserLabel(item));
            }
        });
    }

    private void submitWithStatus(EditorFields fields,
                                  Job source,
                                  JobStatus targetStatus,
                                  Stage stage,
                                  AtomicReference<Job> result) {
        List<String> errors = validate(fields, targetStatus);
        if (!errors.isEmpty()) {
            DialogControllerFactory.validationError(String.join("\n", errors), stage.getOwner());
            return;
        }
        Job job = buildJob(fields, source, targetStatus);
        result.set(job);
        stage.close();
    }

    private List<String> validate(EditorFields fields, JobStatus targetStatus) {
        List<String> errors = new ArrayList<>();
        if (fields.title.getText() == null || fields.title.getText().isBlank()) {
            errors.add("Title is required.");
        }
        if (fields.moduleCode.getText() == null || fields.moduleCode.getText().isBlank()) {
            errors.add("Module code is required.");
        }
        if (fields.moduleName.getText() == null || fields.moduleName.getText().isBlank()) {
            errors.add("Module name is required.");
        }
        if (fields.semester.getText() == null || fields.semester.getText().isBlank()) {
            errors.add("Semester is required.");
        }
        if (parseInt(fields.positions.getText()) <= 0) {
            errors.add("Positions must be greater than 0.");
        }
        if (fields.deadline.getValue() == null) {
            errors.add("Deadline is required.");
        }
        LocalTime deadlineTime = parseTime(fields.deadlineHour.getValue(), fields.deadlineMinute.getValue());
        if (deadlineTime == null) {
            errors.add("Please select both deadline hour and minute.");
        }
        if (targetStatus == JobStatus.OPEN
               && fields.deadline.getValue() != null
               && deadlineTime != null
               && LocalDateTime.of(fields.deadline.getValue(), deadlineTime).isBefore(LocalDateTime.now())) {
            
            errors.add("An OPEN job must have a deadline in the future.");
        }
        String organiserId = resolveOrganiserId(fields);
        if (organiserId == null || organiserId.isBlank()) {
            errors.add("Organiser ID is required.");
        }
        return errors;
    }

    private Job buildJob(EditorFields fields, Job source, JobStatus targetStatus) {
        Job job = new Job();
        if (source != null) {
            job.setJobId(source.getJobId());
            job.setCreatedAt(source.getCreatedAt());
            job.setWeeklyHours(source.getWeeklyHours());
        }
        job.setTitle(trim(fields.title.getText()));
        job.setModuleCode(trim(fields.moduleCode.getText()));
        job.setModuleName(trim(fields.moduleName.getText()));
        job.setSemester(trim(fields.semester.getText()));
        job.setType(fields.type.getValue());
        if (source == null) {
            job.setWeeklyHours(4);
        }
        job.setPositions(parseInt(fields.positions.getText()));
        job.setDeadline(LocalDateTime.of(fields.deadline.getValue(), parseTime(fields.deadlineHour.getValue(), fields.deadlineMinute.getValue())));
        job.setStatus(targetStatus == null ? JobStatus.OPEN : targetStatus);
        job.setDescription(trim(fields.description.getText()));
        job.setRequiredSkills(parseSkills(fields.requiredSkills.getText()));
        job.setPreferredSkills(parseSkills(fields.preferredSkills.getText()));
        job.setOrganiserId(resolveOrganiserId(fields));
        return job;
    }

    private void bindPreviewListeners(EditorFields fields, Runnable updater) {
        fields.title.textProperty().addListener((obs, oldValue, newValue) -> updater.run());
        fields.moduleCode.textProperty().addListener((obs, oldValue, newValue) -> updater.run());
        fields.moduleName.textProperty().addListener((obs, oldValue, newValue) -> updater.run());
        fields.semester.textProperty().addListener((obs, oldValue, newValue) -> updater.run());
        fields.positions.textProperty().addListener((obs, oldValue, newValue) -> updater.run());
        fields.deadline.valueProperty().addListener((obs, oldValue, newValue) -> updater.run());
        fields.deadlineHour.valueProperty().addListener((obs, oldValue, newValue) -> updater.run());
        fields.deadlineMinute.valueProperty().addListener((obs, oldValue, newValue) -> updater.run());
        fields.requiredSkills.textProperty().addListener((obs, oldValue, newValue) -> updater.run());
        fields.preferredSkills.textProperty().addListener((obs, oldValue, newValue) -> updater.run());
        fields.organiser.valueProperty().addListener((obs, oldValue, newValue) -> updater.run());
    }

    private void updatePreview(EditorFields fields,
                               Label cardTitle,
                               Label cardMeta,
                               Label semester,
                               Label positions,
                               Label deadline,
                               Label organiser,
                               Label requiredLine,
                               Label preferredLine) {
        cardTitle.setText(fallback(fields.title.getText(), "Untitled Job"));
        String module = fallback(fields.moduleCode.getText(), "TBD");
        String moduleName = fallback(fields.moduleName.getText(), "Module Name");
        cardMeta.setText(module + " | " + moduleName);

        semester.setText("Semester: " + fallback(fields.semester.getText(), "-"));
        positions.setText("Positions: " + fallback(fields.positions.getText(), "-"));

        deadline.setText("Deadline: " + formatDeadline(fields.deadline.getValue(), parseTime(fields.deadlineHour.getValue(), fields.deadlineMinute.getValue())));

        organiser.setText("Organiser: " + fallback(selectedOrganiserName(fields), "-"));


        List<String> requiredSkills = parseSkills(fields.requiredSkills.getText());
        List<String> preferredSkills = parseSkills(fields.preferredSkills.getText());
        requiredLine.setText(requiredSkills.isEmpty()
                ? "Required skills missing"
                : "Required: " + String.join(", ", capThree(requiredSkills)));
        preferredLine.setText(preferredSkills.isEmpty()
                ? "Preferred skills not set"
                : "Preferred: " + String.join(", ", capThree(preferredSkills)));
    }

    private List<String> capThree(List<String> skills) {
        return skills.stream().limit(3).toList();
    }

    private Label metricLine(String title, String value) {
        Label line = new Label(title + ": " + value);
        line.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;");
        return line;
    }

    private List<String> parseSkills(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String fallback(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private LocalTime parseTime(String hour, String minute) {
        try {
            if (hour == null || minute == null) {
                return null;
            }
            return LocalTime.parse(hour.trim() + ":" + minute.trim(), TIME_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> buildHourOptions() {
        List<String> options = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            options.add(String.format("%02d", hour));
        }
        return options;
    }

    private List<String> buildMinuteOptions() {
        List<String> options = new ArrayList<>();
        for (int minute = 0; minute < 60; minute++) {
            options.add(String.format("%02d", minute));
        }
        return options;
    }

    private void configureNumericField(TextField field) {
        field.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null));
    }

    private String formatDeadline(LocalDate date, LocalTime time) {
        if (date == null) {
            return "-";
        }
        if (time == null) {
            return date.toString();
        }
        return LocalDateTime.of(date, time).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
    private void selectOrganiser(ComboBox<User> organiserBox, String organiserId) {
        if (organiserId == null || organiserId.isBlank()) {
            if (!organiserBox.getItems().isEmpty()) {
                organiserBox.getSelectionModel().selectFirst();
            }
            return;
        }
        boolean matched = organiserBox.getItems().stream()
                .filter(user -> organiserId.equals(user.getUserId()))
                .findFirst()
                .map(user -> {
                    organiserBox.getSelectionModel().select(user);
                    return true;
                })
                .orElse(false);
        if (!matched && !organiserBox.getItems().isEmpty()) {
            organiserBox.getSelectionModel().selectFirst();
        }
    }

    private String resolveOrganiserId(EditorFields fields) {
        User selected = fields.organiser.getValue();
        if (selected != null && selected.getUserId() != null && !selected.getUserId().isBlank()) {
            return selected.getUserId();
        }
        return fields.defaultOrganiserId;
    }

    private String selectedOrganiserName(EditorFields fields) {
        User selected = fields.organiser.getValue();
        if (selected != null) {
            return formatOrganiserLabel(selected);
        }
        return fields.defaultOrganiserId;
    }

    private String formatOrganiserLabel(User organiser) {
        String displayName = organiser.getDisplayName();
        return (displayName == null || displayName.isBlank() ? organiser.getUserId() : displayName)
                + " (" + organiser.getUserId() + ")";
    }

    private static class EditorFields {
        private final TextField title = new TextField();
        private final TextField moduleCode = new TextField();
        private final TextField moduleName = new TextField();
        private final ChoiceBox<JobType> type = new ChoiceBox<>();
        private final TextField semester = new TextField();
        private final TextField positions = new TextField();
        private final DatePicker deadline = new DatePicker();
        private final ComboBox<String> deadlineHour = new ComboBox<>();
        private final ComboBox<String> deadlineMinute = new ComboBox<>();
        private final ChoiceBox<JobStatus> status = new ChoiceBox<>();
        private final TextArea description = new TextArea();
        private final TextArea requiredSkills = new TextArea();
        private final TextArea preferredSkills = new TextArea();
        private final ComboBox<User> organiser = new ComboBox<>();
        private String defaultOrganiserId;
    }
}
