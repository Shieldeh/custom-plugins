package net.runelite.client.plugins.leaguesPlanker;

import net.runelite.api.ItemID;

public enum Logs
{
	NORMAL("Normal", ItemID.PLANK,ItemID.LOGS, 14, 100),
	OAK("Oak", ItemID.OAK_PLANK,ItemID.OAK_LOGS, 15, 250),
	TEAK("Teak", ItemID.TEAK_PLANK,ItemID.TEAK_LOGS, 16, 500),
	MAHOGANY("Mahogany", ItemID.MAHOGANY_PLANK,ItemID.MAHOGANY_LOGS, 17, 1500);

	private final String name;
	private final Integer itemIDPlanks;
	private final Integer itemIDLogs;
	private final Integer itemChildID;
	private final Integer itemCost;

	Logs(String name, Integer itemIDPlanks, Integer itemIDLogs, Integer itemChildID, Integer itemCost)
	{
		this.name = name;
		this.itemIDPlanks = itemIDPlanks;
		this.itemIDLogs = itemIDLogs;
		this.itemChildID = itemChildID;
		this.itemCost = itemCost;
	}

	public String getName()
	{
		return this.name;
	}

	public Integer getItemIDPlanks()
	{
		return this.itemIDPlanks;
	}

	public Integer getItemIDLogs()
	{
		return this.itemIDLogs;
	}

	public Integer getItemChildID()
	{
		return itemChildID;
	}

	public Integer getItemCost()
	{
		return itemCost;
	}
}
