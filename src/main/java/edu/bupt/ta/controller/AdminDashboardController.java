package edu.bupt.ta.controller;

import edu.bupt.ta.enums.RiskLevel;
import edu.bupt.ta.model.Workload;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.List;

public class AdminDashboardController {

    private final ServiceRegistry services;
    private final User user;
    private final VBox view = new VBox(16);

    private final TextField searchField = new TextField();
    private final TableView<Workload> table = new TableView<>();

    public AdminDashboardController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
        refresh();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");
        view.setPadding(new Insets(24));

        view.getChildren().add(buildTopBar());
        view.getChildren().add(buildKpiRow());
        view.getChildren().add(buildTableCard());
    }

    private HBox buildTopBar() {
        Label title = new Label("Workload Monitoring");
        title.getStyleClass().add("page-title");

        searchField.setPromptText("Search TA by name or ID...");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((obs, oldV, newV) -> refresh());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button export = new Button("Export Report");
        export.getStyleClass().add("secondary-button");
        export.setOnAction(event -> exportWorkload());

        Button exportApp = new Button("Export Applications");
        exportApp.getStyleClass().add("secondary-button");
        exportApp.setOnAction(event -> exportApplication());

        HBox row = new HBox(12, title, searchField, spacer, export, exportApp);
        return row;
    }

    private HBox buildKpiRow() {
        long totalJobs = services.jobRepository().findAll().size();
        long totalApplications = services.applicationRepository().findAll().size();
        long accepted = services.exportService().countAcceptedApplications();
        long highRisk = services.workloadService().findAll().stream().filter(workload -> workload.getRiskLevel() == RiskLevel.HIGH).count();

        HBox row = new HBox(16,
                kpiCard("TOTAL JOBS", String.valueOf(totalJobs), "#2563eb"),
                kpiCard("TOTAL APPLICATIONS", String.valueOf(totalApplications), "#7c3aed"),
                kpiCard("ACCEPTED APPS", String.valueOf(accepted), "#059669"),
                kpiCard("HIGH-RISK TAS", String.valueOf(highRisk), "#dc2626")
        );
        return row;
    }

    private VBox kpiCard(String title, String value, String color) {
        VBox card = new VBox(4);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(14));
        card.setMinWidth(240);

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #64748b;");

        Label v = new Label(value);
        v.setStyle("-fx-font-size: 40px; -fx-font-weight: 800; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(t, v);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox buildTableCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(14));

        Label heading = new Label("Workload Monitoring Table");
        heading.getStyleClass().add("section-title");

        Label subtitle = new Label("Real-time tracking of weekly teaching hours per student");
        subtitle.getStyleClass().add("body-muted");

        TableColumn<Workload, String> applicantCol = new TableColumn<>("TA NAME");
        applicantCol.setCellValueFactory(new PropertyValueFactory<>("applicantId"));

        TableColumn<Workload, Integer> acceptedJobs = new TableColumn<>("ACCEPTED JOBS");
        acceptedJobs.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(
                cell.getValue().getAcceptedJobIds() == null ? 0 : cell.getValue().getAcceptedJobIds().size()).asObject());

        TableColumn<Workload, Integer> hoursCol = new TableColumn<>("WEEKLY HOURS");
        hoursCol.setCellValueFactory(new PropertyValueFactory<>("currentWeeklyHours"));

        TableColumn<Workload, Integer> maxCol = new TableColumn<>("MAX LIMIT");
        maxCol.setCellValueFactory(cell -> {
            int max = services.resumeInfoRepository().findByApplicantId(cell.getValue().getApplicantId())
                    .map(resume -> resume.getMaxWeeklyHours())
                    .orElse(0);
            return new javafx.beans.property.SimpleIntegerProperty(max).asObject();
        });

        TableColumn<Workload, String> riskCol = new TableColumn<>("RISK LEVEL");
        riskCol.setCellValueFactory(new PropertyValueFactory<>("riskLevel"));

        table.getColumns().setAll(applicantCol, acceptedJobs, hoursCol, maxCol, riskCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(620);

        card.getChildren().addAll(heading, subtitle, table);
        return card;
    }

    private void refresh() {
        List<Workload> workloads = services.workloadService().findAll();
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        if (!keyword.isEmpty()) {
            workloads = workloads.stream().filter(workload -> workload.getApplicantId().toLowerCase().contains(keyword)).toList();
        }

        table.setItems(FXCollections.observableArrayList(workloads));
    }

    private void exportWorkload() {
        Path path = services.exportService().exportWorkloadReport();
        DialogControllerFactory.info("Workload CSV Generated", "Exported: " + path.toAbsolutePath(),
                view.getScene() == null ? null : view.getScene().getWindow());
    }

    private void exportApplication() {
        Path path = services.exportService().exportApplicationReport();
        DialogControllerFactory.info("Application CSV Generated", "Exported: " + path.toAbsolutePath(),
                view.getScene() == null ? null : view.getScene().getWindow());
    }
}
