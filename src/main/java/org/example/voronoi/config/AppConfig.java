package org.example.voronoi.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application configuration.
 *
 * <p>Values come from {@code application.properties} on the classpath. Secrets
 * are never committed: the {@code VORONOI_DB_*} environment variables override
 * whatever the properties file contains, so the file can ship with an empty
 * password.</p>
 */
public final class AppConfig {

    private static final String PROPERTIES_RESOURCE = "/application.properties";

    private final Properties properties;

    private AppConfig(Properties properties) {
        this.properties = properties;
    }

    public static AppConfig load() {
        Properties properties = new Properties();
        try (InputStream input = AppConfig.class.getResourceAsStream(PROPERTIES_RESOURCE)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + PROPERTIES_RESOURCE, e);
        }

        overrideFromEnvironment(properties, "db.url", "VORONOI_DB_URL");
        overrideFromEnvironment(properties, "db.username", "VORONOI_DB_USERNAME");
        overrideFromEnvironment(properties, "db.password", "VORONOI_DB_PASSWORD");
        overrideFromEnvironment(properties, "db.driver", "VORONOI_DB_DRIVER");
        return new AppConfig(properties);
    }

    private static void overrideFromEnvironment(Properties properties, String key, String environmentVariable) {
        String value = System.getenv(environmentVariable);
        if (value != null && !value.isEmpty()) {
            properties.setProperty(key, value);
        }
    }

    public String get(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    public double getDouble(String key, double defaultValue) {
        String value = get(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Property " + key + " is not a number: " + value);
        }
    }

    public String databaseUrl() {
        return get("db.url", "jdbc:mysql://localhost:3306/yy?serverTimezone=Asia/Shanghai");
    }

    public String databaseUsername() {
        return get("db.username", "root");
    }

    public String databasePassword() {
        return get("db.password", "");
    }

    public String databaseDriver() {
        return get("db.driver", "com.mysql.cj.jdbc.Driver");
    }

    public String defaultOutput() {
        return get("output", "out/cells.geojson");
    }

    public double clipMarginRatio() {
        return getDouble("clip.margin", 0.05);
    }

    public String defaultStationSource() {
        return get("stations.source", "db");
    }

    public String defaultUserSource() {
        return get("users.source", "db");
    }
}
