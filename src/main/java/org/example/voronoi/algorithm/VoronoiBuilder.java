package org.example.voronoi.algorithm;

import org.example.voronoi.geometry.Coordinate;
import org.example.voronoi.geometry.Geometry;
import org.example.voronoi.model.Station;
import org.example.voronoi.model.VoronoiCell;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Builds Voronoi cells by half-plane clipping.
 *
 * <p>For a station {@code s} the cell is the set of points closer to {@code s}
 * than to any other station. That is the intersection of one half-plane per
 * Delaunay neighbour, which is exact and needs no ordering tricks.</p>
 *
 * <p>The original approach connected circumcenters and then deleted every
 * segment that crossed another segment. That only works when the circumcenters
 * happen to be in strictly convex position, and it failed outright for vertical
 * segments because the slope formula produced infinities.</p>
 */
public final class VoronoiBuilder {

    private static final Logger LOG = Logger.getLogger(VoronoiBuilder.class.getName());

    private final double clipMarginRatio;
    private int duplicateSitesSkipped;

    /**
     * @param clipMarginRatio fraction of the station bounding box added on each
     *                        side before clipping, so that boundary cells keep
     *                        the part of their cell outside the station hull
     */
    public VoronoiBuilder(double clipMarginRatio) {
        if (clipMarginRatio < 0 || clipMarginRatio > 1) {
            throw new IllegalArgumentException("clipMarginRatio must be within [0,1]");
        }
        this.clipMarginRatio = clipMarginRatio;
    }

    public List<VoronoiCell> build(Triangulation triangulation, List<Station> stations) {
        List<Station> unique = deduplicate(stations);
        List<VoronoiCell> cells = new ArrayList<>(unique.size());
        if (unique.isEmpty()) {
            return cells;
        }

        List<Coordinate> clipBox = computeClipBox(unique);
        boolean useAllSites = triangulation.getTriangles().isEmpty();

        for (Station station : unique) {
            Coordinate site = station.position();
            List<Coordinate> polygon = new ArrayList<>(clipBox);

            Set<Coordinate> neighbours = useAllSites
                    ? allOtherSites(unique, site)
                    : triangulation.neighboursOf(site);

            if (neighbours.isEmpty() && unique.size() > 1) {
                neighbours = allOtherSites(unique, site);
            }

            for (Coordinate neighbour : neighbours) {
                polygon = clipAgainstBisector(polygon, site, neighbour);
                if (polygon.size() < 3) {
                    break;
                }
            }

            if (polygon.size() < 3) {
                throw new IllegalStateException(
                        "Empty Voronoi cell for " + station + "; the triangulation and the station list disagree");
            }
            cells.add(new VoronoiCell(station, polygon));
        }
        return cells;
    }

    public int getDuplicateSitesSkipped() {
        return duplicateSitesSkipped;
    }

    /**
     * Keeps points closer to {@code site} than to {@code neighbour}.
     *
     * <p>The bisector passes through the midpoint; the half-plane test is the
     * dot product with the vector from the neighbour to the site. No division,
     * no infinite slope.</p>
     */
    private List<Coordinate> clipAgainstBisector(List<Coordinate> polygon, Coordinate site, Coordinate neighbour) {
        Coordinate midpoint = site.midpoint(neighbour);
        double dx = site.getLongitude() - neighbour.getLongitude();
        double dy = site.getLatitude() - neighbour.getLatitude();
        return Geometry.clipHalfPlane(polygon, p ->
                (p.getLongitude() - midpoint.getLongitude()) * dx
                        + (p.getLatitude() - midpoint.getLatitude()) * dy);
    }

    private Set<Coordinate> allOtherSites(List<Station> stations, Coordinate site) {
        Set<Coordinate> result = new LinkedHashSet<>();
        for (Station other : stations) {
            Coordinate position = other.position();
            if (!position.equals(site)) {
                result.add(position);
            }
        }
        return result;
    }

    /** The analysis window: the station bounding box expanded by the margin. */
    public List<Coordinate> computeClipBox(List<Station> stations) {
        List<Coordinate> points = new ArrayList<>(stations.size());
        for (Station station : stations) {
            points.add(station.position());
        }
        double[] box = Geometry.boundingBox(points);
        double spanLongitude = box[2] - box[0];
        double spanLatitude = box[3] - box[1];
        double span = Math.max(spanLongitude, spanLatitude);
        if (span <= 0.0) {
            span = 0.001;
        }
        // A dimension that collapsed (for example two stations on one line) is
        // padded symmetrically so the window never degenerates into a segment.
        double paddingLongitude = spanLongitude > 0.0 ? span * clipMarginRatio : span * 0.5;
        double paddingLatitude = spanLatitude > 0.0 ? span * clipMarginRatio : span * 0.5;
        return Geometry.rectangle(
                box[0] - paddingLongitude, box[1] - paddingLatitude,
                box[2] + paddingLongitude, box[3] + paddingLatitude);
    }

    private List<Station> deduplicate(List<Station> stations) {
        Map<Coordinate, Station> byPosition = new LinkedHashMap<>();
        for (Station station : stations) {
            if (station == null) {
                continue;
            }
            Station previous = byPosition.putIfAbsent(station.position(), station);
            if (previous != null) {
                duplicateSitesSkipped++;
                LOG.warning("Duplicate station position skipped: " + station + " (already present as " + previous + ")");
            }
        }
        return new ArrayList<>(byPosition.values());
    }
}
