package org.example.voronoi.io;

import org.example.voronoi.model.Station;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Station CSV: {@code id,name,longitude,latitude} with an optional header.
 *
 * <p>The name column may be omitted ({@code id,longitude,latitude}).</p>
 */
public final class CsvStationSource implements StationSource {

    private final Path path;

    public CsvStationSource(Path path) {
        this.path = path;
    }

    @Override
    public List<Station> load() {
        List<Station> stations = new ArrayList<>();
        for (Csv.Row row : Csv.read(path)) {
            boolean hasName = row.size() >= 4 && !isNumeric(row.text(1));
            int id = row.integer(0);
            String name = hasName ? row.text(1) : null;
            int lonIndex = hasName ? 2 : 1;
            double longitude = row.decimal(lonIndex);
            double latitude = row.decimal(lonIndex + 1);
            stations.add(new Station(id, name, longitude, latitude));
        }
        if (stations.isEmpty()) {
            throw new IllegalArgumentException("No stations found in " + path);
        }
        return stations;
    }

    private static boolean isNumeric(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
