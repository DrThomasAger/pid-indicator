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
		keyName = "pulseWhenMayChange",
		name = "Pulse when PID may change",
		description = "The crown pulses once the game may hand PID to the other player",
		position = 2
	)
	default boolean pulseWhenMayChange()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showAnimationIds",
		name = "Show animation ids",
		description = "For adding weapons: shows the animation id of each attack under the diamonds",
		position = 3
	)
	default boolean showAnimationIds()
	{
		return false;
	}
}
