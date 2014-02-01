package me.hqm.different.treasure;

import org.bukkit.plugin.java.JavaPlugin;

public class DifferentTreasurePlugin extends JavaPlugin
{
	@Override
	public void onEnable()
	{
		// Load Treasures
		DifferentTreasure.load();

		// Print success!
		getLogger().info("Successfully enabled.");
	}

	@Override
	public void onDisable()
	{
		// Print success!
		getLogger().info("Successfully disabled.");
	}
}
