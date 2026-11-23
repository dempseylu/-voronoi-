package org.example.voronoi.algorithm;

import org.example.voronoi.geometry.Coordinate;
import org.example.voronoi.geometry.Geometry;
import org.example.voronoi.model.Station;
import org.example.voronoi.model.VoronoiCell;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoronoiBuilderTest {

    @Test
    void singleStationGetsTheWholeClipWindow() {
        List<Station> stations = Arrays.asList(new Station(1, "a", 123.0, 41.8));
        VoronoiBuilder builder = new VoronoiBuilder(0.0);
        Triangulation triangulation = new DelaunayTriangulator().triangulate(positions(stations));

        List<VoronoiCell> cells = builder.build(triangulation, stations);
        assertEquals(1, cells.size());
        assertEquals(Geometry.area(builder.computeClipBox(stations)),
                Geometry.area(cells.get(0).getPolygon()), 1e-12);
    }

    @Test
    void fourStationsSplitTheWindowIntoEqualQuadrants() {
        List<Station> stations = Arrays.asList(
                new Station(1, "sw", 123.0, 41.7),
                new Station(2, "se", 123.2, 41.7),
                new Station(3, "ne", 123.2, 41.9),
                new Station(4, "nw", 123.0, 41.9));
        VoronoiBuilder builder = new VoronoiBuilder(0.0);
        List<VoronoiCell> cells = builder.build(
                new DelaunayTriangulator().triangulate(positions(stations)), stations);

        double window = Geometry.area(builder.computeClipBox(stations));
        double sum = 0.0;
        for (VoronoiCell cell : cells) {
            double area = Geometry.area(cell.getPolygon());
            assertEquals(window / 4.0, area, window * 1e-9);
            sum += area;
        }
        assertEquals(window, sum, window * 1e-9);
    }

    @Test
    void cellAreasTileTheClipWindow() {
        List<Station> stations = gridStations(4, 4, 123.0, 41.7, 0.01);
        VoronoiBuilder builder = new VoronoiBuilder(0.05);
        List<VoronoiCell> cells = builder.build(
                new DelaunayTriangulator().triangulate(positions(stations)), stations);

        double window = Geometry.area(builder.computeClipBox(stations));
        double sum = 0.0;
        for (VoronoiCell cell : cells) {
            sum += Geometry.area(cell.getPolygon());
        }
        assertEquals(stations.size(), cells.size());
        assertEquals(window, sum, window * 1e-9);
    }

    @Test
    void everyStationSitsInsideItsOwnCell() {
        List<Station> stations = gridStations(5, 5, 123.0, 41.7, 0.02);
        VoronoiBuilder builder = new VoronoiBuilder(0.05);
        List<VoronoiCell> cells = builder.build(
                new DelaunayTriangulator().triangulate(positions(stations)), stations);

        for (VoronoiCell cell : cells) {
            assertTrue(cell.contains(cell.getStation().position()),
                    "station " + cell.getStation().getId() + " is outside its own cell");
        }
    }

    @Test
    void cellsAreConvexAndCounterClockwise() {
        List<Station> stations = gridStations(4, 4, 123.0, 41.7, 0.01);
        VoronoiBuilder builder = new VoronoiBuilder(0.05);
        List<VoronoiCell> cells = builder.build(
                new DelaunayTriangulator().triangulate(positions(stations)), stations);

        for (VoronoiCell cell : cells) {
            List<Coordinate> polygon = cell.getPolygon();
            assertTrue(polygon.size() >= 3);
            assertTrue(Geometry.signedArea(polygon) > 0, "rings must be counter-clockwise");
            for (int i = 0; i < polygon.size(); i++) {
                Coordinate a = polygon.get(i);
                Coordinate b = polygon.get((i + 1) % polygon.size());
                Coordinate c = polygon.get((i + 2) % polygon.size());
                assertTrue(Geometry.cross(a, b, c) > -1e-12, "cell must stay convex");
            }
        }
    }

    @Test
    void duplicateStationsAreSkipped() {
        List<Station> stations = Arrays.asList(
                new Station(1, "a", 123.0, 41.7),
                new Station(2, "a-copy", 123.0, 41.7),
                new Station(3, "b", 123.2, 41.7),
                new Station(4, "c", 123.2, 41.9));
        VoronoiBuilder builder = new VoronoiBuilder(0.0);
        List<VoronoiCell> cells = builder.build(
                new DelaunayTriangulator().triangulate(positions(stations)), stations);

        assertEquals(3, cells.size());
        assertEquals(1, builder.getDuplicateSitesSkipped());
    }

    @Test
    void collinearStationsStillProduceTilingCells() {
        List<Station> stations = Arrays.asList(
                new Station(1, "a", 123.0, 41.8),
                new Station(2, "b", 123.1, 41.8),
                new Station(3, "c", 123.2, 41.8));
        VoronoiBuilder builder = new VoronoiBuilder(0.0);
        List<VoronoiCell> cells = builder.build(
                new DelaunayTriangulator().triangulate(positions(stations)), stations);

        double window = Geometry.area(builder.computeClipBox(stations));
        double sum = 0.0;
        for (VoronoiCell cell : cells) {
            sum += Geometry.area(cell.getPolygon());
        }
        assertEquals(3, cells.size());
        assertEquals(window, sum, window * 1e-9);
    }

    @Test
    void clipMarginIsValidated() {
        assertThrows(IllegalArgumentException.class, () -> new VoronoiBuilder(-0.1));
        assertThrows(IllegalArgumentException.class, () -> new VoronoiBuilder(1.5));
    }

    private static List<Coordinate> positions(List<Station> stations) {
        List<Coordinate> positions = new ArrayList<>(stations.size());
        for (Station station : stations) {
            positions.add(station.position());
        }
        return positions;
    }

    static List<Station> gridStations(int columns, int rows, double startLon, double startLat, double step) {
        List<Station> stations = new ArrayList<>();
        int id = 1;
        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < rows; y++) {
                stations.add(new Station(id, "s" + id, startLon + x * step, startLat + y * step));
                id++;
            }
        }
        return stations;
    }
}
