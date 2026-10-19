package org.example.weather;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/// JavaFX dashboard for the weather analyzer, including CSS styling and interactive filters.
///
/// This type is also an interface with static methods, so no custom concrete application
/// class is needed to start the JavaFX toolkit.
public interface WeatherDashboard {
    /// Opens the JavaFX dashboard and displays the supplied records.
    ///
    /// @param records weather data loaded from CSV
    static void show(List<WeatherData> records) {
        Platform.startup(() -> buildAndShow(records));
    }

    /// Builds the stage, summary cards, filters, and results table on the JavaFX application thread.
    private static void buildAndShow(List<WeatherData> records) {
        Stage stage = new Stage();
        stage.setTitle("Weather Data Analyzer");
        stage.setMinWidth(900);
        stage.setMinHeight(650);

        Label pageTitle = new Label("Weather Data Analyzer");
        pageTitle.getStyleClass().add("page-title");
        Label subtitle = new Label("Explore daily conditions, monthly averages, and rainy-day patterns.");
        subtitle.getStyleClass().add("subtitle");
        VBox header = new VBox(5, pageTitle, subtitle);
        header.getStyleClass().add("header");

        Label recordValue = new Label(Integer.toString(records.size()));
        Label overallAverageValue = new Label(String.format(Locale.ROOT, "%.1f °C",
                records.stream().mapToDouble(WeatherData::temperature).average().orElse(0.0)));
        Label rainyValue = new Label(Long.toString(WeatherAnalyzer.countRainyDays(records)));
        WeatherData hottest = records.stream().max(Comparator.comparingDouble(WeatherData::temperature)).orElseThrow();
        Label hottestValue = new Label(String.format(Locale.ROOT, "%.1f °C", hottest.temperature()));
        HBox stats = new HBox(14,
                statCard("RECORDS LOADED", recordValue),
                statCard("OVERALL AVERAGE", overallAverageValue),
                statCard("RAINY DAYS", rainyValue),
                statCard("WARMEST DAY", hottestValue));
        stats.getStyleClass().add("stats-row");

        TextField monthField = new TextField(YearMonth.from(records.getFirst().date()).toString());
        monthField.setPromptText("YYYY-MM");
        monthField.setPrefColumnCount(9);
        monthField.setTooltip(new Tooltip("Month format: YYYY-MM, for example 2023-08"));
        Button averageButton = new Button("Calculate average");
        averageButton.getStyleClass().add("button-primary");
        HBox monthControls = new HBox(8, fieldLabel("MONTHLY AVERAGE"), monthField, averageButton);
        monthControls.getStyleClass().add("filter-group");

        TextField thresholdField = new TextField("30");
        thresholdField.setPromptText("°C");
        thresholdField.setPrefColumnCount(5);
        Button thresholdButton = new Button("Find hot days");
        thresholdButton.getStyleClass().add("button-primary");
        HBox thresholdControls = new HBox(8, fieldLabel("TEMPERATURE THRESHOLD (°C)"), thresholdField, thresholdButton);
        thresholdControls.getStyleClass().add("filter-group");

        Button rainyButton = new Button("Show rainy days");
        Button allButton = new Button("Show all records");
        HBox actionControls = new HBox(9, rainyButton, allButton);
        actionControls.getStyleClass().add("filter-group");
        HBox filters = new HBox(16, monthControls, thresholdControls, actionControls);
        filters.getStyleClass().add("filters");

        Label resultLabel = new Label("Showing all weather records. Choose a filter or calculate a monthly average.");
        resultLabel.getStyleClass().add("result-message");
        resultLabel.setWrapText(true);

        TableView<WeatherData> table = new TableView<>(FXCollections.observableArrayList(records));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No weather records match this view."));
        table.getStyleClass().add("weather-table");
        addColumn(table, "Date", data -> data.date().toString());
        addColumn(table, "Temperature", data -> String.format(Locale.ROOT, "%.1f °C", data.temperature()));
        addColumn(table, "Humidity", data -> data.humidity() + "%");
        addColumn(table, "Precipitation", data -> String.format(Locale.ROOT, "%.1f mm", data.precipitation()));
        addColumn(table, "Category", data -> WeatherAnalyzer.categoryFor(data.temperature()));
        VBox.setVgrow(table, Priority.ALWAYS);

        averageButton.setOnAction(event -> {
            try {
                YearMonth month = YearMonth.parse(monthField.getText().strip());
                var average = WeatherAnalyzer.averageTemperatureForMonth(records, month);
                resultLabel.setText(average.isPresent()
                        ? String.format(Locale.ROOT, "Average temperature for %s: %.2f °C", month, average.getAsDouble())
                        : "No weather records were found for " + month + ".");
            } catch (DateTimeParseException exception) {
                resultLabel.setText("Invalid month. Use YYYY-MM, for example 2023-08.");
            }
        });

        thresholdButton.setOnAction(event -> {
            try {
                double threshold = Double.parseDouble(thresholdField.getText().strip());
                List<WeatherData> matches = WeatherAnalyzer.daysAboveThreshold(records, threshold);
                table.setItems(FXCollections.observableArrayList(matches));
                resultLabel.setText(String.format(Locale.ROOT, "%d day(s) were warmer than %.1f °C.", matches.size(), threshold));
            } catch (IllegalArgumentException exception) {
                resultLabel.setText("Invalid threshold. Enter a number such as 30 or 27.5.");
            }
        });

        rainyButton.setOnAction(event -> {
            List<WeatherData> rainyRecords = records.stream().filter(day -> day.precipitation() > 0.0).toList();
            table.setItems(FXCollections.observableArrayList(rainyRecords));
            resultLabel.setText("Showing " + rainyRecords.size() + " rainy day(s). A rainy day has precipitation greater than 0.0 mm.");
        });
        allButton.setOnAction(event -> {
            table.setItems(FXCollections.observableArrayList(records));
            resultLabel.setText("Showing all " + records.size() + " weather records.");
        });

        VBox root = new VBox(18, header, stats, filters, resultLabel, table);
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(26));
        Scene scene = new Scene(root, 1180, 780);
        var stylesheet = WeatherDashboard.class.getResource("/styles.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        stage.setScene(scene);
        stage.show();
    }

    /// Makes a styled dashboard statistic card.
    private static VBox statCard(String heading, Label value) {
        Label title = new Label(heading);
        title.getStyleClass().add("stat-title");
        value.getStyleClass().add("stat-value");
        VBox card = new VBox(9, title, value);
        card.getStyleClass().add("stat-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    /// Creates a compact uppercase label for a filter control.
    private static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    /// Adds a typed table column whose values are extracted with a lambda.
    private static <T> void addColumn(TableView<WeatherData> table, String heading,
                                      Function<WeatherData, T> extractor) {
        TableColumn<WeatherData, T> column = new TableColumn<>(heading);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(extractor.apply(cell.getValue())));
        column.setSortable(true);
        table.getColumns().add(column);
    }
}
