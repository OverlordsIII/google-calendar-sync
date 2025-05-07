package io.github.overlordsiii.google_calendar_sync;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import io.github.overlordsiii.google_calendar_sync.config.PropertiesHandler;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Set;

// https://google-calendar-sync-cpnj.onrender.com
@SpringBootApplication
public class GoogleCalendarSyncApplication {

	public static final Path CONFIG_HOME_DIRECTORY = Paths.get("");

	public static final Path CREDENTIALS_FILE_PATH = CONFIG_HOME_DIRECTORY.resolve("private_config.json");

	public static final Path TOKENS_DIRECTORY_PATH = CONFIG_HOME_DIRECTORY.resolve("tokens");

	public static final Logger LOGGER = LogManager.getLogger("WhenIWorkCalendarSync");

	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

	private static final Set<String> SCOPES = CalendarScopes.all();

	public static Calendar SERVICE;

	public static final PropertiesHandler CONFIG = PropertiesHandler
			.builder()
			.addConfigOption("secondary-calendar-id", "")
			.addConfigOption("primary-calendar-id", "")
			.setFileName("calendar-sync.properties")
			.build();

	public static void main(String[] args) throws GeneralSecurityException, IOException {
		initConfigs();
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		SERVICE =
				new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
						.setApplicationName("WhenIWorkCalendarSync")
						.build();
		SpringApplication.run(GoogleCalendarSyncApplication.class, args);
	}

	private static void initConfigs() {
		try {
			if (!Files.exists(CONFIG_HOME_DIRECTORY)) {
				Files.createDirectory(CONFIG_HOME_DIRECTORY);
			} if (!Files.exists(TOKENS_DIRECTORY_PATH)) {
				Files.createDirectory(TOKENS_DIRECTORY_PATH);
			}
		} catch (IOException e) {
			LOGGER.error("Unable to create config/token directory at: \"" + CONFIG_HOME_DIRECTORY + "\" or \"" + TOKENS_DIRECTORY_PATH + "\"", e);
			e.printStackTrace();
		}
		if (!Files.exists(CREDENTIALS_FILE_PATH)) {
			throw new IllegalArgumentException("Credentials file at: \"" + CREDENTIALS_FILE_PATH + "\" not found!");
		}
	}

	private static Credential getCredentials(NetHttpTransport transport) throws IOException {
		InputStream stream = Files.newInputStream(CREDENTIALS_FILE_PATH);

		GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(stream));

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				transport, JSON_FACTORY, secrets, SCOPES)
				.setDataStoreFactory(new FileDataStoreFactory(TOKENS_DIRECTORY_PATH.toFile()))
				.setAccessType("offline")
				.build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

}
