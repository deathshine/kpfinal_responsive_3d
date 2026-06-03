package sk.tuke.gamestudio.service.jdbc;

public record JdbcConfig(String url, String user, String password) {
    public static JdbcConfig defaultConfig() {
        String url = System.getProperty("db.url", System.getenv().getOrDefault("DB_URL", "jdbc:h2:./gamestudio;MODE=PostgreSQL;AUTO_SERVER=TRUE"));
        String user = System.getProperty("db.user", System.getenv().getOrDefault("DB_USER", "sa"));
        String password = System.getProperty("db.password", System.getenv().getOrDefault("DB_PASSWORD", ""));
        return new JdbcConfig(url, user, password);
    }
}
