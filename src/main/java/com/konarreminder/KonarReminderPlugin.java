package com.konarreminder;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.npcoverlay.HighlightedNpc;
import net.runelite.client.game.npcoverlay.NpcOverlayService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

import static com.konarreminder.KonarReminderConfig.CONFIG_GROUP;

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

	@Inject
	private ClientThread clientThread;

	@Inject
	private NpcOverlayService npcOverlayService;

	@Inject
	private Hooks hooks;

	/**
	 * NPCs to highlight
	 */
	@Getter(AccessLevel.PACKAGE)
	private final Map<NPC, HighlightedNpc> highlightedNpcs = new HashMap<>();

	/**
	 * The time when the last game tick event ran.
	 */
	@Getter(AccessLevel.PACKAGE)
	private Instant lastTickUpdate;

	/**
	 * Highlight strings from the configuration
	 */
	private List<String> highlights = new ArrayList<>();

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;

	private final Function<NPC, HighlightedNpc> isHighlighted = highlightedNpcs::get;

	@Override
	protected void startUp() throws Exception
	{
		npcOverlayService.registerHighlighter(isHighlighted);
		hooks.registerRenderableDrawListener(drawListener);

		clientThread.invoke(this::rebuild);

		log.info("Konar Milestone Reminder started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		npcOverlayService.unregisterHighlighter(isHighlighted);
		hooks.unregisterRenderableDrawListener(drawListener);

		clientThread.invoke(highlightedNpcs::clear);

		log.info("Konar Milestone Reminder stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN ||
				event.getGameState() == GameState.HOPPING)
		{
			highlightedNpcs.clear();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals(CONFIG_GROUP))
		{
			return;
		}

		clientThread.invoke(this::rebuild);
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		final NPC npc = npcSpawned.getNpc();
		final String npcName = npc.getName();

		if (npcName == null)
		{
			return;
		}

		if (highlightMatchesNPCName(npcName))
		{
			highlightedNpcs.put(npc, highlightedNpc(npc));
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		final NPC npc = npcDespawned.getNpc();

		highlightedNpcs.remove(npc);
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		final NPC npc = event.getNpc();
		final String npcName = npc.getName();

		highlightedNpcs.remove(npc);

		if (npcName == null)
		{
			return;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		lastTickUpdate = Instant.now();
	}

	@VisibleForTesting
	List<String> getHighlights()
	{
		return Text.fromCSV(config.SLAYER_MASTERS);
	}


	void rebuild()
	{
		highlights = getHighlights();
		highlightedNpcs.clear();

		if (client.getGameState() != GameState.LOGGED_IN &&
				client.getGameState() != GameState.LOADING)
		{
			// NPCs are still in the client after logging out,
			// but we don't want to highlight those.
			return;
		}

		for (NPC npc : client.getNpcs())
		{
			final String npcName = npc.getName();

			if (npcName == null)
			{
				continue;
			}

			if (highlightMatchesNPCName(npcName))
			{
				highlightedNpcs.put(npc, highlightedNpc(npc));
				continue;
			}
		}

		npcOverlayService.rebuild();
	}

	private boolean highlightMatchesNPCName(String npcName)
	{
		for (String highlight : highlights)
		{
			if (WildcardMatcher.matches(highlight, npcName))
			{
				return true;
			}
		}

		return false;
	}

	private HighlightedNpc highlightedNpc(NPC npc)
	{
		if (config.otherHighlight() && config.getReminderStatus()) {
			return HighlightedNpc.builder()
					.npc(npc)
					.highlightColor(config.highlightColor())
					.fillColor(config.fillColor())
					.hull(config.highlightHull())
					.tile(config.highlightTile())
					.outline(config.highlightOutline())
					.borderWidth((float) config.borderWidth())
					.outlineFeather(config.outlineFeather())
					.build();
		}
		return null;
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		String message = chatMessage.getMessage();
		if (chatMessage.getType() != ChatMessageType.GAMEMESSAGE)
			return;

		String reminderText = config.chatMessageText().isEmpty() ? config.DEFAULT_REMINDER_MSG : config.chatMessageText();
		String reminderMessage = ColorUtil.wrapWithColorTag(reminderText, config.chatMessageColor());
		if (message.equals(reminderMessage))
			return;

		Pattern streakPattern = Pattern.compile("\\d+ tasks");
		Matcher messageMatcher = streakPattern.matcher(message);
		if (!messageMatcher.find())
			return;

		int streak = Integer.parseInt(messageMatcher.group().replaceAll("\\D", ""));
		if ((streak + 1) % config.multiple() == 0) {
			config.setReminderStatus(true);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", reminderMessage, null);
		} else {
			config.setReminderStatus(false);
		}
		rebuild();
	}

	@Provides
	KonarReminderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(KonarReminderConfig.class);
	}

	@VisibleForTesting
	boolean shouldDraw(Renderable renderable, boolean drawingUI)
	{
		if (renderable instanceof NPC)
		{
			NPC npc = (NPC) renderable;

			if (highlightMatchesNPCName(npc.getName()) && config.getReminderStatus())
			{
				return !config.hideOtherSlayerMasters();
			}
		}

		return true;
	}
}
