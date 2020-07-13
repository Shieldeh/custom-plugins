package net.runelite.client.plugins.aaDevelopment;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;


@ConfigGroup("aaDevelopment")
public interface aaDevelopmentConfig extends Config
{
	@ConfigItem(
		name = "Output Key",
		description = "Which key is to be used",
		position = 0,
		keyName = "outputKey"
	)
	default Keybind outputKey()
	{
		return Keybind.NOT_SET;
	}
}
