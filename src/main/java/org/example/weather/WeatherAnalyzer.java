package org.example.weather;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.IntStream;

/// Defines stream-based operations for reading and analyzing weather observations.
///
/// This interface intentionally contains static operations rather than requiring
/// a custom concrete analyzer class.
public interface WeatherAnalyzer {
    /// Reads a weather CSV file, validates its header and rows, and sorts records by date.
    ///
    /// Expected header: `Date,Temperature,Humidity,Precipitation`.
    /// Blank lines are ignored. If a record is malformed, the exception identifies
    /// the CSV line number so the input can be corrected.
    ///
    /// @param csvPath path to the CSV file
    /// @return an immutable list of weather observations ordered by date
    /// @throws IOException if the file cannot be read or its header is invalid
    /// @throws IllegalArgumentException if a data row contains invalid weather values
    static List<WeatherData> readCsv(Path csvPath) throws IOException {
        var lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IOException("CSV file is empty: " + csvPath);
        }

        String header = lines.getFirst().replace("\uFEFF", "").strip().replace(" ", "");
        String expectedHeader = "Date,Temperature,Humidity,Precipitation";
        if (!header.equalsIgnoreCase(expectedHeader)) {
            throw new IOException("Invalid CSV header. Expected: " + expectedHeader);
        }

        record CsvLine(int number, String text) { }

        List<WeatherData> records = IntStream.range(1, lines.size())
                .mapToObj(index -> new CsvLine(index + 1, lines.get(index)))
                .filter(line -> !line.text().isBlank())
                .map(line -> {
                    try {
                        return WeatherData.fromCsvRow(line.text());
                    } catch (IllegalArgumentException exception) {
                        throw new IllegalArgumentException(
                                "Invalid CSV data on line " + line.number() + ": " + exception.getMessage(),
                                exception
                        );
                    }
                })
                .sorted(Comparator.comparing(WeatherData::date))
                .toList();

        if (records.isEmpty()) {
            throw new IOException("CSV file has a header but no weather data rows: " + csvPath);
        }
        return records;
    }

    /// Calculates the mean temperature for one year and month.
    ///
    /// @param records weather observations to search
    /// @param month month and year to analyze
    /// @return average temperature, or an empty result if no record matches the month
    static OptionalDouble averageTemperatureForMonth(List<WeatherData> records, YearMonth month) {
        return records.stream()
                .filter(day -> YearMonth.from(day.date()).equals(month))
                .mapToDouble(WeatherData::temperature)
                .average();
    }

    /// Returns observations with temperature strictly greater than the supplied threshold.
    ///
    /// @param records weather observations to search
    /// @param threshold temperature threshold in degrees Celsius
    /// @return matching observations in date order
    static List<WeatherData> daysAboveThreshold(List<WeatherData> records, double threshold) {
        if (!Double.isFinite(threshold)) {
            throw new IllegalArgumentException("threshold must be a finite number");
        }
        return records.stream()
                .filter(day -> day.temperature() > threshold)
                .sorted(Comparator.comparing(WeatherData::date))
                .toList();
    }

    /// Counts days with measurable precipitation.
    ///
    /// A day is rainy when precipitation is greater than `0.0` millimeters.
    ///
    /// @param records weather observations to search
    /// @return number of rainy observations
    static long countRainyDays(List<WeatherData> records) {
        return records.stream()
                .filter(day -> day.precipitation() > 0.0)
                .count();
    }

    /// Assigns a weather label using an enhanced switch expression.
    ///
    /// | Temperature | Category |
    /// | --- | --- |
    /// | 30 °C and above | Hot |
    /// | 20–29.9 °C | Warm |
    /// | 10–19.9 °C | Mild |
    /// | Below 10 °C | Cold |
    ///
    /// @param temperature temperature in degrees Celsius
    /// @return the category label
    static String categoryFor(double temperature) {
        if (!Double.isFinite(temperature)) {
            throw new IllegalArgumentException("temperature must be a finite number");
        }
        int band = temperature >= 30 ? 3 : temperature >= 20 ? 2 : temperature >= 10 ? 1 : 0;
        return switch (band) {
            case 3 -> "Hot";
            case 2 -> "Warm";
            case 1 -> "Mild";
            default -> "Cold";
        };
    }
}
