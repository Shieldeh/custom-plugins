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
package net.runelite.client.plugins.leaguesZMI;

import com.google.inject.Provides;
import com.owain.chinbreakhandler.ChinBreakHandler;
import java.time.Instant;
import javax.inject.Inject;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.iutils.*;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;
import static net.runelite.client.plugins.leaguesZMI.leaguesZMIState.*;


@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
	name = "Leagues - ZMI",
	enabledByDefault = false,
	description = "Leagues ZMI rcer, banks at Ver Sinhaza.",
	tags = {"zmi", "skill", "boat",},
	type = PluginType.SKILLING
)

@Slf4j
public class leaguesZMIPlugin extends Plugin
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
	private leaguesZMIConfig config;

	@Inject
	PluginManager pluginManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	leaguesZMIOverlay overlay;

	@Inject
	private ChinBreakHandler chinBreakHandler;

	leaguesZMIState state;
	Instant botTimer;
	MenuEntry targetMenu;
	Player player;

	int timeout = 0;
	long sleepLength = 0;
	boolean startZMI;
	public static Set<Integer> RUNES =
		Set.of(ItemID.AIR_RUNE, ItemID.WATER_RUNE, ItemID.EARTH_RUNE, ItemID.FIRE_RUNE, ItemID.MIND_RUNE, ItemID.CHAOS_RUNE, ItemID.DEATH_RUNE, ItemID.BLOOD_RUNE, ItemID.BODY_RUNE, ItemID.COSMIC_RUNE,
			ItemID.LAW_RUNE, ItemID.NATURE_RUNE);
	public static final int TOB = 14642;
	public static final int ZMI = 12119;

	@Override
	protected void startUp()
	{

		chinBreakHandler.registerPlugin(this);
	}

	@Override
	protected void shutDown()
	{
		resetVals();
		chinBreakHandler.unregisterPlugin(this);
	}

	public void resetVals()
	{
		overlayManager.remove(overlay);
		chinBreakHandler.stopPlugin(this);
		startZMI = false;
		botTimer = null;
		timeout = 0;
		targetMenu = null;
	}

	@Provides
	leaguesZMIConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(leaguesZMIConfig.class);
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("leaguesZMI"))
		{
			return;
		}
		switch (configButtonClicked.getKey())
		{
			case "startButton":
				if (!startZMI)
				{
					startZMI = true;
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
		}
	}

	public void initVals()
	{
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup() != "leaguesZMI")
		{
			return;
		}
	}

	// Delay functions
	private long sleepDelay()
	{
		sleepLength = calc.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
		return sleepLength;
	}

	private int tickDelay()
	{
		int tickLength = (int) calc.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
		//log.debug("tick delay for {} ticks", tickLength);
		return tickLength;
	}

	// Teleporting functions
	private void teleport_orb()
	{
		targetMenu = new MenuEntry("", "", 25104, 33, inventory.getWidgetItem(25104).getIndex(), 9764864, false);
		menu.setEntry(targetMenu);
		mouse.delayMouseClick(inventory.getWidgetItem(25104).getCanvasBounds(), sleepDelay());
	}

	private void teleport_talisman()
	{
		targetMenu = new MenuEntry("", "", 2, MenuOpcode.CC_OP.getId(), -1,
			25362448, false);
		menu.setEntry(targetMenu);
		mouse.delayClickRandomPointCenter(100, 100, sleepDelay());
	}

	// Action functions
	private void craft()
	{
		GameObject zmi = object.findNearestGameObject(29631);
		if (zmi != null)
		{
			targetMenu = new MenuEntry("", "", zmi.getId(), 3,
				zmi.getLocalLocation().getSceneX() - 1, zmi.getLocalLocation().getSceneY() - 1, false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(zmi.getConvexHull().getBounds(), sleepDelay());
		}
		else
		{
			utils.sendGameMessage("ZMI altar not found.");
			startZMI = false;
		}
	}

	private void openBank()
	{
		GameObject bankTarget = object.findNearestGameObjectWithin(new WorldPoint(3652, 3207, 0), 0, ObjectID.BANK_BOOTH_32666);
		if (bankTarget != null)
		{
			targetMenu = new MenuEntry("", "", bankTarget.getId(),
				bank.getBankMenuOpcode(bankTarget.getId()), bankTarget.getSceneMinLocation().getX(),
				bankTarget.getSceneMinLocation().getY(), false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(bankTarget.getConvexHull().getBounds(), sleepDelay());
			//utils.sendGameMessage("bank clicked");
		}
	}

	private void withdrawEss()
	{

		if (bank.contains(ItemID.DAEYALT_ESSENCE, 27))
		{
			bank.withdrawAllItem(ItemID.DAEYALT_ESSENCE);
			//utils.sendGameMessage("withdraw daeyalt");
		}
		else
		{
			bank.withdrawAllItem(ItemID.PURE_ESSENCE);
			//utils.sendGameMessage("withdraw essence");
		}
		timeout = 0 + tickDelay();
	}

	public leaguesZMIState getState()
	{
		if (timeout > 0)
		{
			playerUtils.handleRun(20, 30);
			return TIMEOUT;
		}

		if (player.getPoseAnimation() == 819 || (player.getPoseAnimation() == 824) ||
			(player.getPoseAnimation() == 1205) || (player.getPoseAnimation() == 1210))
		{
			return MOVING;
		}

		if (!bank.isOpen())
		{
			if (!inventory.isFull() && client.getLocalPlayer().getWorldLocation().getRegionID() == TOB)
			{
				//utils.sendGameMessage("should open bank");
				return OPEN_BANK;
			}
			if (inventory.isFull() && client.getLocalPlayer().getWorldLocation().getRegionID() == TOB)
			{
				return TELEPORT_ORB;
			}
			if (!inventory.isFull() && client.getLocalPlayer().getWorldLocation().getRegionID() == ZMI)
			{
				return TELEPORT_TALISMAN;
			}
			if (inventory.isFull() && client.getLocalPlayer().getWorldLocation().getRegionID() == ZMI)
			{
				return CRAFT;
			}
		}

		if (bank.isOpen())
		{
			if (!bank.contains(ItemID.PURE_ESSENCE, 27))
			{
				utils.sendGameMessage("get more pure essence");
				return OUT_OF_ESSENCE;
			}
			if (inventory.containsItem(RUNES))
			{
				return DEPOSIT_ALL;
			}
			if (!inventory.isFull() && !inventory.containsItem(RUNES))
			{
				return WITHDRAW_ESSENCE;
			}
			if (inventory.isFull())
			{
				return CLOSE_BANK;
			}
			if (chinBreakHandler.shouldBreak(this))
			{
				return HANDLE_BREAK;
			}
		}
		return IDLING;
	}


	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (!startZMI || chinBreakHandler.isBreakActive(this))
		{
			return;
		}
		player = client.getLocalPlayer();
		if (client != null && player != null && client.getGameState() == GameState.LOGGED_IN)
		{
			if (!client.isResized())
			{
				utils.sendGameMessage("client must be set to resizable");
				startZMI = false;
				return;
			}
			playerUtils.handleRun(40, 20);
			state = getState();
			switch (state)
			{
				case TIMEOUT:
					timeout--;
					break;
				case ITERATING:
					break;
				case IDLING:
					timeout = 1;
					break;
				case MOVING:
					timeout = 1;
					break;
				case OPEN_BANK:
					openBank();
					timeout = tickDelay();
					break;
				case CLOSE_BANK:
					bank.close();
					timeout = tickDelay();
					break;
				case CRAFT:
					craft();
					timeout = tickDelay();
					break;
				case TELEPORT_ORB:
					teleport_orb();
					timeout = 3 + tickDelay();
					break;
				case TELEPORT_TALISMAN:
					teleport_talisman();
					timeout = 3 + tickDelay();
					break;
				case DEPOSIT_ALL:
					bank.depositAll();
					timeout = tickDelay();
					break;
				case WITHDRAW_ESSENCE:
					withdrawEss();
					break;
				case OUT_OF_ESSENCE:
					if (config.logout())
					{
						interfaceUtils.logout();
					}
					startZMI = false;
					resetVals();
					break;
				case HANDLE_BREAK:
					chinBreakHandler.startBreak(this);
					timeout = 8;
					break;
			}
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (!startZMI)
		{
			return;
		}
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			state = IDLING;
			timeout = 2;
		}
	}
}