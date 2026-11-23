package org.example.voronoi.algorithm;

import org.example.voronoi.geometry.Coordinate;
import org.example.voronoi.geometry.Edge;
import org.example.voronoi.geometry.Geometry;
import org.example.voronoi.geometry.Triangle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Bowyer-Watson incremental Delaunay triangulation.
 *
 * <p>Differences from the original implementation, each of which removed a
 * concrete failure mode:</p>
 * <ul>
 *   <li>input points are de-duplicated before insertion (a duplicate produced a
 *       zero-area triangle whose circumcenter was NaN);</li>
 *   <li>the super triangle provably contains every point and is re-scaled if it
 *       does not, instead of relying on hand-tuned magic numbers;</li>
 *   <li>the in-circle test uses a normalised determinant instead of
 *       {@code distance < radius}, which exploded for sliver triangles;</li>
 *   <li>collinear triples are rejected at construction time, so no NaN ever
 *       enters the mesh;</li>
 *   <li>the cavity is closed by counting edges, and only edges used once become
 *       new triangles.</li>
 * </ul>
 *
 * <p>With a correct in-circle test the boundary of the result is already the
 * convex hull, so the original "concave vertex repair" pass is unnecessary.
 * {@link #triangulate(List)} exposes the hull for verification.</p>
 */
public final class DelaunayTriangulator {

    /** Circumcircle tests are O(n^2) per point in the worst case; guard runaway input. */
    private static final int MAX_POINTS = 200_000;

    public Triangulation triangulate(List<Coordinate> rawPoints) {
        List<Coordinate> points = sanitize(rawPoints);
        if (points.isEmpty()) {
            return Triangulation.empty();
        }

        List<Coordinate> hull = Geometry.convexHull(points);
        if (points.size() < 3 || hull.size() < 3) {
            List<Edge> boundary = Triangulation.extractBoundaryEdges(new ArrayList<Triangle>(), hull);
            return new Triangulation(points, new ArrayList<Triangle>(), boundary,
                    Triangulation.chainAdjacency(hull), hull);
        }

        Triangle superTriangle = createSuperTriangle(points);
        Set<Coordinate> superVertices = new HashSet<>(superTriangle.vertices());

        List<Triangle> triangles = new ArrayList<>();
        triangles.add(superTriangle);

        for (Coordinate point : points) {
            List<Triangle> cavity = new ArrayList<>();
            for (Triangle triangle : triangles) {
                if (triangle.circumcircleContains(point)) {
                    cavity.add(triangle);
                }
            }
            if (cavity.isEmpty()) {
                // Only reachable for a point outside the super triangle. Skipping is
                // safer than inserting outside the mesh and corrupting the cavity.
                continue;
            }

            Map<Edge, Integer> edgeUse = new LinkedHashMap<>();
            for (Triangle triangle : cavity) {
                for (Edge edge : triangle.edges()) {
                    edgeUse.merge(edge, 1, Integer::sum);
                }
            }
            triangles.removeAll(new HashSet<>(cavity));

            for (Map.Entry<Edge, Integer> entry : edgeUse.entrySet()) {
                if (entry.getValue() != 1) {
                    continue;
                }
                Triangle created = Triangle.of(entry.getKey().getA(), entry.getKey().getB(), point);
                if (created != null) {
                    triangles.add(created);
                }
            }
        }

        triangles.removeIf(triangle -> {
            for (Coordinate vertex : triangle.vertices()) {
                if (superVertices.contains(vertex)) {
                    return true;
                }
            }
            return false;
        });

        List<Edge> boundary = Triangulation.extractBoundaryEdges(triangles, hull);
        Map<Coordinate, Set<Coordinate>> adjacency = triangles.isEmpty()
                ? Triangulation.chainAdjacency(hull)
                : Triangulation.buildAdjacency(triangles);

        for (Coordinate vertex : points) {
            adjacency.putIfAbsent(vertex, new LinkedHashSet<Coordinate>());
        }

        return new Triangulation(points, triangles, boundary, adjacency, hull);
    }

    /**
     * Sorts and de-duplicates the input.
     *
     * <p>{@link TreeSet} relies on {@link Coordinate#compareTo}, which returns 0
     * for identical coordinates. The old comparator never returned 0, so
     * duplicate points survived and broke the {@link Comparable} contract.</p>
     */
    private List<Coordinate> sanitize(List<Coordinate> rawPoints) {
        if (rawPoints == null || rawPoints.isEmpty()) {
            return new ArrayList<>();
        }
        TreeSet<Coordinate> unique = new TreeSet<>();
        for (Coordinate c : rawPoints) {
            if (c != null) {
                unique.add(c);
            }
        }
        if (unique.size() > MAX_POINTS) {
            throw new IllegalArgumentException("Too many points: " + unique.size() + " (max " + MAX_POINTS + ")");
        }
        return new ArrayList<>(unique);
    }

    /**
     * A triangle that strictly contains the bounding box of all points.
     *
     * <p>The box is covered by a triangle whose base is one span below the box
     * and whose apex is twenty spans above it, which leaves a wide margin for
     * floating point noise. Containment is verified and the triangle is scaled
     * up if a point still falls outside.</p>
     */
    private Triangle createSuperTriangle(List<Coordinate> points) {
        double[] box = Geometry.boundingBox(points);
        double centerLon = (box[0] + box[2]) / 2.0;
        double centerLat = (box[1] + box[3]) / 2.0;
        double span = Math.max(box[2] - box[0], box[3] - box[1]);
        if (span <= 0.0) {
            span = 1.0;
        }

        double scale = span;
        for (int attempt = 0; attempt < 8; attempt++) {
            Coordinate a = new Coordinate(centerLon - 20.0 * scale, centerLat - scale);
            Coordinate b = new Coordinate(centerLon, centerLat + 20.0 * scale);
            Coordinate c = new Coordinate(centerLon + 20.0 * scale, centerLat - scale);
            Triangle triangle = Triangle.of(a, b, c);
            if (triangle == null) {
                throw new IllegalStateException("Super triangle is degenerate for span " + span);
            }
            boolean coversAll = true;
            for (Coordinate point : points) {
                if (!triangle.containsPoint(point)) {
                    coversAll = false;
                    break;
                }
            }
            if (coversAll) {
                return triangle;
            }
            scale *= 10.0;
        }
        throw new IllegalStateException("Unable to build a super triangle covering every input point");
    }
}
