package com.drthomasager.pid;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("pidindicator")
public interface PidIndicatorConfig extends Config
{
	@ConfigItem(
		keyName = "showInPvpPlaces",
		name = "Show outside fights",
		description = "Keep your own diamond over your head in PvP places (Wilderness, PvP, Last Man Standing, high risk and Deadman worlds) even when not fighting",
		position = 1
	)
	default boolean showInPvpPlaces()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSwapTicks",
		name = "Show swap labels",
		description = "Under each diamond, whether PID is safe or can swap now",
		position = 2
	)
	default boolean showSwapTicks()
	{
		return true;
	}
}
