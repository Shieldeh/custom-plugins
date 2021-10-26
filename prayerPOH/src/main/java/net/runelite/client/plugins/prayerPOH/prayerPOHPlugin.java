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
package net.runelite.client.plugins.prayerPOH;

import com.google.inject.Provides;
import com.owain.chinbreakhandler.ChinBreakHandler;
import java.awt.Rectangle;
import java.time.Instant;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
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
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
	name = "Prayer - POH",
	enabledByDefault = false,
	description = "Shieldeh - Prayer plugin. Handles picking a public POH, unnoting all bones, and efficiently using them on the altar.",
	tags = {"Prayer", "skilling", "boat"}
)
public class prayerPOHPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(prayerPOHPlugin.class);
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
	private WalkUtils walk;
	@Inject
	private prayerPOHConfig config;
	@Inject
	PluginManager pluginManager;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	prayerPOHOverlay overlay;
	@Inject
	private ChinBreakHandler chinBreakHandler;

	prayerPOHState state;
	Instant botTimer;
	MenuEntry targetMenu;
	Player player;
	LocalPoint beforeLoc = new LocalPoint(0, 0);

	int i;
	int timeout = 0;
	int notedBonesID;
	long sleepLength = 0;
	boolean startPlugin;
	public static final String FAILED_ENTRY = "You haven't visited anyone this session.";
	public static final String HOUSE_UNAVAILABLE = "That person's house is no longer accessible.";
	public static final String HOST_LOGGED = "The house owner has logged out.";
	public static final int rimmingtonRegionID = 11826;
	public static final int buggedRegionID = 11570;
	public static Set<Integer> ALTARS = Set.of(ObjectID.ALTAR_13197, ObjectID.ALTAR_13199, 40878);
	public static Set<Integer> BONES = Set.of(
		ItemID.BIG_BONES, ItemID.BABY_DRAGON_BONE, ItemID.DRAGON_BONES,
		ItemID.WYRM_BONES, ItemID.DRAKE_BONES, ItemID.WYVERN_BONES,
		ItemID.LAVA_DRAGON_BONES, ItemID.HYDRA_BONES, ItemID.DAGANNOTH_BONES,
		ItemID.SUPERIOR_DRAGON_BONES);

	public prayerPOHPlugin()
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
		startPlugin = false;
		botTimer = null;
		timeout = 0;
		targetMenu = null;
	}

	@Provides
	prayerPOHConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(prayerPOHConfig.class);
	}

	@Subscribe
	private void onChatMessage(ChatMessage event)
	{
		//log.info("event: {}, {}", event.getMessage(), event.getType());
		if (!startPlugin)
		{
			return;
		}
		final String msg = event.getMessage();

		if (event.getType() == ChatMessageType.GAMEMESSAGE && (msg.contains(FAILED_ENTRY)))
		{
			if (config.logout())
			{
				utils.sendGameMessage("Host has not been set. Plugin will now stop.");
				interfaceUtils.logout();
				startPlugin = false;
			}
			utils.sendGameMessage("Host has not been set. Plugin will now stop.");
			startPlugin = false;
		}
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (configButtonClicked.getGroup().equalsIgnoreCase("prayerPOH"))
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
							if (!startPlugin)
							{
								startPlugin = true;
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
		if (event.getGroup() == "prayerPOH")
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
		//log.debug("tick delay for {} ticks", tickLength);
		return tickLength;
	}

	private void unnoteBones()
	{
		if (config.bones() == Bones.WYVERN)
		{
			notedBonesID = (config.bones().getItemID() + 4);
		}
		notedBonesID = (config.bones().getItemID() + 1);
		WidgetItem notedBones = inventory.getWidgetItem(notedBonesID);
		NPC phials = npc.findNearestNpc(NpcID.PHIALS);

		int unnoteCost;
		int boneStackQty = inventory.getItemCount(notedBonesID, true);

		if (boneStackQty < 26)
		{
			unnoteCost = boneStackQty * 5;
		}
		else
		{
			unnoteCost = 130;
		}
		if (client.getWidget(WidgetInfo.DIALOG_OPTION_OPTION1) != null)
		{
			Widget parentUnnoteWidget = client.getWidget(WidgetInfo.DIALOG_OPTION_OPTION1);
			if (parentUnnoteWidget != null)
			{
				Widget unnoteWidget = parentUnnoteWidget.getChild(3);
				if (unnoteWidget != null && !unnoteWidget.isHidden() && unnoteWidget.getText().equals("Exchange All: " + unnoteCost + " coins"))
				{
					Rectangle unnoteButtonBounds = unnoteWidget.getBounds();
					if (unnoteButtonBounds != null)
					{
						targetMenu = new MenuEntry("", "", 0, 30, 3, 14352385, false);
						utils.doActionGameTick(targetMenu, unnoteButtonBounds, tickDelay());
					}
					timeout = tickDelay();
				}
			}
			else {
				log.info("Parent widget is null");
			}
		}
		else if (phials != null && phials.getConvexHull() != null && notedBones != null && inventory.containsStackAmount(ItemID.COINS_995, 130) && inventory.containsStackAmount(notedBonesID, 1))
		{
			targetMenu = new MenuEntry("", "", phials.getIndex(), MenuAction.ITEM_USE_ON_NPC.getId(), 0, 0, false);
			utils.doModifiedActionGameTick(targetMenu, notedBones.getId(), notedBones.getIndex(), MenuAction.ITEM_USE_ON_NPC.getId(), phials.getConvexHull().getBounds(), tickDelay());
			//timeout = tickDelay();
		}
		else
		{
			if (config.logout())
			{
				utils.sendGameMessage("Phials, noted bones, and/or cash not found. Plugin will now stop.");
				log.info("Phials, noted bones, and/or cash not found. Plugin will now stop.");
				interfaceUtils.logout();
				startPlugin = false;
			}
			utils.sendGameMessage("Phials, noted bones, and/or cash not found. Plugin will now stop.");
			log.info("Phials, noted bones, and/or cash not found. Plugin will now stop.");
			startPlugin = false;
		}
	}

	private void exitPOH()
	{
		GameObject houseExitPortal = object.findNearestGameObject(ObjectID.PORTAL_4525);
		if (houseExitPortal != null)
		{
			targetMenu = new MenuEntry("", "", houseExitPortal.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION.getId(),
				houseExitPortal.getSceneMinLocation().getX(), houseExitPortal.getSceneMinLocation().getY(), false);
			utils.doGameObjectActionMsTime(houseExitPortal, MenuAction.GAME_OBJECT_FIRST_OPTION.getId(), sleepDelay());
			timeout = tickDelay();
		}
	}


	private void enterPOH()
	{
		GameObject houseAdvert = object.findNearestGameObject(ObjectID.HOUSE_ADVERTISEMENT);

		if (houseAdvert != null && client.getVarcStrValue(361) != null && houseAdvert.getConvexHull() != null)
		{
			//targetMenu = new MenuEntry("", "", houseAdvert.getId(), MenuAction.GAME_OBJECT_THIRD_OPTION.getId(),houseAdvert.getSceneMinLocation().getX(), houseAdvert.getSceneMinLocation().getY(), false);
			utils.doGameObjectActionMsTime(houseAdvert, MenuAction.GAME_OBJECT_THIRD_OPTION.getId(), sleepDelay());
			//mouse.delayMouseClick(houseAdvert.getConvexHull().getBounds(), sleepDelay());
			timeout = tickDelay();
		}
		else
		{
			utils.sendGameMessage("Host has not been set. Plugin will now stop.");
			log.info("Host has not been set. Plugin will now stop.");
			startPlugin = false;
		}
	}

	private void buryBones()
	{
		GameObject altar = object.findNearestGameObjectWithin(player.getWorldLocation(), 20, ALTARS);
		WidgetItem bone = inventory.getWidgetItem(config.bones().getItemID());
		GameObject burners = object.findNearestGameObject(ObjectID.INCENSE_BURNER_13213);
		GameObject unlitBurners = object.findNearestGameObject(ObjectID.INCENSE_BURNER_13212);

		if (altar != null)
		{
			if (bone != null && burners != null && unlitBurners == null)
			{
				targetMenu = new MenuEntry("", "", altar.getId(), MenuAction.ITEM_USE_ON_GAME_OBJECT.getId(),
					altar.getSceneMinLocation().getX(), altar.getSceneMinLocation().getY(), false);
				utils.doModifiedActionGameTick(targetMenu, bone.getId(), bone.getIndex(), MenuAction.ITEM_USE_ON_GAME_OBJECT.getId(), altar.getConvexHull().getBounds(), 0);
			}
			else
			{
				utils.sendGameMessage("Burners are not lit. Plugin will now stop.");
				log.info("Burners are not lit. Plugin will now stop.");
				startPlugin = false;
			}
		}
		else
		{
			timeout = tickDelay();
		}
	}

	private void setHost()
	{
		// If "Only houses from this portal" is unselected
		if (client.getWidget(52, 28).getSpriteId() == 1215)
		{
			// Click on the button to enable "Only houses from this portal"
			targetMenu = new MenuEntry("", "", 0, MenuAction.CANCEL.getId(), 0, 0, false);
			utils.doActionMsTime(targetMenu, client.getWidget(52, 28).getBounds(), sleepDelay());
		}
		// else if the list of house gilded altars isn't hidden
		else if (!client.getWidget(52, 13).isHidden())
		{
			Widget[] gildedList = client.getWidget(52, 13).getChildren();
			if (gildedList != null)
			{
				for (Widget house : gildedList)
				{
					//utils.sendGameMessage("index is:" + house.getIndex());
					if (client.getWidget(52, 13).getChild(i).getText() == "Y")
					{
						i++;
						targetMenu = new MenuEntry("Enter House", "", 1, MenuAction.CC_OP.getId(), house.getIndex(), 3407891, false);
					}
				}
			}
		}
		// else stop the plugin and let the user know.
		else
		{
			utils.sendGameMessage("Setting host failed, plugin will now stop.");
			startPlugin = false;
		}
	}

	public prayerPOHState getState()
	{
		if (chinBreakHandler.shouldBreak(this))
		{
			return prayerPOHState.HANDLE_BREAK;
		}
		if (timeout > 0)
		{
			playerUtils.handleRun(20, 30);
			return prayerPOHState.TIMEOUT;
		}
		// if not one of the four moving animations below
		else if (player.getPoseAnimation() != 819 && player.getPoseAnimation() != 824 && player.getPoseAnimation() != 1205 && player.getPoseAnimation() != 1210)
		{
			if (client.getLocalPlayer().getWorldLocation().getRegionID() == rimmingtonRegionID)
			{
				if (client.getWidget(229, 1) != null && (client.getWidget(229, 1).getText().equals(HOUSE_UNAVAILABLE) || client.getWidget(229, 1).getText().equals(HOST_LOGGED)))
				{
					utils.sendGameMessage("Host house is unavailable. Plugin will now stop.");
					if (config.logout())
					{
						interfaceUtils.logout();
					}
					startPlugin = false;
				}
				else if (inventory.containsItem(config.bones().getItemID()))
				{
					return prayerPOHState.ENTER_POH;
				}
				// if not in the dialogue with phials
				else
				{
					return prayerPOHState.UNNOTE_BONES;
				}
			}
			if (client.isInInstancedRegion())
			{
				if (!inventory.containsItem(config.bones().getItemID()))
				{
					return prayerPOHState.EXIT_POH;
				}
				else if (object.findNearestGameObjectWithin(player.getWorldLocation(), 20, ALTARS) != null)
				{
					return prayerPOHState.BURY;
				}
			}
			if (client.getLocalPlayer().getWorldLocation().getRegionID() == buggedRegionID)
			{
				utils.sendGameMessage("You have somehow left the POH region. Plugin will now stop.");
				if (config.logout())
				{
					interfaceUtils.logout();
				}
				startPlugin = false;
			}
			return prayerPOHState.IDLING;
		}
		else
		{
			return prayerPOHState.MOVING;
		}
	}


	/*@Subscribe
	private void onChatMessageReceived(ChatMessage message)
	{
		utils.sendGameMessage("message type: " + message.getType() + " and message text: " + message.getMessage());
	}*/

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (startPlugin && !chinBreakHandler.isBreakActive(this))
		{
			player = client.getLocalPlayer();
			if (client != null && player != null && client.getGameState() == GameState.LOGGED_IN)
			{
				if (!client.isResized())
				{
					utils.sendGameMessage("client must be set to resizable");
					startPlugin = false;
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
					case BURY:
						buryBones();
						break;
					case SET_HOST:
						setHost();
						break;
					case EXIT_POH:
						exitPOH();
						break;
					case ENTER_POH:
						enterPOH();
						break;
					case ANIMATING:
						break;
					case UNNOTE_BONES:
						unnoteBones();
						break;
					case OUT_OF_BONES:
						if (config.logout())
						{
							interfaceUtils.logout();
						}

						startPlugin = false;
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
		if (startPlugin)
		{
			if (event.getGameState() == GameState.LOGGED_IN)
			{
				state = prayerPOHState.IDLING;
				//timeout = 1;
			}

		}
	}
}
