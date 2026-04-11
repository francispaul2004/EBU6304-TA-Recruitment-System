package edu.bupt.ta.controller;

import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.ui.IconFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.stream.Collectors;

public class MOProfileController {

    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    // Edit fields
    private final TextField titleField       = new TextField();
    private final TextField displayNameField = new TextField();
    private final TextField departmentField  = new TextField();
    private final TextField contactEmailField = new TextField();

    // Read-only labels
    private final Label titleValue       = readOnlyValue();
    private final Label displayNameValue = readOnlyValue();
    private final Label departmentValue  = readOnlyValue();
    private final Label contactEmailValue = readOnlyValue();

    // Card body — swapped between read/edit mode
    private final VBox cardBody = new VBox(20);

    // Header buttons
    private final Button editBtn  = new Button("Edit");
    private final Button resetBtn = new Button("Reset");
    private final Button saveBtn  = new Button("Save Changes");

    private boolean editing = false;

    public MOProfileController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        VBox page = new VBox(24);
        page.getStyleClass().add("app-surface");
        page.setPadding(new Insets(24));
        page.setFillWidth(true);

        page.getChildren().addAll(
                buildTitleBlock(),
                buildInfoCard(),
                buildCoursesCard()
        );

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        view.setCenter(scroll);
    }

    // ── Title block ──────────────────────────────────────────────────────────

    private VBox buildTitleBlock() {
        Label heading = new Label("My Profile");
        heading.getStyleClass().add("page-title");

        Label subtitle = new Label("Manage your personal information and course assignments.");
        subtitle.getStyleClass().add("body-muted");
        subtitle.setStyle("-fx-font-size: 15px;");

        return new VBox(4, heading, subtitle);
    }

    // ── Info card ────────────────────────────────────────────────────────────

    private VBox buildInfoCard() {
        VBox card = new VBox(20);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(24));

        card.getChildren().addAll(buildCardHeader(), buildAvatarRow(), cardBody);

        loadFields();
        renderReadMode();
        return card;
    }

    private HBox buildCardHeader() {
        Label cardTitle = new Label("Basic Information");
        cardTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        editBtn.getStyleClass().add("secondary-button");
        editBtn.setOnAction(e -> enterEditMode());

        resetBtn.getStyleClass().add("secondary-button");
        resetBtn.setOnAction(e -> {
            loadFields();          // restore to saved values
        });

        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setOnAction(e -> saveProfile());

        HBox header = new HBox(8, cardTitle, spacer, editBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private HBox buildAvatarRow() {
        Label avatar = new Label(initials());
        avatar.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: white;"
                + "-fx-background-color: #354a5f; -fx-background-radius: 999;"
                + "-fx-min-width: 72; -fx-min-height: 72; -fx-pref-width: 72; -fx-pref-height: 72;"
                + "-fx-alignment: center;");

        Label nameLabel = new Label(safe(user.getDisplayName(), "Module Organiser"));
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Label roleLabel = new Label("Module Organiser  ·  " + safe(user.getTitle(), "No title set"));
        roleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        VBox meta = new VBox(4, nameLabel, roleLabel);
        HBox row = new HBox(16, avatar, meta);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ── Read mode ────────────────────────────────────────────────────────────

    private void renderReadMode() {
        editing = false;
        refreshReadLabels();

        GridPane grid = new GridPane();
        grid.setHgap(32);
        grid.setVgap(16);

        grid.add(infoCell("Academic Title",  titleValue),        0, 0);
        grid.add(infoCell("Full Name",        displayNameValue),  1, 0);
        grid.add(infoCell("Department",       departmentValue),   0, 1);
        grid.add(infoCell("Contact Email",    contactEmailValue), 1, 1);

        cardBody.getChildren().setAll(grid);

        // Header: show only Edit button
        HBox header = (HBox) ((VBox) cardBody.getParent()).getChildren().get(0);
        header.getChildren().setAll(
                header.getChildren().get(0), // cardTitle label
                header.getChildren().get(1), // spacer
                editBtn
        );
    }

    // ── Edit mode ────────────────────────────────────────────────────────────

    private void enterEditMode() {
        editing = true;
        loadFields();

        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(14);

        form.add(formField("Academic Title",  titleField),        0, 0);
        form.add(formField("Full Name",        displayNameField),  1, 0);
        form.add(formField("Department",       departmentField),   0, 1);
        form.add(formField("Contact Email",    contactEmailField), 1, 1);

        cardBody.getChildren().setAll(form);

        // Header: show Reset + Save buttons
        HBox header = (HBox) ((VBox) cardBody.getParent()).getChildren().get(0);
        header.getChildren().setAll(
                header.getChildren().get(0), // cardTitle label
                header.getChildren().get(1), // spacer
                resetBtn,
                saveBtn
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private VBox infoCell(String label, Label value) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("field-label");
        VBox cell = new VBox(4, lbl, value);
        cell.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(cell, Priority.ALWAYS);
        return cell;
    }

    private VBox formField(String label, TextField input) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("field-label");
        input.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(6, lbl, input);
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private static Label readOnlyValue() {
        Label lbl = new Label("-");
        lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #0f172a;");
        lbl.setWrapText(true);
        return lbl;
    }

    private void refreshReadLabels() {
        titleValue.setText(safe(user.getTitle(), "-"));
        displayNameValue.setText(safe(user.getDisplayName(), "-"));
        departmentValue.setText(safe(user.getDepartment(), "-"));
        contactEmailValue.setText(safe(user.getContactEmail(), "-"));
    }

    private void loadFields() {
        titleField.setText(safe(user.getTitle(), ""));
        displayNameField.setText(safe(user.getDisplayName(), ""));
        departmentField.setText(safe(user.getDepartment(), ""));
        contactEmailField.setText(safe(user.getContactEmail(), ""));
    }

    private void saveProfile() {
        String name = displayNameField.getText().trim();
        if (name.isBlank()) {
            DialogControllerFactory.validationError("Full name is required.",
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }
        user.setTitle(titleField.getText().trim());
        user.setDisplayName(name);
        user.setDepartment(departmentField.getText().trim());
        user.setContactEmail(contactEmailField.getText().trim());
        services.userRepository().save(user);
        DialogControllerFactory.success("Profile Saved", "Your profile has been updated.",
                view.getScene() == null ? null : view.getScene().getWindow());
        renderReadMode();
    }

    // ── Courses card ─────────────────────────────────────────────────────────

    private VBox buildCoursesCard() {
        List<Job> jobs = services.jobService().getJobsByOrganiser(user.getUserId());

        VBox card = new VBox(16);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(24));

        Label cardTitle = new Label("Courses I Teach");
        cardTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        if (jobs.isEmpty()) {
            Label empty = new Label("No courses found. Create a job post to associate a course with your profile.");
            empty.setWrapText(true);
            empty.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
            card.getChildren().addAll(cardTitle, empty);
            return card;
        }

        List<Job> unique = jobs.stream()
                .filter(j -> j.getModuleCode() != null && !j.getModuleCode().isBlank())
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                j -> j.getModuleCode().trim(),
                                j -> j,
                                (a, b) -> a
                        ),
                        m -> List.copyOf(m.values())
                ));

        VBox rows = new VBox(10);
        for (Job job : unique) {
            rows.getChildren().add(buildCourseRow(job));
        }

        card.getChildren().addAll(cardTitle, rows);
        return card;
    }

    private HBox buildCourseRow(Job job) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-padding: 12 16 12 16;");

        row.getChildren().add(IconFactory.glyph(IconFactory.IconType.BRIEFCASE, 15, Color.web("#354a5f")));

        VBox meta = new VBox(3);
        HBox.setHgrow(meta, Priority.ALWAYS);

        Label code = new Label(safe(job.getModuleCode(), "-") + "  " + safe(job.getModuleName(), ""));
        code.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

        Label detail = new Label(safe(job.getSemester(), "-") + "  ·  " + job.getPositions() + " TA position(s)");
        detail.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        meta.getChildren().addAll(code, detail);

        String bg = job.getStatus() == null ? "#f1f5f9" : switch (job.getStatus()) {
            case OPEN -> "#dcfce7"; case DRAFT -> "#e0e7ff";
            case CLOSED -> "#e2e8f0"; case EXPIRED -> "#ffedd5";
        };
        String fg = job.getStatus() == null ? "#64748b" : switch (job.getStatus()) {
            case OPEN -> "#16a34a"; case DRAFT -> "#2563eb";
            case CLOSED -> "#64748b"; case EXPIRED -> "#ea580c";
        };
        Label chip = new Label(job.getStatus() == null ? "-" : job.getStatus().name());
        chip.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";"
                + "-fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 999; -fx-padding: 4 10 4 10;");

        row.getChildren().addAll(meta, chip);
        return row;
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private String initials() {
        String name = user.getDisplayName();
        if (name == null || name.isBlank()) return "MO";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
