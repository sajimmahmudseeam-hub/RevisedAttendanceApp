package com.example.revisedattendanceapp;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AttendanceController {

    @FXML private Label lblHeaderTitle;
    @FXML private Label lblPresent, lblAbsent, lblLate, lblRate;
    @FXML private TextField searchField, profField;
    @FXML private DatePicker datePicker; // Default Date Picker (Used for Monthly View)
    @FXML private ComboBox<String> courseComboBox;
    @FXML private PieChart attendancePieChart;

    // --- MANUAL CONTROLS ---
    @FXML private ComboBox<String> viewModeComboBox;  // "Weekly View", "Monthly View"
    @FXML private DatePicker startDatePicker;         // Button 1: Manual Start Date (e.g., 1 Oct)
    @FXML private Spinner<Integer> weekSpinner;       // Button 2: Manual Week Count (e.g., 20 Weeks)

    // --- SIDEBAR DASHBOARD BINDINGS ---
    @FXML private Button btnEditStudent, btnDeleteStudent;
    @FXML private Label lblSelectedStatus, lblTotalStudentsBadge, lblTotalLogs;
    @FXML private Label lblPresentPct, lblAbsentPct, lblLatePct;
    @FXML private Label lblAtRiskCount, lblDonutRate;
    @FXML private ProgressBar progressAttendance;
    @FXML private HBox cardAtRisk;

    @FXML private TableView<AttendanceRecord> leftTable;
    @FXML private TableView<AttendanceRecord> rightTable;

    @FXML private TableColumn<AttendanceRecord, Object> colSerial;
    @FXML private TableColumn<AttendanceRecord, String> colId, colName;
    @FXML private TableColumn<AttendanceRecord, Number> colOverall;

    // --- COLLAPSIBLE SECTIONS ---
    @FXML private ScrollPane leftSidebarScroll;
    @FXML private Button btnToggleLeft;
    @FXML private VBox middleSectionContainer;

    private TableColumn<AttendanceRecord, String> colFirstName;
    private boolean isLeftCollapsed = false;
    private boolean isMiddleCollapsed = false;

    private final ObservableList<AttendanceRecord> recordList = FXCollections.observableArrayList();
    private FilteredList<AttendanceRecord> filteredData;
    private boolean isSyncingSelection = false;

    private final Map<String, String> courseProfMap = new HashMap<>();

    // Manual Weekly View buttons
    private Button btnStartingDate;
    private Button btnNumberOfWeeks;
    private boolean weeklyButtonsAdded = false;
    private LocalDate manualStartDate = LocalDate.of(2026, 10, 1);
    private int manualWeekCount = 20;

    @FXML
    @SuppressWarnings("SpellCheckingInspection")
    public void initialize() {
        DatabaseHandler.initDatabase();

        if (lblHeaderTitle != null) {
            lblHeaderTitle.setText("Seeam's Attendance Sheet");
        }

        if (cardAtRisk != null) {
            cardAtRisk.setOnMouseClicked(event -> handleShowAtRiskStudents());
            cardAtRisk.setStyle(cardAtRisk.getStyle() + " -fx-cursor: hand;");
        }


        if (lblTotalStudentsBadge != null) {
            lblTotalStudentsBadge.setOnMouseClicked(event -> handleShowDailyAttendanceSummary());
        }

        // Assigned professors map
        courseProfMap.put("Probability Theory", "Dr. Justice");
        courseProfMap.put("Computer Organization Principles", "Dr. Umair");
        courseProfMap.put("Data Structures", "Dr. Hammad");
        courseProfMap.put("Basic Circuit Analysis", "Dr. Hammad");
        courseProfMap.put("Comprehensive Design of Data Structures", "Dr. Lord");
        courseProfMap.put("Chinese 3", "Chen Junlan");

        courseComboBox.getItems().addAll(courseProfMap.keySet());
        courseComboBox.setValue("Probability Theory");

        if (profField != null) {
            profField.setText(courseProfMap.get("Probability Theory"));
            profField.setEditable(false);
        }

        if (datePicker != null && datePicker.getValue() == null) {
            datePicker.setValue(LocalDate.now());
        }

        // Setup controls for Start Date & Week Spinner
        setupViewModeControls();

        leftTable.getSelectionModel().setCellSelectionEnabled(true);
        leftTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        leftTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        rightTable.getSelectionModel().setCellSelectionEnabled(true);
        rightTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // Keep Edit/Delete working for the currently selected student.
        if (btnEditStudent != null) {
            btnEditStudent.setOnAction(event -> handleEditStudent());
        }
        if (btnDeleteStudent != null) {
            btnDeleteStudent.setOnAction(event -> handleDeleteStudent());
        }

        leftTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            updateStudentActionButtons();
            if (lblSelectedStatus != null) {
                lblSelectedStatus.setText(newSel != null ? newSel.getId() : "No Selection");
            }
        });

        rightTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            updateStudentActionButtons();
            if (lblSelectedStatus != null && newSel != null) {
                lblSelectedStatus.setText(newSel.getId());
            }
        });

        // SL No.
        if (colSerial != null) {
            colSerial.setText("SL No.");
            colSerial.setSortable(false);
            colSerial.setPrefWidth(55.0);
            colSerial.setMinWidth(45.0);
            colSerial.setMaxWidth(65.0);
            colSerial.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
            colSerial.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(String.valueOf(getIndex() + 1));
                        setStyle("-fx-alignment: CENTER; -fx-text-fill: #1F2937; -fx-font-weight: bold; -fx-font-size: 12px;");
                    }
                }
            });
        }

        // First Name Column (Used when collapsed)
        colFirstName = new TableColumn<>();
        colFirstName.setPrefWidth(100.0);
        colFirstName.setCellValueFactory(cell -> {
            String fullName = cell.getValue().getName();
            if (fullName == null || fullName.trim().isEmpty()) return new SimpleStringProperty("");
            return new SimpleStringProperty(fullName.trim().split("\\s+")[0]);
        });
        colFirstName.setVisible(false);
        leftTable.getColumns().add(1, colFirstName);
        updateColFirstNameHeader();

        if (colId != null) {
            colId.setCellValueFactory(cell -> cell.getValue().idProperty());
            colId.setPrefWidth(100.0);
            colId.setMinWidth(80.0);
        }

        if (colName != null) {
            colName.setCellValueFactory(cell -> cell.getValue().nameProperty());
            colName.setPrefWidth(175.0);
            colName.setMinWidth(120.0);
            updateColNameHeader();
            colName.setCellFactory(col -> new TableCell<>() {
                private final Text textNode = new Text();
                {
                    textNode.wrappingWidthProperty().bind(col.widthProperty().subtract(10));
                    textNode.setStyle("-fx-fill: -fx-text-inner-color;");
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        textNode.setText(item);
                        setGraphic(textNode);
                        setText(null);
                        setTooltip(new Tooltip(item));
                    }
                }
            });
        }

        if (colOverall != null) {
            colOverall.setCellValueFactory(cell -> cell.getValue().overallRateProperty());
            colOverall.setPrefWidth(75.0);
            colOverall.setMinWidth(65.0);
            colOverall.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }

                    AttendanceRecord record = getTableRow().getItem();
                    double val;

                    // Keep Weekly and Monthly Overall % completely separate.
                    // Weekly uses only the manually selected weekly dates.
                    // Monthly uses only the selected calendar month.
                    val = calculateActiveOverallRate(record);

                    setText(String.format("%.1f%%", val));
                    setStyle(val < 80.0
                            ? "-fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-alignment: CENTER;"
                            : "-fx-text-fill: #16A34A; -fx-font-weight: bold; -fx-alignment: CENTER;");
                }
            });
        }

        filteredData = new FilteredList<>(recordList, record -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(record -> {
                if (newVal == null || newVal.trim().isEmpty()) return true;
                String lower = newVal.toLowerCase().trim();
                return record.getName().toLowerCase().contains(lower) || record.getId().toLowerCase().contains(lower);
            });
            leftTable.refresh();
        });

        leftTable.setItems(filteredData);
        rightTable.setItems(filteredData);

        setupTableSynchronization();
        setupCourseCustomCells();

        courseComboBox.setOnAction(event -> {
            String selectedCourse = courseComboBox.getValue();
            if (selectedCourse != null && profField != null) {
                profField.setText(courseProfMap.getOrDefault(selectedCourse, ""));
            }
            loadAttendanceData();
        });

        if (datePicker != null) {
            datePicker.setOnAction(event -> loadAttendanceData());
        }

        loadAttendanceData();
    }

    // --- MANUAL SETUP: STARTING DATE + NUMBER OF WEEKS ---
    private void setupViewModeControls() {
        if (viewModeComboBox != null) {
            if (viewModeComboBox.getItems().isEmpty()) {
                viewModeComboBox.getItems().addAll("Weekly View", "Monthly View");
            }

            if (viewModeComboBox.getValue() == null) {
                viewModeComboBox.setValue("Weekly View");
            }

            viewModeComboBox.setOnAction(e -> {
                boolean isWeekly = "Weekly View".equalsIgnoreCase(viewModeComboBox.getValue());

                if (btnStartingDate != null) {
                    btnStartingDate.setVisible(isWeekly);
                    btnStartingDate.setManaged(isWeekly);
                }

                if (btnNumberOfWeeks != null) {
                    btnNumberOfWeeks.setVisible(isWeekly);
                    btnNumberOfWeeks.setManaged(isWeekly);
                }

                // Keep the original controls as hidden backing controls.
                if (startDatePicker != null) {
                    startDatePicker.setVisible(false);
                    startDatePicker.setManaged(false);
                }

                if (weekSpinner != null) {
                    weekSpinner.setVisible(false);
                    weekSpinner.setManaged(false);
                }

                if (datePicker != null) {
                    datePicker.setVisible(!isWeekly);
                    datePicker.setManaged(!isWeekly);
                }

                loadAttendanceData();
            });
        }

        // Backing value for the manual Starting Date button.
        if (startDatePicker != null) {
            if (startDatePicker.getValue() == null) {
                startDatePicker.setValue(manualStartDate);
            } else {
                manualStartDate = startDatePicker.getValue();
            }
            startDatePicker.setVisible(false);
            startDatePicker.setManaged(false);
        }

        // Backing value for the manual Number of Weeks button.
        if (weekSpinner != null) {
            if (weekSpinner.getValueFactory() == null) {
                weekSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 52, manualWeekCount));
            } else if (weekSpinner.getValue() != null) {
                manualWeekCount = weekSpinner.getValue();
            }
            weekSpinner.setVisible(false);
            weekSpinner.setManaged(false);
        }

        createManualWeeklyButtons();

        boolean isWeekly = viewModeComboBox != null
                && "Weekly View".equalsIgnoreCase(viewModeComboBox.getValue());

        if (btnStartingDate != null) {
            btnStartingDate.setVisible(isWeekly);
            btnStartingDate.setManaged(isWeekly);
        }
        if (btnNumberOfWeeks != null) {
            btnNumberOfWeeks.setVisible(isWeekly);
            btnNumberOfWeeks.setManaged(isWeekly);
        }
    }

    private void createManualWeeklyButtons() {
        if (weeklyButtonsAdded || viewModeComboBox == null) return;

        LocalDate startDate = manualStartDate;
        int weeks = manualWeekCount;

        btnStartingDate = new Button();
        btnNumberOfWeeks = new Button();

        updateStartingDateButtonText(startDate);
        updateNumberOfWeeksButtonText(weeks);

        String buttonStyle =
                "-fx-background-color: #FFFFFF; " +
                        "-fx-border-color: #CBD5E1; " +
                        "-fx-border-radius: 5px; " +
                        "-fx-background-radius: 5px; " +
                        "-fx-padding: 7px 12px; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #334155; " +
                        "-fx-cursor: hand;";

        btnStartingDate.setStyle(buttonStyle);
        btnNumberOfWeeks.setStyle(buttonStyle);

        btnStartingDate.setOnAction(e -> openStartingDateDialog());
        btnNumberOfWeeks.setOnAction(e -> openNumberOfWeeksDialog());

        if (viewModeComboBox.getParent() instanceof HBox parent) {
            int comboIndex = parent.getChildren().indexOf(viewModeComboBox);
            int insertIndex = comboIndex >= 0 ? comboIndex + 1 : parent.getChildren().size();

            parent.getChildren().add(insertIndex, btnStartingDate);
            parent.getChildren().add(insertIndex + 1, btnNumberOfWeeks);
            weeklyButtonsAdded = true;
        }
    }

    private void updateStartingDateButtonText(LocalDate date) {
        if (btnStartingDate != null && date != null) {
            btnStartingDate.setText("Starting Date: " + date.format(DateTimeFormatter.ofPattern("d MMM yyyy")));
        }
    }

    private void updateNumberOfWeeksButtonText(int weeks) {
        if (btnNumberOfWeeks != null) {
            btnNumberOfWeeks.setText("Number of Weeks: " + weeks);
        }
    }

    private void openStartingDateDialog() {
        DatePicker picker = new DatePicker(manualStartDate);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Starting Date");
        dialog.setHeaderText("Select the starting date for Weekly View");

        ButtonType setButton = new ButtonType("Set Date", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(setButton, ButtonType.CANCEL);

        VBox box = new VBox(10, new Label("Starting Date:"), picker);
        box.setPadding(new Insets(15));
        dialog.getDialogPane().setContent(box);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == setButton && picker.getValue() != null) {
            manualStartDate = picker.getValue();
            if (startDatePicker != null) {
                startDatePicker.setValue(manualStartDate);
            }
            updateStartingDateButtonText(manualStartDate);
            loadAttendanceData();
        }
    }

    private void openNumberOfWeeksDialog() {
        int currentWeeks = manualWeekCount;

        TextInputDialog dialog = new TextInputDialog(String.valueOf(currentWeeks));
        dialog.setTitle("Number of Weeks");
        dialog.setHeaderText("Enter the number of weeks for Weekly View");
        dialog.setContentText("Number of Weeks (1-52):");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String input = result.get().trim();
            try {
                int weeks = Integer.parseInt(input);

                if (weeks < 1 || weeks > 52) {
                    showAlert("Invalid Number", "Please enter a number between 1 and 52.");
                    return;
                }

                manualWeekCount = weeks;
                if (weekSpinner != null) {
                    weekSpinner.getValueFactory().setValue(weeks);
                }

                updateNumberOfWeeksButtonText(weeks);
                loadAttendanceData();
            } catch (NumberFormatException ex) {
                showAlert("Invalid Number", "Please enter a valid whole number between 1 and 52.");
            }
        }
    }

    /** Returns whether a log date belongs to the currently displayed view. */
    private boolean isDateInActiveView(LocalDate date) {
        if (date == null) return false;

        boolean isWeekly = viewModeComboBox != null
                && "Weekly View".equalsIgnoreCase(viewModeComboBox.getValue());

        if (isWeekly) {
            // Weekly View displays one log date per week, starting at manualStartDate.
            for (int i = 0; i < manualWeekCount; i++) {
                if (manualStartDate.plusWeeks(i).equals(date)) return true;
            }
            return false;
        }

        LocalDate selectedDate = (datePicker != null && datePicker.getValue() != null)
                ? datePicker.getValue() : LocalDate.now();
        return YearMonth.from(date).equals(YearMonth.from(selectedDate));
    }

    private double calculateActiveOverallRate(AttendanceRecord record) {
        return viewModeComboBox != null
                && "Weekly View".equalsIgnoreCase(viewModeComboBox.getValue())
                ? calculateWeeklyOverallRate(record)
                : calculateMonthlyOverallRate(record);
    }

    private void loadAttendanceData() {
        boolean isWeekly = viewModeComboBox != null && "Weekly View".equalsIgnoreCase(viewModeComboBox.getValue());
        if (isWeekly) {
            loadWeeklyData();
        } else {
            loadMonthlyData();
        }

        // Refresh only the Overall % column so switching views immediately
        // uses that view's own calculation.
        if (colOverall != null) {
            colOverall.setVisible(false);
            colOverall.setVisible(true);
        }
        if (leftTable != null) leftTable.refresh();
        if (rightTable != null) rightTable.refresh();
    }

    private double calculateMonthlyOverallRate(AttendanceRecord record) {
        if (record == null) return 0.0;

        LocalDate selectedDate = (datePicker != null && datePicker.getValue() != null)
                ? datePicker.getValue()
                : LocalDate.now();
        YearMonth selectedMonth = YearMonth.from(selectedDate);

        int present = 0;
        int absent = 0;
        int late = 0;

        for (Map.Entry<String, String> entry : record.getDailyMap().entrySet()) {
            try {
                LocalDate date = LocalDate.parse(entry.getKey());
                if (!YearMonth.from(date).equals(selectedMonth)) continue;

                String status = entry.getValue();
                if ("Present".equalsIgnoreCase(status)) {
                    present++;
                } else if ("Absent".equalsIgnoreCase(status)) {
                    absent++;
                } else if ("Late".equalsIgnoreCase(status)) {
                    late++;
                }
            } catch (Exception ignored) {
                // Ignore invalid date keys.
            }
        }

        int totalMarked = present + absent + late;
        if (totalMarked == 0) return 0.0;

        return ((present + late) * 100.0) / totalMarked;
    }

    private double calculateWeeklyOverallRate(AttendanceRecord record) {
        if (record == null) return 0.0;

        LocalDate startDate = manualStartDate;
        int totalWeeks = manualWeekCount;

        int present = 0;
        int absent = 0;
        int late = 0;

        for (int i = 0; i < totalWeeks; i++) {
            LocalDate weekDate = startDate.plusWeeks(i);
            String status = record.getStatusForDate(weekDate.toString());

            if ("Present".equalsIgnoreCase(status)) {
                present++;
            } else if ("Absent".equalsIgnoreCase(status)) {
                absent++;
            } else if ("Late".equalsIgnoreCase(status)) {
                late++;
            }
        }

        int totalMarked = present + absent + late;
        if (totalMarked == 0) return 0.0;

        return ((present + late) * 100.0) / totalMarked;
    }

    // --- DYNAMIC WEEK GENERATOR BASED ON START DATE AND TOTAL WEEKS ---
    private void loadWeeklyData() {
        String course = courseComboBox.getValue();
        if (course == null) return;

        // Get manual Start Date (e.g. 1 Oct)
        LocalDate startDate = manualStartDate;

        // Get total weeks count selected manually (e.g. 20 weeks)
        int totalWeeks = manualWeekCount;

        LocalDate endDate = startDate.plusWeeks(totalWeeks - 1);

        // Build columns: 1 Oct, 8 Oct, 15 Oct, 22 Oct ... up to N weeks
        buildWeeklyColumns(startDate, totalWeeks);

        recordList.clear();
        recordList.addAll(fetchAttendanceForDateRange(course, startDate, endDate));

        leftTable.refresh();
        rightTable.refresh();
        updateDashboardMetrics();
    }

    private void buildWeeklyColumns(LocalDate startDate, int totalWeeks) {
        rightTable.getColumns().clear();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d\n(EEE)");

        for (int i = 0; i < totalWeeks; i++) {
            // Adds 7 days per week step: 1 Oct, 8 Oct, 15 Oct...
            LocalDate currentDate = startDate.plusWeeks(i);
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);

            final String dateKey = currentDate.toString();
            String columnTitle = currentDate.format(formatter);

            TableColumn<AttendanceRecord, String> weekCol = new TableColumn<>();

            Label headerLabel = new Label(columnTitle);
            headerLabel.setStyle(isWeekend
                    ? "-fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-text-alignment: center;"
                    : "-fx-text-alignment: center;");
            weekCol.setGraphic(headerLabel);

            weekCol.setPrefWidth(85.0);
            weekCol.setMinWidth(75.0);
            weekCol.setMaxWidth(110.0);
            weekCol.setResizable(true);

            weekCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatusForDate(dateKey)));
            attachCellFactory(weekCol, dateKey, isWeekend);

            rightTable.getColumns().add(weekCol);
        }
    }

    // --- MONTHLY VIEW LOGIC ---
    private void loadMonthlyData() {
        String course = courseComboBox.getValue();
        LocalDate selectedDate = (datePicker != null && datePicker.getValue() != null) ? datePicker.getValue() : LocalDate.now();
        YearMonth yearMonth = YearMonth.from(selectedDate);

        buildMonthlyColumns(yearMonth);

        if (course != null) {
            recordList.clear();
            recordList.addAll(DatabaseHandler.getMonthlyAttendance(course, yearMonth));
            leftTable.refresh();
            rightTable.refresh();
            updateDashboardMetrics();
        }
    }

    private void buildMonthlyColumns(YearMonth yearMonth) {
        rightTable.getColumns().clear();

        int daysInMonth = yearMonth.lengthOfMonth();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM");
        String monthName = yearMonth.format(monthFormatter);

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentLocalDate = yearMonth.atDay(day);
            DayOfWeek dayOfWeek = currentLocalDate.getDayOfWeek();
            boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);

            final String dateKey = String.format("%04d-%02d-%02d", yearMonth.getYear(), yearMonth.getMonthValue(), day);
            String columnTitle = monthName + " " + day;

            TableColumn<AttendanceRecord, String> dayCol = new TableColumn<>();

            Label headerLabel = new Label(columnTitle);
            if (isWeekend) {
                headerLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
            }
            dayCol.setGraphic(headerLabel);

            dayCol.setPrefWidth(65.0);
            dayCol.setMinWidth(65.0);
            dayCol.setMaxWidth(65.0);

            dayCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatusForDate(dateKey)));
            attachCellFactory(dayCol, dateKey, isWeekend);

            rightTable.getColumns().add(dayCol);
        }
    }

    private void attachCellFactory(TableColumn<AttendanceRecord, String> dayCol, String dateKey, boolean isWeekend) {
        dayCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    setStyle(isWeekend ? "-fx-background-color: #FEF2F2;" : "");
                    setContextMenu(null);
                    setOnMouseClicked(null);
                    return;
                }

                AttendanceRecord record = getTableRow().getItem();
                String reason = record.getReasonForDate(dateKey);

                setStyle(isWeekend ? "-fx-background-color: #FEF2F2; -fx-alignment: CENTER;" : "-fx-alignment: CENTER;");

                if (item == null || item.trim().isEmpty()) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label();
                    badge.setPrefWidth(38);
                    badge.setPrefHeight(24);
                    badge.setAlignment(Pos.CENTER);

                    if ("Present".equalsIgnoreCase(item)) {
                        badge.setText("P");
                        badge.setStyle("-fx-background-color: #22C55E; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-font-size: 13px;");
                        setTooltip(new Tooltip("Status: Present"));
                    } else if ("Late".equalsIgnoreCase(item)) {
                        badge.setText(reason != null && !reason.isEmpty() ? "L (" + reason + ")" : "L");
                        badge.setStyle("-fx-background-color: #EAB308; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-font-size: 11px;");
                        setTooltip(new Tooltip("Status: Late" + (reason != null && !reason.isEmpty() ? "\nReason: " + reason : "")));
                    } else if ("Absent".equalsIgnoreCase(item)) {
                        badge.setText("A");
                        badge.setStyle("-fx-background-color: #EF4444; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-font-size: 13px;");
                        setTooltip(new Tooltip("Status: Absent"));
                    }

                    setAlignment(Pos.CENTER);
                    setGraphic(badge);
                    setText(null);
                }

                ContextMenu cellMenu = new ContextMenu();
                MenuItem itemP = new MenuItem("Present");
                MenuItem itemA = new MenuItem("Absent");
                MenuItem itemL = new MenuItem("Late");
                MenuItem itemC = new MenuItem("Clear");

                itemP.setOnAction(evt -> updateSingleCellStatus(record, dateKey, "Present", null));
                itemA.setOnAction(evt -> updateSingleCellStatus(record, dateKey, "Absent", null));
                itemL.setOnAction(evt -> {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Mark Late");
                    dialog.setHeaderText("Reason for Late Arrival");
                    dialog.setContentText("Please enter the reason:");
                    Optional<String> res = dialog.showAndWait();
                    String r = res.orElse("Late").trim();
                    if (r.isEmpty()) r = "Late";
                    updateSingleCellStatus(record, dateKey, "Late", r);
                });
                itemC.setOnAction(evt -> updateSingleCellStatus(record, dateKey, "", null));

                cellMenu.getItems().addAll(itemP, itemA, itemL, itemC);
                setContextMenu(cellMenu);

                setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        String currentStatus = record.getStatusForDate(dateKey);
                        String nextStatus = "Present".equalsIgnoreCase(currentStatus) ? "Absent" : "Present";
                        updateSingleCellStatus(record, dateKey, nextStatus, null);
                        rightTable.getSelectionModel().clearAndSelect(getIndex(), dayCol);
                        event.consume();
                    }
                });
            }
        });
    }

    private List<AttendanceRecord> fetchAttendanceForDateRange(String course, LocalDate start, LocalDate end) {
        Map<String, AttendanceRecord> recordMap = new LinkedHashMap<>();

        LocalDate current = start.withDayOfMonth(1);
        LocalDate endMonth = end.withDayOfMonth(1);

        while (!current.isAfter(endMonth)) {
            YearMonth ym = YearMonth.from(current);
            List<AttendanceRecord> monthRecords = DatabaseHandler.getMonthlyAttendance(course, ym);

            for (AttendanceRecord r : monthRecords) {
                AttendanceRecord existing = recordMap.get(r.getId());
                if (existing != null) {
                    for (Map.Entry<String, String> entry : r.getDailyMap().entrySet()) {
                        existing.addStatusForDate(entry.getKey(), entry.getValue(), r.getReasonForDate(entry.getKey()));
                    }
                } else {
                    recordMap.put(r.getId(), r);
                }
            }
            current = current.plusMonths(1);
        }

        return new ArrayList<>(recordMap.values());
    }

    @FXML
    private void handleShowAtRiskStudents() {
        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (AttendanceRecord record : recordList) {
            double activeRate = calculateActiveOverallRate(record);
            if (activeRate < 80.0) {
                count++;
                sb.append(String.format("%d. %s  (ID: %s)  —  %.1f%%\n",
                        count, record.getName(), record.getId(), activeRate));
            }
        }

        if (count == 0) {
            showAlert("At-Risk Students", "Great news! No students currently have attendance below 80%.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("At-Risk Students");
        alert.setHeaderText("Students with < 80% Attendance (" + count + " Total)");

        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(180);
        textArea.setPrefWidth(380);
        textArea.setStyle("-fx-font-family: 'monospace'; -fx-font-size: 13px;");

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    @FXML
    private void handleToggleLeftSidebar() {
        isLeftCollapsed = !isLeftCollapsed;
        if (isLeftCollapsed) {
            leftSidebarScroll.setVisible(false);
            leftSidebarScroll.setManaged(false);
            btnToggleLeft.setText("▶");
        } else {
            leftSidebarScroll.setVisible(true);
            leftSidebarScroll.setManaged(true);
            btnToggleLeft.setText("◀");
        }
    }

    @FXML
    private void handleToggleMiddleSection() {
        isMiddleCollapsed = !isMiddleCollapsed;
        toggleMiddleSectionState(isMiddleCollapsed);
    }

    private void toggleMiddleSectionState(boolean collapse) {
        if (collapse) {
            if (colId != null) colId.setVisible(false);
            if (colName != null) colName.setVisible(false);
            if (colOverall != null) colOverall.setVisible(false);
            if (colFirstName != null) colFirstName.setVisible(true);

            if (middleSectionContainer != null) {
                middleSectionContainer.setPrefWidth(160.0);
                middleSectionContainer.setMinWidth(160.0);
                middleSectionContainer.setMaxWidth(160.0);
            }
        } else {
            if (colId != null) colId.setVisible(true);
            if (colName != null) colName.setVisible(true);
            if (colOverall != null) colOverall.setVisible(true);
            if (colFirstName != null) colFirstName.setVisible(false);

            if (middleSectionContainer != null) {
                middleSectionContainer.setPrefWidth(405.0);
                middleSectionContainer.setMinWidth(405.0);
                middleSectionContainer.setMaxWidth(405.0);
            }
        }
        leftTable.refresh();
    }

    private void updateColNameHeader() {
        if (colName == null) return;

        HBox headerBox = new HBox(6);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label("Name");
        nameLabel.setStyle("-fx-font-weight: bold;");

        Button btnCollapseInline = new Button("◀");
        btnCollapseInline.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-font-size: 9px; -fx-padding: 1 4 1 4; -fx-background-radius: 3px; -fx-cursor: hand;");
        btnCollapseInline.setOnAction(e -> handleToggleMiddleSection());

        headerBox.getChildren().addAll(nameLabel, btnCollapseInline);
        colName.setGraphic(headerBox);
    }

    private void updateColFirstNameHeader() {
        if (colFirstName == null) return;

        HBox headerBox = new HBox(6);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label("First Name");
        nameLabel.setStyle("-fx-font-weight: bold;");

        Button btnExpandInline = new Button("▶");
        btnExpandInline.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-font-size: 9px; -fx-padding: 1 4 1 4; -fx-background-radius: 3px; -fx-cursor: hand;");
        btnExpandInline.setOnAction(e -> handleToggleMiddleSection());

        headerBox.getChildren().addAll(nameLabel, btnExpandInline);
        colFirstName.setGraphic(headerBox);
    }

    private void setupCourseCustomCells() {
        courseComboBox.setCellFactory(listView -> new ListCell<>() {
            private final Label label = new Label();
            private final Button btnEdit = new Button("✎");
            private final Button btnDelete = new Button("🗑");
            private final HBox hbox = new HBox(5);

            {
                btnEdit.setStyle("-fx-padding: 2 5 2 5; -fx-font-size: 11px; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-padding: 2 5 2 5; -fx-font-size: 11px; -fx-text-fill: red; -fx-cursor: hand;");

                HBox.setHgrow(label, Priority.ALWAYS);
                label.setMaxWidth(Double.MAX_VALUE);
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.getChildren().addAll(label, btnEdit, btnDelete);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    label.setText(item);
                    btnEdit.setOnAction(event -> {
                        event.consume();
                        courseComboBox.hide();
                        handleEditClassByName(item);
                    });
                    btnDelete.setOnAction(event -> {
                        event.consume();
                        courseComboBox.hide();
                        handleDeleteClassByName(item);
                    });
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });
    }

    @FXML
    private void handlePreviousDay() {
        boolean isWeekly = viewModeComboBox != null && "Weekly View".equalsIgnoreCase(viewModeComboBox.getValue());
        if (isWeekly) {
            manualStartDate = manualStartDate.minusDays(1);
            if (startDatePicker != null) startDatePicker.setValue(manualStartDate);
            updateStartingDateButtonText(manualStartDate);
            loadAttendanceData();
        } else if (datePicker != null) {
            LocalDate current = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
            datePicker.setValue(current.minusDays(1));
        }
    }

    @FXML
    private void handleNextDay() {
        boolean isWeekly = viewModeComboBox != null && "Weekly View".equalsIgnoreCase(viewModeComboBox.getValue());
        if (isWeekly) {
            manualStartDate = manualStartDate.plusDays(1);
            if (startDatePicker != null) startDatePicker.setValue(manualStartDate);
            updateStartingDateButtonText(manualStartDate);
            loadAttendanceData();
        } else if (datePicker != null) {
            LocalDate current = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
            datePicker.setValue(current.plusDays(1));
        }
    }

    private void setupTableSynchronization() {
        leftTable.getSelectionModel().getSelectedIndices().addListener((ListChangeListener<Integer>) change -> {
            if (!isSyncingSelection && !leftTable.getSelectionModel().getSelectedIndices().isEmpty()) {
                isSyncingSelection = true;
                int idx = leftTable.getSelectionModel().getSelectedIndices().getFirst();
                rightTable.getSelectionModel().select(idx, rightTable.getColumns().isEmpty() ? null : rightTable.getColumns().getFirst());
                isSyncingSelection = false;
            }
        });

        rightTable.getSelectionModel().getSelectedIndices().addListener((ListChangeListener<Integer>) change -> {
            if (!isSyncingSelection && !rightTable.getSelectionModel().getSelectedIndices().isEmpty()) {
                isSyncingSelection = true;
                int idx = rightTable.getSelectionModel().getSelectedIndices().getFirst();
                leftTable.getSelectionModel().select(idx, leftTable.getColumns().isEmpty() ? null : leftTable.getColumns().getFirst());
                isSyncingSelection = false;
            }
        });

        Platform.runLater(() -> {
            ScrollBar leftVerticalBar = findScrollBar(leftTable);
            ScrollBar rightVerticalBar = findScrollBar(rightTable);

            if (leftVerticalBar != null && rightVerticalBar != null) {
                leftVerticalBar.valueProperty().bindBidirectional(rightVerticalBar.valueProperty());
                leftVerticalBar.setPrefWidth(0);
                leftVerticalBar.setMaxWidth(0);
                leftVerticalBar.setOpacity(0);
            }
        });
    }

    private ScrollBar findScrollBar(TableView<?> table) {
        for (Node node : table.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar bar && bar.getOrientation() == Orientation.VERTICAL) {
                return bar;
            }
        }
        return null;
    }

    @FXML
    private void handleAddClassManually() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Class");
        dialog.setHeaderText("Enter Class and Professor Information");

        ButtonType saveButtonType = new ButtonType("Add Class", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtClassName = new TextField();
        txtClassName.setPromptText("e.g. Operating Systems");

        TextField txtProfName = new TextField();
        txtProfName.setPromptText("e.g. Dr. John Doe");

        grid.add(new Label("Class Name:"), 0, 0);
        grid.add(txtClassName, 1, 0);
        grid.add(new Label("Professor Name:"), 0, 1);
        grid.add(txtProfName, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            String className = txtClassName.getText().trim();
            String profName = txtProfName.getText().trim();

            if (!className.isEmpty()) {
                DatabaseHandler.addClass(className);
                courseProfMap.put(className, profName);

                if (!courseComboBox.getItems().contains(className)) {
                    courseComboBox.getItems().add(className);
                }

                courseComboBox.setValue(className);

                // A newly created class starts completely empty.
                // Do not carry over students from the previously selected class.
                recordList.clear();
                if (leftTable != null) leftTable.refresh();
                if (rightTable != null) rightTable.refresh();
                updateDashboardMetrics();

                if (profField != null) {
                    profField.setText(profName);
                }

                showAlert("Success", "Class '" + className + "' added successfully!");
            } else {
                showAlert("Input Error", "Class name cannot be empty.");
            }
        }
    }

    private void handleEditClassByName(String targetClass) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Class");
        dialog.setHeaderText("Editing details for: " + targetClass);

        ButtonType saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtClassName = new TextField(targetClass);
        TextField txtProfName = new TextField(courseProfMap.getOrDefault(targetClass, ""));

        grid.add(new Label("Class Name:"), 0, 0);
        grid.add(txtClassName, 1, 0);
        grid.add(new Label("Professor Name:"), 0, 1);
        grid.add(txtProfName, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            String newClassName = txtClassName.getText().trim();
            String newProfName = txtProfName.getText().trim();

            if (!newClassName.isEmpty()) {
                int index = courseComboBox.getItems().indexOf(targetClass);
                courseProfMap.remove(targetClass);
                courseProfMap.put(newClassName, newProfName);

                if (index >= 0) {
                    courseComboBox.getItems().set(index, newClassName);
                }

                courseComboBox.setValue(newClassName);
                if (profField != null) {
                    profField.setText(newProfName);
                }

                showAlert("Success", "Class updated successfully!");
            }
        }
    }

    private void handleDeleteClassByName(String targetClass) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Class");
        alert.setHeaderText("Delete '" + targetClass + "'?");
        alert.setContentText("Are you sure you want to remove this class?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            courseProfMap.remove(targetClass);
            courseComboBox.getItems().remove(targetClass);

            if (!courseComboBox.getItems().isEmpty()) {
                courseComboBox.setValue(courseComboBox.getItems().getFirst());
            } else {
                courseComboBox.setValue(null);
                if (profField != null) profField.setText("");
            }

            showAlert("Success", "Class deleted successfully.");
        }
    }

    private void updateSingleCellStatus(AttendanceRecord record, String dateKey, String newStatus, String reason) {
        DatabaseHandler.saveAttendanceStatus(record.getId(), courseComboBox.getValue(), dateKey, newStatus, reason);
        record.addStatusForDate(dateKey, newStatus, reason);

        rightTable.refresh();
        leftTable.refresh();
        updateDashboardMetrics();
    }

    @FXML
    private void handleMarkAllPresent() {
        String course = courseComboBox.getValue();
        boolean isWeekly = viewModeComboBox != null && "Weekly View".equalsIgnoreCase(viewModeComboBox.getValue());
        LocalDate selectedDate = isWeekly
                ? manualStartDate
                : (datePicker != null ? datePicker.getValue() : LocalDate.now());

        if (course == null || selectedDate == null) {
            showAlert("Error", "Please select a valid course and date.");
            return;
        }

        String dateStr = selectedDate.toString();
        DatabaseHandler.markAllPresentForCourseAndDate(course, dateStr);

        for (AttendanceRecord record : recordList) {
            record.addStatusForDate(dateStr, "Present", null);
        }

        leftTable.refresh();
        rightTable.refresh();
        updateDashboardMetrics();

        showAlert("Success", "Marked all students as Present for " + dateStr);
    }

    private void handleShowDailyAttendanceSummary() {
        boolean isWeekly = viewModeComboBox != null
                && "Weekly View".equalsIgnoreCase(viewModeComboBox.getValue());

        LocalDate selectedDate = isWeekly
                ? manualStartDate
                : (datePicker != null && datePicker.getValue() != null
                ? datePicker.getValue()
                : LocalDate.now());

        int present = 0;
        int absent = 0;
        int late = 0;

        for (AttendanceRecord record : recordList) {
            String status = record.getStatusForDate(selectedDate.toString());

            if ("Present".equalsIgnoreCase(status)) {
                present++;
            } else if ("Absent".equalsIgnoreCase(status)) {
                absent++;
            } else if ("Late".equalsIgnoreCase(status)) {
                late++;
            }
        }

        int marked = present + absent + late;
        int unmarked = Math.max(0, recordList.size() - marked);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Daily Attendance Summary");
        alert.setHeaderText("Attendance for " + selectedDate);
        alert.setContentText(
                "Enrolled Students: " + recordList.size() + "\n"
                        + "Present: " + present + "\n"
                        + "Absent: " + absent + "\n"
                        + "Late: " + late + "\n"
                        + "Not Marked: " + unmarked
        );
        alert.showAndWait();
    }

    private void updateDashboardMetrics() {
        int totalStudents = recordList.size();
        long present = 0, absent = 0, late = 0;
        int atRiskStudents = 0;

        for (AttendanceRecord record : recordList) {
            if (calculateActiveOverallRate(record) < 80.0) {
                atRiskStudents++;
            }

            // Count only logs that belong to the active view; never mix months
            // into Monthly View or dates outside the configured weekly columns.
            for (Map.Entry<String, String> entry : record.getDailyMap().entrySet()) {
                LocalDate logDate;
                try {
                    logDate = LocalDate.parse(entry.getKey());
                } catch (Exception ignored) {
                    continue;
                }
                if (!isDateInActiveView(logDate)) continue;

                String status = entry.getValue();
                if ("Present".equalsIgnoreCase(status)) present++;
                else if ("Absent".equalsIgnoreCase(status)) absent++;
                else if ("Late".equalsIgnoreCase(status)) late++;
            }
        }

        long totalLogs = present + absent + late;
        double rate = totalLogs > 0 ? (((double) (present + late)) / totalLogs) * 100.0 : 0.0;

        if (lblTotalStudentsBadge != null) lblTotalStudentsBadge.setText(totalStudents + " Enrolled");
        if (lblTotalLogs != null) lblTotalLogs.setText(String.valueOf(totalLogs));

        if (lblPresent != null) lblPresent.setText(String.valueOf(present));
        if (lblAbsent != null) lblAbsent.setText(String.valueOf(absent));
        if (lblLate != null) lblLate.setText(String.valueOf(late));

        if (lblPresentPct != null) lblPresentPct.setText(totalLogs > 0 ? String.format("(%.1f%%)", (present * 100.0) / totalLogs) : "(0.0%)");
        if (lblAbsentPct != null) lblAbsentPct.setText(totalLogs > 0 ? String.format("(%.1f%%)", (absent * 100.0) / totalLogs) : "(0.0%)");
        if (lblLatePct != null) lblLatePct.setText(totalLogs > 0 ? String.format("(%.1f%%)", (late * 100.0) / totalLogs) : "(0.0%)");

        String formattedRate = String.format("%.1f%%", rate);
        if (lblRate != null) {
            lblRate.setText(formattedRate);
            lblRate.setStyle(rate < 80.0 ? "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #DC2626;" : "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #15803D;");
        }
        if (lblDonutRate != null) {
            lblDonutRate.setText(formattedRate);
            lblDonutRate.setStyle(rate < 80.0 ? "-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #DC2626;" : "-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #15803D;");
        }
        if (progressAttendance != null) progressAttendance.setProgress(rate / 100.0);

        if (lblAtRiskCount != null && cardAtRisk != null) {
            lblAtRiskCount.setText(atRiskStudents + " Student" + (atRiskStudents == 1 ? "" : "s") + " < 80% Attendance");
            cardAtRisk.setVisible(atRiskStudents > 0);
            cardAtRisk.setManaged(atRiskStudents > 0);
        }

        if (attendancePieChart != null) {
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                    new PieChart.Data("Present", present),
                    new PieChart.Data("Absent", absent),
                    new PieChart.Data("Late", late)
            );
            attendancePieChart.setData(pieData);

            for (PieChart.Data data : pieData) {
                if (data.getNode() != null) {
                    setSliceColor(data);
                } else {
                    data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                        if (newNode != null) {
                            setSliceColor(data);
                        }
                    });
                }
            }
        }
    }

    private void setSliceColor(PieChart.Data data) {
        Node node = data.getNode();
        if (node == null) return;

        switch (data.getName().toLowerCase()) {
            case "present" -> node.setStyle("-fx-pie-color: #22C55E;");
            case "absent" -> node.setStyle("-fx-pie-color: #EF4444;");
            case "late" -> node.setStyle("-fx-pie-color: #EAB308;");
        }
    }

    @FXML
    private void handleImportStudents() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Student CSV File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        File selectedFile = fileChooser.showOpenDialog(leftTable.getScene().getWindow());

        if (selectedFile != null) {
            importCSVData(selectedFile);
        }
    }

    private void importCSVData(File file) {
        String line;
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 2) {
                    String id = data[0].trim();
                    String name = data[1].trim();

                    DatabaseHandler.insertStudent(id, name);
                    count++;
                }
            }
            showAlert("Import Successful", count + " students imported successfully!");
            loadAttendanceData();
        } catch (Exception e) {
            showAlert("Error", "Failed to import CSV file: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddStudent() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Student");
        dialog.setHeaderText("Enter Student Details");

        ButtonType addButtonType = new ButtonType("Add Student", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField txtId = new TextField();
        txtId.setPromptText("e.g. 4420250990");

        TextField txtName = new TextField();
        txtName.setPromptText("e.g. Seeam Sajim Mahmud");

        grid.add(new Label("Student ID:"), 0, 0);
        grid.add(txtId, 1, 0);
        grid.add(new Label("Full Name:"), 0, 1);
        grid.add(txtName, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == addButtonType) {
            String id = txtId.getText().trim();
            String name = txtName.getText().trim();

            if (!id.isEmpty() && !name.isEmpty()) {
                DatabaseHandler.addStudent(id, name);
                loadAttendanceData();
            } else {
                showAlert("Input Error", "Please provide both Student ID and Name.");
            }
        }
    }

    private void updateStudentActionButtons() {
        boolean hasSelection = leftTable.getSelectionModel().getSelectedItem() != null
                || rightTable.getSelectionModel().getSelectedItem() != null;

        if (btnEditStudent != null) btnEditStudent.setDisable(!hasSelection);
        if (btnDeleteStudent != null) btnDeleteStudent.setDisable(!hasSelection);
    }

    @FXML
    private void handleEditStudent() {
        AttendanceRecord selected = leftTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            selected = rightTable.getSelectionModel().getSelectedItem();
        }

        if (selected == null) {
            showAlert("No Selection", "Please select a student from the table to edit.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Student Information");
        dialog.setHeaderText("Updating Details for ID: " + selected.getId());

        ButtonType updateButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField editId = new TextField(selected.getId());
        TextField editName = new TextField(selected.getName());

        grid.add(new Label("Student ID:"), 0, 0);
        grid.add(editId, 1, 0);
        grid.add(new Label("Full Name:"), 0, 1);
        grid.add(editName, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == updateButtonType) {
            String newId = editId.getText().trim();
            String newName = editName.getText().trim();

            if (!newId.isEmpty() && !newName.isEmpty()) {
                DatabaseHandler.updateStudent(selected.getId(), newId, newName);
                loadAttendanceData();
            }
        }
    }

    @FXML
    private void handleDeleteStudent() {
        AttendanceRecord selected = leftTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            selected = rightTable.getSelectionModel().getSelectedItem();
        }

        if (selected != null) {
            DatabaseHandler.deleteStudent(selected.getId());
            loadAttendanceData();
        } else {
            showAlert("No Selection", "Select a student to delete.");
        }
    }

    @FXML
    private void handleExportReport() {
        handleExportCSV();
    }

    @FXML
    private void handleExportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Attendance Report");
        LocalDate date = (datePicker != null && datePicker.getValue() != null) ? datePicker.getValue() : LocalDate.now();
        fileChooser.setInitialFileName("Attendance_" + (courseComboBox.getValue() != null ? courseComboBox.getValue().replace(" ", "_") : "Report") + "_" + date + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File file = fileChooser.showSaveDialog(leftTable.getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("ID,Name,Course,AttendanceRate");
                for (AttendanceRecord r : recordList) {
                    writer.printf("%s,%s,%s,%.1f%%%n", r.getId(), r.getName(), r.getCourse(), r.getOverallRate());
                }
                showAlert("Success", "Exported attendance successfully!");
            } catch (Exception e) {
                showAlert("Error", "Failed to export CSV: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

