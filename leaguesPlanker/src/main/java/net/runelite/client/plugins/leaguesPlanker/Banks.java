package net.runelite.client.plugins.leaguesPlanker;

import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldPoint;

public enum Banks
{
	VER_SINHAZA("Ver Sinhaza", 14642, 32666, new WorldPoint(3652, 3207, 0)),
	CRAFTING_GUILD("Crafting Guild", 11571, ObjectID.BANK_CHEST_14886),
	SEERS("Seers", 10806, ObjectID.BANK_BOOTH_25808),
	CASTLE_WARS("Castle Wars", 9776, ObjectID.BANK_CHEST_4483);

	private final String name;
	private final Integer regionID;
	private final Integer bankObjID;
	private WorldPoint bankLoc;

	Banks(String name, Integer regionID, Integer bankObjID, WorldPoint bankLoc)
	{
		this.name = name;
		this.regionID = regionID;
		this.bankObjID = bankObjID;
		this.bankLoc = bankLoc;
	}

	Banks(String name, Integer regionID, Integer bankObjID)
	{
		this.name = name;
		this.regionID = regionID;
		this.bankObjID = bankObjID;
	}

	public String getName()
	{
		return this.name;
	}

	public Integer getRegionID()
	{
		return this.regionID;
	}

	public Integer getBankObjID()
	{
		return bankObjID;
	}

	public WorldPoint getBankLoc()
	{
		return bankLoc;
	}
}
