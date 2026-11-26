package com.zamplia.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Reads config.properties. Any key can be overridden with -Dkey=value on the mvn command line. */
public final class ConfigReader {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream is = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is == null) throw new IllegalStateException("config.properties not found on classpath");
            PROPS.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load config.properties", e);
        }
    }

    private ConfigReader() { }

    public static String get(String key) {
        String value = System.getProperty(key, PROPS.getProperty(key));
        if (value == null) throw new IllegalArgumentException("Missing config key: " + key);
        return value.trim();
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }
}
