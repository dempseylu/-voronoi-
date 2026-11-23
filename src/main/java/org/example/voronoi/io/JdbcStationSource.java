package org.example.voronoi.io;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.example.voronoi.db.StationMapper;
import org.example.voronoi.model.Station;

import java.util.List;

/** Loads stations from the database configured in {@code application.properties}. */
public final class JdbcStationSource implements StationSource {

    private final SqlSessionFactory sessionFactory;

    public JdbcStationSource(SqlSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<Station> load() {
        try (SqlSession session = sessionFactory.openSession()) {
            return session.getMapper(StationMapper.class).findAll();
        }
    }
}
