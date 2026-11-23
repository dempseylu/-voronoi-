package org.example.voronoi.geometry;

import java.util.Objects;

/**
 * An undirected segment between two coordinates.
 *
 * <p>Equality and hash code are order insensitive, so {@code (a,b)} and
 * {@code (b,a)} collapse to the same key. This is required to count shared
 * triangle edges, and the hash code is consistent with equals so edges can be
 * used as {@link java.util.HashMap} keys.</p>
 */
public final class Edge {

    private final Coordinate a;
    private final Coordinate b;

    public Edge(Coordinate a, Coordinate b) {
        this.a = Objects.requireNonNull(a, "a");
        this.b = Objects.requireNonNull(b, "b");
    }

    public Coordinate getA() {
        return a;
    }

    public Coordinate getB() {
        return b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Edge)) {
            return false;
        }
        Edge that = (Edge) o;
        return (a.equals(that.a) && b.equals(that.b)) || (a.equals(that.b) && b.equals(that.a));
    }

    @Override
    public int hashCode() {
        // Symmetric so both orientations hash to the same bucket.
        return a.hashCode() ^ b.hashCode();
    }

    @Override
    public String toString() {
        return a + "-" + b;
    }
}
