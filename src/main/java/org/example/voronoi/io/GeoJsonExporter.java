package org.example.voronoi.io;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.voronoi.geometry.Coordinate;
import org.example.voronoi.geometry.Geometry;
import org.example.voronoi.model.VoronoiCell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the result as a GeoJSON {@code FeatureCollection}.
 *
 * <p>GeoJSON is used instead of a bespoke JSON shape because every map library
 * (Leaflet, Mapbox GL, Kepler, QGIS) consumes it directly. Coordinates are
 * {@code [longitude, latitude]} and exterior rings are counter-clockwise, as
 * required by RFC 7946.</p>
 */
public final class GeoJsonExporter {

    private final ObjectMapper mapper = new ObjectMapper();

    public void export(List<VoronoiCell> cells, Path output) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "FeatureCollection");

        ObjectNode metadata = root.putObject("properties");
        metadata.put("generatedAt", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        metadata.put("crs", "EPSG:4326");
        metadata.put("cellCount", cells.size());

        ArrayNode features = root.putArray("features");
        double totalArea = 0.0;
        long totalUsers = 0;
        for (VoronoiCell cell : cells) {
            ObjectNode feature = features.addObject();
            feature.put("type", "Feature");

            ObjectNode geometry = feature.putObject("geometry");
            geometry.put("type", "Polygon");
            ArrayNode rings = geometry.putArray("coordinates");
            ArrayNode ring = rings.addArray();
            List<Coordinate> polygon = counterClockwise(cell.getPolygon());
            for (Coordinate c : polygon) {
                ring.addArray().add(c.getLongitude()).add(c.getLatitude());
            }
            Coordinate first = polygon.get(0);
            ring.addArray().add(first.getLongitude()).add(first.getLatitude());

            ObjectNode properties = feature.putObject("properties");
            properties.put("stationId", cell.getStation().getId());
            properties.put("stationName", cell.getStation().getName());
            properties.put("stationLongitude", cell.getStation().getLongitude());
            properties.put("stationLatitude", cell.getStation().getLatitude());
            properties.put("areaKm2", round(cell.getAreaSquareKilometres(), 6));
            properties.put("totalUsers", cell.getTotalUsers());
            properties.set("hourlyUsers", toArray(cell.getHourlyUsers()));
            properties.set("hourlyCapacity", toArray(cell.getHourlyCapacity()));
            properties.set("hourlyChangeRate", toArray(cell.getHourlyChangeRate()));

            totalArea += cell.getAreaSquareKilometres();
            totalUsers += cell.getTotalUsers();
        }

        metadata.put("totalAreaKm2", round(totalArea, 6));
        metadata.put("totalUsers", totalUsers);

        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (java.io.OutputStream out = Files.newOutputStream(output);
             JsonGenerator generator = mapper.getFactory().createGenerator(out)) {
            generator.useDefaultPrettyPrinter();
            mapper.writeTree(generator, root);
        }
    }

    private static List<Coordinate> counterClockwise(List<Coordinate> polygon) {
        if (Geometry.signedArea(polygon) >= 0) {
            return polygon;
        }
        List<Coordinate> reversed = new ArrayList<>(polygon);
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    private static ArrayNode toArray(long[] values) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        for (long value : values) {
            array.add(value);
        }
        return array;
    }

    private static ArrayNode toArray(double[] values) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        for (double value : values) {
            array.add(round(value, 6));
        }
        return array;
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
