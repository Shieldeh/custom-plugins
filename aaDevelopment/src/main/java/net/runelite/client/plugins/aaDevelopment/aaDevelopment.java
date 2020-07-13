package net.runelite.client.plugins.aaDevelopment;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.botutils.BotUtils;
import net.runelite.client.util.HotkeyListener;
import org.pf4j.Extension;

@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
	name = "aaDevelopment",
	description = "Debug plugin to grab in-game data easily.",
	tags = "debug",
	type = PluginType.UTILITY
)
@Singleton
@Slf4j
@SuppressWarnings("unused")

public class aaDevelopment extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private aaDevelopmentConfig config;
	@Inject
	private KeyManager keyManager;
	@Inject
	private BotUtils botUtils;
	@Inject
	private ConfigManager configManager;

	@Provides
	aaDevelopmentConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(aaDevelopmentConfig.class);
	}

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.outputKey())
	{
		public void hotkeyPressed()
		{
			log.info(String.valueOf(client.getLeftClickMenuEntry()));
		}
	};

	@Override
	protected void startUp()
	{
		keyManager.registerKeyListener(hotkeyListener);
	}

	@Override
	protected void shutDown()
	{
		keyManager.unregisterKeyListener(hotkeyListener);
	}

}
