package io.github.overlordsiii.google_calendar_sync.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.overlordsiii.google_calendar_sync.GoogleCalendarSyncApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class JsonHandler {
    private final Path configPath;

    private JsonObject base = new JsonObject();

    public static final Path CONFIG_HOME_DIRECTORY = Paths.get(System.getProperty("user.home")).resolve("WhenIWorkCalendarSyncConfig");

    public static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    static {
        if (!Files.exists(CONFIG_HOME_DIRECTORY)) {
            try {
                Files.createDirectory(CONFIG_HOME_DIRECTORY);
                GoogleCalendarSyncApplication.LOGGER.info("Creating config directory at: \"" + CONFIG_HOME_DIRECTORY + "\"");
            } catch (IOException e) {
                GoogleCalendarSyncApplication.LOGGER.error("Error while creating config home directory at: \"" + CONFIG_HOME_DIRECTORY + "\"");
                e.printStackTrace();
            }
        }
    }

    public JsonHandler(String fileName) {
        this.configPath = CONFIG_HOME_DIRECTORY.resolve(fileName);
    }

    public JsonHandler initialize() {
        try {
            load();
            save();
        } catch (IOException e) {
            throw new RuntimeException("Could not load config files for json handler: " + configPath.getFileName(), e);
        }

        return this;
    }

    public JsonHandler reload() {
        try {
            save();
            load();
        } catch (IOException e) {
            throw new RuntimeException("Could not load config files for json handler: " + configPath.getFileName(), e);
        }

        return this;
    }

    public void save() throws IOException {

        if (!Files.exists(configPath.getParent())) {
            throw new RuntimeException("Could not find resources dir!");
        }

        String file = GSON.toJson(base);

        Files.writeString(configPath, file);

    }

    public void load() throws IOException {
        if (!Files.exists(configPath)) {
            throw new RuntimeException("Could not find config file!");
        }

        this.base = JsonParser.parseReader(Files.newBufferedReader(configPath)).getAsJsonObject();
    }

    public JsonObject getBase() {
        return base;
    }

    public void setBase(JsonObject base) {
        this.base = base;
    }
}
