package com.booker.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigManager {

    private static final ConfigManager INSTANCE = new ConfigManager();

    private final String environmentName;
    private final String baseUri;
    private final String username;
    private final String password;

    private ConfigManager() {
        this.environmentName = firstNonBlankValue(System.getenv("BOOKER_ENV"), System.getProperty("env"), "dev");
        Properties envProperties = loadEnvProperties("config/" + environmentName + ".properties", environmentName);
        this.baseUri = overrideFromEnvironment("BOOKER_BASE_URI", envProperties.getProperty("base.uri"));
        this.username = overrideFromEnvironment("BOOKER_USERNAME", envProperties.getProperty("auth.username"));
        this.password = overrideFromEnvironment("BOOKER_PASSWORD", envProperties.getProperty("auth.password"));
        requireNonBlank("base.uri", baseUri);
        requireNonBlank("auth.username", username);
        requireNonBlank("auth.password", password);
    }

    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    public String env() {
        return environmentName;
    }

    public String baseUri() {
        return baseUri;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    private void requireNonBlank(String propertyKey, String propertyValue) {
        if (propertyValue == null || propertyValue.isBlank()) {
            throw new ConfigException(
                    "Missing required property '" + propertyKey + "' for env '" + environmentName + "'");
        }
    }

    /** Env var wins when set; otherwise the value from the properties file is used. */
    private static String overrideFromEnvironment(String environmentVariableName, String valueFromPropertiesFile) {
        return firstNonBlankValue(System.getenv(environmentVariableName), valueFromPropertiesFile);
    }

    private static String firstNonBlankValue(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private static Properties loadEnvProperties(String classpathLocation, String environmentName) {
        try (InputStream propertiesStream = ConfigManager.class.getClassLoader()
                .getResourceAsStream(classpathLocation)) {
            if (propertiesStream == null) {
                throw new ConfigException(
                        "Unknown env '" + environmentName + "': classpath resource '"
                                + classpathLocation + "' was not found");
            }
            Properties envProperties = new Properties();
            envProperties.load(propertiesStream);
            return envProperties;
        } catch (IOException ioException) {
            throw new ConfigException("Failed to read '" + classpathLocation + "'", ioException);
        }
    }
}
