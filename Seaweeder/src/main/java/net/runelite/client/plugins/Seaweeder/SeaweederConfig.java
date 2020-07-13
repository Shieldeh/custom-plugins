package net.runelite.client.plugins.Seaweeder;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("Seaweeder")
public interface SeaweederConfig extends Config
{
	@ConfigItem(
		name = "Start Keybind",
		description = "Which key is to be used to start the process",
		position = 0,
		keyName = "startKey"
	)
	default Keybind startKey()
	{
		return Keybind.NOT_SET;
	}
}
