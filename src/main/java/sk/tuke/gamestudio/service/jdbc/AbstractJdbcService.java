package sk.tuke.gamestudio.service.jdbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractJdbcService {
    private static final Set<String> INITIALIZED_DATABASES = ConcurrentHashMap.newKeySet();

    private final JdbcConfig config;

    protected AbstractJdbcService() {
        this(JdbcConfig.defaultConfig());
    }

    protected AbstractJdbcService(JdbcConfig config) {
        this.config = config;
        initializeSchemaIfNeeded();
    }

    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(config.url(), config.user(), config.password());
    }

    private void initializeSchemaIfNeeded() {
        String key = config.url() + "|" + config.user();
        if (INITIALIZED_DATABASES.contains(key)) {
            return;
        }
        synchronized (INITIALIZED_DATABASES) {
            if (INITIALIZED_DATABASES.contains(key)) {
                return;
            }
            try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
                for (String sql : loadSchemaStatements()) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty()) {
                        statement.execute(trimmed);
                    }
                }
                INITIALIZED_DATABASES.add(key);
            } catch (SQLException | IOException e) {
                throw new IllegalStateException("Cannot initialize database schema.", e);
            }
        }
    }

    private String[] loadSchemaStatements() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("db/schema.sql");
        if (inputStream == null) {
            throw new IOException("Resource db/schema.sql not found.");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            return builder.toString().split(";");
        }
    }
}
