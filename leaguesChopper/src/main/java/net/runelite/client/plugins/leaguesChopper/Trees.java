package net.runelite.client.plugins.leaguesChopper;

import net.runelite.api.ItemID;

public enum Trees
{
	TEAK("Teak", ItemID.TEAK_LOGS, 9036),
	MAHOGANY("Mahogany", ItemID.MAHOGANY_LOGS, 9034);

	private final String name;
	private final Integer itemID;
	private final Integer treeObjID;

	Trees(String name, Integer itemID, Integer treeObjID)
	{
		this.name = name;
		this.itemID = itemID;
		this.treeObjID = treeObjID;
	}

	public String getName()
	{
		return this.name;
	}

	public Integer getItemID()
	{
		return this.itemID;
	}

	public Integer gettreeObjID()
	{
		return this.treeObjID;
	}
}
