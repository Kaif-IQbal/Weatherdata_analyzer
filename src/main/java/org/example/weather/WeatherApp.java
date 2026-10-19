package org.example.weather;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.Scanner;

/// Entry point for the Weather Data Analyzer.
///
/// The application entry point is an interface with static methods, keeping the
/// project free of custom concrete application classes. Use `--gui` to open the
/// JavaFX dashboard; otherwise, the command-line menu is shown.
///
/// Examples:
/// ```shell
/// mvn exec:java
/// mvn javafx:run -Djavafx.args=--gui
/// mvn exec:java -Dexec.args="data/weatherdata.csv"
/// ```
public interface WeatherApp {
    /// Loads the CSV data and starts either the command-line menu or graphical dashboard.
    ///
    /// @param args optional CSV path and/or `--gui`
    static void main(String[] args) {
        boolean guiMode = Arrays.asList(args).contains("--gui");
        Path csvPath = chooseCsvPath(args);

        final List<WeatherData> records;
        try {
            records = WeatherAnalyzer.readCsv(csvPath);
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("Could not load weather data: " + exception.getMessage());
            System.err.println("Check the CSV path and format. Default file: data/weatherdata.csv");
            return;
        }

        if (guiMode) {
            WeatherDashboard.show(records);
        } else {
            runMenu(records, csvPath);
        }
    }

    /// Chooses the explicitly supplied CSV path or the bundled sample-data path.
    private static Path chooseCsvPath(String[] args) {
        var explicitPath = Arrays.stream(args)
                .filter(argument -> !argument.equals("--gui"))
                .findFirst();
        if (explicitPath.isPresent()) {
            return Path.of(explicitPath.get());
        }

        // IntelliJ can use the extracted parent folder as the working directory.
        // Search this folder and its parents so data/weatherdata.csv is found even
        // when the project folder was extracted inside another folder of the same name.
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        List<Path> candidates = new ArrayList<>();
        Path folder = workingDirectory;
        for (int level = 0; folder != null && level < 8; level++, folder = folder.getParent()) {
            candidates.add(folder.resolve("data/weatherdata.csv"));
            candidates.add(folder.resolve("weatherdata.csv"));
            candidates.add(folder.resolve("weather-data-analyzer/data/weatherdata.csv"));
            candidates.add(folder.resolve("weather-data-analyzer/weather-data-analyzer/data/weatherdata.csv"));
        }

        return candidates.stream()
                .distinct()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElse(workingDirectory.resolve("data/weatherdata.csv"));
    }

    /// Runs the interactive command-line menu until the user selects Exit.
    private static void runMenu(List<WeatherData> records, Path csvPath) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("""

                    ==============================================
                       WEATHER DATA ANALYZER  |  JAVA 24
                    ==============================================
                    """);
            System.out.printf(Locale.ROOT, "Loaded %,d weather records from %s%n", records.size(), csvPath);

            boolean running = true;
            while (running && scanner.hasNextLine()) {
                System.out.println("""

                    1. Average temperature for a month
                    2. Days above a temperature threshold
                    3. Count rainy days
                    4. Show complete summary
                    0. Exit
                    """);
                System.out.print("Choose an option: ");
                String choice = scanner.nextLine().strip();

                switch (choice) {
                    case "1" -> showMonthlyAverage(records, scanner);
                    case "2" -> showDaysAboveThreshold(records, scanner);
                    case "3" -> System.out.printf("Rainy days: %d%n", WeatherAnalyzer.countRainyDays(records));
                    case "4" -> showSummary(records);
                    case "0" -> running = false;
                    default -> System.out.println("Please enter 1, 2, 3, 4, or 0.");
                }
            }
        }
        System.out.println("Thanks for using Weather Data Analyzer.");
    }

    /// Prompts for a month in `YYYY-MM` form and prints its average temperature.
    private static void showMonthlyAverage(List<WeatherData> records, Scanner scanner) {
        System.out.print("Enter year and month (YYYY-MM, for example 2023-08): ");
        String input = scanner.nextLine().strip();
        try {
            YearMonth month = YearMonth.parse(input);
            OptionalDouble average = WeatherAnalyzer.averageTemperatureForMonth(records, month);
            if (average.isPresent()) {
                System.out.printf(Locale.ROOT, "Average temperature for %s: %.2f °C%n", month, average.getAsDouble());
            } else {
                System.out.println("No records were found for " + month + ".");
            }
        } catch (DateTimeParseException exception) {
            System.out.println("Invalid month. Use YYYY-MM, such as 2023-08.");
        }
    }

    /// Prompts for a Celsius threshold and prints matching days.
    private static void showDaysAboveThreshold(List<WeatherData> records, Scanner scanner) {
        System.out.print("Enter temperature threshold in °C: ");
        String input = scanner.nextLine().strip();
        try {
            double threshold = Double.parseDouble(input);
            List<WeatherData> matches = WeatherAnalyzer.daysAboveThreshold(records, threshold);
            if (matches.isEmpty()) {
                System.out.printf(Locale.ROOT, "No days were hotter than %.1f °C.%n", threshold);
                return;
            }
            System.out.printf(Locale.ROOT, "%d day(s) were hotter than %.1f °C:%n", matches.size(), threshold);
            matches.forEach(day -> System.out.printf(Locale.ROOT,
                    "  %s | %.1f °C | %s | humidity %d%% | rain %.1f mm%n",
                    day.date(), day.temperature(), WeatherAnalyzer.categoryFor(day.temperature()),
                    day.humidity(), day.precipitation()));
        } catch (IllegalArgumentException exception) {
            System.out.println("Invalid threshold. Enter a number such as 30 or 27.5.");
        }
    }

    /// Prints overall statistics and a few useful details from the loaded dataset.
    private static void showSummary(List<WeatherData> records) {
        double average = records.stream().mapToDouble(WeatherData::temperature).average().orElse(Double.NaN);
        double highest = records.stream().mapToDouble(WeatherData::temperature).max().orElse(Double.NaN);
        double lowest = records.stream().mapToDouble(WeatherData::temperature).min().orElse(Double.NaN);
        long rainyDays = WeatherAnalyzer.countRainyDays(records);
        var first = records.getFirst();
        var last = records.getLast();

        String report = """
                WEATHER SUMMARY
                --------------
                Records analyzed: %d
                Date range:       %s to %s
                Average temp:     %.2f °C
                Highest temp:     %.1f °C (%s)
                Lowest temp:      %.1f °C (%s)
                Rainy days:       %d
                Hot days (>=30):  %d
                """.formatted(records.size(), first.date(), last.date(), average,
                highest, records.stream().max(java.util.Comparator.comparingDouble(WeatherData::temperature)).orElseThrow().date(),
                lowest, records.stream().min(java.util.Comparator.comparingDouble(WeatherData::temperature)).orElseThrow().date(),
                rainyDays, records.stream().filter(day -> day.temperature() >= 30).count());
        System.out.println(report);
    }
}
