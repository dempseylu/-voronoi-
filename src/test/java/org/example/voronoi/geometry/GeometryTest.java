package org.example.voronoi.geometry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryTest {

    @Test
    void crossProductSignsFollowOrientation() {
        Coordinate a = new Coordinate(0, 0);
        Coordinate b = new Coordinate(1, 0);
        Coordinate c = new Coordinate(0, 1);
        assertTrue(Geometry.cross(a, b, c) > 0, "counter-clockwise must be positive");
        assertTrue(Geometry.cross(a, c, b) < 0, "clockwise must be negative");
    }

    @Test
    void signedAreaOfUnitSquare() {
        List<Coordinate> square = square();
        assertEquals(1.0, Geometry.signedArea(square), 1e-12);
        List<Coordinate> reversed = new ArrayList<>(square);
        java.util.Collections.reverse(reversed);
        assertEquals(-1.0, Geometry.signedArea(reversed), 1e-12);
    }

    @Test
    void squareMetreAreaMatchesTheSphericalReference() {
        // 0.01 degree x 0.01 degree cell near Shenyang.
        double west = 123.00;
        double east = 123.01;
        double south = 41.70;
        double north = 41.71;
        List<Coordinate> cell = Geometry.rectangle(west, south, east, north);

        double radius = 6_371_008.8;
        double deltaLongitude = Math.toRadians(east - west);
        double spherical = radius * radius * deltaLongitude
                * (Math.sin(Math.toRadians(north)) - Math.sin(Math.toRadians(south)));

        double computed = Geometry.areaInSquareMetres(cell);
        assertEquals(spherical, computed, spherical * 0.002,
                "equirectangular area must be within 0.2% of the spherical area");
    }

    @Test
    void pointInPolygonHandlesInsideOutsideAndBoundary() {
        List<Coordinate> square = square();
        assertTrue(Geometry.containsPoint(square, new Coordinate(0.5, 0.5)));
        assertFalse(Geometry.containsPoint(square, new Coordinate(1.5, 0.5)));
        assertFalse(Geometry.containsPoint(square, new Coordinate(-0.001, 0.5)));
        assertTrue(Geometry.containsPoint(square, new Coordinate(0.5, 0.0)), "edge point is inside");
        assertTrue(Geometry.containsPoint(square, new Coordinate(1.0, 1.0)), "corner is inside");
    }

    @Test
    void clipHalfPlaneCutsTheSquareInHalf() {
        List<Coordinate> half = Geometry.clipHalfPlane(square(),
                p -> 0.5 - p.getLongitude());
        assertEquals(0.5, Geometry.area(half), 1e-12);
        for (Coordinate c : half) {
            assertTrue(c.getLongitude() <= 0.5 + 1e-12);
        }
    }

    @Test
    void clipHalfPlaneCanProduceAnEmptyPolygon() {
        List<Coordinate> empty = Geometry.clipHalfPlane(square(), p -> -1.0);
        assertTrue(empty.isEmpty());
    }

    @Test
    void segmentsIntersectHandlesVerticalSegments() {
        Coordinate verticalA = new Coordinate(0.5, 0.0);
        Coordinate verticalB = new Coordinate(0.5, 1.0);
        Coordinate horizontalA = new Coordinate(0.0, 0.5);
        Coordinate horizontalB = new Coordinate(1.0, 0.5);
        assertTrue(Geometry.segmentsIntersect(verticalA, verticalB, horizontalA, horizontalB));

        // A vertical segment that stops short of the horizontal one.
        Coordinate otherA = new Coordinate(0.4, 0.6);
        Coordinate otherB = new Coordinate(0.4, 1.0);
        assertFalse(Geometry.segmentsIntersect(otherA, otherB, horizontalA, horizontalB));
    }

    @Test
    void segmentsIntersectDetectsTouchingAndCollinearOverlap() {
        assertTrue(Geometry.segmentsIntersect(
                new Coordinate(0, 0), new Coordinate(1, 1),
                new Coordinate(0, 1), new Coordinate(1, 0)));
        assertTrue(Geometry.segmentsIntersect(
                new Coordinate(0, 0), new Coordinate(1, 0),
                new Coordinate(1, 0), new Coordinate(2, 0)));
        assertFalse(Geometry.segmentsIntersect(
                new Coordinate(0, 0), new Coordinate(1, 0),
                new Coordinate(2, 0), new Coordinate(3, 0)));
    }

    @Test
    void convexHullDropsInteriorPoints() {
        List<Coordinate> points = new ArrayList<>(Arrays.asList(
                new Coordinate(0, 0), new Coordinate(2, 0), new Coordinate(2, 2), new Coordinate(0, 2),
                new Coordinate(1, 1), new Coordinate(1, 0.5)));
        List<Coordinate> hull = Geometry.convexHull(points);
        assertEquals(4, hull.size());
        assertEquals(4.0, Geometry.area(hull), 1e-12);
    }

    @Test
    void coordinateOrderingIsTotalAndReturnsZeroForEqualValues() {
        Coordinate a = new Coordinate(1.0, 2.0);
        Coordinate b = new Coordinate(1.0, 3.0);
        Coordinate same = new Coordinate(1.0, 2.0);
        assertEquals(0, a.compareTo(same));
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
    }

    private static List<Coordinate> square() {
        return Geometry.rectangle(0, 0, 1, 1);
    }
}
