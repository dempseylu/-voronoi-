package org.example.voronoi.config;

import org.example.voronoi.db.MyBatisSessionFactory;
import org.example.voronoi.db.StationMapper;
import org.example.voronoi.db.UserRecordMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    void defaultsAreLoadedFromTheClasspath() {
        AppConfig config = AppConfig.load();
        assertTrue(config.databaseUrl().startsWith("jdbc:mysql://"), config.databaseUrl());
        assertEquals("com.mysql.cj.jdbc.Driver", config.databaseDriver());
        assertEquals(0.05, config.clipMarginRatio(), 1e-12);
        assertEquals("out/cells.geojson", config.defaultOutput());
    }

    @Test
    void missingKeysFallBackToDefaults() {
        AppConfig config = AppConfig.load();
        assertEquals("fallback", config.get("does.not.exist", "fallback"));
        assertFalse(config.databasePassword() == null);
    }

    @Test
    void myBatisFactoryRegistersBothMappers() {
        SqlSessionFactory factory = MyBatisSessionFactory.create(AppConfig.load());
        assertTrue(factory.getConfiguration().hasMapper(StationMapper.class));
        assertTrue(factory.getConfiguration().hasMapper(UserRecordMapper.class));
        assertTrue(factory.getConfiguration().hasStatement(
                StationMapper.class.getName() + ".findAll"));
    }
}
