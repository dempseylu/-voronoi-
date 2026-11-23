package org.example.voronoi.algorithm;

import org.example.voronoi.geometry.Coordinate;
import org.example.voronoi.geometry.Edge;
import org.example.voronoi.geometry.Triangle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelaunayTriangulatorTest {

    private final DelaunayTriangulator triangulator = new DelaunayTriangulator();

    @Test
    void fourCornersProduceTwoTriangles() {
        Triangulation result = triangulator.triangulate(Arrays.asList(
                new Coordinate(0, 0), new Coordinate(1, 0),
                new Coordinate(1, 1), new Coordinate(0, 1)));
        assertEquals(2, result.getTriangles().size());
        assertEquals(4, result.getVertices().size());
    }

    @Test
    void squareWithCenterProducesFourTriangles() {
        Triangulation result = triangulator.triangulate(Arrays.asList(
                new Coordinate(0, 0), new Coordinate(1, 0), new Coordinate(1, 1), new Coordinate(0, 1),
                new Coordinate(0.5, 0.5)));
        assertEquals(4, result.getTriangles().size());
    }

    @Test
    void triangleCountMatchesEulerFormulaForRandomPoints() {
        List<Coordinate> points = randomPoints(200, 42L);
        Triangulation result = triangulator.triangulate(points);
        int n = result.getVertices().size();
        int boundaryPoints = result.getBoundaryEdges().size();
        assertEquals(2 * n - 2 - boundaryPoints, result.getTriangles().size(),
                "a Delaunay triangulation of n points with h hull points has 2n-2-h triangles");
    }

    @Test
    void gridProducesTheExpectedTriangleCount() {
        List<Coordinate> points = grid(5, 5);
        Triangulation result = triangulator.triangulate(points);
        int n = result.getVertices().size();
        assertEquals(16, result.getBoundaryEdges().size(), "a 5x5 grid has 16 points on its boundary");
        assertEquals(32, result.getTriangles().size());
        assertEquals(2 * n - 2 - result.getBoundaryEdges().size(), result.getTriangles().size());
    }

    @Test
    void noPointLiesInsideAForeignCircumcircle() {
        List<Coordinate> points = randomPoints(120, 7L);
        Triangulation result = triangulator.triangulate(points);
        for (Triangle triangle : result.getTriangles()) {
            for (Coordinate point : result.getVertices()) {
                if (!triangle.contains(point)) {
                    assertFalse(triangle.circumcircleContains(point),
                            "point " + point + " violates the empty circumcircle property of " + triangle);
                }
            }
        }
    }

    @Test
    void everyVertexIsUsedByAtLeastOneTriangle() {
        List<Coordinate> points = randomPoints(80, 11L);
        Triangulation result = triangulator.triangulate(points);
        for (Coordinate vertex : result.getVertices()) {
            boolean used = false;
            for (Triangle triangle : result.getTriangles()) {
                if (triangle.contains(vertex)) {
                    used = true;
                    break;
                }
            }
            assertTrue(used, "vertex " + vertex + " was dropped from the mesh");
        }
    }

    @Test
    void duplicatePointsAreRemoved() {
        List<Coordinate> points = new ArrayList<>(Arrays.asList(
                new Coordinate(0, 0), new Coordinate(0, 0),
                new Coordinate(1, 0), new Coordinate(1, 0),
                new Coordinate(1, 1), new Coordinate(0, 1), new Coordinate(0, 1)));
        Triangulation result = triangulator.triangulate(points);
        assertEquals(4, result.getVertices().size());
        assertEquals(2, result.getTriangles().size());
    }

    @Test
    void collinearPointsProduceNoTrianglesAndNoNaN() {
        List<Coordinate> points = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            points.add(new Coordinate(123.0 + i * 0.01, 41.8));
        }
        Triangulation result = triangulator.triangulate(points);
        assertTrue(result.getTriangles().isEmpty());
        assertEquals(2, result.getConvexHull().size());
        assertEquals(1, result.getBoundaryEdges().size());
        for (Coordinate c : result.getVertices()) {
            assertFalse(Double.isNaN(c.getLongitude()) || Double.isNaN(c.getLatitude()));
        }
    }

    @Test
    void singleAndTwoPointsAreHandled() {
        assertEquals(0, triangulator.triangulate(new ArrayList<Coordinate>()).getTriangles().size());
        assertEquals(0, triangulator.triangulate(Arrays.asList(new Coordinate(0, 0))).getTriangles().size());

        Triangulation two = triangulator.triangulate(Arrays.asList(
                new Coordinate(0, 0), new Coordinate(1, 1)));
        assertEquals(0, two.getTriangles().size());
        assertEquals(1, two.getBoundaryEdges().size());
    }

    @Test
    void boundaryEdgesEncloseTheHull() {
        Triangulation result = triangulator.triangulate(randomPoints(60, 3L));
        for (Edge edge : result.getBoundaryEdges()) {
            assertTrue(result.getConvexHull().contains(edge.getA()));
            assertTrue(result.getConvexHull().contains(edge.getB()));
        }
        assertEquals(result.getConvexHull().size(), result.getBoundaryEdges().size(),
                "for a convex hull without collinear sides every boundary edge is one hull side");
    }

    @Test
    void adjacencyIsSymmetric() {
        Triangulation result = triangulator.triangulate(randomPoints(40, 5L));
        for (Coordinate vertex : result.getVertices()) {
            for (Coordinate neighbour : result.neighboursOf(vertex)) {
                assertTrue(result.neighboursOf(neighbour).contains(vertex));
            }
        }
        assertNotNull(result.getVertices());
    }

    private static List<Coordinate> randomPoints(int count, long seed) {
        Random random = new Random(seed);
        List<Coordinate> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            points.add(new Coordinate(
                    Math.round((123.0 + random.nextDouble() * 0.4) * 1e6) / 1e6,
                    Math.round((41.7 + random.nextDouble() * 0.3) * 1e6) / 1e6));
        }
        return points;
    }

    private static List<Coordinate> grid(int columns, int rows) {
        List<Coordinate> points = new ArrayList<>();
        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < rows; y++) {
                points.add(new Coordinate(123.0 + x * 0.01, 41.7 + y * 0.01));
            }
        }
        return points;
    }
}
