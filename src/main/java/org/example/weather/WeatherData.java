package org.example.weather;

import java.time.LocalDate;
import java.util.Objects;

/// An immutable weather observation for a single calendar day.
///
/// Temperature is measured in degrees Celsius, precipitation in millimeters,
/// and humidity as a percentage from 0 through 100.
///
/// Example:
/// ```java
/// var day = new WeatherData(LocalDate.parse("2023-08-01"), 32.5, 65, 0.0);
/// ```
///
/// @param date observation date
/// @param temperature temperature in degrees Celsius
/// @param humidity relative humidity percentage
/// @param precipitation precipitation in millimeters
public record WeatherData(LocalDate date, double temperature, int humidity, double precipitation) {
    /// Validates the values stored in the immutable weather record.
    public WeatherData {
        Objects.requireNonNull(date, "date cannot be null");
        if (!Double.isFinite(temperature)) {
            throw new IllegalArgumentException("temperature must be a finite number");
        }
        if (humidity < 0 || humidity > 100) {
            throw new IllegalArgumentException("humidity must be between 0 and 100");
        }
        if (!Double.isFinite(precipitation) || precipitation < 0) {
            throw new IllegalArgumentException("precipitation must be finite and non-negative");
        }
    }

    /// Converts one CSV data row into a validated weather record.
    ///
    /// The expected order is `Date,Temperature,Humidity,Precipitation`.
    ///
    /// Example:
    /// ```java
    /// WeatherData day = WeatherData.fromCsvRow("2023-08-01,32.5,65,0.0");
    /// ```
    ///
    /// @param row one comma-separated data row, without the header
    /// @return the parsed weather observation
    /// @throws IllegalArgumentException when the row has the wrong number of fields or invalid values
    public static WeatherData fromCsvRow(String row) {
        Objects.requireNonNull(row, "row cannot be null");
        String[] fields = row.split(",", -1);
        if (fields.length != 4) {
            throw new IllegalArgumentException("expected 4 comma-separated fields but found " + fields.length);
        }

        try {
            return new WeatherData(
                    LocalDate.parse(fields[0].strip()),
                    Double.parseDouble(fields[1].strip()),
                    Integer.parseInt(fields[2].strip()),
                    Double.parseDouble(fields[3].strip())
            );
        } catch (RuntimeException exception) {
            if (exception instanceof IllegalArgumentException illegalArgumentException
                    && illegalArgumentException.getMessage() != null
                    && (illegalArgumentException.getMessage().startsWith("expected ")
                    || illegalArgumentException.getMessage().startsWith("humidity ")
                    || illegalArgumentException.getMessage().startsWith("temperature ")
                    || illegalArgumentException.getMessage().startsWith("precipitation "))) {
                throw illegalArgumentException;
            }
            throw new IllegalArgumentException("could not parse date or numeric weather values: " + row, exception);
        }
    }
}
