package org.example.voronoi.io;

import org.example.voronoi.model.Station;
import org.example.voronoi.model.UserRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvSourceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void readsStationsWithHeaderAndNames() throws IOException {
        Path file = write("stations.csv", String.join("\n",
                "id,name,longitude,latitude",
                "1,tower-a,123.100,41.800",
                "2,tower-b,123.200,41.810",
                ""));

        List<Station> stations = new CsvStationSource(file).load();

        assertEquals(2, stations.size());
        assertEquals(1, stations.get(0).getId());
        assertEquals("tower-a", stations.get(0).getName());
        assertEquals(123.1, stations.get(0).getLongitude(), 1e-9);
    }

    @Test
    void readsStationsWithoutNameColumn() throws IOException {
        Path file = write("stations.csv", String.join("\n",
                "# a comment",
                "7,123.1,41.8",
                "8,123.2,41.9"));

        List<Station> stations = new CsvStationSource(file).load();

        assertEquals(2, stations.size());
        assertEquals("station-7", stations.get(0).getName());
    }

    @Test
    void stripsByteOrderMark() throws IOException {
        Path file = write("bom.csv", "\uFEFFid,longitude,latitude\n1,123.1,41.8\n");
        List<Station> stations = new CsvStationSource(file).load();
        assertEquals(1, stations.size());
        assertEquals(1, stations.get(0).getId());
    }

    @Test
    void readsUsers() throws IOException {
        Path file = write("users.csv", String.join("\n",
                "time,imsi,longitude,latitude,lac",
                "20200301120000,1001,123.15,41.95,12",
                "20200301130000,1002,123.25,41.85,13"));

        List<UserRecord> users = new CsvUserSource(file).load();

        assertEquals(2, users.size());
        assertEquals(12, users.get(0).hourOfDay());
        assertEquals(13, users.get(1).hourOfDay());
        assertEquals(12, users.get(0).getLac());
    }

    @Test
    void reportsTheOffendingLine() throws IOException {
        Path file = write("broken.csv", "1,a,not-a-number,41.8\n");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new CsvStationSource(file).load());
        assertTrue(error.getMessage().contains("broken.csv:1"), error.getMessage());
    }

    @Test
    void rejectsEmptyStationFiles() throws IOException {
        Path file = write("empty.csv", "id,longitude,latitude\n");
        assertThrows(IllegalArgumentException.class, () -> new CsvStationSource(file).load());
    }

    @Test
    void missingFileFailsWithAClearMessage() {
        Path missing = temporaryDirectory.resolve("nope.csv");
        assertThrows(RuntimeException.class, () -> new CsvStationSource(missing).load());
    }

    private Path write(String name, String content) throws IOException {
        Path file = temporaryDirectory.resolve(name);
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
