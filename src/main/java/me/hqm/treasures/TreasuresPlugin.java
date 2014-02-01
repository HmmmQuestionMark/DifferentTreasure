package me.hqm.treasures;

import org.bukkit.plugin.java.JavaPlugin;

public class TreasuresPlugin extends JavaPlugin
{
	@Override
	public void onEnable()
	{
		// Load Treasures
		Treasures.load();

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
