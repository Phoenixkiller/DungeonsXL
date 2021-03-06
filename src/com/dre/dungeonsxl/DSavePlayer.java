package com.dre.dungeonsxl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DSavePlayer {
	private static P p = P.p;
	
	private static CopyOnWriteArrayList<DSavePlayer> savePlayers=new CopyOnWriteArrayList<DSavePlayer>();
	
	//Variables
	private String playerName;
	
	private Location oldLocation;
	private ItemStack[] oldInventory;
	private ItemStack[] oldArmor;
	private int oldLvl;
	private int oldExp;
	private int oldHealth;
	private int oldFoodLevel;
	private int oldFireTicks;
	private GameMode oldGamemode;
	
	public DSavePlayer(String playerName, Location oldLocation, ItemStack[] oldInventory, ItemStack[] oldArmor, int oldLvl, int oldExp, int oldHealth, int oldFoodLevel, int oldFireTicks, GameMode oldGamemode){
		savePlayers.add(this);
		
		this.playerName = playerName;
		
		this.oldLocation = oldLocation;
		this.oldInventory = oldInventory;
		this.oldArmor = oldArmor;
		this.oldExp = oldExp;
		this.oldHealth = oldHealth;
		this.oldFoodLevel = oldFoodLevel;
		this.oldGamemode = oldGamemode;
		this.oldLvl = oldLvl;
		this.oldFireTicks = oldFireTicks;
		
		save();
	}
	
	public void reset(){
		Player onlinePlayer = p.getServer().getPlayer(this.playerName);
		if(onlinePlayer!=null){
			/* Player is online */
			onlinePlayer.teleport(this.oldLocation);
			onlinePlayer.getInventory().setContents(this.oldInventory);
			onlinePlayer.getInventory().setArmorContents(this.oldArmor);
			onlinePlayer.setTotalExperience(this.oldExp);
			onlinePlayer.setLevel(this.oldLvl);
			onlinePlayer.setHealth(this.oldHealth);
			onlinePlayer.setFoodLevel(this.oldFoodLevel);
			onlinePlayer.setGameMode(this.oldGamemode);
			onlinePlayer.setFireTicks(this.oldFireTicks);
		} else {
			/* Player is offline */
			Player offlinePlayer = p.getOfflinePlayer(this.playerName, this.oldLocation);
			if(offlinePlayer!=null){
				offlinePlayer.getInventory().setContents(this.oldInventory);
				offlinePlayer.getInventory().setArmorContents(this.oldArmor);
				offlinePlayer.setTotalExperience(this.oldExp);
				offlinePlayer.setLevel(this.oldLvl);
				offlinePlayer.setHealth(this.oldHealth);
				offlinePlayer.setFoodLevel(this.oldFoodLevel);
				offlinePlayer.setGameMode(this.oldGamemode);
				offlinePlayer.setFireTicks(this.oldFireTicks);
				
				offlinePlayer.saveData();
			}
		}
		savePlayers.remove(this);
		save();
	}
	
	//Static
	public static void save(){
		FileConfiguration configFile = new YamlConfiguration();

		for(DSavePlayer savePlayer: savePlayers){
			configFile.set(savePlayer.playerName+".oldGamemode", savePlayer.oldGamemode.getValue());
			configFile.set(savePlayer.playerName+".oldFireTicks", savePlayer.oldFireTicks);
			configFile.set(savePlayer.playerName+".oldFoodLevel", savePlayer.oldFoodLevel);
			configFile.set(savePlayer.playerName+".oldHealth", savePlayer.oldHealth);
			configFile.set(savePlayer.playerName+".oldExp", savePlayer.oldExp);
			configFile.set(savePlayer.playerName+".oldLvl", savePlayer.oldLvl);
			configFile.set(savePlayer.playerName+".oldArmor", savePlayer.oldArmor);
			configFile.set(savePlayer.playerName+".oldInventory", savePlayer.oldInventory);
			configFile.set(savePlayer.playerName+".oldLocation.x", savePlayer.oldLocation.getX());
			configFile.set(savePlayer.playerName+".oldLocation.y", savePlayer.oldLocation.getY());
			configFile.set(savePlayer.playerName+".oldLocation.z", savePlayer.oldLocation.getZ());
			configFile.set(savePlayer.playerName+".oldLocation.yaw", savePlayer.oldLocation.getYaw());
			configFile.set(savePlayer.playerName+".oldLocation.pitch", savePlayer.oldLocation.getPitch());
			configFile.set(savePlayer.playerName+".oldLocation.world", savePlayer.oldLocation.getWorld().getName());
		}

		try {
			configFile.save(new File(p.getDataFolder(), "savePlayers.yml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void load(){
		FileConfiguration configFile = YamlConfiguration.loadConfiguration(new File(p.getDataFolder(), "savePlayers.yml"));
		
		for(String playerName : configFile.getKeys(false)){
			
			//Load inventory data
			ArrayList<ItemStack> oldInventoryList = (ArrayList<ItemStack>) configFile.get(playerName+".oldInventory");
			ArrayList<ItemStack> oldArmorList = (ArrayList<ItemStack>) configFile.get(playerName+".oldArmor");
			
			ItemStack[] oldInventory = oldInventoryList.toArray(new ItemStack[oldInventoryList.size()]);
			ItemStack[] oldArmor = oldArmorList.toArray(new ItemStack[oldArmorList.size()]);
			
			//Load other data
			int oldLvl = configFile.getInt(playerName+".oldLvl");
			int oldExp = configFile.getInt(playerName+".oldExp");
			int oldHealth = configFile.getInt(playerName+".oldHealth");
			int oldFoodLevel = configFile.getInt(playerName+".oldFoodLevel");
			int oldFireTicks = configFile.getInt(playerName+".oldFireTicks");
			GameMode oldGamemode = GameMode.getByValue(configFile.getInt(playerName+".oldGamemode"));
			
			//Location
			World world = p.getServer().getWorld(configFile.getString(playerName+".oldLocation.world"));
			if(world==null){
				world = p.getServer().getWorlds().get(0);
			}
			
			Location oldLocation = new Location(
					world, 
					configFile.getDouble(playerName+".oldLocation.x"), 
					configFile.getDouble(playerName+".oldLocation.y"), 
					configFile.getDouble(playerName+".oldLocation.z"), 
					configFile.getInt(playerName+".oldLocation.yaw"), 
					configFile.getInt(playerName+".oldLocation.pitch"));
			
			//Create Player
			DSavePlayer savePlayer = new DSavePlayer(playerName, oldLocation, oldInventory, oldArmor, oldLvl, oldExp, oldHealth, oldFoodLevel, oldFireTicks, oldGamemode);
			savePlayer.reset();
		}
	}

}
