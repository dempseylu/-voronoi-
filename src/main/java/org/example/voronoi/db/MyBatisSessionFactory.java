package org.example.voronoi.db;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.example.voronoi.config.AppConfig;

/**
 * Builds the MyBatis session factory in code.
 *
 * <p>The original {@code mybatis-config.xml} lived under {@code src/main/config},
 * which Maven never copies to the classpath, and it contained a plain-text root
 * password. Both problems disappear when the configuration is assembled from
 * {@link AppConfig}: credentials come from the environment and there is no XML
 * path to get wrong.</p>
 */
public final class MyBatisSessionFactory {

    private MyBatisSessionFactory() {
    }

    public static SqlSessionFactory create(AppConfig config) {
        PooledDataSource dataSource = new PooledDataSource();
        dataSource.setDriver(config.databaseDriver());
        dataSource.setUrl(config.databaseUrl());
        dataSource.setUsername(config.databaseUsername());
        dataSource.setPassword(config.databasePassword());

        Environment environment = new Environment("default", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(StationMapper.class);
        configuration.addMapper(UserRecordMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }
}
