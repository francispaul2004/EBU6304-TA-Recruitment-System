package edu.bupt.ta.controller;

import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.ValidationResult;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class ApplicantProfileController {

    private final ServiceRegistry services;
    private final User user;
    private final VBox view = new VBox(12);

    private ApplicantProfile profile;

    private final TextField fullName = new TextField();
    private final TextField studentId = new TextField();
    private final TextField programme = new TextField();
    private final TextField year = new TextField();
    private final TextField email = new TextField();
    private final TextField phone = new TextField();

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

        Label heading = new Label("Applicant Profile");
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Full Name"), fullName);
        form.addRow(1, new Label("Student ID"), studentId);
        form.addRow(2, new Label("Programme"), programme);
        form.addRow(3, new Label("Year"), year);
        form.addRow(4, new Label("Email"), email);
        form.addRow(5, new Label("Phone"), phone);

        Button save = new Button("Save Profile");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> saveProfile());

        loadFromModel();

        view.setPadding(new Insets(16));
        view.getChildren().addAll(heading, form, save);
    }

    private void loadFromModel() {
        fullName.setText(nullToEmpty(profile.getFullName()));
        studentId.setText(nullToEmpty(profile.getStudentId()));
        programme.setText(nullToEmpty(profile.getProgramme()));
        year.setText(profile.getYear() > 0 ? String.valueOf(profile.getYear()) : "");
        email.setText(nullToEmpty(profile.getEmail()));
        phone.setText(nullToEmpty(profile.getPhone()));
    }

    private void saveProfile() {
        profile.setFullName(fullName.getText());
        profile.setStudentId(studentId.getText());
        profile.setProgramme(programme.getText());
        profile.setEmail(email.getText());
        profile.setPhone(phone.getText());
        try {
            profile.setYear(Integer.parseInt(year.getText()));
        } catch (NumberFormatException e) {
            profile.setYear(0);
        }

        ValidationResult result = services.applicantProfileService().saveProfile(profile);
        if (!result.isValid()) {
            Alert err = new Alert(Alert.AlertType.ERROR, String.join("\n", result.getErrors()));
            err.setHeaderText("Validation error");
            err.showAndWait();
            return;
        }

        Alert ok = new Alert(Alert.AlertType.INFORMATION, "Profile saved successfully.");
        ok.setHeaderText("Saved");
        ok.showAndWait();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
