package com.konarreminder;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;

@Slf4j
@PluginDescriptor(
	name = "Konar Milestone Reminder"
)
public class KonarReminderPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private KonarReminderConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Konar Milestone Reminder started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Konar Milestone Reminder stopped!");
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		if (chatMessage.getType() == ChatMessageType.GAMEMESSAGE && chatMessage.getMessage().matches(".*You've completed .*\\d tasks.*in a row.*"))
		{
			String message = ColorUtil.wrapWithColorTag("You should visit Konar to get bonus points for your next task.", config.chatMessageColor());
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
		}
	}

	@Provides
	KonarReminderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(KonarReminderConfig.class);
	}
}
