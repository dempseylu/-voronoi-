package org.example.voronoi;

import org.example.voronoi.io.CsvStationSource;
import org.example.voronoi.io.CsvUserSource;
import org.example.voronoi.model.Station;
import org.example.voronoi.model.UserRecord;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guard on the official A06 competition data.
 *
 * <p>The 2020 development log recorded 1519 stations and 3022 triangles. This
 * test pins the numbers produced by the corrected implementation so that a
 * future change cannot silently alter the geometry.</p>
 *
 * <p>It is skipped when {@code data/a06} is absent, so the build still works
 * without the optional data set.</p>
 */
class RealDataRegressionTest {

    private static final Path STATIONS = Paths.get("data", "a06", "stations.csv");
    private static final Path USERS = Paths.get("data", "a06", "users.csv");

    @Test
    void officialA06DataReproducesTheOriginalCounts() {
        Assumptions.assumeTrue(Files.exists(STATIONS) && Files.exists(USERS),
                "data/a06 is not present; run tools/import_a06.py first");

        List<Station> stations = new CsvStationSource(STATIONS).load();
        List<UserRecord> users = new CsvUserSource(USERS).load();

        Pipeline.Result result = new Pipeline().run(stations, users, 0.05);

        assertEquals(1519, stations.size(), "station coordinates after de-duplication");
        assertEquals(1519, result.getCells().size());
        assertEquals(3023, result.getTriangulation().getTriangles().size());
        assertEquals(9849, users.size());
        assertEquals(9849, result.getPopulation().getInsideCells(),
                "every record must fall inside a cell");
        assertEquals(0, result.getPopulation().getUnassigned());

        double area = result.totalAreaSquareKilometres();
        assertTrue(area > 20_000 && area < 30_000, "unexpected analysis window area: " + area);
    }
}
