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
package net.runelite.client.plugins.leaguesPlanker;

import com.google.inject.Provides;
import com.owain.chinbreakhandler.ChinBreakHandler;
import java.awt.Rectangle;
import java.time.Instant;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuOpcode;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Player;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.iutils.BankUtils;
import net.runelite.client.plugins.iutils.CalculationUtils;
import net.runelite.client.plugins.iutils.InterfaceUtils;
import net.runelite.client.plugins.iutils.InventoryUtils;
import net.runelite.client.plugins.iutils.MenuUtils;
import net.runelite.client.plugins.iutils.MouseUtils;
import net.runelite.client.plugins.iutils.NPCUtils;
import net.runelite.client.plugins.iutils.ObjectUtils;
import net.runelite.client.plugins.iutils.PlayerUtils;
import net.runelite.client.plugins.iutils.iUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
	name = "Leagues - Planker",
	enabledByDefault = false,
	description = "Leagues Planker, banks at Ver Sinhaza, Seers Village with Kandarin Hard, or Crafting Guild.",
	tags = {"Planker", "skill", "boat"},
	type = PluginType.SKILLING
)
public class leaguesPlankerPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(leaguesPlankerPlugin.class);
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
	private leaguesPlankerConfig config;
	@Inject
	PluginManager pluginManager;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	leaguesPlankerOverlay overlay;
	@Inject
	private ChinBreakHandler chinBreakHandler;
	leaguesPlankerState state;
	Instant botTimer;
	MenuEntry targetMenu;
	Player player;
	int timeout = 0;
	long sleepLength = 0L;
	boolean startPlanker;
	public static final int skillingRegionID = 13110;
	public static Set<Integer> RINGS = Set.of(
		ItemID.RING_OF_DUELING1, ItemID.RING_OF_DUELING2, ItemID.RING_OF_DUELING3,
		ItemID.RING_OF_DUELING4, ItemID.RING_OF_DUELING5, ItemID.RING_OF_DUELING6,
		ItemID.RING_OF_DUELING7, ItemID.RING_OF_DUELING8
	);
	public static Set<Integer> STAVES = Set.of(
		ItemID.MYSTIC_AIR_STAFF, ItemID.STAFF_OF_AIR, ItemID.AIR_BATTLESTAFF,
		ItemID.SMOKE_BATTLESTAFF, ItemID.MYSTIC_SMOKE_STAFF, ItemID.MIST_BATTLESTAFF,
		ItemID.MYSTIC_MIST_STAFF, ItemID.DUST_BATTLESTAFF, ItemID.MYSTIC_DUST_STAFF);

	public leaguesPlankerPlugin()
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
		startPlanker = false;
		botTimer = null;
		timeout = 0;
		targetMenu = null;
	}

	@Provides
	leaguesPlankerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(leaguesPlankerConfig.class);
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (configButtonClicked.getGroup().equalsIgnoreCase("leaguesPlanker"))
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
							if (!startPlanker)
							{
								startPlanker = true;
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
		if (event.getGroup() == "leaguesPlanker")
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
		int tickLength = (int) calc.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
		log.debug("tick delay for {} ticks", tickLength);
		return tickLength;
	}

	private void teleportCrystal()
	{
		targetMenu = new MenuEntry("", "", 25104, 33, inventory.getWidgetItem(25104).getIndex(), 9764864, false);
		menu.setEntry(targetMenu);
		mouse.delayMouseClick(inventory.getWidgetItem(25104).getCanvasBounds(), sleepDelay());
	}

	private void equipRingInBank()
	{
		targetMenu = new MenuEntry("Wear", "Ring of Dueling (8)", 9, 1007, inventory.getWidgetItem(ItemID.RING_OF_DUELING8).getIndex(), 983043, true);
		menu.setEntry(targetMenu);
		mouse.delayMouseClick(inventory.getWidgetItem(ItemID.RING_OF_DUELING8).getCanvasBounds(), sleepDelay());
	}

	private void teleportBank()
	{
		switch (config.banks())
		{
			case VER_SINHAZA:
				targetMenu = new MenuEntry("", "", 2, MenuOpcode.CC_OP.getId(), -1, 25362448, false);
				break;
			case CASTLE_WARS:
				targetMenu = new MenuEntry("", "", 3, MenuOpcode.CC_OP.getId(), -1, 25362455, false);
				break;
			case CRAFTING_GUILD:
				targetMenu = new MenuEntry("", "", 3, MenuOpcode.CC_OP.getId(), -1, 25362447, false);
				break;
			case SEERS:
				if (playerUtils.isItemEquipped(STAVES))
				{
					targetMenu = new MenuEntry("", "", 2, MenuOpcode.CC_OP.getId(), -1, WidgetInfo.SPELL_CAMELOT_TELEPORT.getId(), false);
				}
				else
				{
					utils.sendGameMessage("You must equip an infinite source of air runes.");
					startPlanker = false;
				}
				break;
		}

		menu.setEntry(targetMenu);
		mouse.delayClickRandomPointCenter(100, 100, sleepDelay());
	}

	private void openPlankMenu()
	{
		NPC planker = npc.findNearestNpc(NpcID.SAWMILL_OPERATOR);
		if (planker != null)
		{
			utils.doNpcActionGameTick(planker, 11, tickDelay());
		}
		else
		{
			utils.sendGameMessage("Sawmill Operator not found.");
			startPlanker = false;
		}

	}

	private void openBank()
	{
		GameObject bankTarget;
		switch (config.banks())
		{
			case VER_SINHAZA:
				bankTarget = object.findNearestGameObjectWithin(Banks.VER_SINHAZA.getBankLoc(), 0, Banks.VER_SINHAZA.getBankObjID());
				break;
			case CASTLE_WARS:
				bankTarget = object.findNearestGameObject(Banks.CASTLE_WARS.getBankObjID());
				break;
			case CRAFTING_GUILD:
				bankTarget = object.findNearestGameObject(Banks.CRAFTING_GUILD.getBankObjID());
				break;
			case SEERS:
				bankTarget = object.findNearestGameObject(Banks.SEERS.getBankObjID());
				break;
			default:
				bankTarget = null;
				break;
		}
		if (bankTarget != null)
		{
			utils.doGameObjectActionGameTick(bankTarget, bank.getBankMenuOpcode(bankTarget.getId()), tickDelay());
		}
	}

	private void withdrawLogs()
	{
		int logID = config.logs().getItemIDLogs();
		if (bank.contains(logID, 1))
		{
			bank.withdrawAllItem(logID);
		}
	}

	private void clickPlankWidget()
	{
		Widget plankWidget = client.getWidget(WidgetInfo.MULTI_SKILL_MENU);
		MenuEntry plankMenuEntry;
		switch (config.logs())
		{
			case NORMAL:
				plankMenuEntry = new MenuEntry("Make","", 1 , 57,-1, 17694734,false);
				break;
			case OAK:
				plankMenuEntry = new MenuEntry("Make","", 1 , 57,-1, 17694735,false);
				break;
			case TEAK:
				plankMenuEntry = new MenuEntry("Make","", 1 , 57,-1, 17694736,false);
				break;
			case MAHOGANY:
				plankMenuEntry = new MenuEntry("Make","", 1 , 57,-1, 17694737,false);
				break;
			default:
				plankMenuEntry = null;
				break;
		}
		if (plankWidget != null)
		{
			int plankChildID = config.logs().getItemChildID();
			Rectangle plankButtonBounds = client.getWidget(WidgetID.MULTISKILL_MENU_GROUP_ID, plankChildID).getBounds();

			menu.setEntry(plankMenuEntry);
			utils.sendGameMessage("plankMenuEntry = " + plankMenuEntry);
			mouse.delayMouseClick(plankButtonBounds, sleepDelay());
		}
		else
		{
			utils.sendGameMessage("Widget not found.");
			startPlanker = false;
		}
	}

	public leaguesPlankerState getState()
	{
		if (chinBreakHandler.shouldBreak(this))
		{
			return leaguesPlankerState.HANDLE_BREAK;
		}
		if (timeout > 0)
		{
			playerUtils.handleRun(20, 30);
			return leaguesPlankerState.TIMEOUT;
		}
		else if (player.getPoseAnimation() != 819 && player.getPoseAnimation() != 824 && player.getPoseAnimation() != 1205 && player.getPoseAnimation() != 1210)
		{
			if (client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.CRAFTING_GUILD.getRegionID() ||
				client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.VER_SINHAZA.getRegionID() ||
				client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.SEERS.getRegionID() ||
				client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.CASTLE_WARS.getRegionID())
			{
				if (!inventory.containsItem(config.logs().getItemIDLogs()) && !bank.isOpen())
				{
					return leaguesPlankerState.OPEN_BANK;
				}
				if (bank.isOpen())
				{
					if (inventory.containsItem(config.logs().getItemIDPlanks()))
					{
						return leaguesPlankerState.DEPOSIT_ALL_PLANKS;
					}
					else if (!bank.contains(config.logs().getItemIDLogs(), 1))
					{
						utils.sendGameMessage("You have run out of the selected log type. Ending Plugin now.");
						return leaguesPlankerState.OUT_OF_LOGS;
					}
					else if (config.banks().equals(Banks.CASTLE_WARS) && !playerUtils.isItemEquipped(RINGS))
					{
						if (inventory.containsItem(ItemID.RING_OF_DUELING8))
						{
							return leaguesPlankerState.EQUIP_RING;
						}
						else if (bank.contains(ItemID.RING_OF_DUELING8, 1))
						{
							return leaguesPlankerState.WITHDRAW_RING;
						}
						else
						{
							utils.sendGameMessage("You have run out of Rings of Dueling. Plugin will now stop.");
							startPlanker = false;
						}
					}
					else if (config.banks().equals(Banks.SEERS) && !inventory.containsStackAmount(ItemID.LAW_RUNE, 1))
					{
						if (bank.contains(ItemID.LAW_RUNE, 1))
						{
							return leaguesPlankerState.WITHDRAW_LAWS;
						}
						else
						{
							utils.sendGameMessage("You have run out of Law runes. Plugin will now stop.");
							startPlanker = false;
						}
					}
					else if (inventory.containsItem(config.logs().getItemIDLogs()))
					{
						return leaguesPlankerState.CLOSE_BANK;
					}
					else
					{
						return leaguesPlankerState.WITHDRAW_LOGS;
					}
				}
				else if (inventory.containsItem(config.logs().getItemIDLogs()) && inventory.containsStackAmount(ItemID.COINS_995, (42000)))
				{
					if (config.banks().equals(Banks.CASTLE_WARS) && !playerUtils.isItemEquipped(RINGS))
					{
						utils.sendGameMessage("Start the script with an empty inventory if you want to use this bank.");
						startPlanker = false;
					} else
					{
						return leaguesPlankerState.TELEPORT_CRYSTAL;
					}
				}
				else {
					utils.sendGameMessage("You are below 42000gp left in your inventory. Plugin will now stop.");
					startPlanker = false;
				}
			}
			if (client.getLocalPlayer().getWorldLocation().getRegionID() == skillingRegionID)
			{
				if (inventory.containsItem(config.logs().getItemIDPlanks()))
				{
					return leaguesPlankerState.TELEPORT_BANK;
				}
				else if (client.getWidget(WidgetInfo.MULTI_SKILL_MENU) == null){
					return leaguesPlankerState.INTERACT_SAWMILL;
				}
				return leaguesPlankerState.CLICK_PLANK_WIDGET;
			}
			return leaguesPlankerState.IDLING;
		}
		else
		{
			return leaguesPlankerState.MOVING;
		}
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (startPlanker && !chinBreakHandler.isBreakActive(this))
		{
			player = client.getLocalPlayer();
			if (client != null && player != null && client.getGameState() == GameState.LOGGED_IN)
			{
				if (!client.isResized())
				{
					utils.sendGameMessage("client must be set to resizable");
					startPlanker = false;
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
					case IDLING:
						timeout = tickDelay();
						break;
					case MOVING:
						timeout = tickDelay();
						break;
					case OPEN_BANK:
						openBank();
						timeout = 1 + tickDelay();
						break;
					case EQUIP_RING:
						equipRingInBank();
						timeout = +1;
						break;
					case WITHDRAW_RING:
						//utils.sendGameMessage("Withdrawing a fresh ring");
						bank.withdrawItem(ItemID.RING_OF_DUELING8);
						timeout = 1;
						break;
					case CLOSE_BANK:
						bank.close();
						timeout = 1 + tickDelay();
						break;
					case CLICK_PLANK_WIDGET:
						clickPlankWidget();
						timeout = 1 + tickDelay();
						break;
					case INTERACT_SAWMILL:
						openPlankMenu();
						timeout = 1 + tickDelay();
						break;
					case TELEPORT_CRYSTAL:
						teleportCrystal();
						timeout = 3 + tickDelay();
						break;
					case TELEPORT_BANK:
						teleportBank();
						timeout = 2 + tickDelay();
						break;
					case DEPOSIT_ALL_PLANKS:
						bank.depositAllOfItem(config.logs().getItemIDPlanks());
						timeout = 1 + tickDelay();
						break;
					case WITHDRAW_LOGS:
						withdrawLogs();
						timeout = 1 + tickDelay();
						break;
					case WITHDRAW_LAWS:
						bank.withdrawAllItem(ItemID.LAW_RUNE);
						break;
					case OUT_OF_LOGS:
						if (config.logout())
						{
							interfaceUtils.logout();
						}

						startPlanker = false;
						resetVals();
						break;
					case HANDLE_BREAK:
						chinBreakHandler.startBreak(this);
						timeout = 8;
						break;
				}
			}

		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (startPlanker)
		{
			if (event.getGameState() == GameState.LOGGED_IN)
			{
				state = leaguesPlankerState.IDLING;
				timeout = 2;
			}

		}
	}
}
