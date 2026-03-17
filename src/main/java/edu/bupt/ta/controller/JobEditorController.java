package edu.bupt.ta.controller;

import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.enums.JobType;
import edu.bupt.ta.model.Job;
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.Optional;

public class JobEditorController {

    public Optional<Job> show(Job source, String organiserId) {
        Dialog<Job> dialog = new Dialog<>();
        dialog.setTitle(source == null ? "Create Job" : "Edit Job");

        ButtonType saveDraft = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveDraft, cancel);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        TextField title = new TextField();
        TextField moduleCode = new TextField();
        TextField moduleName = new TextField();
        ComboBox<JobType> type = new ComboBox<>(FXCollections.observableArrayList(JobType.values()));
        TextField weeklyHours = new TextField();
        TextField positions = new TextField();
        DatePicker deadline = new DatePicker();
        ComboBox<JobStatus> status = new ComboBox<>(FXCollections.observableArrayList(JobStatus.values()));
        TextArea description = new TextArea();
        description.setPrefRowCount(4);

        form.addRow(0, new Label("Title"), title);
        form.addRow(1, new Label("Module Code"), moduleCode);
        form.addRow(2, new Label("Module Name"), moduleName);
        form.addRow(3, new Label("Type"), type);
        form.addRow(4, new Label("Weekly Hours"), weeklyHours);
        form.addRow(5, new Label("Positions"), positions);
        form.addRow(6, new Label("Deadline"), deadline);
        form.addRow(7, new Label("Status"), status);
        form.addRow(8, new Label("Description"), description);

        if (source != null) {
            title.setText(source.getTitle());
            moduleCode.setText(source.getModuleCode());
            moduleName.setText(source.getModuleName());
            type.setValue(source.getType());
            weeklyHours.setText(String.valueOf(source.getWeeklyHours()));
            positions.setText(String.valueOf(source.getPositions()));
            deadline.setValue(source.getDeadline());
            status.setValue(source.getStatus());
            description.setText(source.getDescription());
        } else {
            type.setValue(JobType.MODULE_TA);
            status.setValue(JobStatus.DRAFT);
            deadline.setValue(LocalDate.now().plusDays(7));
        }

        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(button -> {
            if (button != saveDraft) {
                return null;
            }
            Job job = source == null ? new Job() : source;
            job.setTitle(title.getText());
            job.setModuleCode(moduleCode.getText());
            job.setModuleName(moduleName.getText());
            job.setType(type.getValue());
            job.setWeeklyHours(parseOrZero(weeklyHours.getText()));
            job.setPositions(parseOrZero(positions.getText()));
            job.setDeadline(deadline.getValue());
            job.setStatus(status.getValue());
            job.setDescription(description.getText());
            job.setOrganiserId(organiserId);
            return job;
        });

        return dialog.showAndWait();
    }

    private int parseOrZero(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
