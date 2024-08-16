package com.konarreminder;

import java.awt.Color;

import net.runelite.client.config.*;

@ConfigGroup(KonarReminderConfig.CONFIG_GROUP)
public interface KonarReminderConfig extends Config
{
	String CONFIG_GROUP = "konarreminder";
	String TASK_UNIT = " tasks";
	String DEFAULT_REMINDER_MSG = "You should visit Konar to get bonus points for your next task.";
	String SLAYER_MASTERS = "Turael,Aya,Spria,Krystilia,Mazchna,Achtryn,Vannaka,Chaeldar,Nieve,Steve,Duradel,Kuradal";
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

	@ConfigItem(
			keyName = "chatMessageText",
			name = "Chat Message Text",
			description = "The chat message that is sent in the chatbox."
	)
	default String chatMessageText() { return DEFAULT_REMINDER_MSG; }

	@ConfigSection(
			name = "Other Master Highlight style",
			description = "The render style of NPC highlighting for other slayer masters",
			position = 0
	)
	String renderStyleSection = "renderStyleSection";

	@ConfigItem(
			keyName = "hideOtherSlayerMasters",
			name = "Hide non-Konar on reminder",
			description = "Configures whether or not other slayer masters should be hidden when you should go to konar",
			section = renderStyleSection,
			position = -2
	)
	default boolean hideOtherSlayerMasters()
	{
		return false;
	}

	@ConfigItem(
			keyName = "otherHighlight",
			name = "Highlight non-Konar on reminder",
			description = "Configures whether or not other slayer masters should be highlighted when you should go to konar",
			section = renderStyleSection,
			position = -1
	)
	default boolean otherHighlight()
	{
		return true;
	}

	@ConfigItem(
			position = 0,
			keyName = "highlightHull",
			name = "Highlight hull",
			description = "Configures whether or not NPC should be highlighted by hull",
			section = renderStyleSection
	)
	default boolean highlightHull()
	{
		return false;
	}

	@ConfigItem(
			position = 1,
			keyName = "highlightTile",
			name = "Highlight tile",
			description = "Configures whether or not NPC should be highlighted by tile",
			section = renderStyleSection
	)
	default boolean highlightTile()
	{
		return false;
	}

	@ConfigItem(
			position = 5,
			keyName = "highlightOutline",
			name = "Highlight outline",
			description = "Configures whether or not the model of the NPC should be highlighted by outline",
			section = renderStyleSection
	)
	default boolean highlightOutline()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
			position = 10,
			keyName = "npcColor",
			name = "Highlight Color",
			description = "Color of the NPC highlight border, menu, and text",
			section = renderStyleSection
	)
	default Color highlightColor()
	{
		return Color.RED;
	}

	@Alpha
	@ConfigItem(
			position = 11,
			keyName = "fillColor",
			name = "Fill Color",
			description = "Color of the NPC highlight fill",
			section = renderStyleSection
	)
	default Color fillColor()
	{
		return new Color(0, 255, 255, 20);
	}

	@ConfigItem(
			position = 12,
			keyName = "borderWidth",
			name = "Border Width",
			description = "Width of the highlighted NPC border",
			section = renderStyleSection
	)
	default double borderWidth()
	{
		return 2;
	}

	@ConfigItem(
			position = 13,
			keyName = "outlineFeather",
			name = "Outline feather",
			description = "Specify between 0-4 how much of the model outline should be faded",
			section = renderStyleSection
	)
	@Range(
			min = 0,
			max = 4
	)
	default int outlineFeather()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "reminderStatus",
			name = "",
			description = "",
			hidden = true
	)
	default boolean getReminderStatus()
	{
		return false;
	}

	@ConfigItem(
			keyName = "reminderStatus",
			name = "",
			description = ""
	)
	void setReminderStatus(boolean reminderStatus);

}
