package net.runelite.client.plugins.prayerPOH;

import net.runelite.api.ItemID;

public enum Bones
{
	BIG_("Big", ItemID.BIG_BONES),
	BABY_DRAGON("Baby Drag", ItemID.BABYDRAGON_BONES),
	DRAGON("Dragon", ItemID.DRAGON_BONES),
	WYRM("Wyrm", ItemID.WYRM_BONES),
	DRAKE("Drake", ItemID.DRAKE_BONES),
	WYVERN("Wyvern", ItemID.WYVERN_BONES),
	LAVA_DRAGON("Lava Drag", ItemID.LAVA_DRAGON_BONES),
	HYDRA("Hydra", ItemID.HYDRA_BONES),
	DAGANNOTH("Dagannoth", ItemID.DAGANNOTH_BONES),
	SUPERIOR_DRAGON("Superior", ItemID.SUPERIOR_DRAGON_BONES);

	private final String name;
	private final Integer itemID;


	Bones(String name, Integer itemID)
	{
		this.name = name;
		this.itemID = itemID;
	}

	public String getName()
	{
		return this.name;
	}

	public Integer getItemID()
	{
		return itemID;
	}
}
