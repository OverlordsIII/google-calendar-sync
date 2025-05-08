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
import com.google.api.services.calendar.model.Channel;
import io.github.overlordsiii.google_calendar_sync.config.JsonHandler;
import io.github.overlordsiii.google_calendar_sync.config.PropertiesHandler;
import io.github.overlordsiii.google_calendar_sync.utils.GoogleCalendarUtil;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
// https://google-calendar-sync-cpnj.onrender.com

// https://google-calendar-sync-cpnj.onrender.com
@SpringBootApplication
public class GoogleCalendarSyncApplication {
	//TODO fix google auth in render
	//TODO Use environment variables for primary-id, secondary-id, webhook-id

	public static final Path CONFIG_HOME_DIRECTORY = Paths.get("tmp");

	public static final Path CREDENTIALS_FILE_PATH = Paths.get("etc", "secrets").resolve("private_config.json");

	public static final Path TOKENS_DIRECTORY_PATH = CONFIG_HOME_DIRECTORY.resolve("tokens");

	public static final Logger LOGGER = LogManager.getLogger("WhenIWorkCalendarSync");

	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

	private static final Set<String> SCOPES = CalendarScopes.all();

	public static Calendar SERVICE;

	public static final PropertiesHandler CONFIG = PropertiesHandler
			.builder()
			.addConfigOption("secondary-calendar-id", "")
			.addConfigOption("primary-calendar-id", "")
			.addConfigOption("sync-token", "")
			.addConfigOption("webhook-id", "")
			.setFileName("calendar-sync.properties")
			.build();

	public static final JsonHandler EVENT_CONFIG = new JsonHandler("event-config.json");

	public static void main(String[] args) throws IOException, GeneralSecurityException {
		initConfigs();
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		SERVICE =
				new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
						.setApplicationName("WhenIWorkCalendarSync")
						.build();
		createAndUpdateCopyCalendar();
		registerWebhook(CONFIG.getConfigOption("primary-calendar-id"), "https://google-calendar-sync-cpnj.onrender.com");
		SpringApplication.run(GoogleCalendarSyncApplication.class, args);
	}

	private static void createAndUpdateCopyCalendar() throws IOException {
		String cbeId = GoogleCalendarUtil.getCBECalendarId();
		CONFIG.setConfigOption("primary-calendar-id", cbeId);
		CONFIG.reload();
		String secondaryCalendar = GoogleCalendarUtil.getOrCreateCopy(cbeId);
		CONFIG.setConfigOption("secondary-calendar-id", secondaryCalendar);
		CONFIG.reload();
	}

	public static void registerWebhook(String calendarId, String webhookUrl) throws IOException {
		String channelId = UUID.randomUUID().toString();

		Channel channel = new Channel()
				.setId(channelId)
				.setType("web_hook")
				.setAddress(webhookUrl);

		Channel responseChannel = SERVICE.events().watch(calendarId, channel).execute();

		LOGGER.info("Webhook registered:");
		LOGGER.info("Channel ID: " + responseChannel.getId());
	}

	private static void initConfigs() {
		try {
			EVENT_CONFIG.save();
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
