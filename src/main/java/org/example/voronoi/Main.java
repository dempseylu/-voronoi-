package org.example.voronoi;

import org.example.voronoi.config.AppConfig;
import org.example.voronoi.io.GeoJsonExporter;
import org.example.voronoi.io.SourceFactory;
import org.example.voronoi.io.StationSource;
import org.example.voronoi.io.UserSource;
import org.example.voronoi.model.Station;
import org.example.voronoi.model.UserRecord;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Command line entry point.
 *
 * <pre>
 * java -jar voronoi-analysis.jar --stations=csv:samples/stations.csv \
 *                                 --users=csv:samples/users.csv \
 *                                 --out=out/cells.geojson
 * </pre>
 */
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    private static final String USAGE = String.join(System.lineSeparator(),
            "Usage: java -jar voronoi-analysis.jar [options]",
            "",
            "  --stations=<db|csv:path>   station source (default: stations.source)",
            "  --users=<db|csv:path>      user source    (default: users.source)",
            "  --out=<path>               GeoJSON output (default: output)",
            "  --clip-margin=<ratio>      Voronoi clip margin, 0..1 (default: clip.margin)",
            "  --help                     show this message");

    private Main() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.println(USAGE);
            System.exit(2);
        } catch (Exception e) {
            LOG.severe("Analysis failed: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {
        Map<String, String> options = parse(args);
        if (options.containsKey("help")) {
            System.out.println(USAGE);
            return;
        }

        AppConfig config = AppConfig.load();
        String stationSpec = options.getOrDefault("stations", config.defaultStationSource());
        String userSpec = options.getOrDefault("users", config.defaultUserSource());
        Path output = Paths.get(options.getOrDefault("out", config.defaultOutput()));
        double clipMargin = options.containsKey("clip-margin")
                ? parseDouble("clip-margin", options.get("clip-margin"))
                : config.clipMarginRatio();

        SourceFactory sources = new SourceFactory(config);

        long start = System.currentTimeMillis();
        try (StationSource stationSource = sources.stationSource(stationSpec);
             UserSource userSource = sources.userSource(userSpec)) {

            List<Station> stations = stationSource.load();
            List<UserRecord> users = userSource.load();
            LOG.info("Loaded " + stations.size() + " stations and " + users.size() + " user records");

            Pipeline.Result result = new Pipeline().run(stations, users, clipMargin);
            new GeoJsonExporter().export(result.getCells(), output);

            long elapsed = System.currentTimeMillis() - start;
            LOG.info("Triangulation : " + result.getTriangulation().getTriangles().size() + " triangles, "
                    + result.getTriangulation().getBoundaryEdges().size() + " boundary points");
            LOG.info("Voronoi cells : " + result.getCells().size()
                    + " (duplicates skipped: " + result.getDuplicateSitesSkipped() + ")");
            LOG.info("Population    : " + result.getPopulation());
            LOG.info("Total area    : " + String.format("%.3f km2", result.totalAreaSquareKilometres()));
            LOG.info("Output        : " + output.toAbsolutePath() + " (" + elapsed + " ms)");
        }
    }

    private static Map<String, String> parse(String[] args) {        Map<String, String> options = new HashMap<>();
        for (String arg : args) {
            if ("--help".equals(arg) || "-h".equals(arg)) {
                options.put("help", "true");
                continue;
            }
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Unexpected argument: " + arg);
            }
            int separator = arg.indexOf('=');
            if (separator < 0) {
                throw new IllegalArgumentException("Option needs a value: " + arg);
            }
            String key = arg.substring(2, separator);
            String value = arg.substring(separator + 1);
            switch (key) {
                case "stations":
                case "users":
                case "out":
                case "clip-margin":
                    options.put(key, value);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown option: --" + key);
            }
        }
        return options;
    }

    private static double parseDouble(String name, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--" + name + " expects a number, got: " + value);
        }
    }
}
