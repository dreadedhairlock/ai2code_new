package com.sap.cap.ai2code.handlers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class envLoader {

    private static final Map<String, String> envVars = new HashMap<>();
    private static boolean loaded = false;

    static {
        loadEnvFile();
    }

    private static void loadEnvFile() {
        if (loaded) {
            return;
        }

        // Try multiple locations for .env file
        String[] envPaths = {
            ".env", // Current directory
            System.getProperty("user.dir") + "/.env", // Working directory
            "/etc/myapp/.env", // System config
            System.getProperty("user.home") + "/.env" // User home
        };

        for (String envPath : envPaths) {
            try (BufferedReader reader = new BufferedReader(new FileReader(envPath))) {
                loadFromReader(reader);
                System.out.println("Environment variables loaded from: " + envPath);
                return;
            } catch (IOException e) {
                // Continue to next path
            }
        }

        System.err.println("No .env file found in any of the expected locations");
    }

    private static void loadFromReader(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Split on first = sign
            int equalIndex = line.indexOf('=');
            if (equalIndex > 0) {
                String key = line.substring(0, equalIndex).trim();
                String value = line.substring(equalIndex + 1).trim();

                // Remove quotes if present
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                envVars.put(key, value);
            }
        }
        loaded = true;
    }

    public static String get(String key) {
        // First check system environment variables
        String systemValue = System.getenv(key);
        if (systemValue != null) {
            return systemValue;
        }

        // Then check .env file
        return envVars.get(key);
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    // Method to get required variables without defaults - throws exception if not found
    public static String getRequired(String key) {
        String value = get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Required environment variable '" + key + "' is not set");
        }
        return value;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid integer format for key: " + key);
            }
        }
        return defaultValue;
    }

    public static int getRequiredInt(String key) {
        String value = getRequired(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Environment variable '" + key + "' must be a valid integer");
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
}
