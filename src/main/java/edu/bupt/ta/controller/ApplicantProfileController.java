package edu.bupt.ta.controller;

import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.service.ApplicantProfileService;
import edu.bupt.ta.util.ValidationResult;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ApplicantProfileController {

    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    private ApplicantProfile profile;

    private final TextField fullName = new TextField();
    private final TextField studentId = new TextField();
    private final TextField programme = new TextField();
    private final ComboBox<Integer> year = new ComboBox<>();
    private final TextField email = new TextField();
    private final TextField phone = new TextField();
    private final ComboBox<String> currentCampus = new ComboBox<>();
    private final ToggleGroup crossCampusGroup = new ToggleGroup();
    private final RadioButton crossCampusYes = new RadioButton("Yes");
    private final RadioButton crossCampusNo = new RadioButton("No");

    public ApplicantProfileController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        profile = services.applicantProfileService().getOrCreateProfile(user.getUserId());

        VBox root = new VBox(14);
        root.getStyleClass().add("app-surface");
        root.setFillWidth(true);
        root.setPadding(new Insets(4, 0, 0, 0));

        HBox header = new HBox();
        header.setSpacing(14);
        Label heading = new Label("Edit Basic Information");
        heading.getStyleClass().add("section-title");

        Label subtitle = new Label("Update your academic background and availability for this semester.");
        subtitle.getStyleClass().add("body-muted");

        VBox titleBlock = new VBox(4, heading, subtitle);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button reset = new Button("Reset");
        reset.getStyleClass().add("secondary-button");
        reset.setOnAction(event -> loadFromModel());

        Button save = new Button("Save Changes");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> saveProfile());

        header.getChildren().addAll(titleBlock, spacer, reset, save);

        VBox formCard = new VBox(14);
        formCard.getStyleClass().add("panel-card");
        formCard.setPadding(new Insets(16));
        formCard.setFillWidth(true);

        Label formTitle = new Label("Personal Information");
        formTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(14);

        form.add(field("Full Name", fullName), 0, 0);
        form.add(field("Student ID", studentId), 1, 0);
        form.add(field("School Email", email), 0, 1);
        form.add(yearField("Academic Year"), 1, 1);
        form.add(field("Phone Number", phone), 0, 2);
        form.add(field("Major", programme), 1, 2);
        form.add(campusField("Current Campus"), 0, 3);
        form.add(crossCampusField("Willing to cross campus"), 1, 3);

        formCard.getChildren().addAll(formTitle, form);

        VBox tipCard = new VBox(6);
        tipCard.getStyleClass().add("soft-card");
        tipCard.setPadding(new Insets(12));

        Label tipTitle = new Label("Pro Tip");
        tipTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #b45309;");

        Label tipBody = new Label("Complete profile and resume to unlock all apply actions in the job browser.");
        tipBody.setWrapText(true);
        tipBody.setStyle("-fx-font-size: 12px; -fx-text-fill: #92400e;");

        tipCard.getChildren().addAll(tipTitle, tipBody);

        root.getChildren().addAll(header, formCard, tipCard);
        view.setCenter(root);

        loadFromModel();
    }

    private VBox field(String title, TextField input) {
        return field(title, (Node) input);
    }

    private VBox field(String title, Node input) {
        VBox box = new VBox(6);
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        Label label = new Label(title);
        label.getStyleClass().add("field-label");
        if (input instanceof TextField textField) {
            textField.setMaxWidth(Double.MAX_VALUE);
        } else if (input instanceof ComboBox<?> comboBox) {
            comboBox.setMaxWidth(Double.MAX_VALUE);
        }
        box.getChildren().addAll(label, input);
        return box;
    }

    private VBox yearField(String title) {
        VBox box = new VBox(6);
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        Label label = new Label(title);
        label.getStyleClass().add("field-label");
        year.getItems().setAll(1, 2, 3, 4, 5, 6, 7, 8);
        year.setPromptText("Select Year");
        year.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(label, year);
        return box;
    }

    private VBox campusField(String title) {
        currentCampus.getItems().setAll(ApplicantProfileService.CAMPUS_HAIDIAN, ApplicantProfileService.CAMPUS_SHAHE);
        currentCampus.setPromptText("Select Campus");
        return field(title, currentCampus);
    }

    private VBox crossCampusField(String title) {
        crossCampusYes.setToggleGroup(crossCampusGroup);
        crossCampusNo.setToggleGroup(crossCampusGroup);
        if (!crossCampusYes.getStyleClass().contains("grade-radio")) {
            crossCampusYes.getStyleClass().add("grade-radio");
        }
        if (!crossCampusNo.getStyleClass().contains("grade-radio")) {
            crossCampusNo.getStyleClass().add("grade-radio");
        }
        HBox row = new HBox(16, crossCampusYes, crossCampusNo);
        row.setPadding(new Insets(6, 0, 0, 0));
        return field(title, row);
    }

    private void loadFromModel() {
        fullName.setText(nullToEmpty(profile.getFullName()));
        studentId.setText(nullToEmpty(profile.getStudentId()));
        programme.setText(nullToEmpty(profile.getProgramme()));
        year.setValue(profile.getYear() > 0 ? profile.getYear() : null);
        email.setText(nullToEmpty(profile.getEmail()));
        phone.setText(nullToEmpty(profile.getPhone()));
        currentCampus.setValue(profile.getCurrentCampus());

        Boolean willing = profile.getWillingToCrossCampus();
        if (willing == null) {
            crossCampusGroup.selectToggle(null);
        } else if (willing) {
            crossCampusGroup.selectToggle(crossCampusYes);
        } else {
            crossCampusGroup.selectToggle(crossCampusNo);
        }
    }

    private void saveProfile() {
        profile.setFullName(fullName.getText());
        profile.setStudentId(studentId.getText());
        profile.setProgramme(programme.getText());
        profile.setEmail(email.getText());
        profile.setPhone(phone.getText());
        Integer selectedYear = year.getValue();
        profile.setYear(selectedYear == null ? 0 : selectedYear);
        profile.setCurrentCampus(currentCampus.getValue());
        profile.setWillingToCrossCampus(readCrossCampusSelection());

        ValidationResult result = services.applicantProfileService().saveProfile(profile);
        if (!result.isValid()) {
            DialogControllerFactory.validationError(String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        DialogControllerFactory.success("Profile Saved", "Profile saved successfully.",
                view.getScene() == null ? null : view.getScene().getWindow());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Boolean readCrossCampusSelection() {
        Toggle selected = crossCampusGroup.getSelectedToggle();
        if (selected == null) {
            return null;
        }
        if (selected == crossCampusYes) {
            return true;
        }
        return false;
    }
}
