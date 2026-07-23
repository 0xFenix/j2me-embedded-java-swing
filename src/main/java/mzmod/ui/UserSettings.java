package mzmod.ui;

import java.io.*;
import java.util.Properties;

/**
 * Persists user settings to a local file.
 */
public final class UserSettings {

    private static final File SETTINGS_FILE = new File("mzmod-settings.properties");

    private UserSettings() {}

    private static final String KEY_PROGRAM_NAME = "programName";
    private static final String KEY_DISPLAY_NAME = "displayName";
    private static final String KEY_VENDOR       = "vendor";
    private static final String KEY_ICON_PATH    = "iconPath";
    private static final String KEY_SAVE_PATH    = "savePath";
    private static final String KEY_COPY_COUNT   = "copyCount";

    public static void save(String programName, String displayName, String vendor,
                            String iconPath, String savePath, String copyCount) {
        Properties props = new Properties();
        props.setProperty(KEY_PROGRAM_NAME, programName);
        props.setProperty(KEY_DISPLAY_NAME, displayName);
        props.setProperty(KEY_VENDOR, vendor);
        props.setProperty(KEY_ICON_PATH, iconPath);
        props.setProperty(KEY_SAVE_PATH, savePath);
        props.setProperty(KEY_COPY_COUNT, copyCount);
        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            props.store(fos, "MZMOD Advance Menu Settings");
        } catch (IOException e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }

    public static String get(String key, String defaultValue) {
        Properties props = loadProps();
        return props.getProperty(key, defaultValue);
    }

    private static Properties loadProps() {
        Properties props = new Properties();
        if (SETTINGS_FILE.exists()) {
            try (FileInputStream fis = new FileInputStream(SETTINGS_FILE)) {
                props.load(fis);
            } catch (IOException e) {
                System.err.println("Failed to load settings: " + e.getMessage());
            }
        }
        return props;
    }
}
