package org.example.voronoi.model;

import org.example.voronoi.geometry.Coordinate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * One signalling record: who was where and when.
 *
 * <p>Supported timestamp encodings, detected by magnitude:</p>
 * <ul>
 *   <li>epoch milliseconds (13 digits);</li>
 *   <li>{@code yyyyMMddHHmmss} (14 digits) as used by the original data set;</li>
 *   <li>an hour of day between 0 and 23.</li>
 * </ul>
 */
public class UserRecord {

    private long time;
    private long imsi;
    private double longitude;
    private double latitude;
    private int lac;
    private int hour = -1;

    /** Required by MyBatis. */
    public UserRecord() {
    }

    public UserRecord(long time, long imsi, double longitude, double latitude, int lac) {
        this.time = time;
        this.imsi = imsi;
        this.longitude = longitude;
        this.latitude = latitude;
        this.lac = lac;
    }

    public Coordinate position() {
        return new Coordinate(longitude, latitude);
    }

    /** Hour of day in {@code [0, 23]}, or -1 when the timestamp cannot be decoded. */
    public int hourOfDay() {
        if (hour >= 0 && hour <= 23) {
            return hour;
        }
        if (time >= 0 && time <= 23) {
            return (int) time;
        }
        if (time >= 10_000_000_000_000L) {
            return (int) (time / 10000L % 100L);
        }
        if (time >= 100_000_000_000L) {
            ZonedDateTime dateTime = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault());
            return dateTime.getHour();
        }
        return -1;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getImsi() {
        return imsi;
    }

    public void setImsi(long imsi) {
        this.imsi = imsi;
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

    public int getLac() {
        return lac;
    }

    public void setLac(int lac) {
        this.lac = lac;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }
}
