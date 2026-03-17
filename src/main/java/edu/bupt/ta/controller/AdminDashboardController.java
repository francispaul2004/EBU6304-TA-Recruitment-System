package edu.bupt.ta.controller;

import edu.bupt.ta.enums.RiskLevel;
import edu.bupt.ta.model.Workload;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.List;

public class AdminDashboardController {

    private final ServiceRegistry services;
    private final User user;
    private final VBox view = new VBox(12);

    private final ComboBox<String> riskFilter = new ComboBox<>();
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
        view.setPadding(new Insets(20));

        Label heading = new Label("Admin Dashboard / Workload Monitor");
        heading.setStyle("-fx-font-size: 30px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        GridPane kpi = new GridPane();
        kpi.setHgap(12);
        kpi.setVgap(12);

        long totalApplications = services.applicationRepository().findAll().size();
        long accepted = services.exportService().countAcceptedApplications();
        long highRisk = services.workloadService().findAll().stream().filter(w -> w.getRiskLevel() == RiskLevel.HIGH).count();

        kpi.add(card("Total Applications", String.valueOf(totalApplications)), 0, 0);
        kpi.add(card("Accepted", String.valueOf(accepted)), 1, 0);
        kpi.add(card("High Risk", String.valueOf(highRisk)), 2, 0);

        riskFilter.getItems().addAll("ALL", "LOW", "MEDIUM", "HIGH");
        riskFilter.setValue("ALL");
        riskFilter.valueProperty().addListener((obs, oldV, newV) -> refresh());

        Button exportWorkload = new Button("Export Workload CSV");
        exportWorkload.getStyleClass().add("secondary-button");
        exportWorkload.setOnAction(event -> exportWorkload());

        Button exportApplication = new Button("Export Application CSV");
        exportApplication.getStyleClass().add("secondary-button");
        exportApplication.setOnAction(event -> exportApplication());

        HBox actions = new HBox(10, riskFilter, exportWorkload, exportApplication);

        TableColumn<Workload, String> applicantCol = new TableColumn<>("Applicant ID");
        applicantCol.setCellValueFactory(new PropertyValueFactory<>("applicantId"));

        TableColumn<Workload, Integer> hoursCol = new TableColumn<>("Current Hours");
        hoursCol.setCellValueFactory(new PropertyValueFactory<>("currentWeeklyHours"));

        TableColumn<Workload, String> riskCol = new TableColumn<>("Risk");
        riskCol.setCellValueFactory(new PropertyValueFactory<>("riskLevel"));

        table.getColumns().setAll(applicantCol, hoursCol, riskCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        view.getChildren().addAll(heading, kpi, actions, table);
    }

    private void refresh() {
        List<Workload> workloads = services.workloadService().findAll();
        if (!"ALL".equals(riskFilter.getValue())) {
            RiskLevel level = RiskLevel.valueOf(riskFilter.getValue());
            workloads = workloads.stream().filter(w -> w.getRiskLevel() == level).toList();
        }
        table.setItems(FXCollections.observableArrayList(workloads));
    }

    private void exportWorkload() {
        Path path = services.exportService().exportWorkloadReport();
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Exported: " + path.toAbsolutePath());
        alert.setHeaderText("Workload CSV generated");
        alert.showAndWait();
    }

    private void exportApplication() {
        Path path = services.exportService().exportApplicationReport();
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Exported: " + path.toAbsolutePath());
        alert.setHeaderText("Application CSV generated");
        alert.showAndWait();
    }

    private VBox card(String title, String value) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-radius: 12; -fx-background-radius: 12;");

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #354A5F;");

        card.getChildren().addAll(t, v);
        return card;
    }
}
