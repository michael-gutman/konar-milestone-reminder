package com.konarreminder;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Units;

@ConfigGroup(KonarReminderConfig.CONFIG_GROUP)
public interface KonarReminderConfig extends Config
{
	String CONFIG_GROUP = "konarreminder";
	String TASK_UNIT = " tasks";
	@ConfigItem(
		keyName = "multiple",
		name = "Milestone Reminder",
		description = "Sets the task multiple/milestone to activate the reminder before"
	)
	@Units(KonarReminderConfig.TASK_UNIT)
	default int multiple()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "chatMessageColor",
		name = "Chat Message Color",
		description = "The chat message reminder to use Konar will be this color."
	)
	default Color chatMessageColor()
	{
		return Color.decode("#11979B");
	}

}
