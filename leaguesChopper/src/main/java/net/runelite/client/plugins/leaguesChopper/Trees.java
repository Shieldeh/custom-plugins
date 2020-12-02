package net.runelite.client.plugins.leaguesChopper;

public enum Trees
{
	TEAK("Teak", 9036),
	MAHOGANY("Mahogany", 9034);

	private final String name;
	private final Integer treeObjID;

	Trees(String name, Integer treeObjID)
	{
		this.name = name;
		this.treeObjID = treeObjID;
	}

	public String getName()
	{
		return this.name;
	}


	public Integer gettreeObjID()
	{
		return this.treeObjID;
	}
}
