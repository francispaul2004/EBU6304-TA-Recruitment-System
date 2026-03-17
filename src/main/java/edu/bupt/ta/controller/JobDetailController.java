package edu.bupt.ta.controller;

import edu.bupt.ta.dto.MatchExplanationDTO;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.model.Job;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class JobDetailController {

    private final VBox view = new VBox(10);
    private final Label titleLabel = new Label("Select a job");
    private final Label moduleLabel = new Label("-");
    private final Label metaLabel = new Label("-");
    private final Label descLabel = new Label("Please select a job from the left list to view details.");
    private final Label matchLabel = new Label("AI Match: -");
    private final Label missingLabel = new Label("Missing Skills: -");
    private final Label workloadLabel = new Label("Projected Workload: -");
    private final TextArea statementArea = new TextArea();
    private final Button applyButton = new Button("Apply");

    private Job currentJob;
    private Consumer<String> onApply;

    public JobDetailController() {
        initialize();
    }

    public Parent getView() {
        return view;
    }

    public void setOnApply(Consumer<String> onApply) {
        this.onApply = onApply;
    }

    public void setJob(Job job) {
        this.currentJob = job;
        if (job == null) {
            titleLabel.setText("Select a job");
            moduleLabel.setText("-");
            metaLabel.setText("-");
            descLabel.setText("Please select a job from the left list to view details.");
            setMatchExplanation(null);
            applyButton.setDisable(true);
            return;
        }

        titleLabel.setText(job.getTitle());
        moduleLabel.setText(job.getModuleCode() + " · " + job.getModuleName());
        metaLabel.setText(job.getType() + "  |  " + job.getWeeklyHours() + "h/week  |  Deadline: " + job.getDeadline());
        descLabel.setText(job.getDescription());
        applyButton.setDisable(job.getStatus() != JobStatus.OPEN);
    }

    public void setMatchExplanation(MatchExplanationDTO dto) {
        if (dto == null) {
            matchLabel.setText("AI Match: -");
            missingLabel.setText("Missing Skills: -");
            workloadLabel.setText("Projected Workload: -");
            matchLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
            return;
        }
        matchLabel.setText("AI Match: " + dto.score() + "%");
        missingLabel.setText("Missing Skills: " + (dto.missingSkills().isEmpty() ? "None" : String.join(", ", dto.missingSkills())));
        workloadLabel.setText("Projected Workload: " + dto.projectedWorkload() + "h/week");
        matchLabel.setStyle(dto.score() >= 80
                ? "-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #00A07B;"
                : "-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #B45309;");
    }

    private void initialize() {
        view.setPadding(new Insets(20));
        view.setMinWidth(360);
        view.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 0 1;");

        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        moduleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #00C29F;");
        metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
        missingLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        workloadLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        Label statementLabel = new Label("Application Statement");
        statementLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #334155;");

        statementArea.setPromptText("Why are you suitable for this role?");
        statementArea.setWrapText(true);
        statementArea.setPrefRowCount(6);

        applyButton.getStyleClass().add("primary-button");
        applyButton.setPrefHeight(40);
        applyButton.setMaxWidth(Double.MAX_VALUE);
        applyButton.setDisable(true);
        applyButton.setOnAction(event -> {
            if (onApply != null && currentJob != null) {
                onApply.accept(statementArea.getText());
            }
        });

        VBox.setVgrow(descLabel, Priority.ALWAYS);
        view.getChildren().addAll(titleLabel, moduleLabel, metaLabel, descLabel, matchLabel, missingLabel,
                workloadLabel, statementLabel, statementArea, applyButton);
    }
}
