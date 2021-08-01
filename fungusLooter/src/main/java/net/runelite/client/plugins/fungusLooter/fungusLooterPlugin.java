/*
 * Copyright (c) 2020, c17 <https://github.com/cyborg-17/c17-plugins>
 * Copyright (c) 2020, Shieldeh <https://github.com/oplosthee>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.fungusLooter;

import com.google.inject.Provides;
import com.owain.chinbreakhandler.ChinBreakHandler;
import java.time.Instant;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.ObjectID;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.iutils.BankUtils;
import net.runelite.client.plugins.iutils.CalculationUtils;
import net.runelite.client.plugins.iutils.InterfaceUtils;
import net.runelite.client.plugins.iutils.InventoryUtils;
import net.runelite.client.plugins.iutils.MenuUtils;
import net.runelite.client.plugins.iutils.MouseUtils;
import net.runelite.client.plugins.iutils.NPCUtils;
import net.runelite.client.plugins.iutils.ObjectUtils;
import net.runelite.client.plugins.iutils.PlayerUtils;
import net.runelite.client.plugins.iutils.WalkUtils;
import net.runelite.client.plugins.iutils.iUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;


@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
	name = "Fungus Looter",
	enabledByDefault = false,
	description = "Farms Mort Myre Fungus",
	tags = {"money", "herblore", "boat"}
)
@Slf4j
public class fungusLooterPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private iUtils utils;
	@Inject
	private MouseUtils mouse;
	@Inject
	private PlayerUtils playerUtils;
	@Inject
	private InventoryUtils inventory;
	@Inject
	private InterfaceUtils interfaceUtils;
	@Inject
	private CalculationUtils calc;
	@Inject
	private MenuUtils menu;
	@Inject
	private ObjectUtils object;
	@Inject
	private BankUtils bank;
	@Inject
	private NPCUtils npc;
	@Inject
	private WalkUtils walkUtils;
	@Inject
	public fungusLooterConfig config;
	@Inject
	PluginManager pluginManager;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	public fungusLooterOverlay overlay;
	@Inject
	private ChinBreakHandler chinBreakHandler;
	fungusLooterState state;
	Instant botTimer;
	MenuEntry targetMenu;
	Player player;
	GameObject fairyRing;
	LocalPoint beforeLoc = new LocalPoint(0, 0);
	int ringOpcode;
	int timeout = 0;
	long sleepLength = 0;
	boolean startBot;
	public static final int swampRegionID = 13877;
	public static final int altarRegionID = 10290;
	public static final int ardyRingRegionID = 10546;
	public static final int zanarisRegionID = 9541;
	public static final WorldArea zanarisBankArea = new WorldArea(new WorldPoint(2383, 4445, 0), new WorldPoint(2397, 4445, 0));
	public static Set<Integer> CAPES = Set.of(
		ItemID.ARDOUGNE_CLOAK_1, ItemID.ARDOUGNE_CLOAK_2, ItemID.ARDOUGNE_CLOAK_3,
		ItemID.ARDOUGNE_CLOAK_4);

	public fungusLooterPlugin()
	{
	}

	protected void startUp()
	{
		chinBreakHandler.registerPlugin(this);
	}

	protected void shutDown()
	{
		resetVals();
		chinBreakHandler.unregisterPlugin(this);
	}

	public void resetVals()
	{
		overlayManager.remove(overlay);
		chinBreakHandler.stopPlugin(this);
		startBot = false;
		botTimer = null;
		timeout = 0;
		targetMenu = null;
		fairyRing = null;
	}

	@Provides
	fungusLooterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(fungusLooterConfig.class);
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (configButtonClicked.getGroup().equalsIgnoreCase("fungusLooter"))
		{
			String var2 = configButtonClicked.getKey();
			byte var3 = -1;
			switch (var2.hashCode())
			{
				case 1943111220:
					if (var2.equals("startButton"))
					{
						var3 = 0;
					}
				default:
					switch (var3)
					{
						case 0:
							if (!startBot)
							{
								startBot = true;
								chinBreakHandler.startPlugin(this);
								botTimer = Instant.now();
								state = null;
								targetMenu = null;
								timeout = 0;
								overlayManager.add(overlay);
								initVals();
							}
							else
							{
								resetVals();
							}
						default:
					}
			}
		}
	}

	public void initVals()
	{
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup() == "fungusLooter")
		{
		}
	}

	private long sleepDelay()
	{
		sleepLength = calc.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
		return sleepLength;
	}

	private int tickDelay()
	{
		int tickLength = (int) calc
			.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
		return tickLength;
	}


	private void pray()
	{
		GameObject altar = object.findNearestGameObject(ObjectID.ALTAR);
		if (altar != null && altar.getConvexHull() != null)
		{
			utils.doGameObjectActionGameTick(altar, MenuAction.GAME_OBJECT_FIRST_OPTION.getId(), tickDelay());
		}
		else
		{
			utils.sendGameMessage("altar not found.");
			log.info("altar not found.");
			startBot = false;
		}
	}

	private void pickFungus()
	{
		GameObject fungus = object.findNearestGameObject(ObjectID.FUNGI_ON_LOG);
		if (fungus != null && fungus.getConvexHull() != null)
		{
			utils.doGameObjectActionGameTick(fungus, MenuAction.GAME_OBJECT_SECOND_OPTION.getId(), tickDelay());
		}
		else
		{
			utils.sendGameMessage("fungi_on_log not found.");
			log.info("fungi_on_log not found.");
			startBot = false;
		}
	}

	private void bloom()
	{
		if (inventory.containsItem(ItemID.SILVER_SICKLE_B))
		{
			WidgetItem bloom = inventory.getWidgetItem(ItemID.SILVER_SICKLE_B);
			utils.doItemActionGameTick(bloom, MenuAction.ITEM_THIRD_OPTION.getId(), 9764864, tickDelay());
		}
		else if (inventory.containsItem(ItemID.IVANDIS_FLAIL))
		{
			WidgetItem bloom = inventory.getWidgetItem(ItemID.IVANDIS_FLAIL);
			utils.doItemActionGameTick(bloom, MenuAction.ITEM_THIRD_OPTION.getId(), 9764864, tickDelay());
		}
		else if (inventory.containsItem(ItemID.BLISTERWOOD_FLAIL))
		{
			WidgetItem bloom = inventory.getWidgetItem(ItemID.BLISTERWOOD_FLAIL);
			utils.doItemActionGameTick(bloom, MenuAction.ITEM_THIRD_OPTION.getId(), 9764864, tickDelay());
		}
		else
		{
			utils.sendGameMessage("You don't have a Silver Sickle (B) or an Ivandis/Blisterwood Flail. Plugin will now stop.");
			log.info("You don't have a Silver Sickle (B) or an Ivandis/Blisterwood Flail. Plugin will now stop.");
			startBot = false;
		}
	}

	private void teleportRing()
	{
		if (client.getLocalPlayer().getWorldLocation().getRegionID() == ardyRingRegionID)
		{

			fairyRing = object.findNearestGameObject(29495);
			ringOpcode = MenuAction.GAME_OBJECT_FIRST_OPTION.getId();
		}
		if (client.getLocalPlayer().getWorldLocation().getRegionID() == zanarisRegionID)
		{

			fairyRing = object.findNearestGameObject(29560);
			ringOpcode = MenuAction.GAME_OBJECT_THIRD_OPTION.getId();
		}

		if (fairyRing != null)
		{
			utils.doGameObjectActionGameTick(fairyRing, ringOpcode, tickDelay());
		}
		else
		{
			utils.sendGameMessage("fairyRing not found.");
			startBot = false;
		}
	}

	// Teleports to the Ardy Altar using the Ardy Cape that is equipped.
	private void teleportAltar()
	{
		if (playerUtils.isItemEquipped(CAPES))
		{
			//Fixed this by updating actionParam1 by adding +1.
			targetMenu = new MenuEntry("", "", 2, MenuAction.CC_OP.getId(), -1, 25362448, false);
			menu.setEntry(targetMenu);
			mouse.delayClickRandomPointCenter(100, 100, sleepDelay());
		}
		else
		{
			utils.sendGameMessage("You don't have an Ardy cape equipped. Plugin will now stop.");
			log.info("You don't have an Ardy cape equipped. Plugin will now stop.");
			startBot = false;
		}

	}

	private void openBank()
	{
		NPC bankerTarget = npc.findNearestNpc(Banks.ZANARIS.getBankObjID());

		if (bankerTarget != null && bankerTarget.getConvexHull() != null)
		{
			utils.doNpcActionGameTick(bankerTarget, 11, tickDelay());
		}
	}

	public fungusLooterState getState()
	{
		if (chinBreakHandler.shouldBreak(this))
		{
			return fungusLooterState.HANDLE_BREAK;
		}
		else if (timeout > 0)
		{
			playerUtils.handleRun(20, 30);
			return fungusLooterState.TIMEOUT;
		}
		else if (player.getPoseAnimation() == 867)
		{
			return fungusLooterState.ANIMATING;
		}
		else if (player.getPoseAnimation() != 819 && player.getPoseAnimation() != 824 && player.getPoseAnimation() != 1205 && player.getPoseAnimation() != 1210)
		{
			if (client.getLocalPlayer().getWorldLocation().getRegionID() == zanarisRegionID)
			{
				if (npc.findNearestNpc(NpcID.BANKER_3092) != null)
				{
					if (inventory.containsItem(ItemID.MORT_MYRE_FUNGUS))
					{
						if (!bank.isOpen())
						{
							return fungusLooterState.OPEN_BANK;
						}
						return fungusLooterState.DEPOSIT_ALL;
					}
					if (!inventory.containsItem(ItemID.MORT_MYRE_FUNGUS))
					{
						return fungusLooterState.CLOSE_BANK;
					}
					else
					{
						return fungusLooterState.TELEPORT_RING;
					}
				}
				else if (inventory.containsItem(ItemID.MORT_MYRE_FUNGUS) && npc.findNearestNpc(NpcID.BANKER_3092) == null)
				{
					return fungusLooterState.RUN_TO_BANK;
				}
			}
			if (client.getLocalPlayer().getWorldLocation().getRegionID() == swampRegionID)
			{
				if (inventory.isFull() || client.getBoostedSkillLevel(Skill.PRAYER) == 0)
				{
					return fungusLooterState.TELEPORT_ALTAR;
				}
				if (object.findNearestGameObjectWithin(new WorldPoint(3474, 3419, 0), 1, ObjectID.FUNGI_ON_LOG) != null)
				{
					return fungusLooterState.PICK_FUNGUS;
				}
				if (!client.getLocalPlayer().getWorldLocation().equals(new WorldPoint(3474, 3419, 0)) &&
					object.findNearestGameObjectWithin(new WorldPoint(3474, 3419, 0), 1, ObjectID.FUNGI_ON_LOG) == null)
				{
					return fungusLooterState.RUN_TO_LOGS;
				}
				else if (client.getLocalPlayer().getWorldLocation().equals(new WorldPoint(3474, 3419, 0)) &&
					object.findNearestGameObjectWithin(new WorldPoint(3474, 3419, 0), 1, ObjectID.FUNGI_ON_LOG) == null)
				{
					return fungusLooterState.BLOOM;
				}
			}
			if (client.getLocalPlayer().getWorldLocation().getRegionID() == altarRegionID)
			{
				if (client.getBoostedSkillLevel(Skill.PRAYER) != client.getRealSkillLevel(Skill.PRAYER))
				{
					return fungusLooterState.PRAY;
				}
				else
				{
					return fungusLooterState.RUN_TO_RING;
				}
			}
			if (client.getLocalPlayer().getWorldLocation().getRegionID() == ardyRingRegionID)
			{
				return fungusLooterState.TELEPORT_RING;
			}
			if (client.getLocalPlayer().getWorldLocation().getRegionID() == 13621)
			{
				return fungusLooterState.RUN_TO_LOGS;
			}
			return fungusLooterState.IDLING;
		}
		else
		{
			return fungusLooterState.MOVING;
		}
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (startBot && !chinBreakHandler.isBreakActive(this))
		{
			player = client.getLocalPlayer();
			if (client != null && player != null && client.getGameState() == GameState.LOGGED_IN)
			{
				if (!client.isResized())
				{
					utils.sendGameMessage("client must be set to resizable");
					startBot = false;
					return;
				}

				playerUtils.handleRun(20, 40);
				state = getState();
				switch (state)
				{
					case TIMEOUT:
						--timeout;
					case ITERATING:
					default:
						break;
					case ANIMATING:
					case IDLING:
					case MOVING:
						timeout = 1;
						break;
					case OPEN_BANK:
						openBank();
						timeout = tickDelay();
						break;
					case CLOSE_BANK:
						bank.close();
						timeout = 1 + tickDelay();
					case TELEPORT_RING:
						teleportRing();
						timeout = 3 + tickDelay();
						break;
					case BLOOM:
						bloom();
						timeout = 1 + tickDelay();
						break;
					case PICK_FUNGUS:
						pickFungus();
						timeout = tickDelay();
						break;
					case PRAY:
						pray();
						timeout = 2 + tickDelay();
						break;
					case RUN_TO_BANK:
						walkUtils.sceneWalk(new WorldPoint(2394, 4454, 0), 2, sleepDelay());
						timeout = 1 + tickDelay();
						break;
					case RUN_TO_LOGS:
						walkUtils.sceneWalk(new WorldPoint(3474, 3419, 0), 0, sleepDelay());
						timeout = 1 + tickDelay();
						break;
					case RUN_TO_RING:
						walkUtils.sceneWalk(new WorldPoint(2644, 3230, 0), 3, sleepDelay());
						timeout = 1 + tickDelay();
						break;
					case TELEPORT_ALTAR:
						teleportAltar();
						timeout = 2 + tickDelay();
						break;
					case DEPOSIT_ALL:
						bank.depositAllOfItem(ItemID.MORT_MYRE_FUNGUS);
						timeout = tickDelay();
						break;
					case HANDLE_BREAK:
						chinBreakHandler.startBreak(this);
						timeout = 8;

				}
			}

		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (startBot)
		{
			if (event.getGameState() == GameState.LOGGED_IN)
			{
				state = fungusLooterState.IDLING;
				timeout = 2;
			}

		}
	}
}
