package me.hqm.treasures;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sk89q.worldguard.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Treasures
{
	// Define variables
	public static final Random RAND = new Random();
	public static final TreasuresPlugin PLUGIN = (TreasuresPlugin) Bukkit.getServer().getPluginManager().getPlugin("Treasures");

	protected static void load()
	{
		// Load the config.
		loadConfig();

		// Load commands
		loadCommands();

		// Start teasing the treasure
		startTeasing();
	}

	private static void loadConfig()
	{
		Configuration config = PLUGIN.getConfig();
		config.options().copyDefaults(true);
		PLUGIN.saveConfig();
	}

	private static void loadCommands()
	{
		Commands executor = new Commands();
		PLUGIN.getCommand("treasures").setExecutor(executor);
	}

	private static void startTeasing()
	{
		Bukkit.getScheduler().scheduleSyncRepeatingTask(PLUGIN, new Runnable()
		{
			@Override
			public void run()
			{
				if(Bukkit.getOnlinePlayers().length > 0) createChest();
			}
		}, 6, TimeUnit.MINUTES.toSeconds(PLUGIN.getConfig().getInt("minutes")) * 20);
	}

	private static void createChest()
	{
		if(!PLUGIN.getConfig().contains("regions")) return;

		final ConfigurationSection regions = PLUGIN.getConfig().getConfigurationSection("regions");

		List<World> enabledWorlds = Lists.newArrayList(Collections2.filter(Bukkit.getWorlds(), new Predicate<World>()
		{
			@Override
			public boolean apply(World world)
			{
				List<String> worldRegions = regions.getStringList(world.getName());
				return worldRegions != null && !worldRegions.isEmpty();
			}
		}));

		if(enabledWorlds.isEmpty()) return;

		World world = enabledWorlds.get(RAND.nextInt(enabledWorlds.size()));

		List<String> worldRegions = regions.getStringList(world.getName());

		String region = worldRegions.get(RAND.nextInt(worldRegions.size()));

		if(!WorldGuardPlugin.inst().getGlobalRegionManager().get(world).hasRegion(region)) return;

		ProtectedRegion protectedRegion = WorldGuardPlugin.inst().getGlobalRegionManager().get(world).getRegion(region);

		Location location = getRandomLocationInRegion(world, protectedRegion);

		if(!location.getChunk().isLoaded() || !WorldGuardPlugin.inst().getRegionManager(world).getApplicableRegions(location).allows(DefaultFlag.ENDER_BUILD)) return;

		location.getBlock().setType(Material.CHEST);
		Chest chest = (Chest) location.getBlock().getState();
		setItemsForChest(chest);

		String message = ChatColor.translateAlternateColorCodes('&', PLUGIN.getConfig().getString("broadcast.message"));

		if(message.contains("%{world}")) message = StringUtils.replace(message, "%{world}", world.getName());
		if(message.contains("%{coordinates}")) message = StringUtils.replace(message, "%{coordinates}", "X: " + location.getBlockX() + ", Y: " + location.getBlockY() + ", Z: " + location.getBlockZ());

		Bukkit.broadcastMessage(message);
	}

	private static Location getRandomLocationInRegion(World world, ProtectedRegion region)
	{
		Location max = BukkitUtil.toLocation(world, region.getMaximumPoint());
		Location min = BukkitUtil.toLocation(world, region.getMinimumPoint());

		int x = RAND.nextInt(max.getBlockX() - min.getBlockX() + 1) + min.getBlockX();
		int z = RAND.nextInt(max.getBlockZ() - min.getBlockZ() + 1) + min.getBlockZ();

		return new Location(world, x, world.getHighestBlockYAt(x, z), z);
	}

	private static final Map<Integer, ItemStack> DEFAULT_ITEMS = Maps.newHashMap();

	static
	{
		ItemStack goldBars = new ItemStack(Material.GOLD_INGOT, 3);
		ItemStack ironSword = new ItemStack(Material.IRON_SWORD);
		ItemStack pork = new ItemStack(Material.PORK, 2);

		ItemStack specialArmor = new ItemStack(Material.GOLD_CHESTPLATE);
		ItemMeta armorMeta = specialArmor.getItemMeta();
		armorMeta.setDisplayName(ChatColor.RED + "Fire Armor");
		armorMeta.setLore(Lists.newArrayList(ChatColor.RED + "Forged with lava from the Nether.", ChatColor.DARK_RED + "Do not fear the flames."));
		specialArmor.setItemMeta(armorMeta);
		specialArmor.addEnchantment(Enchantment.PROTECTION_FIRE, 2);

		DEFAULT_ITEMS.put(4, goldBars);
		DEFAULT_ITEMS.put(11, specialArmor);
		DEFAULT_ITEMS.put(19, ironSword);
		DEFAULT_ITEMS.put(23, pork);

		if(!PLUGIN.getConfig().contains("item_sets.default")) PLUGIN.getConfig().set("item_sets.default", (new Function<Map<Integer, ItemStack>, Map<String, Object>>()
		{
			@Override
			public Map<String, Object> apply(Map<Integer, ItemStack> items)
			{
				Map<String, Object> map = Maps.newHashMap();
				for(Map.Entry<Integer, ItemStack> entry : items.entrySet())
				{
					map.put(String.valueOf(entry.getKey()), entry.getValue().serialize());
				}
				return map;
			}
		}).apply(DEFAULT_ITEMS));
	}

	private static Map<Integer, ItemStack> getRandomSetOfItems(String worldName)
	{
		Map<Integer, ItemStack> map = Maps.newHashMap();
		if(PLUGIN.getConfig().isList("items." + worldName))
		{
			List<String> sets = PLUGIN.getConfig().getStringList("items." + worldName);

			String set = sets.get(RAND.nextInt(sets.size()));

			ConfigurationSection section = PLUGIN.getConfig().getConfigurationSection("item_sets." + set);

			if(section != null)
			{
				for(Map.Entry<String, Object> entry : section.getValues(true).entrySet())
				{
					try
					{
						map.put(Integer.parseInt(entry.getKey()), ItemStack.deserialize(section.getConfigurationSection(entry.getKey()).getValues(true)));
					}
					catch(Exception ignored)
					{}
				}
			}
		}
		if(map.isEmpty()) map = DEFAULT_ITEMS;
		return map;
	}

	private static void setItemsForChest(Chest chest)
	{
		chest.update();
		for(Map.Entry<Integer, ItemStack> entry : getRandomSetOfItems(chest.getLocation().getWorld().getName()).entrySet())
		{
			chest.getBlockInventory().setItem(entry.getKey(), entry.getValue());
		}
		chest.update();
	}

	private static class Commands implements CommandExecutor
	{
		@Override
		public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
		{
			if("treasures".equals(command.getName()))
			{
				if(args.length > 0 && "reload".equalsIgnoreCase(args[0]))
				{
					if(sender.hasPermission("treasure.reload"))
					{
						PLUGIN.reloadConfig();
						Bukkit.getScheduler().cancelTasks(PLUGIN);
						startTeasing();
						sender.sendMessage(ChatColor.YELLOW + "The plugin has finished reloading.");
					}
					else
					{
						sender.sendMessage(ChatColor.RED + "You can't use that command.");
					}
					return true;
				}

				if(!sender.hasPermission("treasures.info"))
				{
					sender.sendMessage(ChatColor.RED + "You can't use that command.");
					return true;
				}

				sender.sendMessage("Treasures v" + PLUGIN.getDescription().getVersion() + ".");
			}

			return true;
		}
	}
}
