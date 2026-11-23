package org.example.voronoi.io;

import org.example.voronoi.model.Station;

import java.util.List;

/** Reads station coordinates from somewhere. */
public interface StationSource extends AutoCloseable {

    List<Station> load();

    @Override
    default void close() {
        // Nothing to release by default.
    }
}
