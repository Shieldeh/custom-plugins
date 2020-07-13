package net.runelite.client.plugins.Seaweeder;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
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
	name = "Seaweeder",
	description = "Dude weed lmao",
	tags = "skilling, bot",
	type = PluginType.SKILLING
)
@Singleton
@Slf4j
@SuppressWarnings("unused")

public class Seaweeder extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private SeaweederConfig config;
	@Inject
	private BotUtils botUtils;
	@Inject
	private ConfigManager configManager;

	/*
	//-----------\\
	|| VARIABLES ||
	\\-----------//
	*/
	private boolean run;

	@Provides
	SeaweederConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SeaweederConfig.class);
	}

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.startKey())
	{
		public void hotkeyPressed()
		{

		}
	};

	@Override
	protected void startUp()
	{
		setVariables();
	}

	@Override
	protected void shutDown()
	{

	}

	/*
	//-----------\\
	|| FUNCTIONS ||
	\\-----------//
	*/

	private void setVariables()
	{
		run = false;
	}


}
