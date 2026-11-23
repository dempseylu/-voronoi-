package org.example.voronoi.io;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.example.voronoi.db.UserRecordMapper;
import org.example.voronoi.model.UserRecord;

import java.util.List;

/** Loads signalling records from the database configured in {@code application.properties}. */
public final class JdbcUserSource implements UserSource {

    private final SqlSessionFactory sessionFactory;

    public JdbcUserSource(SqlSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<UserRecord> load() {
        try (SqlSession session = sessionFactory.openSession()) {
            return session.getMapper(UserRecordMapper.class).findAll();
        }
    }
}
