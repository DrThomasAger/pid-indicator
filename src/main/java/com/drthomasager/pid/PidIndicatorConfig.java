package com.drthomasager.pid;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("pidindicator")
public interface PidIndicatorConfig extends Config
{
	@ConfigItem(
		keyName = "markWhenMayChange",
		name = "Mark when PID may change",
		description = "The diamonds read 1? and 2? once the game may hand PID to the other player",
		position = 1
	)
	default boolean markWhenMayChange()
	{
		return true;
	}
}
