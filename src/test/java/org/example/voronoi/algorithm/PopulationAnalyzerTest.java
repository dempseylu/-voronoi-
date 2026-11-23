package org.example.voronoi.algorithm;

import org.example.voronoi.model.Station;
import org.example.voronoi.model.UserRecord;
import org.example.voronoi.model.VoronoiCell;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PopulationAnalyzerTest {

    @Test
    void usersAreAssignedByContainmentNotByAreaDifference() {
        List<VoronoiCell> cells = twoCells();
        List<UserRecord> users = new ArrayList<>();
        users.add(new UserRecord(0, 1, 0.25, 0.5, 1));
        users.add(new UserRecord(0, 2, 0.25, 0.5, 1));
        users.add(new UserRecord(1, 3, 0.25, 0.5, 1));
        users.add(new UserRecord(1, 4, 0.25, 0.5, 1));
        users.add(new UserRecord(1, 5, 0.25, 0.5, 1));
        users.add(new UserRecord(2, 6, 0.75, 0.5, 2));
        users.add(new UserRecord(3, 7, 0.75, 0.5, 2));
        users.add(new UserRecord(10, 8, 0.9, 0.9, 2));
        users.add(new UserRecord(0, 9, 0.75, 0.25, 2));
        users.add(new UserRecord(0, 10, 5.0, 5.0, 9));

        PopulationAnalyzer.Result result = new PopulationAnalyzer().analyze(cells, users);

        assertEquals(9, result.getInsideCells());
        assertEquals(1, result.getAssignedToNearest(), "the point outside every cell goes to the nearest station");
        assertEquals(0, result.getUnassigned());
        assertEquals(10, result.getTotalUsers());

        VoronoiCell west = cells.get(0);
        VoronoiCell east = cells.get(1);
        assertEquals(5, west.getTotalUsers());
        assertEquals(5, east.getTotalUsers());
    }

    @Test
    void hourlyCountsAndChangeRateUseFloatingPoint() {
        List<VoronoiCell> cells = twoCells();
        List<UserRecord> users = new ArrayList<>();
        users.add(new UserRecord(0, 1, 0.25, 0.5, 1));
        users.add(new UserRecord(0, 2, 0.25, 0.5, 1));
        users.add(new UserRecord(1, 3, 0.25, 0.5, 1));
        users.add(new UserRecord(1, 4, 0.25, 0.5, 1));
        users.add(new UserRecord(1, 5, 0.25, 0.5, 1));

        new PopulationAnalyzer().analyze(cells, users);

        VoronoiCell west = cells.get(0);
        assertEquals(2, west.getHourlyUsers()[0]);
        assertEquals(3, west.getHourlyUsers()[1]);
        assertEquals(0.5, west.getHourlyChangeRate()[0], 1e-12,
                "2 -> 3 is +50%, not the 100% an int division would produce");
        assertEquals(-1.0, west.getHourlyChangeRate()[1], 1e-12);
        assertEquals(0.0, west.getHourlyChangeRate()[5], 1e-12);
    }

    @Test
    void capacityIsFiniteForEmptySingleAndBusyHours() {
        List<VoronoiCell> cells = twoCells();
        List<UserRecord> users = new ArrayList<>();
        users.add(new UserRecord(0, 1, 0.25, 0.5, 1));
        users.add(new UserRecord(0, 2, 0.25, 0.5, 1));
        users.add(new UserRecord(5, 3, 0.75, 0.5, 2));

        new PopulationAnalyzer().analyze(cells, users);

        VoronoiCell west = cells.get(0);
        assertEquals(0, west.getHourlyCapacity()[1], "an empty hour has no capacity estimate");
        assertEquals(11, west.getHourlyCapacity()[0], "two users: ceil((1.1 + 1/ln2) * 2) + 5");

        VoronoiCell east = cells.get(1);
        assertEquals(2, east.getHourlyCapacity()[5], "a single user keeps the minimum");
    }

    @Test
    void usersWithUndecodableHourAreCountedSeparately() {
        List<VoronoiCell> cells = twoCells();
        List<UserRecord> users = Arrays.asList(
                new UserRecord(-5, 1, 0.25, 0.5, 1));

        PopulationAnalyzer.Result result = new PopulationAnalyzer().analyze(cells, users);

        assertEquals(1, result.getInsideCells());
        assertEquals(1, result.getUnknownHour());
        assertEquals(1, cells.get(0).getTotalUsers());
        assertEquals(0, cells.get(0).getHourlyUsers()[0]);
    }

    @Test
    void noUsersLeavesEverythingAtZero() {
        List<VoronoiCell> cells = twoCells();
        PopulationAnalyzer.Result result = new PopulationAnalyzer()
                .analyze(cells, new ArrayList<UserRecord>());

        assertEquals(0, result.getTotalUsers());
        for (VoronoiCell cell : cells) {
            assertEquals(0, cell.getTotalUsers());
            assertEquals(0, cell.getHourlyCapacity()[0]);
        }
    }

    /** Two stations at (0, 0.5) and (1, 0.5) split the unit square in half. */
    private static List<VoronoiCell> twoCells() {
        List<Station> stations = Arrays.asList(
                new Station(1, "west", 0.0, 0.5),
                new Station(2, "east", 1.0, 0.5));
        VoronoiBuilder builder = new VoronoiBuilder(0.0);
        List<Station> copy = new ArrayList<>(stations);
        return builder.build(new DelaunayTriangulator().triangulate(positions(copy)), copy);
    }

    private static List<org.example.voronoi.geometry.Coordinate> positions(List<Station> stations) {
        List<org.example.voronoi.geometry.Coordinate> positions = new ArrayList<>();
        for (Station station : stations) {
            positions.add(station.position());
        }
        return positions;
    }
}
