package org.example.voronoi.geometry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Planar geometry helpers shared by the triangulation, the Voronoi builder and
 * the point-in-polygon lookup.
 *
 * <p>All intersection logic is expressed with cross products. The original code
 * used slope-intercept equations ({@code y = kx + b}), which produce infinities
 * for vertical segments and NaNs for parallel ones; that silently disabled
 * several predicates. Cross products have no such special case.</p>
 */
public final class Geometry {

    /** Tolerance in degrees for orientation and clipping comparisons. */
    public static final double EPS = 1e-12;

    /** Mean Earth radius in metres. */
    private static final double EARTH_RADIUS_M = 6_371_008.8;

    private Geometry() {
    }

    /** Twice the signed area of triangle {@code (a, b, c)}; positive when counter-clockwise. */
    public static double cross(Coordinate a, Coordinate b, Coordinate c) {
        return (b.getLongitude() - a.getLongitude()) * (c.getLatitude() - a.getLatitude())
                - (b.getLatitude() - a.getLatitude()) * (c.getLongitude() - a.getLongitude());
    }

    /** Signed area of a closed ring (shoelace); positive when counter-clockwise. */
    public static double signedArea(List<Coordinate> ring) {
        double sum = 0.0;
        for (int i = 0; i < ring.size(); i++) {
            Coordinate p = ring.get(i);
            Coordinate q = ring.get((i + 1) % ring.size());
            sum += p.getLongitude() * q.getLatitude() - q.getLongitude() * p.getLatitude();
        }
        return sum / 2.0;
    }

    public static double area(List<Coordinate> ring) {
        return Math.abs(signedArea(ring));
    }

    /** Area of a polygon in square metres, using a local equirectangular projection. */
    public static double areaInSquareMetres(List<Coordinate> ring) {
        if (ring.size() < 3) {
            return 0.0;
        }
        double meanLatitude = 0.0;
        for (Coordinate c : ring) {
            meanLatitude += c.getLatitude();
        }
        meanLatitude /= ring.size();

        double metresPerDegreeLatitude = Math.PI * EARTH_RADIUS_M / 180.0;
        double metresPerDegreeLongitude = metresPerDegreeLatitude * Math.cos(Math.toRadians(meanLatitude));

        double sum = 0.0;
        for (int i = 0; i < ring.size(); i++) {
            Coordinate p = ring.get(i);
            Coordinate q = ring.get((i + 1) % ring.size());
            double px = p.getLongitude() * metresPerDegreeLongitude;
            double py = p.getLatitude() * metresPerDegreeLatitude;
            double qx = q.getLongitude() * metresPerDegreeLongitude;
            double qy = q.getLatitude() * metresPerDegreeLatitude;
            sum += px * qy - qx * py;
        }
        return Math.abs(sum) / 2.0;
    }

