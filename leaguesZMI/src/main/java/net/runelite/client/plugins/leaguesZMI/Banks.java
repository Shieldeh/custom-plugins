package net.runelite.client.plugins.leaguesZMI;

public enum Banks
{
	VER_SINHAZA("Ver Sinhaza", 14642),
	CRAFTING_GUILD("Crafting Guild", 11571);

	private final String name;
	private final Integer regionID;

	Banks(String name, Integer regionID)
	{
		this.name = name;
		this.regionID = regionID;
	}

	public String getName()
	{
		return this.name;
	}

	public Integer getRegionID()
	{
		return this.regionID;
	}
}
