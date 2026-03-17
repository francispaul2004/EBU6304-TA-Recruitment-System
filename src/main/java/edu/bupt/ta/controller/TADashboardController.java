package edu.bupt.ta.controller;

import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.repository.WorkloadRepository;
import edu.bupt.ta.service.ServiceRegistry;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class TADashboardController {

    private final ServiceRegistry services;
    private final User user;
    private final VBox view = new VBox(16);

    public TADashboardController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.setPadding(new Insets(24));

        String applicantId = services.applicantProfileService().getOrCreateProfile(user.getUserId()).getApplicantId();
        int profileCompletion = services.applicantProfileService().calculateProfileCompletion(applicantId);
        int resumeCompletion = services.resumeService().calculateResumeCompletion(applicantId);
        int applicationCount = services.applicationService().getApplicationsByApplicant(applicantId).size();

        WorkloadRepository workloadRepository = services.workloadRepository();
        int currentHours = workloadRepository.findByApplicantId(applicantId).map(w -> w.getCurrentWeeklyHours()).orElse(0);

        Label title = new Label("TA Dashboard");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        GridPane cards = new GridPane();
        cards.setHgap(12);
        cards.setVgap(12);

        cards.add(card("Profile Completion", profileCompletion + "%"), 0, 0);
        cards.add(card("Resume Completion", resumeCompletion + "%"), 1, 0);
        cards.add(card("Current Workload", currentHours + " h/week"), 2, 0);
        cards.add(card("My Applications", String.valueOf(applicationCount)), 3, 0);

        Label recentLabel = new Label("Recent Open Jobs");
        recentLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

        ListView<String> jobs = new ListView<>();
        List<Job> openJobs = services.jobService().searchJobs(null).stream().filter(j -> j.getStatus().name().equals("OPEN")).limit(8).toList();
        for (Job job : openJobs) {
            jobs.getItems().add(job.getTitle() + "  |  " + job.getModuleCode() + "  |  Deadline: " + job.getDeadline());
        }
        VBox.setVgrow(jobs, Priority.ALWAYS);

        view.getChildren().addAll(title, cards, recentLabel, jobs);
    }

    private VBox card(String title, String value) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setMinWidth(220);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-radius: 12; -fx-background-radius: 12;");

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #354A5F;");
        card.getChildren().addAll(t, v);
        return card;
    }
}
