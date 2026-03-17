package edu.bupt.ta.controller;

import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class MyCvController {

    private final ServiceRegistry services;
    private final User user;
    private final VBox view = new VBox(16);
    private final StackPane contentPane = new StackPane();

    public MyCvController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");
        view.setPadding(new Insets(24));

        ApplicantProfile profile = services.applicantProfileService().getOrCreateProfile(user.getUserId());
        ResumeInfo resume = services.resumeService().getOrCreateResume(profile.getApplicantId());

        Label heading = new Label("CV Management");
        heading.getStyleClass().add("page-title");

        Label subtitle = new Label("Upload and manage your curriculum vitae to apply for Teaching Assistant positions.");
        subtitle.getStyleClass().add("body-muted");

        VBox basicInfo = buildBasicInfoCard(profile);

        Button editProfile = new Button("Edit Basic Information");
        editProfile.getStyleClass().add("secondary-button");

        Button editResume = new Button("Edit Resume Information");
        editResume.getStyleClass().add("secondary-button");

        HBox switches = new HBox(10, editProfile, editResume);

        Parent profileView = new ApplicantProfileController(services, user).getView();
        Parent resumeView = new ResumeInfoController(services, user).getView();
        contentPane.getChildren().setAll(resumeView);

        editProfile.setOnAction(event -> contentPane.getChildren().setAll(profileView));
        editResume.setOnAction(event -> contentPane.getChildren().setAll(resumeView));

        HBox statusArea = new HBox(16,
                statusCard("CV Uploaded Successfully", "Verification Complete", resume.getRelevantModules().isEmpty() ? "Not Uploaded" : "Uploaded"),
                statusCard("Next Step", "Browse positions and apply", "Ready")
        );

        VBox guideCard = new VBox(8);
        guideCard.getStyleClass().add("panel-card");
        guideCard.setPadding(new Insets(14));

        Label guideTitle = new Label("CV Guidelines for Applicants");
        guideTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334155;");

        List<String> lines = List.of(
                "Ensure your GPA and relevant course grades are clearly visible.",
                "List any previous teaching assistant or research assistant experience.",
                "Include your proficiency in English and required languages for the course.",
                "Keep file size and structured data concise for review speed."
        );

        guideCard.getChildren().add(guideTitle);
        for (String line : lines) {
            Label row = new Label(line);
            row.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
            guideCard.getChildren().add(row);
        }

        VBox.setVgrow(contentPane, Priority.ALWAYS);
        view.getChildren().addAll(heading, subtitle, basicInfo, switches, statusArea, contentPane, guideCard);
    }

    private VBox buildBasicInfoCard(ApplicantProfile profile) {
        VBox card = new VBox(10);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(16));

        Label title = new Label("Basic Information");
        title.getStyleClass().add("section-title");

        HBox row1 = new HBox(20,
                infoCol("FULL NAME", safe(profile.getFullName())),
                infoCol("STUDENT ID", safe(profile.getStudentId())),
                infoCol("DEGREE PROGRAM", safe(profile.getProgramme()))
        );

        HBox row2 = new HBox(20,
                infoCol("EMAIL", safe(profile.getEmail())),
                infoCol("YEAR", profile.getYear() > 0 ? String.valueOf(profile.getYear()) : "-"),
                infoCol("PHONE", safe(profile.getPhone()))
        );

        card.getChildren().addAll(title, row1, row2);
        return card;
    }

    private VBox infoCol(String key, String value) {
        VBox col = new VBox(2);
        Label k = new Label(key);
        k.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");
        col.getChildren().addAll(k, v);
        HBox.setHgrow(col, Priority.ALWAYS);
        return col;
    }

    private VBox statusCard(String title, String subtitle, String value) {
        VBox card = new VBox(6);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(16));

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #334155;");

        Label s = new Label(subtitle);
        s.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        Label v = new Label(value);
        v.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #00a58a;");

        card.getChildren().addAll(t, s, v);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
