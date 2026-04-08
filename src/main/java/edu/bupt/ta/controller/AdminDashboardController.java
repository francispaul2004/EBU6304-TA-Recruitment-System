package edu.bupt.ta.controller;

import edu.bupt.ta.dto.AdminDashboardSummaryDTO;
import edu.bupt.ta.dto.AdminWorkloadRowDTO;
import edu.bupt.ta.dto.AuditLogItemDTO;
import edu.bupt.ta.enums.RiskLevel;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AdminDashboardController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final double HOURS_BAR_WIDTH = 92;

    private enum SortMode {
        RISK_PRIORITY,
        HOURS_DESC
    }

    private enum MetricIcon {
        JOBS,
        APPLICATIONS,
        ACCEPTED,
        RISK
    }

    private final ServiceRegistry services;
    private final User user;
    private final VBox view = new VBox(16);

    private final TextField searchField = new TextField();
    private final ComboBox<String> riskFilter = new ComboBox<>();
    private final TableView<AdminWorkloadRowDTO> workloadTable = new TableView<>();
    private final TableView<AuditLogItemDTO> auditLogTable = new TableView<>();

    private final Label totalJobsValue = new Label("0");
    private final Label totalApplicationsValue = new Label("0");
    private final Label acceptedValue = new Label("0");
    private final Label highRiskValue = new Label("0");
    private final Label workloadFooter = new Label("Showing 0 of 0 TAs");
    private final Button sortButton = new Button("Sort");

    private SortMode sortMode = SortMode.RISK_PRIORITY;

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

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        VBox content = new VBox(18);
        content.getChildren().addAll(buildTopBar(), buildKpiRow(), buildWorkloadCard(), buildAuditCard());
        scrollPane.setContent(content);

        searchField.textProperty().addListener((obs, oldV, newV) -> refresh());
        riskFilter.valueProperty().addListener((obs, oldV, newV) -> refresh());

        view.getChildren().add(scrollPane);
    }

    private HBox buildTopBar() {
        Label title = new Label("Workload Monitoring");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");

        searchField.setPromptText("Search TA by name or ID...");
        searchField.setPrefWidth(340);

        riskFilter.getItems().setAll("ALL RISK", "HIGH", "MEDIUM", "LOW");
        riskFilter.setValue("ALL RISK");
        riskFilter.setPrefWidth(146);

        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(event -> refresh());

        Button export = new Button("Export Report");
        export.setStyle("-fx-background-color: #354a5f; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 8; -fx-padding: 10 16 10 16;");
        export.setOnAction(event -> exportWorkload());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(14, title, searchField, riskFilter, spacer, refreshButton, export);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildKpiRow() {
        HBox row = new HBox(16,
                metricCard(MetricIcon.JOBS, "TOTAL JOBS", totalJobsValue, "+5%", "#2563eb", "#eff6ff", false),
                metricCard(MetricIcon.APPLICATIONS, "TOTAL APPLICATIONS", totalApplicationsValue, "-2%", "#7c3aed", "#f5f3ff", false),
                metricCard(MetricIcon.ACCEPTED, "ACCEPTED APPS", acceptedValue, "+12%", "#10b981", "#ecfdf3", false),
                metricCard(MetricIcon.RISK, "HIGH-RISK TAS", highRiskValue, "+3%", "#ef4444", "#fff1f2", true)
        );
        return row;
    }

    private VBox metricCard(MetricIcon iconType,
                            String title,
                            Label valueLabel,
                            String delta,
                            String accent,
                            String iconTint,
                            boolean danger) {
        VBox card = new VBox(16);
        card.getStyleClass().add("metric-card");
        if (danger) {
            card.getStyleClass().add("metric-card-danger");
        }
        card.setMinWidth(220);
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = buildMetricIcon(iconType, accent, iconTint);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label trend = new Label(delta);
        trend.getStyleClass().add("metric-subtle");
        trend.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + accent + ";");

        Label titleNode = new Label(title);
        titleNode.getStyleClass().add("metric-kicker");
        titleNode.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");

        valueLabel.getStyleClass().add("metric-value");
        valueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        if (danger) {
            valueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #e11d48;");
        }

        card.getChildren().addAll(top);
        top.getChildren().addAll(icon, spacer, trend);
        card.getChildren().addAll(titleNode, valueLabel);
        return card;
    }

    private VBox buildWorkloadCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(18));

        VBox titleBlock = new VBox(4);
        Label heading = new Label("Workload Monitoring Table");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");

        Label subtitle = new Label("Real-time tracking of weekly teaching hours per student");
        subtitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #94a3b8;");
        titleBlock.getChildren().addAll(heading, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button filterButton = new Button("Filter");
        filterButton.getStyleClass().add("secondary-button");
        filterButton.setOnAction(event -> riskFilter.show());

        sortButton.getStyleClass().add("secondary-button");
        sortButton.setOnAction(event -> toggleSortMode());

        HBox header = new HBox(12, titleBlock, spacer, filterButton, sortButton);
        header.setAlignment(Pos.CENTER_LEFT);

        TableColumn<AdminWorkloadRowDTO, String> applicantCol = new TableColumn<>("TA NAME");
        applicantCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().applicantName()));
        applicantCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                AdminWorkloadRowDTO row = getTableView().getItems().get(getIndex());
                Label avatar = new Label(initials(row.applicantName()));
                avatar.setMinSize(30, 30);
                avatar.setAlignment(Pos.CENTER);
                avatar.setStyle("-fx-background-color: #eef2f7; -fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999;");

                Label name = new Label(row.applicantName());
                name.setWrapText(false);
                name.setTextOverrun(OverrunStyle.ELLIPSIS);
                name.setMaxWidth(180);
                name.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1f2937;");

                HBox box = new HBox(12, avatar, name);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });

        TableColumn<AdminWorkloadRowDTO, String> studentIdCol = new TableColumn<>("STUDENT ID");
        studentIdCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().studentId()));
        studentIdCol.setCellFactory(column -> mutedTextCell());

        TableColumn<AdminWorkloadRowDTO, Number> acceptedJobsCol = new TableColumn<>("ACCEPTED JOBS");
        acceptedJobsCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().acceptedJobs()));
        acceptedJobsCol.setCellFactory(column -> centeredNumberCell());

        TableColumn<AdminWorkloadRowDTO, Number> hoursCol = new TableColumn<>("WEEKLY HOURS");
        hoursCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().currentWeeklyHours()));
        hoursCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                AdminWorkloadRowDTO row = getTableView().getItems().get(getIndex());
                HBox box = new HBox(10, buildHoursBar(row.currentWeeklyHours(), row.maxWeeklyHours(), row.riskLevel()),
                        coloredValueLabel(row.currentWeeklyHours() + "h", riskTextColor(row.riskLevel())));
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });

        TableColumn<AdminWorkloadRowDTO, Number> maxCol = new TableColumn<>("MAX LIMIT");
        maxCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().maxWeeklyHours()));
        maxCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.intValue() + "h");
                if (!empty) {
                    setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: 700;");
                } else {
                    setStyle("");
                }
                setGraphic(null);
            }
        });

        TableColumn<AdminWorkloadRowDTO, String> riskCol = new TableColumn<>("RISK LEVEL");
        riskCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().riskLevel()));
        riskCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label chip = new Label(item);
                chip.setStyle(riskChipStyle(item));
                setGraphic(chip);
                setText(null);
            }
        });

        TableColumn<AdminWorkloadRowDTO, String> noteCol = new TableColumn<>("NOTES");
        noteCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().note()));
        noteCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label note = new Label(item);
                note.setWrapText(true);
                note.setTextFill(Color.web(noteColor(item)));
                note.setStyle("-fx-font-size: 12px; -fx-font-weight: 700;");
                setGraphic(note);
                setText(null);
            }
        });

        workloadTable.getColumns().setAll(applicantCol, studentIdCol, acceptedJobsCol, hoursCol, maxCol, riskCol, noteCol);
        workloadTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        workloadTable.setFixedCellSize(74);
        workloadTable.setPrefHeight(390);
        workloadTable.setPlaceholder(new Label("No workload records match the current filters."));

        workloadFooter.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        card.getChildren().addAll(header, workloadTable, workloadFooter);
        return card;
    }

    private VBox buildAuditCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(18));

        Label heading = new Label("Audit Log");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");

        Label subtitle = new Label("Recent system actions recorded for jobs, applications, and review decisions");
        subtitle.getStyleClass().add("body-muted");

        TableColumn<AuditLogItemDTO, String> timeCol = new TableColumn<>("TIME");
        timeCol.setCellValueFactory(cell -> new SimpleStringProperty(formatTime(cell.getValue().timestamp())));
        timeCol.setCellFactory(column -> mutedTextCell());

        TableColumn<AuditLogItemDTO, String> actorCol = new TableColumn<>("ACTOR");
        actorCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().actorName() + " (" + cell.getValue().actorUserId() + ")"));

        TableColumn<AuditLogItemDTO, String> actionCol = new TableColumn<>("ACTION");
        actionCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().action()));
        actionCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label chip = new Label(item.replace('_', ' '));
                chip.getStyleClass().add("tag-chip");
                setGraphic(chip);
                setText(null);
            }
        });

        TableColumn<AuditLogItemDTO, String> detailCol = new TableColumn<>("DETAIL");
        detailCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().detail()));

        auditLogTable.getColumns().setAll(timeCol, actorCol, actionCol, detailCol);
        auditLogTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        auditLogTable.setPrefHeight(250);
        auditLogTable.setPlaceholder(new Label("No audit logs found."));

        card.getChildren().addAll(heading, subtitle, auditLogTable);
        return card;
    }

    private void refresh() {
        AdminDashboardSummaryDTO summary = services.adminMonitoringService().getDashboardSummary();
        totalJobsValue.setText(String.valueOf(summary.totalJobs()));
        totalApplicationsValue.setText(String.valueOf(summary.totalApplications()));
        acceptedValue.setText(String.valueOf(summary.acceptedApplications()));
        highRiskValue.setText(String.valueOf(summary.highRiskApplicants()));

        String keyword = normalize(searchField.getText());
        String selectedRisk = riskFilter.getValue();

        List<AdminWorkloadRowDTO> allWorkloads = services.adminMonitoringService().getWorkloadRows();
        List<AdminWorkloadRowDTO> workloads = allWorkloads;
        if (!keyword.isEmpty()) {
            workloads = workloads.stream()
                    .filter(workload -> contains(workload.applicantName(), keyword)
                            || contains(workload.studentId(), keyword)
                            || contains(workload.applicantId(), keyword)
                            || contains(workload.note(), keyword))
                    .toList();
        }
        if (selectedRisk != null && !"ALL RISK".equals(selectedRisk)) {
            RiskLevel targetRisk = RiskLevel.valueOf(selectedRisk);
            workloads = workloads.stream()
                    .filter(workload -> targetRisk.name().equals(workload.riskLevel()))
                    .toList();
        }

        Comparator<AdminWorkloadRowDTO> comparator = sortMode == SortMode.RISK_PRIORITY
                ? Comparator.comparingInt((AdminWorkloadRowDTO row) -> riskPriority(row.riskLevel()))
                .thenComparing(AdminWorkloadRowDTO::currentWeeklyHours, Comparator.reverseOrder())
                .thenComparing(AdminWorkloadRowDTO::applicantName)
                : Comparator.comparing(AdminWorkloadRowDTO::currentWeeklyHours, Comparator.reverseOrder())
                .thenComparingInt(row -> riskPriority(row.riskLevel()))
                .thenComparing(AdminWorkloadRowDTO::applicantName);
        workloads = workloads.stream().sorted(comparator).toList();

        workloadTable.setItems(FXCollections.observableArrayList(workloads));
        workloadFooter.setText("Showing " + workloads.size() + " of " + allWorkloads.size() + " TAs");
        sortButton.setText(sortMode == SortMode.RISK_PRIORITY ? "Sort: Risk" : "Sort: Hours");

        List<AuditLogItemDTO> auditLogs = services.adminMonitoringService().getAuditLogs();
        if (!keyword.isEmpty()) {
            auditLogs = auditLogs.stream()
                    .filter(item -> contains(item.actorName(), keyword)
                            || contains(item.actorUserId(), keyword)
                            || contains(item.action(), keyword)
                            || contains(item.detail(), keyword))
                    .toList();
        }
        auditLogTable.setItems(FXCollections.observableArrayList(auditLogs.stream().limit(8).toList()));
    }

    private void toggleSortMode() {
        sortMode = sortMode == SortMode.RISK_PRIORITY ? SortMode.HOURS_DESC : SortMode.RISK_PRIORITY;
        refresh();
    }

    private <T> TableCell<T, String> mutedTextCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                if (!empty) {
                    setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: 500;");
                } else {
                    setStyle("");
                }
                setGraphic(null);
            }
        };
    }

    private TableCell<AdminWorkloadRowDTO, Number> centeredNumberCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.valueOf(item.intValue()));
                if (!empty) {
                    setStyle("-fx-font-weight: 700; -fx-text-fill: #1f2937;");
                } else {
                    setStyle("");
                }
                setGraphic(null);
            }
        };
    }

    private HBox buildHoursBar(int currentHours, int maxHours, String riskLevel) {
        double ratio = maxHours <= 0 ? 0 : Math.min(1.0, (double) currentHours / maxHours);

        Rectangle track = new Rectangle(HOURS_BAR_WIDTH, 8);
        track.setArcWidth(6);
        track.setArcHeight(6);
        track.setFill(Color.web("#dde6f0"));

        double fillWidth = ratio <= 0 ? 0 : Math.max(18, HOURS_BAR_WIDTH * ratio);
        Rectangle fill = new Rectangle(fillWidth, 8);
        fill.setArcWidth(6);
        fill.setArcHeight(6);
        fill.setFill(Color.web(riskTextColor(riskLevel)));

        StackPane bar = new StackPane(track);
        bar.setMinSize(HOURS_BAR_WIDTH, 8);
        bar.setPrefSize(HOURS_BAR_WIDTH, 8);
        bar.setMaxSize(HOURS_BAR_WIDTH, 8);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        bar.getChildren().add(fill);

        HBox wrapper = new HBox(bar);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        return wrapper;
    }

    private Label coloredValueLabel(String text, String color) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: " + color + ";");
        return label;
    }

    private StackPane buildMetricIcon(MetricIcon type, String accent, String tint) {
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(40, 40);
        iconBox.setPrefSize(40, 40);
        iconBox.setMaxSize(40, 40);
        iconBox.setStyle("-fx-background-color: " + tint + "; -fx-background-radius: 10;");
        iconBox.getChildren().add(createMetricIconShape(type, accent));
        return iconBox;
    }

    private Node createMetricIconShape(MetricIcon type, String accent) {
        return switch (type) {
            case JOBS -> buildJobsIcon(accent);
            case APPLICATIONS -> buildApplicationsIcon(accent);
            case ACCEPTED -> buildAcceptedIcon(accent);
            case RISK -> buildRiskIcon(accent);
        };
    }

    private Node buildJobsIcon(String accent) {
        Color stroke = Color.web(accent);

        Rectangle board = new Rectangle(5, 6, 12, 14);
        board.setArcWidth(3);
        board.setArcHeight(3);
        board.setFill(Color.TRANSPARENT);
        board.setStroke(stroke);
        board.setStrokeWidth(1.8);

        Rectangle clip = new Rectangle(8, 3, 6, 5);
        clip.setArcWidth(3);
        clip.setArcHeight(3);
        clip.setFill(Color.TRANSPARENT);
        clip.setStroke(stroke);
        clip.setStrokeWidth(1.8);

        Line line1 = new Line(8, 11, 14, 11);
        line1.setStroke(stroke);
        line1.setStrokeWidth(1.8);

        Line line2 = new Line(8, 14.5, 14, 14.5);
        line2.setStroke(stroke);
        line2.setStrokeWidth(1.8);

        Line line3 = new Line(8, 18, 12, 18);
        line3.setStroke(stroke);
        line3.setStrokeWidth(1.8);

        Pane pane = new Pane(board, clip, line1, line2, line3);
        pane.setMinSize(22, 22);
        pane.setPrefSize(22, 22);
        pane.setMaxSize(22, 22);
        return pane;
    }

    private Node buildApplicationsIcon(String accent) {
        Color stroke = Color.web(accent);

        Circle head = new Circle(9, 8, 3.4);
        head.setFill(Color.TRANSPARENT);
        head.setStroke(stroke);
        head.setStrokeWidth(1.8);

        Polyline shoulders = new Polyline(
                4.5, 16.0,
                4.5, 14.2,
                6.5, 12.4,
                11.5, 12.4,
                13.5, 14.2,
                13.5, 16.0
        );
        shoulders.setFill(Color.TRANSPARENT);
        shoulders.setStroke(stroke);
        shoulders.setStrokeWidth(1.8);

        Line plusV = new Line(16.5, 5.0, 16.5, 11.0);
        plusV.setStroke(stroke);
        plusV.setStrokeWidth(1.8);

        Line plusH = new Line(13.5, 8.0, 19.5, 8.0);
        plusH.setStroke(stroke);
        plusH.setStrokeWidth(1.8);

        Pane pane = new Pane(head, shoulders, plusV, plusH);
        pane.setMinSize(22, 22);
        pane.setPrefSize(22, 22);
        pane.setMaxSize(22, 22);
        return pane;
    }

    private Node buildAcceptedIcon(String accent) {
        Circle circle = new Circle(8);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web(accent));
        circle.setStrokeWidth(1.6);

        Polyline check = new Polyline(-4.0, 0.0, -1.0, 3.0, 4.0, -3.0);
        check.setStroke(Color.web(accent));
        check.setStrokeWidth(1.8);
        check.setFill(Color.TRANSPARENT);

        return new StackPane(circle, check);
    }

    private Node buildRiskIcon(String accent) {
        Polygon triangle = new Polygon(
                0.0, -8.0,
                8.0, 7.0,
                -8.0, 7.0
        );
        triangle.setFill(Color.TRANSPARENT);
        triangle.setStroke(Color.web(accent));
        triangle.setStrokeWidth(1.6);

        Line line = new Line(0, -2, 0, 3);
        line.setStroke(Color.web(accent));
        line.setStrokeWidth(1.8);

        Circle dot = new Circle(1.2);
        dot.setFill(Color.web(accent));
        dot.setTranslateY(5);

        return new StackPane(triangle, line, dot);
    }

    private String riskChipStyle(String riskLevel) {
        return switch (riskLevel == null ? "" : riskLevel) {
            case "HIGH" -> "-fx-background-color: #fff1f2; -fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-padding: 4 10 4 10;";
            case "MEDIUM" -> "-fx-background-color: #fff7ed; -fx-text-fill: #f59e0b; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-padding: 4 10 4 10;";
            default -> "-fx-background-color: #ecfdf3; -fx-text-fill: #10b981; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-padding: 4 10 4 10;";
        };
    }

    private String riskTextColor(String riskLevel) {
        return switch (riskLevel == null ? "" : riskLevel) {
            case "HIGH" -> "#ef4444";
            case "MEDIUM" -> "#f59e0b";
            default -> "#10b981";
        };
    }

    private String noteColor(String note) {
        String normalized = normalize(note);
        if (normalized.contains("immediate")) {
            return "#ef4444";
        }
        if (normalized.contains("close")) {
            return "#f59e0b";
        }
        return "#94a3b8";
    }

    private String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "TA";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private int riskPriority(String riskLevel) {
        return switch (riskLevel == null ? "" : riskLevel) {
            case "HIGH" -> 0;
            case "MEDIUM" -> 1;
            case "LOW" -> 2;
            default -> 3;
        };
    }

    private void exportWorkload() {
        Path path = services.exportService().exportWorkloadReport();
        DialogControllerFactory.info("Workload CSV Generated", "Exported: " + path.toAbsolutePath(),
                view.getScene() == null ? null : view.getScene().getWindow());
    }

    private String formatTime(LocalDateTime timestamp) {
        return timestamp == null ? "-" : timestamp.format(TIME_FORMAT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean contains(String text, String keyword) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
