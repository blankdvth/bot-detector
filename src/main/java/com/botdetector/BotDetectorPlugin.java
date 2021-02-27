package net.runelite.client.plugins.botdetector;
import com.google.inject.Provides;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.config.ConfigManager;
import javax.inject.Inject;
import java.io.*;
import java.io.File;
import net.runelite.api.Client;
import net.runelite.api.Player;
import okhttp3.*;

import java.io.IOException;

@PluginDescriptor(
		name = "Bot Detector",
		description = "This plugin collects all nearby player names in order to externally detect Botting Behaviours.",
		tags = {"Bot", "Detector", "Player"},
		loadWhenOutdated = true,
		enabledByDefault = false
)

public class BotDetectorPlugin extends Plugin {

	int breaker = 0;

	public File fileInput = new File("temp.txt");
	private PrintWriter input = new PrintWriter(fileInput);

	public File fileOutput = new File("PlayerNames.txt");
	private PrintWriter pw = new PrintWriter(fileOutput);

	public static final MediaType MEDIA_TYPE_MARKDOWN = MediaType.parse("text/x-markdown; charset=utf-8");
	private final OkHttpClient okclient = new OkHttpClient();
	public BotDetectorPlugin() throws IOException {
	}
	@Inject
	private BotDetectorConfig config;

	@Inject
	private Client client;

	@Provides
	BotDetectorConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(BotDetectorConfig.class);
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned event) throws IOException {
		Player player = event.getPlayer();
		input.println(player.getName());
		System.out.println(player.getName());
		if(breaker == 0){
			if (config.nameCollect()) {
				input.close();
				BufferedReader br1 = new BufferedReader(new FileReader("temp.txt"));
				String line1 = br1.readLine();
				while (line1 != null) {
					boolean flag = false;
					BufferedReader br2 = new BufferedReader(new FileReader("PlayerNames.txt"));
					String line2 = br2.readLine();
					while (line2 != null) {
						if (line1.equals(line2)) {
							flag = true;
							break;
						}
						line2 = br2.readLine();
					}
					if (!flag) {
						pw.println(line1);
						pw.flush();
					}
					line1 = br1.readLine();
				}
				br1.close();
				pw.close();
				System.out.println("Duplicates Successfully Removed.");
				File file = new File("PlayerNames.txt");
				Request request = new Request.Builder()
						.url("http://73.118.45.55:8000/") //server to post the file upload to
						.post(RequestBody.create(MEDIA_TYPE_MARKDOWN, file))
						.build();
				try (Response response = okclient.newCall(request).execute()) {
				}
				breaker++;
			}
		}
	}
	@Override
	protected void startUp() throws Exception {
		fileInput.createNewFile();
		fileOutput.createNewFile();
	}

	@Override
	protected void shutDown() throws Exception {
		input.close();
		pw.close();
	}
}