# Weather Data Analyzer

A Java weather-data project that reads a CSV file and analyzes temperature, humidity, and precipitation. It includes an interactive command-line menu and an optional JavaFX dashboard styled with CSS.

## Requirements

- JDK 24 (recommended, and required by this Maven configuration)
- IntelliJ IDEA with Maven support
- Internet access the first time Maven downloads JavaFX, JUnit, and Maven plugins

**Why Java 24?** Markdown documentation comments written with `///` were introduced in JDK 23. The project targets Java 24 and uses these comments as requested in the assignment. JavaFX powers the optional GUI.

## Open in IntelliJ IDEA

1. Download the ZIP and extract it into a new folder.
2. In IntelliJ, select **File → Open** and choose the extracted folder that contains `pom.xml` directly.
3. Select **Open as Project** if prompted, and allow IntelliJ to import Maven dependencies.
4. Set the Project SDK to **JDK 24** in **File → Project Structure → Project**.
5. Keep `data/weatherdata.csv` in the project. The app searches the project folder and its parents for this file, so a nested extraction folder will not prevent it from loading.

## Run the application

For the usual IntelliJ workflow, open `WeatherApp.java` and click the green Run triangle next to `main`. You do not need to type Maven commands every time.

You can optionally open the IntelliJ terminal at the folder containing `pom.xml` and run the following checks.

### Command-line application

```powershell
mvn clean test
mvn exec:java
```

The menu can calculate the average temperature for a month, list dates above a threshold, count rainy days, and print a full summary.

### JavaFX dashboard (CSS-styled GUI)

```powershell
mvn javafx:run -Djavafx.args=--gui
```

The dashboard includes summary cards, a monthly-average control, a temperature-threshold filter, a rainy-day filter, and a sortable weather table.

### Use another CSV file

```powershell
mvn exec:java -Dexec.args="C:/full/path/to/weatherdata.csv"
```

The CSV header must be:

```csv
Date,Temperature,Humidity,Precipitation
```

Rows use ISO dates (`YYYY-MM-DD`), temperature in Celsius, humidity from 0 through 100, and precipitation in millimeters. Blank lines are ignored. The parser reports the line number for malformed records.

## Example CSV

The included `data/weatherdata.csv` contains daily sample readings for August and September 2023. These values are example data for demonstrating the program, not verified historical observations.

```csv
Date,Temperature,Humidity,Precipitation
2023-08-01,32.5,65,0.0
2023-08-02,35.0,60,0.2
```

## Features mapped to the assignment

| Assignment requirement | Implementation |
| --- | --- |
| Parse weather data from CSV | `WeatherAnalyzer.readCsv(Path)` |
| Average temperature for a specific month | `averageTemperatureForMonth` using a stream and `OptionalDouble` |
| Find days above a threshold | `daysAboveThreshold` with a lambda predicate |
| Count rainy days | `countRainyDays` (`precipitation > 0.0`) |
| Enhanced switch | `categoryFor` uses a switch expression with arrow labels |
| Java records | `WeatherData` is an immutable record with validation |
| Lambdas and streams | CSV pipeline, filtering, sorting, mapping, averages, and counts |
| Text blocks | CLI banner, summary output, and documentation examples |
| Modern Java (15–24) | Records, switch expressions, `Stream.toList()`, `List.getFirst/getLast`, and Markdown Javadoc comments |
| Markdown Javadoc | `///` comments and fenced Java examples on public APIs and helper methods |
| No explicit application classes | Application logic uses interfaces and the `WeatherData` record; no custom concrete application class is declared |
| GUI with CSS (extra credit) | Optional JavaFX dashboard in `WeatherDashboard`, styled in `src/main/resources/styles.css` |
| GitHub README | This document explains setup, use, features, and input format |

## Weather category rules

- **Hot:** 30 °C or higher
- **Warm:** 20 °C to below 30 °C
- **Mild:** 10 °C to below 20 °C
- **Cold:** below 10 °C

A day counts as rainy when precipitation is greater than `0.0` mm. “Above a threshold” means strictly greater than the threshold, not equal to it.

## Tests

Run all automated unit tests with:

```powershell
mvn test
```

Tests cover monthly averages, empty-month behavior, threshold comparisons, rainy-day counts, category boundaries, CSV parsing/sorting, and invalid humidity.

## Generate Javadoc

With JDK 24 selected:

```powershell
mvn javadoc:javadoc
```

The generated documentation is written under `target/site/apidocs/`.

## Upload to GitHub

1. Sign in to GitHub and create a **new empty repository** named `weather-data-analyzer` (do not add a second README or `.gitignore` on GitHub).
2. In IntelliJ, open the Terminal at the folder containing `pom.xml` and run these commands, replacing `YOUR-USERNAME` with your GitHub username:

```powershell
git init
git add .
git commit -m "Complete Weather Data Analyzer assignment"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/weather-data-analyzer.git
git push -u origin main
```

3. Open the GitHub repository in your browser and confirm that `README.md`, `pom.xml`, `src/`, and `data/weatherdata.csv` appear. Submit the repository URL in your course assignment page.

If Git says that your identity is not configured, set it once with your own information:

```powershell
git config --global user.name "Your Name"
git config --global user.email "your-github-email@example.com"
```

## Project structure

```text
weather-data-analyzer/
├── data/
│   └── weatherdata.csv
├── src/
│   ├── main/
│   │   ├── java/org/example/weather/
│   │   │   ├── WeatherApp.java
│   │   │   ├── WeatherAnalyzer.java
│   │   │   ├── WeatherData.java
│   │   │   └── WeatherDashboard.java
│   │   └── resources/styles.css
│   └── test/java/org/example/weather/WeatherAnalyzerTest.java
├── pom.xml
└── README.md
```
