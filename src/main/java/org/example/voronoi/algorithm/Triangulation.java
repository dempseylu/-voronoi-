package org.example.voronoi.algorithm;

import org.example.voronoi.geometry.Coordinate;
import org.example.voronoi.geometry.Edge;
import org.example.voronoi.geometry.Triangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Result of a Delaunay triangulation.
 *
 * <p>Vertex adjacency is pre-computed once so the Voronoi builder never has to
 * rescan the triangle list per site.</p>
 */
public final class Triangulation {

    private final List<Coordinate> vertices;
    private final List<Triangle> triangles;
    private final List<Edge> boundaryEdges;
    private final Map<Coordinate, Set<Coordinate>> adjacency;
    private final List<Coordinate> convexHull;

    Triangulation(List<Coordinate> vertices,
                  List<Triangle> triangles,
                  List<Edge> boundaryEdges,
                  Map<Coordinate, Set<Coordinate>> adjacency,
                  List<Coordinate> convexHull) {
        this.vertices = Collections.unmodifiableList(new ArrayList<>(vertices));
        this.triangles = Collections.unmodifiableList(new ArrayList<>(triangles));
        this.boundaryEdges = Collections.unmodifiableList(new ArrayList<>(boundaryEdges));
        this.adjacency = adjacency;
        this.convexHull = Collections.unmodifiableList(new ArrayList<>(convexHull));
    }

    static Triangulation empty() {
        return new Triangulation(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new HashMap<>(), new ArrayList<>());
    }

    public List<Coordinate> getVertices() {
        return vertices;
    }

    public List<Triangle> getTriangles() {
        return triangles;
    }

    public List<Edge> getBoundaryEdges() {
        return boundaryEdges;
    }

    public List<Coordinate> getConvexHull() {
        return convexHull;
    }

    /** Delaunay neighbours of a site, or an empty set when the vertex is unknown. */
    public Set<Coordinate> neighboursOf(Coordinate vertex) {
        Set<Coordinate> neighbours = adjacency.get(vertex);
        return neighbours == null ? Collections.<Coordinate>emptySet() : neighbours;
    }

    public boolean isOnBoundary(Coordinate vertex) {
        if (!adjacency.containsKey(vertex)) {
            return true;
        }
        // A hull vertex has at least one boundary edge.
        for (Edge edge : boundaryEdges) {
            if (edge.getA().equals(vertex) || edge.getB().equals(vertex)) {
                return true;
            }
        }
        return false;
    }

    /** Builds undirected adjacency from a triangle list. */
    static Map<Coordinate, Set<Coordinate>> buildAdjacency(List<Triangle> triangles) {
        Map<Coordinate, Set<Coordinate>> adjacency = new HashMap<>();
        for (Triangle triangle : triangles) {
            List<Coordinate> vs = triangle.vertices();
            for (int i = 0; i < 3; i++) {
                Coordinate from = vs.get(i);
                Coordinate to = vs.get((i + 1) % 3);
                adjacency.computeIfAbsent(from, k -> new LinkedHashSet<>()).add(to);
                adjacency.computeIfAbsent(to, k -> new LinkedHashSet<>()).add(from);
            }
        }
        return adjacency;
    }

    /** Boundary edges are the triangle edges used by exactly one triangle. */
    static List<Edge> extractBoundaryEdges(List<Triangle> triangles, List<Coordinate> hull) {
        Map<Edge, Integer> counts = new HashMap<>();
        for (Triangle triangle : triangles) {
            for (Edge edge : triangle.edges()) {
                counts.merge(edge, 1, Integer::sum);
            }
        }
        List<Edge> boundary = new ArrayList<>();
        for (Map.Entry<Edge, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == 1) {
                boundary.add(entry.getKey());
            }
        }
        if (!boundary.isEmpty()) {
            return boundary;
        }
        // Degenerate (all collinear) input: fall back to the hull chain.
        for (int i = 0; i + 1 < hull.size(); i++) {
            boundary.add(new Edge(hull.get(i), hull.get(i + 1)));
        }
        return boundary;
    }

    /** Collinear fallback adjacency: a simple chain along the hull. */
    static Map<Coordinate, Set<Coordinate>> chainAdjacency(List<Coordinate> hull) {
        Map<Coordinate, Set<Coordinate>> adjacency = new HashMap<>();
        for (int i = 0; i + 1 < hull.size(); i++) {
            Coordinate a = hull.get(i);
            Coordinate b = hull.get(i + 1);
            adjacency.computeIfAbsent(a, k -> new LinkedHashSet<>()).add(b);
            adjacency.computeIfAbsent(b, k -> new LinkedHashSet<>()).add(a);
        }
        return adjacency;
    }
}
