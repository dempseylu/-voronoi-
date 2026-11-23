package org.example.voronoi.io;

import org.apache.ibatis.session.SqlSessionFactory;
import org.example.voronoi.config.AppConfig;
import org.example.voronoi.db.MyBatisSessionFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves a source specification such as {@code db} or
 * {@code csv:samples/stations.csv} into a concrete reader.
 *
 * <p>The database session factory is created lazily, so CSV runs never touch a
 * database driver or a connection pool.</p>
 */
public final class SourceFactory {

    private final AppConfig config;
    private SqlSessionFactory sessionFactory;

    public SourceFactory(AppConfig config) {
        this.config = config;
    }

    public StationSource stationSource(String specification) {
        Path path = csvPath(specification);
        return path != null ? new CsvStationSource(path) : new JdbcStationSource(sessionFactory());
    }

    public UserSource userSource(String specification) {
        Path path = csvPath(specification);
        return path != null ? new CsvUserSource(path) : new JdbcUserSource(sessionFactory());
    }

    private Path csvPath(String specification) {
        if (specification == null) {
            throw new IllegalArgumentException("Data source specification must not be null");
        }
        String value = specification.trim();
        if (value.isEmpty() || "db".equalsIgnoreCase(value)) {
            return null;
        }
        if (value.startsWith("csv:")) {
            return Paths.get(value.substring("csv:".length()));
        }
        if (value.endsWith(".csv")) {
            return Paths.get(value);
        }
        throw new IllegalArgumentException("Unsupported source '" + specification
                + "'; use db, csv:<path> or a path ending in .csv");
    }

    private synchronized SqlSessionFactory sessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = MyBatisSessionFactory.create(config);
        }
        return sessionFactory;
    }
}
