package org.example.voronoi.geometry;

/**
 * An immutable longitude/latitude pair in decimal degrees.
 *
 * <p>Instances are value objects: equality uses exact double comparison so that
 * duplicate rows loaded from a data source can be removed reliably before the
 * triangulation starts.</p>
 */
public final class Coordinate implements Comparable<Coordinate> {

    private final double longitude;
    private final double latitude;

    public Coordinate(double longitude, double latitude) {
        if (Double.isNaN(longitude) || Double.isNaN(latitude)
                || Double.isInfinite(longitude) || Double.isInfinite(latitude)) {
            throw new IllegalArgumentException("Coordinate must be finite: " + longitude + "," + latitude);
        }
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double distanceTo(Coordinate other) {
        double dx = longitude - other.longitude;
        double dy = latitude - other.latitude;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public Coordinate midpoint(Coordinate other) {
        return new Coordinate((longitude + other.longitude) / 2.0, (latitude + other.latitude) / 2.0);
    }

    /**
     * Lexicographic order by longitude, then latitude. Equal coordinates return 0,
     * which the {@link Comparable} contract requires and which the original
     * implementation violated.
     */
    @Override
    public int compareTo(Coordinate other) {
        int byLongitude = Double.compare(longitude, other.longitude);
        return byLongitude != 0 ? byLongitude : Double.compare(latitude, other.latitude);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Coordinate)) {
            return false;
        }
        Coordinate that = (Coordinate) o;
        return Double.compare(that.longitude, longitude) == 0
                && Double.compare(that.latitude, latitude) == 0;
    }

    @Override
    public int hashCode() {
        long lonBits = Double.doubleToLongBits(longitude);
        long latBits = Double.doubleToLongBits(latitude);
        int result = (int) (lonBits ^ (lonBits >>> 32));
        return 31 * result + (int) (latBits ^ (latBits >>> 32));
    }

    @Override
    public String toString() {
        return "(" + longitude + ", " + latitude + ")";
    }
}
