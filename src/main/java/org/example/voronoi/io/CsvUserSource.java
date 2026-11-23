package org.example.voronoi.io;

import org.example.voronoi.model.UserRecord;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * User CSV: {@code time,imsi,longitude,latitude[,lac]} with an optional header.
 */
public final class CsvUserSource implements UserSource {

    private final Path path;

    public CsvUserSource(Path path) {
        this.path = path;
    }

    @Override
    public List<UserRecord> load() {
        List<UserRecord> users = new ArrayList<>();
        for (Csv.Row row : Csv.read(path)) {
            long time = row.longValue(0);
            long imsi = row.longValue(1);
            double longitude = row.decimal(2);
            double latitude = row.decimal(3);
            int lac = row.size() > 4 ? row.integer(4) : -1;
            users.add(new UserRecord(time, imsi, longitude, latitude, lac));
        }
        return users;
    }
}
