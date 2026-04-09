package edu.bupt.ta.controller;

import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.service.ResumeService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Apply confirmation dialog: read-only CV profile fields, CV preview button,
 * and editable per-job application statement.
 */
public final class JobApplyDialog {

    private static final String STYLESHEET = "/styles/app.css";

    private JobApplyDialog() {
    }

    /**
     * @return empty if user cancelled; otherwise the statement text for this job (may be blank).
     */
    public static Optional<String> showAndWait(Job job, ApplicantProfile profile,
                                               ResumeService resumeService, String applicantId, Window owner) {
        Stage stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Apply for: " + (job != null && job.getTitle() != null ? job.getTitle() : ""));

        Label hint = new Label("Information below is from your CV (read only).");
        hint.setWrapText(true);
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        int row = 0;
        grid.add(fieldLabel("Name"), 0, row);
        grid.add(readOnlyField(safe(profile.getFullName())), 1, row++);

        grid.add(fieldLabel("Student ID"), 0, row);
        grid.add(readOnlyField(safe(profile.getStudentId())), 1, row++);

        grid.add(fieldLabel("Email address"), 0, row);
        grid.add(readOnlyField(safe(profile.getEmail())), 1, row++);

        grid.add(fieldLabel("Phone number"), 0, row);
        grid.add(readOnlyField(safe(profile.getPhone())), 1, row++);

        grid.add(fieldLabel("Major"), 0, row);
        grid.add(readOnlyField(safe(profile.getProgramme())), 1, row++);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setMinWidth(220);
        grid.getColumnConstraints().addAll(c0, c1);

        Label stmtTitle = new Label("Application Statement");
        stmtTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #334155;");

        Label stmtHint = new Label("请说明您的申请原因和特长（英语）");
        stmtHint.setWrapText(true);
        stmtHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        TextArea statement = new TextArea();
        statement.setPromptText("Please describe your application reasons and strengths.");
        statement.setWrapText(true);
        statement.setPrefRowCount(6);

        // CV preview button
        Button previewBtn = new Button("Preview CV");
        previewBtn.setOnAction(e -> openCvFile(resumeService, applicantId, stage));

        final boolean[] submitted = {false};
        final String[] textOut = new String[1];

        Button cancel = new Button("Cancel");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> stage.close());

        Button submit = new Button("Submit");
        submit.getStyleClass().add("primary-button");
        submit.setDefaultButton(true);
        submit.setOnAction(e -> {
            submitted[0] = true;
            String t = statement.getText();
            textOut[0] = t == null ? "" : t.trim();
            stage.close();
        });

        HBox buttons = new HBox(12, previewBtn, cancel, submit);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(8, 0, 0, 0));

        VBox root = new VBox(14);
        root.setPadding(new Insets(8));
        VBox stmtBlock = new VBox(4, stmtTitle, stmtHint, statement);

        root.getChildren().addAll(hint, grid, stmtBlock, buttons);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(scroll, 520, 560);
        if (JobApplyDialog.class.getResource(STYLESHEET) != null) {
            scene.getStylesheets().add(JobApplyDialog.class.getResource(STYLESHEET).toExternalForm());
        }
        stage.setScene(scene);
        stage.setMinWidth(480);
        stage.setMinHeight(420);
        stage.setResizable(true);

        stage.showAndWait();

        if (!submitted[0]) {
            return Optional.empty();
        }
        return Optional.of(textOut[0] == null ? "" : textOut[0]);
    }

    private static void openCvFile(ResumeService resumeService, String applicantId, Window owner) {
        Optional<Path> filePath = resumeService.getCvFilePath(applicantId);
        if (filePath.isEmpty()) {
            DialogControllerFactory.info("CV Not Found",
                    "No uploaded CV file exists for this account.", owner);
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                DialogControllerFactory.operationFailed("Open CV Failed",
                        "Desktop open action is not supported in this environment.", owner);
                return;
            }
            Desktop.getDesktop().open(filePath.get().toFile());
        } catch (Exception ex) {
            DialogControllerFactory.operationFailed("Open CV Failed",
                    "Unable to open file: " + ex.getMessage(), owner);
        }
    }

    private static Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #475569;");
        return l;
    }

    private static TextField readOnlyField(String text) {
        TextField tf = new TextField(text);
        tf.setEditable(false);
        tf.setFocusTraversable(false);
        tf.setStyle(
                "-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; "
                        + "-fx-background-insets: 0; -fx-border-color: #e2e8f0; "
                        + "-fx-border-radius: 6; -fx-background-radius: 6;");
        return tf;
    }

    private static String safe(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
