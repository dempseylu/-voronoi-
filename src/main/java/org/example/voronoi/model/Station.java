package org.example.voronoi.model;

import org.example.voronoi.geometry.Coordinate;

/**
 * A radio station (base station) with a flat shape so that JDBC/MyBatis can map
 * columns directly onto it.
 *
 * <p>The original model nested a {@code Site} object inside {@code Point}. That
 * prevented MyBatis from populating coordinates and made every setter throw a
 * NullPointerException. Flat fields plus {@link #position()} avoid the problem.</p>
 */
public class Station {

    private int id;
    private String name;
    private double longitude;
    private double latitude;

    /** Required by MyBatis. */
    public Station() {
    }

    public Station(int id, String name, double longitude, double latitude) {
        this.id = id;
        this.name = name == null || name.trim().isEmpty() ? "station-" + id : name;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public Coordinate position() {
        return new Coordinate(longitude, latitude);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
        if (name == null) {
            name = "station-" + id;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @Override
    public String toString() {
        return "Station{" + id + ", " + name + ", " + longitude + ", " + latitude + "}";
    }
}
