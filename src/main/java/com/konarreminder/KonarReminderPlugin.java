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
import net.runelite.api.coords.WorldPoint;
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
import net.runelite.client.ui.overlay.OverlayManager;
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
	private static final int MAX_ACTOR_VIEW_RANGE = 15;

	@Inject
	private Client client;

	@Inject
	private KonarReminderConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private NpcOverlayService npcOverlayService;

	@Inject
	private Hooks hooks;

	private boolean hideOtherSlayerMasters;

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

	/**
	 * The players location on the last game tick.
	 */
	private WorldPoint lastPlayerLocation;

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;

	private final Function<NPC, HighlightedNpc> isHighlighted = highlightedNpcs::get;

	@Override
	protected void startUp() throws Exception
	{
		npcOverlayService.registerHighlighter(isHighlighted);
		hooks.registerRenderableDrawListener(drawListener);

		updateConfig();

		clientThread.invoke(() ->
		{
			rebuild();
		});

		log.info("Konar Milestone Reminder started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		npcOverlayService.unregisterHighlighter(isHighlighted);
		hooks.unregisterRenderableDrawListener(drawListener);

		clientThread.invoke(() ->
		{
			highlightedNpcs.clear();
		});

		log.info("Konar Milestone Reminder stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN ||
				event.getGameState() == GameState.HOPPING)
		{
			highlightedNpcs.clear();
			lastPlayerLocation = null;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals(CONFIG_GROUP))
		{
			return;
		}

		updateConfig();
		clientThread.invoke(this::rebuild);
	}

	private void updateConfig()
	{
		hideOtherSlayerMasters = config.hideOtherSlayerMasters();
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
		lastPlayerLocation = client.getLocalPlayer().getWorldLocation();
	}

	private void updateNpcsToHighlight(String npc)
	{
		final List<String> highlightedNpcs = new ArrayList<>(highlights);

		if (!highlightedNpcs.removeIf(npc::equalsIgnoreCase))
		{
			highlightedNpcs.add(npc);
		}

		// this triggers the config change event and rebuilds npcs
		config.setNpcToHighlight(Text.toCSV(highlightedNpcs));
	}

	private static boolean isInViewRange(WorldPoint wp1, WorldPoint wp2)
	{
		int distance = wp1.distanceTo(wp2);
		return distance < MAX_ACTOR_VIEW_RANGE;
	}

	private static WorldPoint getWorldLocationBehind(NPC npc)
	{
		final int orientation = npc.getOrientation() / 256;
		int dx = 0, dy = 0;

		switch (orientation)
		{
			case 0: // South
				dy = -1;
				break;
			case 1: // Southwest
				dx = -1;
				dy = -1;
				break;
			case 2: // West
				dx = -1;
				break;
			case 3: // Northwest
				dx = -1;
				dy = 1;
				break;
			case 4: // North
				dy = 1;
				break;
			case 5: // Northeast
				dx = 1;
				dy = 1;
				break;
			case 6: // East
				dx = 1;
				break;
			case 7: // Southeast
				dx = 1;
				dy = -1;
				break;
		}

		final WorldPoint currWP = npc.getWorldLocation();
		return new WorldPoint(currWP.getX() - dx, currWP.getY() - dy, currWP.getPlane());
	}

	@VisibleForTesting
	List<String> getHighlights()
	{
		final String configNpcs = config.getNpcToHighlight();

		if (configNpcs.isEmpty())
		{
			return Collections.emptyList();
		}

		return Text.fromCSV(configNpcs);
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
		if (chatMessage.getType() == ChatMessageType.GAMEMESSAGE)
		{
			Pattern streakPattern = Pattern.compile("\\d+ tasks");
			Matcher messageMatcher = streakPattern.matcher(message);
			if (messageMatcher.find()) {
				int streak = Integer.parseInt(messageMatcher.group().replaceAll("\\D", ""));
				if ((streak + 1) % config.multiple() == 0) {
					config.setReminderStatus(true);
					String reminderMessage =
							config.chatMessageText().isEmpty()
									? ColorUtil.wrapWithColorTag("You should visit Konar to get bonus points for your next task.", config.chatMessageColor())
									: ColorUtil.wrapWithColorTag(config.chatMessageText(), config.chatMessageColor());
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", reminderMessage, null);
				} else {
					config.setReminderStatus(false);
				}
				updateConfig();
				rebuild();
			}
		}
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
				return !hideOtherSlayerMasters;
			}
		}

		return true;
	}
}
