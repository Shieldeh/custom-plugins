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
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuOpcode;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
	name = "Leagues - ZMI",
	enabledByDefault = false,
	description = "Leagues ZMI runecrafter, banks at Ver Sinhaza or Crafting Guild.",
	tags = {"zmi", "skill", "boat"},
	type = PluginType.SKILLING
)
public class leaguesZMIPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(leaguesZMIPlugin.class);
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
	long sleepLength = 0L;
	boolean startZMI;
	public static Set<Integer> RUNES = Set.of(556, 555, 557, 554, 558, 562, 560, 565, 559, 564, 563, 561);
	public static final int ZMI = 12119;

	public leaguesZMIPlugin()
	{
	}

	protected void startUp()
	{
		this.chinBreakHandler.registerPlugin(this);
	}

	protected void shutDown()
	{
		this.resetVals();
		this.chinBreakHandler.unregisterPlugin(this);
	}

	public void resetVals()
	{
		this.overlayManager.remove(this.overlay);
		this.chinBreakHandler.stopPlugin(this);
		this.startZMI = false;
		this.botTimer = null;
		this.timeout = 0;
		this.targetMenu = null;
	}

	@Provides
	leaguesZMIConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(leaguesZMIConfig.class);
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (configButtonClicked.getGroup().equalsIgnoreCase("leaguesZMI"))
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
							if (!this.startZMI)
							{
								this.startZMI = true;
								this.chinBreakHandler.startPlugin(this);
								this.botTimer = Instant.now();
								this.state = null;
								this.targetMenu = null;
								this.timeout = 0;
								this.overlayManager.add(this.overlay);
								this.initVals();
							}
							else
							{
								this.resetVals();
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
		if (event.getGroup() == "leaguesZMI")
		{
		}
	}

	private long sleepDelay()
	{
		this.sleepLength = this.calc.randomDelay(this.config.sleepWeightedDistribution(), this.config.sleepMin(), this.config.sleepMax(), this.config.sleepDeviation(), this.config.sleepTarget());
		return this.sleepLength;
	}

	private int tickDelay()
	{
		int tickLength = (int) this.calc
			.randomDelay(this.config.tickDelayWeightedDistribution(), this.config.tickDelayMin(), this.config.tickDelayMax(), this.config.tickDelayDeviation(), this.config.tickDelayTarget());
		return tickLength;
	}

	private void teleportCrystal()
	{
		this.targetMenu = new MenuEntry("", "", 25104, 33, this.inventory.getWidgetItem(25104).getIndex(), 9764864, false);
		this.menu.setEntry(this.targetMenu);
		this.mouse.delayMouseClick(this.inventory.getWidgetItem(25104).getCanvasBounds(), this.sleepDelay());
	}

	private void teleportBank()
	{
		if (this.config.banks().equals(Banks.CRAFTING_GUILD))
		{
			this.targetMenu = new MenuEntry("", "", 3, MenuOpcode.CC_OP.getId(), -1, 25362447, false);
		}

		if (this.config.banks().equals(Banks.VER_SINHAZA))
		{
			this.targetMenu = new MenuEntry("", "", 2, MenuOpcode.CC_OP.getId(), -1, 25362448, false);
		}

		this.menu.setEntry(this.targetMenu);
		this.mouse.delayClickRandomPointCenter(100, 100, this.sleepDelay());
	}

	private void craft()
	{
		GameObject zmi = this.object.findNearestGameObject(29631);
		if (zmi != null)
		{
			this.targetMenu = new MenuEntry("", "", zmi.getId(), 3, zmi.getLocalLocation().getSceneX() - 1, zmi.getLocalLocation().getSceneY() - 1, false);
			this.menu.setEntry(this.targetMenu);
			this.mouse.delayMouseClick(zmi.getConvexHull().getBounds(), this.sleepDelay());
		}
		else
		{
			this.utils.sendGameMessage("ZMI altar not found.");
			this.startZMI = false;
		}

	}

	private void openBank()
	{
		GameObject bankTarget;
		if (this.config.banks().equals(Banks.CRAFTING_GUILD))
		{
			bankTarget = this.object.findNearestGameObject(14886);
			if (bankTarget != null)
			{
				this.targetMenu =
					new MenuEntry("", "", bankTarget.getId(), this.bank.getBankMenuOpcode(bankTarget.getId()), bankTarget.getSceneMinLocation().getX(), bankTarget.getSceneMinLocation().getY(), false);
				this.menu.setEntry(this.targetMenu);
				this.mouse.delayMouseClick(bankTarget.getConvexHull().getBounds(), this.sleepDelay());
			}
		}

		if (this.config.banks().equals(Banks.VER_SINHAZA))
		{
			bankTarget = this.object.findNearestGameObjectWithin(new WorldPoint(3652, 3207, 0), 0, 32666);
			if (bankTarget != null)
			{
				this.targetMenu =
					new MenuEntry("", "", bankTarget.getId(), this.bank.getBankMenuOpcode(bankTarget.getId()), bankTarget.getSceneMinLocation().getX(), bankTarget.getSceneMinLocation().getY(), false);
				this.menu.setEntry(this.targetMenu);
				this.mouse.delayMouseClick(bankTarget.getConvexHull().getBounds(), this.sleepDelay());
			}
		}

	}

	private void withdrawEss()
	{
		if (this.bank.contains(24704, 27))
		{
			this.bank.withdrawAllItem(24704);
		}
		else
		{
			this.bank.withdrawAllItem(7936);
		}

		this.timeout = 0 + this.tickDelay();
	}

	public leaguesZMIState getState()
	{
		if (this.timeout > 0)
		{
			this.playerUtils.handleRun(20, 30);
			return leaguesZMIState.TIMEOUT;
		}
		else if (this.player.getPoseAnimation() != 819 && this.player.getPoseAnimation() != 824 && this.player.getPoseAnimation() != 1205 && this.player.getPoseAnimation() != 1210)
		{
			if (!this.bank.isOpen())
			{
				if (!this.inventory.isFull() && this.client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.CRAFTING_GUILD.getRegionID() ||
					!this.inventory.isFull() && this.client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.VER_SINHAZA.getRegionID())
				{
					return leaguesZMIState.OPEN_BANK;
				}

				if (this.inventory.isFull() && this.client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.CRAFTING_GUILD.getRegionID() ||
					this.inventory.isFull() && this.client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.VER_SINHAZA.getRegionID())
				{
					return leaguesZMIState.TELEPORT_CRYSTAL;
				}

				if (!this.inventory.isFull() && this.client.getLocalPlayer().getWorldLocation().getRegionID() == 12119)
				{
					return leaguesZMIState.TELEPORT_BANK;
				}

				if (this.inventory.isFull() && this.client.getLocalPlayer().getWorldLocation().getRegionID() == 12119)
				{
					return leaguesZMIState.CRAFT;
				}
			}

			if (this.bank.isOpen())
			{
				if (!this.bank.contains(7936, 27))
				{
					this.utils.sendGameMessage("get more pure essence");
					return leaguesZMIState.OUT_OF_ESSENCE;
				}

				if (this.inventory.containsItem(RUNES))
				{
					return leaguesZMIState.DEPOSIT_ALL;
				}

				if (!this.inventory.isFull() && !this.inventory.containsItem(RUNES))
				{
					return leaguesZMIState.WITHDRAW_ESSENCE;
				}

				if (this.inventory.isFull())
				{
					return leaguesZMIState.CLOSE_BANK;
				}

				if (this.chinBreakHandler.shouldBreak(this))
				{
					return leaguesZMIState.HANDLE_BREAK;
				}
			}

			return leaguesZMIState.IDLING;
		}
		else
		{
			return leaguesZMIState.MOVING;
		}
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (this.startZMI && !this.chinBreakHandler.isBreakActive(this))
		{
			this.player = this.client.getLocalPlayer();
			if (this.client != null && this.player != null && this.client.getGameState() == GameState.LOGGED_IN)
			{
				if (!this.client.isResized())
				{
					this.utils.sendGameMessage("client must be set to resizable");
					this.startZMI = false;
					return;
				}

				this.playerUtils.handleRun(40, 20);
				this.state = this.getState();
				switch (this.state)
				{
					case TIMEOUT:
						--this.timeout;
					case ITERATING:
					default:
						break;
					case IDLING:
						this.timeout = 1;
						break;
					case MOVING:
						this.timeout = 1;
						break;
					case OPEN_BANK:
						this.openBank();
						this.timeout = this.tickDelay();
						break;
					case CLOSE_BANK:
						this.bank.close();
						this.timeout = this.tickDelay();
						break;
					case CRAFT:
						this.craft();
						this.timeout = this.tickDelay();
						break;
					case TELEPORT_CRYSTAL:
						this.teleportCrystal();
						this.timeout = 3 + this.tickDelay();
						break;
					case TELEPORT_BANK:
						this.teleportBank();
						this.timeout = 3 + this.tickDelay();
						break;
					case DEPOSIT_ALL:
						this.bank.depositAll();
						this.timeout = this.tickDelay();
						break;
					case WITHDRAW_ESSENCE:
						this.withdrawEss();
						break;
					case OUT_OF_ESSENCE:
						if (this.config.logout())
						{
							this.interfaceUtils.logout();
						}

						this.startZMI = false;
						this.resetVals();
						break;
					case HANDLE_BREAK:
						this.chinBreakHandler.startBreak(this);
						this.timeout = 8;
				}
			}

		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (this.startZMI)
		{
			if (event.getGameState() == GameState.LOGGED_IN)
			{
				this.state = leaguesZMIState.IDLING;
				this.timeout = 2;
			}

		}
	}
}
