package org.example.voronoi.model;

import org.example.voronoi.geometry.Coordinate;
import org.example.voronoi.geometry.Geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Voronoi cell of one station, plus the population figures derived from it.
 *
 * <p>The polygon is a counter-clockwise convex ring clipped to the analysis
 * window. Its area is stored in square metres, computed with a local
 * equirectangular projection instead of the hard-coded {@code cos(42°)} factor
 * of the original code.</p>
 */
public final class VoronoiCell {

    private final Station station;
    private final List<Coordinate> polygon;
    private final double areaSquareMeters;
    private final double minLongitude;
    private final double minLatitude;
    private final double maxLongitude;
    private final double maxLatitude;

    private final long[] hourlyUsers = new long[24];
    private final long[] hourlyCapacity = new long[24];
    private final double[] hourlyChangeRate = new double[24];
    private long totalUsers;

    public VoronoiCell(Station station, List<Coordinate> polygon) {
        if (polygon.size() < 3) {
            throw new IllegalArgumentException("A Voronoi cell needs at least three vertices");
        }
        this.station = station;
        this.polygon = Collections.unmodifiableList(new ArrayList<>(polygon));
        this.areaSquareMeters = Geometry.areaInSquareMetres(polygon);

        double[] box = Geometry.boundingBox(polygon);
        this.minLongitude = box[0];
        this.minLatitude = box[1];
        this.maxLongitude = box[2];
        this.maxLatitude = box[3];
    }

    /** Bounding-box pre-check followed by an exact ray casting test. */
    public boolean contains(Coordinate point) {
        if (point.getLongitude() < minLongitude || point.getLongitude() > maxLongitude
                || point.getLatitude() < minLatitude || point.getLatitude() > maxLatitude) {
            return false;
        }
        return Geometry.containsPoint(polygon, point);
    }

    public void addUser(int hour) {
        totalUsers++;
        if (hour >= 0 && hour < 24) {
            hourlyUsers[hour]++;
        }
    }

    /**
     * Heuristic upper bound of people per hour, kept from the referenced paper.
     *
     * <p>The original divided by {@code ln(0)} for empty hours and by
     * {@code ln(1) = 0} for single-user hours. Empty hours now yield 0 and the
     * single-user case is handled explicitly.</p>
     */
    public void computeCapacity() {
        for (int hour = 0; hour < 24; hour++) {
            long observed = hourlyUsers[hour];
            if (observed == 0) {
                hourlyCapacity[hour] = 0;
            } else if (observed == 1) {
                hourlyCapacity[hour] = 2;
            } else {
                hourlyCapacity[hour] = (long) Math.ceil((1.1 + 1.0 / Math.log(observed)) * observed) + 5;
            }
        }
    }

    /** Relative change towards the next hour, as a fraction. */
    public void computeChangeRate() {
        for (int hour = 0; hour < 24; hour++) {
            long current = hourlyUsers[hour];
            long next = hourlyUsers[(hour + 1) % 24];
            hourlyChangeRate[hour] = current == 0 ? 0.0 : (next - current) / (double) current;
        }
    }

    public Station getStation() {
        return station;
    }

    public List<Coordinate> getPolygon() {
        return polygon;
    }

    public double getAreaSquareMeters() {
        return areaSquareMeters;
    }

    public double getAreaSquareKilometres() {
        return areaSquareMeters / 1_000_000.0;
    }

    public long[] getHourlyUsers() {
        return hourlyUsers.clone();
    }

    public long[] getHourlyCapacity() {
        return hourlyCapacity.clone();
    }

    public double[] getHourlyChangeRate() {
        return hourlyChangeRate.clone();
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public double getMinLongitude() {
        return minLongitude;
    }

    public double getMinLatitude() {
        return minLatitude;
    }

    public double getMaxLongitude() {
        return maxLongitude;
    }

    public double getMaxLatitude() {
        return maxLatitude;
    }

    @Override
    public String toString() {
        return "VoronoiCell{" + station.getId() + ", vertices=" + polygon.size()
                + ", areaM2=" + Math.round(areaSquareMeters) + ", users=" + totalUsers + "}";
    }
}
