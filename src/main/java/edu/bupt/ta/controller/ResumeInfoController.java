package edu.bupt.ta.controller;

import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.ValidationResult;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ResumeInfoController {

    private final ServiceRegistry services;
    private final User user;
    private final VBox view = new VBox(12);

    private ResumeInfo resume;

    private final TextField relevantModules = new TextField();
    private final TextField technicalSkills = new TextField();
    private final TextField languageSkills = new TextField();
    private final TextField availability = new TextField();
    private final TextField maxWeeklyHours = new TextField();
    private final TextArea experience = new TextArea();
    private final TextArea personalStatement = new TextArea();

    public ResumeInfoController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        String applicantId = services.applicantProfileService().getOrCreateProfile(user.getUserId()).getApplicantId();
        resume = services.resumeService().getOrCreateResume(applicantId);

        Label heading = new Label("Resume Information");
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        experience.setPrefRowCount(3);
        personalStatement.setPrefRowCount(4);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Relevant Modules (comma separated)"), relevantModules);
        form.addRow(1, new Label("Technical Skills"), technicalSkills);
        form.addRow(2, new Label("Language Skills"), languageSkills);
        form.addRow(3, new Label("Availability"), availability);
        form.addRow(4, new Label("Max Weekly Hours"), maxWeeklyHours);
        form.addRow(5, new Label("Experience"), experience);
        form.addRow(6, new Label("Personal Statement"), personalStatement);

        Button save = new Button("Save Resume");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> saveResume());

        loadFromModel();

        view.setPadding(new Insets(16));
        view.getChildren().addAll(heading, form, save);
    }

    private void loadFromModel() {
        relevantModules.setText(String.join(", ", resume.getRelevantModules()));
        technicalSkills.setText(String.join(", ", resume.getTechnicalSkills()));
        languageSkills.setText(String.join(", ", resume.getLanguageSkills()));
        availability.setText(String.join(", ", resume.getAvailability()));
        maxWeeklyHours.setText(resume.getMaxWeeklyHours() > 0 ? String.valueOf(resume.getMaxWeeklyHours()) : "");
        experience.setText(resume.getExperienceText() == null ? "" : resume.getExperienceText());
        personalStatement.setText(resume.getPersonalStatement() == null ? "" : resume.getPersonalStatement());
    }

    private void saveResume() {
        resume.setRelevantModules(split(relevantModules.getText()));
        resume.setTechnicalSkills(split(technicalSkills.getText()));
        resume.setLanguageSkills(split(languageSkills.getText()));
        resume.setAvailability(split(availability.getText()));
        resume.setExperienceText(experience.getText());
        resume.setPersonalStatement(personalStatement.getText());

        try {
            resume.setMaxWeeklyHours(Integer.parseInt(maxWeeklyHours.getText()));
        } catch (NumberFormatException e) {
            resume.setMaxWeeklyHours(0);
        }

        ValidationResult result = services.resumeService().saveResume(resume);
        if (!result.isValid()) {
            Alert err = new Alert(Alert.AlertType.ERROR, String.join("\n", result.getErrors()));
            err.setHeaderText("Validation error");
            err.showAndWait();
            return;
        }

        Alert ok = new Alert(Alert.AlertType.INFORMATION, "Resume saved successfully.");
        ok.setHeaderText("Saved");
        ok.showAndWait();
    }

    private List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
