package org.example.voronoi.io;

import org.example.voronoi.model.UserRecord;

import java.util.List;

/** Reads signalling records from somewhere. */
public interface UserSource extends AutoCloseable {

    List<UserRecord> load();

    @Override
    default void close() {
        // Nothing to release by default.
    }
}
