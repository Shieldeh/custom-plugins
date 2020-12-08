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
package net.runelite.client.plugins.leaguesChopper;

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
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuOpcode;
import net.runelite.api.Player;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
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


@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
	name = "Leagues - Chopper",
	enabledByDefault = false,
	description = "Leagues hardwood cutter, banks at Ver Sinhaza, Castle Wars, or Crafting Guild.",
	tags = {"woodcutting", "skill", "boat"},
	type = PluginType.SKILLING
)
@Slf4j
public class leaguesChopperPlugin extends Plugin
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
	private leaguesChopperConfig config;
	@Inject
	PluginManager pluginManager;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	leaguesChopperOverlay overlay;
	@Inject
	private ChinBreakHandler chinBreakHandler;
	leaguesChopperState state;
	Instant botTimer;
	MenuEntry targetMenu;
	Player player;
	int timeout = 0;
	long sleepLength = 0L;
	boolean startChopper;
	public static Set<Integer> RINGS = Set.of(
		ItemID.RING_OF_DUELING1, ItemID.RING_OF_DUELING2, ItemID.RING_OF_DUELING3,
		ItemID.RING_OF_DUELING4, ItemID.RING_OF_DUELING5, ItemID.RING_OF_DUELING6,
		ItemID.RING_OF_DUELING7, ItemID.RING_OF_DUELING8);
	public static Set<Integer> teleportItems = Set.of(25104);
	// 11312 = hardwood grove
	public static final int skillingRegionID = 11312;

	public leaguesChopperPlugin()
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
		startChopper = false;
		botTimer = null;
		timeout = 0;
		targetMenu = null;
	}

	@Provides
	leaguesChopperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(leaguesChopperConfig.class);
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (configButtonClicked.getGroup().equalsIgnoreCase("leaguesChopper"))
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
							if (!startChopper)
							{
								startChopper = true;
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
		if (event.getGroup() == "leaguesChopper")
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

	private void teleportCrystal()
	{
		targetMenu = new MenuEntry("", "", 25104, 33, inventory.getWidgetItem(25104).getIndex(), 9764864, false);
		menu.setEntry(targetMenu);
		mouse.delayMouseClick(inventory.getWidgetItem(25104).getCanvasBounds(), sleepDelay());
	}

	private void equipRingInBank()
	{
		// ForceLeftClick must be true to click on an item in the inventory while the bank is open.
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
			case CRAFTING_GUILD:
				targetMenu = new MenuEntry("", "", 3, MenuOpcode.CC_OP.getId(), -1, 25362447, false);
				break;
			case CASTLE_WARS:
				targetMenu = new MenuEntry("", "", 3, MenuOpcode.CC_OP.getId(), -1, 25362455, false);
				break;
		}

		menu.setEntry(targetMenu);
		mouse.delayClickRandomPointCenter(100, 100, sleepDelay());
	}

	private void chop()
	{
		GameObject tree = object.findNearestGameObject(config.trees().gettreeObjID());
		if (tree != null)
		{
			utils.doGameObjectActionGameTick(tree, MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(), tickDelay());
		}
		else
		{
			utils.sendGameMessage("Tree not found." + config.trees().getName());
			startChopper = false;
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
			default:
				bankTarget = null;
				break;
		}
		if (bankTarget != null)
		{
			utils.doGameObjectActionGameTick(bankTarget, bank.getBankMenuOpcode(bankTarget.getId()), tickDelay());
		}
	}

	public leaguesChopperState getState()
	{
		if (chinBreakHandler.shouldBreak(this))
		{
			return leaguesChopperState.HANDLE_BREAK;
		}
		else if (timeout > 0)
		{
			playerUtils.handleRun(20, 30);
			return leaguesChopperState.TIMEOUT;
		}
		else if (player.getPoseAnimation() == 867)
		{
			return leaguesChopperState.ANIMATING;
		}
		else if (player.getPoseAnimation() != 819 && player.getPoseAnimation() != 824 && player.getPoseAnimation() != 1205 && player.getPoseAnimation() != 1210)
		{
			if (client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.CRAFTING_GUILD.getRegionID() ||
				client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.VER_SINHAZA.getRegionID() ||
				client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.CASTLE_WARS.getRegionID())
			{
				if (inventory.isFull())
				{
					if (!bank.isOpen())
					{
						return leaguesChopperState.OPEN_BANK;
					}
					return leaguesChopperState.DEPOSIT_ALL;
				}
				if (bank.isOpen())
				{
					//utils.sendGameMessage("bank is open");
					if (config.banks().equals(Banks.CASTLE_WARS) && !playerUtils.isItemEquipped(RINGS))
					{
						if (inventory.containsItem(ItemID.RING_OF_DUELING8))
						{
							return leaguesChopperState.EQUIP_RING;
						}
						else if (bank.contains(ItemID.RING_OF_DUELING8, 1))
						{
							return leaguesChopperState.WITHDRAW_RING;
						}
						else
						{
							utils.sendGameMessage("You have run out of Rings of Dueling. Plugin will now stop.");
							startChopper = false;
						}
					}
					return leaguesChopperState.CLOSE_BANK;
				}
				if (!inventory.isFull())
				{
					return leaguesChopperState.CLOSE_BANK;
				}
				if (config.banks().equals(Banks.CASTLE_WARS) && !playerUtils.isItemEquipped(RINGS))
				{
					return leaguesChopperState.OPEN_BANK;
				}
				else {
					return leaguesChopperState.TELEPORT_CRYSTAL;
				}
			}
			if (client.getLocalPlayer().getWorldLocation().getRegionID() == skillingRegionID)
			{
				if (inventory.isFull()){
					return leaguesChopperState.TELEPORT_BANK;
				}
				if (client.getLocalPlayer().getAnimation() == -1)
				{
					return leaguesChopperState.CHOP;
				}
			}
			return leaguesChopperState.IDLING;
		}
		else
		{
			return leaguesChopperState.MOVING;
		}
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (startChopper && !chinBreakHandler.isBreakActive(this))
		{
			player = client.getLocalPlayer();
			if (client != null && player != null && client.getGameState() == GameState.LOGGED_IN)
			{
				if (!client.isResized())
				{
					utils.sendGameMessage("client must be set to resizable");
					startChopper = false;
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
					case TELEPORT_CRYSTAL:
						teleportCrystal();
						timeout = 3 + tickDelay();
						break;
					case CHOP:
						chop();
						timeout = 2 + tickDelay();
						break;
					case TELEPORT_BANK:
						teleportBank();
						timeout = 2 + tickDelay();
						break;
					case DEPOSIT_ALL:
						bank.depositAllExcept(teleportItems);
						timeout = tickDelay();
						break;
					case EQUIP_RING:
						equipRingInBank();
						timeout = +1;
						break;
					case WITHDRAW_RING:
						bank.withdrawItem(ItemID.RING_OF_DUELING8);
						timeout = +1;
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
		if (startChopper)
		{
			if (event.getGameState() == GameState.LOGGED_IN)
			{
				state = leaguesChopperState.IDLING;
				timeout = 2;
			}

		}
	}
}
