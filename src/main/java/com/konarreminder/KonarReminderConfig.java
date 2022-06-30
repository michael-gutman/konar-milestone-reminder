package com.konarreminder;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Units;

@ConfigGroup("example")
public interface KonarReminderConfig extends Config
{

	String TASK_UNIT = " tasks";
	@ConfigItem(
		keyName = "multiple",
		name = "Milestone Reminder",
		description = "Sets the task multiple/milestone to activate the reminder before"
	)
	@Units(TASK_UNIT)
	default int multiple()
	{
		return 50;
	}
}
