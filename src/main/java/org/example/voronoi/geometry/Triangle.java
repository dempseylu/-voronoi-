package org.example.voronoi.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A Delaunay triangle whose vertices are stored in counter-clockwise order.
 *
 * <p>Because the orientation is normalised, the sign of the in-circle
 * determinant is stable and the circumcircle never needs a square root.</p>
 */
public final class Triangle {

    private static final double DEGENERATE_AREA = 1e-14;

    private final Coordinate a;
    private final Coordinate b;
    private final Coordinate c;
    private final Coordinate circumcenter;

    private Triangle(Coordinate a, Coordinate b, Coordinate c) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.circumcenter = computeCircumcenter(a, b, c);
    }

    /**
     * Creates a triangle with counter-clockwise orientation.
     *
     * @return the triangle, or {@code null} when the three points are collinear
     *         (a zero-area triangle would produce a NaN circumcenter and poison
     *         every later in-circle test).
     */
    public static Triangle of(Coordinate a, Coordinate b, Coordinate c) {
        double area2 = Geometry.cross(a, b, c);
        if (Math.abs(area2) <= DEGENERATE_AREA) {
            return null;
        }
        return area2 > 0 ? new Triangle(a, b, c) : new Triangle(a, c, b);
    }

    private static Coordinate computeCircumcenter(Coordinate a, Coordinate b, Coordinate c) {
        double ax = a.getLongitude();
        double ay = a.getLatitude();
        double bx = b.getLongitude();
        double by = b.getLatitude();
        double cx = c.getLongitude();
        double cy = c.getLatitude();

        double d = 2.0 * (ax * (by - cy) + bx * (cy - ay) + cx * (ay - by));
        double aSq = ax * ax + ay * ay;
        double bSq = bx * bx + by * by;
        double cSq = cx * cx + cy * cy;

        double ux = (aSq * (by - cy) + bSq * (cy - ay) + cSq * (ay - by)) / d;
        double uy = (aSq * (cx - bx) + bSq * (ax - cx) + cSq * (bx - ax)) / d;
        return new Coordinate(ux, uy);
    }

    private static double squaredDistance(Coordinate p, Coordinate q) {
        double dx = p.getLongitude() - q.getLongitude();
        double dy = p.getLatitude() - q.getLatitude();
        return dx * dx + dy * dy;
    }

    public Coordinate getA() {
        return a;
    }

    public Coordinate getB() {
        return b;
    }

    public Coordinate getC() {
        return c;
    }

    public Coordinate getCircumcenter() {
        return circumcenter;
    }

    public List<Coordinate> vertices() {
        return Arrays.asList(a, b, c);
    }

    public List<Edge> edges() {
        List<Edge> edges = new ArrayList<>(3);
        edges.add(new Edge(a, b));
        edges.add(new Edge(b, c));
        edges.add(new Edge(c, a));
        return edges;
    }

    public boolean contains(Coordinate p) {
        return a.equals(p) || b.equals(p) || c.equals(p);
    }

    /** True when the point is inside the triangle or on its boundary. */
    public boolean containsPoint(Coordinate p) {
        return Geometry.cross(a, b, p) >= -Geometry.EPS
                && Geometry.cross(b, c, p) >= -Geometry.EPS
                && Geometry.cross(c, a, p) >= -Geometry.EPS;
    }

    /**
     * True when the point lies strictly inside the circumcircle.
     *
     * <p>The determinant is evaluated relative to the query point, so its
     * magnitude stays local. The result is compared against a relative
     * epsilon: points that are cocircular within rounding noise count as
     * outside, which keeps the cavity connected for grid-shaped input.</p>
     */
    public boolean circumcircleContains(Coordinate p) {
        double ax = a.getLongitude() - p.getLongitude();
        double ay = a.getLatitude() - p.getLatitude();
        double bx = b.getLongitude() - p.getLongitude();
        double by = b.getLatitude() - p.getLatitude();
        double cx = c.getLongitude() - p.getLongitude();
        double cy = c.getLatitude() - p.getLatitude();

        double aSq = ax * ax + ay * ay;
        double bSq = bx * bx + by * by;
        double cSq = cx * cx + cy * cy;

        double t1 = aSq * (bx * cy - by * cx);
        double t2 = bSq * (ax * cy - ay * cx);
        double t3 = cSq * (ax * by - ay * bx);
        double determinant = t1 - t2 + t3;

        double scale = Math.max(Math.abs(t1), Math.max(Math.abs(t2), Math.abs(t3)));
        if (scale == 0.0) {
            return false;
        }
        return determinant > 1e-12 * scale;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Triangle)) {
            return false;
        }
        Triangle that = (Triangle) o;
        return a.equals(that.a) && b.equals(that.b) && c.equals(that.c);
    }

    @Override
    public int hashCode() {
        int result = a.hashCode();
        result = 31 * result + b.hashCode();
        return 31 * result + c.hashCode();
    }

    @Override
    public String toString() {
        return "Triangle{" + a + ", " + b + ", " + c + "}";
    }
}
