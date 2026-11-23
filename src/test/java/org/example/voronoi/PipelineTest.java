package org.example.voronoi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.voronoi.geometry.Coordinate;
import org.example.voronoi.geometry.Geometry;
import org.example.voronoi.io.GeoJsonExporter;
import org.example.voronoi.model.Station;
import org.example.voronoi.model.UserRecord;
import org.example.voronoi.model.VoronoiCell;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void endToEndRunTilesTheWindowAndAssignsEveryUser() {
        List<Station> stations = new ArrayList<>();
        int id = 1;
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                stations.add(new Station(id++, "s" + id, 123.0 + x * 0.02, 41.7 + y * 0.02));
            }
        }

        List<UserRecord> users = new ArrayList<>();
        Random random = new Random(2020L);
        for (int i = 0; i < 500; i++) {
            users.add(new UserRecord(
                    random.nextInt(24),
                    1000 + i,
                    123.0 + random.nextDouble() * 0.08,
                    41.7 + random.nextDouble() * 0.08,
                    1));
        }

        Pipeline.Result result = new Pipeline().run(stations, users, 0.05);

        assertEquals(25, result.getCells().size());
        assertEquals(500, result.getPopulation().getInsideCells() + result.getPopulation().getAssignedToNearest());
        assertEquals(0, result.getPopulation().getUnassigned());

        double expectedWindow = Geometry.areaInSquareMetres(
                Geometry.rectangle(123.0 - 0.08 * 0.05, 41.7 - 0.08 * 0.05,
                        123.08 + 0.08 * 0.05, 41.78 + 0.08 * 0.05));
        assertEquals(expectedWindow / 1_000_000.0, result.totalAreaSquareKilometres(),
                expectedWindow / 1_000_000.0 * 1e-6,
                "cell areas must tile the clip window");

        for (VoronoiCell cell : result.getCells()) {
            assertTrue(cell.getAreaSquareKilometres() > 0);
        }
    }

    @Test
    void geoJsonExportIsValidAndClosed() throws IOException {
        List<Station> stations = new ArrayList<>();
        stations.add(new Station(1, "s1", 123.00, 41.70));
        stations.add(new Station(2, "s2", 123.10, 41.70));
        stations.add(new Station(3, "s3", 123.10, 41.80));
        stations.add(new Station(4, "s4", 123.00, 41.80));

        List<UserRecord> users = new ArrayList<>();
        users.add(new UserRecord(9, 1, 123.01, 41.71, 1));

        Pipeline.Result result = new Pipeline().run(stations, users, 0.05);
        Path output = temporaryDirectory.resolve("cells.geojson");
        new GeoJsonExporter().export(result.getCells(), output);

        JsonNode root = new ObjectMapper().readTree(output.toFile());
        assertEquals("FeatureCollection", root.get("type").asText());
        assertEquals(4, root.get("features").size());
        assertEquals(4, root.get("properties").get("cellCount").asInt());

        for (JsonNode feature : root.get("features")) {
            assertEquals("Feature", feature.get("type").asText());
            JsonNode geometry = feature.get("geometry");
            assertEquals("Polygon", geometry.get("type").asText());
            JsonNode ring = geometry.get("coordinates").get(0);
            assertTrue(ring.size() >= 4);

            JsonNode first = ring.get(0);
            JsonNode last = ring.get(ring.size() - 1);
            assertEquals(first.get(0).asDouble(), last.get(0).asDouble(), 1e-12,
                    "rings must be closed");
            assertEquals(first.get(1).asDouble(), last.get(1).asDouble(), 1e-12);

            assertTrue(feature.get("properties").get("areaKm2").asDouble() > 0);
            assertEquals(24, feature.get("properties").get("hourlyUsers").size());
        }
    }

    @Test
    void emptyUserListIsAllowed() {
        List<Station> stations = new ArrayList<>();
        stations.add(new Station(1, "a", 123.0, 41.7));
        stations.add(new Station(2, "b", 123.1, 41.7));
        stations.add(new Station(3, "c", 123.1, 41.8));

        Pipeline.Result result = new Pipeline().run(stations, new ArrayList<UserRecord>(), 0.05);
        assertEquals(3, result.getCells().size());
        assertEquals(0, result.getPopulation().getTotalUsers());
    }

    @Test
    void coordinateAreaHelperAgreesWithStationBox() {
        Coordinate a = new Coordinate(123.0, 41.7);
        Coordinate b = new Coordinate(123.01, 41.71);
        double area = Geometry.areaInSquareMetres(Geometry.rectangle(
                a.getLongitude(), a.getLatitude(), b.getLongitude(), b.getLatitude()));
        assertTrue(area > 900_000 && area < 950_000, "a 0.01 degree cell near Shenyang is about 0.92 km2");
    }
}
