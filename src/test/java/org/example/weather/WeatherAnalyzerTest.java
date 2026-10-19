package org.example.weather;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeatherAnalyzerTest {
    @TempDir
    Path tempDir;

    private static final List<WeatherData> SAMPLE = List.of(
            new WeatherData(LocalDate.parse("2023-08-01"), 32.5, 65, 0.0),
            new WeatherData(LocalDate.parse("2023-08-02"), 35.0, 60, 0.2),
            new WeatherData(LocalDate.parse("2023-08-03"), 25.0, 72, 1.5),
            new WeatherData(LocalDate.parse("2023-09-01"), 18.0, 80, 0.0)
    );

    @Test
    void calculatesAverageForSpecificMonth() {
        var average = WeatherAnalyzer.averageTemperatureForMonth(SAMPLE, YearMonth.of(2023, 8));
        assertTrue(average.isPresent());
        assertEquals((32.5 + 35.0 + 25.0) / 3.0, average.getAsDouble(), 0.0001);
    }

    @Test
    void returnsEmptyAverageWhenMonthHasNoRecords() {
        assertTrue(WeatherAnalyzer.averageTemperatureForMonth(SAMPLE, YearMonth.of(2024, 1)).isEmpty());
    }

    @Test
    void thresholdUsesStrictlyGreaterThan() {
        assertEquals(2, WeatherAnalyzer.daysAboveThreshold(SAMPLE, 30.0).size());
        assertEquals(0, WeatherAnalyzer.daysAboveThreshold(SAMPLE, 35.0).size());
    }

    @Test
    void countsRainyDaysWithPositivePrecipitation() {
        assertEquals(2, WeatherAnalyzer.countRainyDays(SAMPLE));
    }

    @Test
    void categorizesTemperatureWithSwitchExpression() {
        assertEquals("Hot", WeatherAnalyzer.categoryFor(30.0));
        assertEquals("Warm", WeatherAnalyzer.categoryFor(20.0));
        assertEquals("Mild", WeatherAnalyzer.categoryFor(10.0));
        assertEquals("Cold", WeatherAnalyzer.categoryFor(9.9));
    }

    @Test
    void parsesCsvAndSortsByDate() throws Exception {
        Path csv = tempDir.resolve("weatherdata.csv");
        Files.writeString(csv, "Date,Temperature,Humidity,Precipitation\n"
                + "2023-08-02,35.0,60,0.2\n"
                + "2023-08-01,32.5,65,0.0\n");

        List<WeatherData> loaded = WeatherAnalyzer.readCsv(csv);
        assertEquals(2, loaded.size());
        assertEquals(LocalDate.parse("2023-08-01"), loaded.getFirst().date());
    }

    @Test
    void rejectsInvalidHumidity() {
        assertThrows(IllegalArgumentException.class,
                () -> WeatherData.fromCsvRow("2023-08-01,32.5,120,0.0"));
    }
}
