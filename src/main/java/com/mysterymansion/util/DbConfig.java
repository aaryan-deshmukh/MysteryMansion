package com.mysterymansion.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Database settings, kept out of source code.
 * Order of precedence: environment variables, then config.properties (working directory or classpath).
 *
 *   MANSION_DB_URL / MANSION_DB_USER / MANSION_DB_PASSWORD
 *   db.url / db.user / db.password
 */
public final class DbConfig {
    private static final String FILE_NAME = "config.properties";
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/mystery_mansion?serverTimezone=UTC";

    private static Properties cached;

    private DbConfig() {}

    public static String url() { return get("MANSION_DB_URL", "db.url", DEFAULT_URL); }

    public static String user() { return get("MANSION_DB_USER", "db.user", "root"); }

    /** @throws IllegalStateException if no password has been configured at all */
    public static String password() {
        String value = get("MANSION_DB_PASSWORD", "db.password", null);
        if (value == null) {
            throw new IllegalStateException(
                    "Database password is not configured.\n"
                    + "Copy config.example.properties to " + FILE_NAME + " and fill in db.password,\n"
                    + "or set the MANSION_DB_PASSWORD environment variable.");
        }
        return value;
    }

    /** Fails fast with a readable message if the configuration is incomplete. */
    public static void validate() {
        url();
        user();
        password();
    }

    private static String get(String envName, String propName, String fallback) {
        String env = System.getenv(envName);
        if (env != null) return env;
        String prop = load().getProperty(propName);
        return prop != null ? prop.trim() : fallback;
    }

    private static synchronized Properties load() {
        if (cached != null) return cached;
        Properties p = new Properties();
        Path local = Paths.get(FILE_NAME);
        try {
            if (Files.exists(local)) {
                try (InputStream in = Files.newInputStream(local)) {
                    p.load(in);
                }
            } else {
                try (InputStream in = DbConfig.class.getResourceAsStream("/" + FILE_NAME)) {
                    if (in != null) p.load(in);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + FILE_NAME + ": " + e.getMessage(), e);
        }
        cached = p;
        return cached;
    }
}