    /** True when the point lies inside the ring or on its boundary. */
    public static boolean containsPoint(List<Coordinate> ring, Coordinate p) {
        boolean inside = false;
        int n = ring.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Coordinate a = ring.get(i);
            Coordinate b = ring.get(j);

            if (isOnSegment(p, a, b)) {
                return true;
            }
            // Horizontal edges never satisfy this test, so no division by zero.
            if ((a.getLatitude() > p.getLatitude()) != (b.getLatitude() > p.getLatitude())) {
                double x = a.getLongitude()
                        + (p.getLatitude() - a.getLatitude())
                        * (b.getLongitude() - a.getLongitude())
                        / (b.getLatitude() - a.getLatitude());
                if (p.getLongitude() < x) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    private static boolean isOnSegment(Coordinate p, Coordinate a, Coordinate b) {
        if (Math.abs(cross(a, b, p)) > 1e-9) {
            return false;
        }
        double minLon = Math.min(a.getLongitude(), b.getLongitude()) - 1e-12;
        double maxLon = Math.max(a.getLongitude(), b.getLongitude()) + 1e-12;
        double minLat = Math.min(a.getLatitude(), b.getLatitude()) - 1e-12;
        double maxLat = Math.max(a.getLatitude(), b.getLatitude()) + 1e-12;
        return p.getLongitude() >= minLon && p.getLongitude() <= maxLon
                && p.getLatitude() >= minLat && p.getLatitude() <= maxLat;
    }

    /**
     * Clips a convex polygon by the half-plane where {@code signedDistance >= 0}.
     *
     * <p>Sutherland-Hodgman. The polygon order is preserved, so the result stays
     * a counter-clockwise convex ring.</p>
     */
    public static List<Coordinate> clipHalfPlane(List<Coordinate> polygon, ToDoubleFunction<Coordinate> signedDistance) {
        List<Coordinate> output = new ArrayList<>();
        int n = polygon.size();
        if (n == 0) {
            return output;
        }
        for (int i = 0; i < n; i++) {
            Coordinate current = polygon.get(i);
            Coordinate next = polygon.get((i + 1) % n);
            double fCurrent = signedDistance.applyAsDouble(current);
            double fNext = signedDistance.applyAsDouble(next);

            boolean currentInside = fCurrent >= -EPS;
            boolean nextInside = fNext >= -EPS;

            if (currentInside) {
                output.add(current);
            }
            if (currentInside != nextInside) {
                double denominator = fCurrent - fNext;
                if (Math.abs(denominator) > 0.0) {
                    double t = fCurrent / denominator;
                    output.add(new Coordinate(
                            current.getLongitude() + t * (next.getLongitude() - current.getLongitude()),
                            current.getLatitude() + t * (next.getLatitude() - current.getLatitude())));
                }
            }
        }
        return output;
    }

    /** Cross product based segment test that also handles vertical segments. */
    public static boolean segmentsIntersect(Coordinate a, Coordinate b, Coordinate c, Coordinate d) {
        double d1 = cross(a, b, c);
        double d2 = cross(a, b, d);
        double d3 = cross(c, d, a);
        double d4 = cross(c, d, b);

        if (((d1 > EPS && d2 < -EPS) || (d1 < -EPS && d2 > EPS))
                && ((d3 > EPS && d4 < -EPS) || (d3 < -EPS && d4 > EPS))) {
            return true;
        }
        return (Math.abs(d1) <= EPS && isOnSegment(c, a, b))
                || (Math.abs(d2) <= EPS && isOnSegment(d, a, b))
                || (Math.abs(d3) <= EPS && isOnSegment(a, c, d))
                || (Math.abs(d4) <= EPS && isOnSegment(b, c, d));
    }

    /** Convex hull (monotone chain), counter-clockwise, without collinear points. */
    public static List<Coordinate> convexHull(List<Coordinate> points) {
        List<Coordinate> sorted = new ArrayList<>(points);
        sorted.sort(Comparator.naturalOrder());
        if (sorted.size() < 3) {
            return sorted;
        }

        List<Coordinate> lower = new ArrayList<>();
        for (Coordinate p : sorted) {
            while (lower.size() >= 2 && cross(lower.get(lower.size() - 2), lower.get(lower.size() - 1), p) <= EPS) {
                lower.remove(lower.size() - 1);
            }
            lower.add(p);
        }

        List<Coordinate> upper = new ArrayList<>();
        for (int i = sorted.size() - 1; i >= 0; i--) {
            Coordinate p = sorted.get(i);
            while (upper.size() >= 2 && cross(upper.get(upper.size() - 2), upper.get(upper.size() - 1), p) <= EPS) {
                upper.remove(upper.size() - 1);
            }
            upper.add(p);
        }

        lower.remove(lower.size() - 1);
        upper.remove(upper.size() - 1);
        lower.addAll(upper);
        return lower;
    }

    /** Axis-aligned bounding box as {@code [minLon, minLat, maxLon, maxLat]}. */
    public static double[] boundingBox(List<Coordinate> points) {
        double minLon = Double.POSITIVE_INFINITY;
        double minLat = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        for (Coordinate c : points) {
            minLon = Math.min(minLon, c.getLongitude());
            minLat = Math.min(minLat, c.getLatitude());
            maxLon = Math.max(maxLon, c.getLongitude());
            maxLat = Math.max(maxLat, c.getLatitude());
        }
        return new double[]{minLon, minLat, maxLon, maxLat};
    }

    /** A counter-clockwise rectangle covering the given box. */
    public static List<Coordinate> rectangle(double minLon, double minLat, double maxLon, double maxLat) {
        List<Coordinate> ring = new ArrayList<>(4);
        ring.add(new Coordinate(minLon, minLat));
        ring.add(new Coordinate(maxLon, minLat));
        ring.add(new Coordinate(maxLon, maxLat));
        ring.add(new Coordinate(minLon, maxLat));
        return ring;
    }

}