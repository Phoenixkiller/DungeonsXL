package com.dre.dungeonsxl;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.getspout.spoutapi.Spout;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.dre.dungeonsxl.game.GameCheckpoint;
import com.dre.dungeonsxl.game.GameWorld;

public class DPlayer {
	public static P p=P.p;

	public static CopyOnWriteArrayList<DPlayer> players=new CopyOnWriteArrayList<DPlayer>();

	//Variables
	public Player player;
	public World world;

	public boolean isinTestMode=false;
	
	public DSavePlayer savePlayer;

	public boolean isEditing;
	public boolean isInDungeonChat=false;
	public boolean isReady=false;
	public boolean isFinished=false;

	public DClass dclass;
	public GameCheckpoint checkpoint;
	public Wolf wolf;
	public int wolfRespawnTime=30;
	public long offlineTime;
	public CopyOnWriteArrayList<ItemStack> respawnInventory=new CopyOnWriteArrayList<ItemStack>();

	public Inventory treasureInv = P.p.getServer().createInventory(player,  45, "Belohnungen");


	public DPlayer(Player player, World world, Location teleport, boolean isEditing){
		players.add(this);
		
		this.player=player;
		this.world=world;
		
		this.savePlayer = new DSavePlayer(
				player.getName(), 
				player.getLocation(), 
				player.getInventory().getContents(), 
				player.getInventory().getArmorContents(),
				player.getLevel(),
				player.getTotalExperience(),
				player.getHealth(),
				player.getFoodLevel(),
				player.getFireTicks(),
				player.getGameMode()
				);
		
		this.player.teleport(teleport);
		this.player.getInventory().clear();
		this.player.getInventory().setArmorContents(null);
		this.player.setTotalExperience(0);
		this.player.setLevel(0);
		this.player.setHealth(20);
		this.player.setFoodLevel(20);
		this.isEditing = isEditing;
		
		if(isEditing) this.player.setGameMode(GameMode.CREATIVE); else this.player.setGameMode(GameMode.SURVIVAL);

		if(!isEditing){
			if(GameWorld.get(world).config.isLobbyDisabled()){
				this.ready();
			}
		}
		
		//Spout set Texturepack
		if(!isEditing){
			if(p.isSpoutEnabled){
				GameWorld gworld = GameWorld.get(world);
				if(gworld.config.getSpoutTexturepackURL()!=null){
					SpoutPlayer sPlayer = Spout.getServer().getPlayer(player.getName());
					if(sPlayer!=null){
						sPlayer.sendNotification(ChatColor.GOLD+"DungeonsXL",ChatColor.RED+"Download texturepack!", Material.FIREBALL);
						sPlayer.setTexturePack(gworld.config.getSpoutTexturepackURL());
					}
				}
			}
		}
	}

	public void leave(){
		remove(this);

		this.savePlayer.reset();

		if(this.isEditing){
			EditWorld eworld=EditWorld.get(this.world);
			if(eworld!=null){
				eworld.save();
			}
		}else{
			GameWorld gworld=GameWorld.get(this.world);
			DGroup dgroup=DGroup.get(this.player);
			if(dgroup!=null){
				dgroup.removePlayer(this.player);
			}

			//Belohnung
			if(!this.isinTestMode){//Nur wenn man nicht am Testen ist
				if(isFinished){
					this.addTreasure();

					//Set Time
					File file=new File(p.getDataFolder()+"/dungeons/"+gworld.dungeonname, "players.yml");

					if(!file.exists()){
						try {
							file.createNewFile();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(file);

					playerConfig.set(player.getName(), System.currentTimeMillis());

					try {
						playerConfig.save(file);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					//Tutorial Permissions
					if(gworld.isTutorial){
						p.permission.playerAddGroup(this.player, p.mainConfig.tutorialEndGroup);
						p.permission.playerRemoveGroup(this.player, p.mainConfig.tutorialStartGroup);
					}
				}
			}

			//Give Secure Objects other Players
			if(dgroup!=null){
				if(!dgroup.isEmpty()){
					int i=0;
					Player groupplayer;
					do{
						groupplayer=dgroup.getPlayers().get(i);
						if(groupplayer!=null){
							for(ItemStack istack:this.player.getInventory()){
								if(istack!=null){
									if(gworld.secureObjects.contains(istack.getType())){
										groupplayer.getInventory().addItem(istack);
									}
								}
							}
						}
						i++;
					}while(groupplayer==null);
				}
			}
			
			//Spout
			if(p.isSpoutEnabled){
				SpoutPlayer sPlayer = Spout.getServer().getPlayer(player.getName());
				if(sPlayer!=null){
					sPlayer.resetTexturePack();
					sPlayer.resetSkin();
				}
			}
		}
	}

	public void ready(){
		this.isReady=true;

		DGroup dgroup=DGroup.get(this.player);
		if(!dgroup.isPlaying){
			if(dgroup!=null){
				for(Player player:dgroup.getPlayers()){
					DPlayer dplayer=get(player);
					if(!dplayer.isReady){
						return;
					}
				}
			}


			dgroup.startGame();
		}else{
			this.respawn();
		}
	}

	public void respawn(){
		DGroup dgroup=DGroup.get(this.player);
		if(this.checkpoint==null){
			this.player.teleport(dgroup.getGworld().locStart);
		}else{
			this.player.teleport(this.checkpoint.location);
		}
		if(this.wolf!=null){
			this.wolf.teleport(this.player);
		}

		//Respawn Items
		for(ItemStack istack:this.respawnInventory){
			if(istack!=null){
				this.player.getInventory().addItem(istack);
			}
		}
		this.respawnInventory.clear();
		//P.p.updateInventory(this.player);
	}

	public void finish(){
		p.msg(this.player, p.language.get("Player_FinishedDungeon"));
		this.isFinished=true;

		DGroup dgroup=DGroup.get(this.player);
		if(dgroup!=null){
			if(dgroup.isPlaying){
				for(Player player:dgroup.getPlayers()){
					DPlayer dplayer=get(player);
					if(!dplayer.isFinished){
						p.msg(this.player, p.language.get("Player_WaitForOtherPlayers"));
						return;
					}
				}

				for(Player player:dgroup.getPlayers()){
					DPlayer dplayer=get(player);
					dplayer.leave();
				}
			}
		}
	}

	public void msg(String msg){
		if(this.isEditing){
			EditWorld eworld=EditWorld.get(this.world);
			eworld.msg(msg);
			for(Player player:p.chatSpyer){
				if(!eworld.world.getPlayers().contains(player)){
					p.msg(player, ChatColor.GREEN+"[Chatspy] "+ChatColor.WHITE+msg);
				}
			}
		}else{
			GameWorld gworld = GameWorld.get(this.world);
			gworld.msg(msg);
			for(Player player:p.chatSpyer){
				if(!gworld.world.getPlayers().contains(player)){
					p.msg(player, ChatColor.GREEN+"[Chatspy] "+ChatColor.WHITE+msg);
				}
			}
		}
	}

	public void setClass(String classname) {
		GameWorld gworld=GameWorld.get(this.player.getWorld());
		if(gworld==null) return;

		DClass dclass=gworld.config.getClass(classname);
		if(dclass!=null){
			if(this.dclass!=dclass){
				this.dclass=dclass;

				/* Set Dog */
				if(this.wolf!=null){
					this.wolf.remove();
					this.wolf=null;
				}

				if(dclass.hasDog){
					this.wolf=(Wolf) this.world.spawnEntity(this.player.getLocation(), EntityType.WOLF);
					this.wolf.setTamed(true);
					this.wolf.setOwner(this.player);
					this.wolf.setHealth(this.wolf.getMaxHealth());
				}

				/* Delete Inventory */
				this.player.getInventory().clear();
				this.player.getInventory().setArmorContents(null);
				player.getInventory().setItemInHand(new ItemStack(0));
				
				/* Set Inventory */
				for(ItemStack istack:dclass.items){

					//Leggings
					if(istack.getTypeId()==300||
							istack.getTypeId()==304||
							istack.getTypeId()==308||
							istack.getTypeId()==312||
							istack.getTypeId()==316)
					{
						this.player.getInventory().setLeggings(istack);
					}
					//Helmet
					else if(istack.getTypeId()==298||
							istack.getTypeId()==302||
							istack.getTypeId()==306||
							istack.getTypeId()==310||
							istack.getTypeId()==314)
					{
						this.player.getInventory().setHelmet(istack);
					}
					//Chestplate
					else if(istack.getTypeId()==299||
							istack.getTypeId()==303||
							istack.getTypeId()==307||
							istack.getTypeId()==311||
							istack.getTypeId()==315)
					{
						this.player.getInventory().setChestplate(istack);
					}
					//Boots
					else if(istack.getTypeId()==301||
							istack.getTypeId()==305||
							istack.getTypeId()==309||
							istack.getTypeId()==313||
							istack.getTypeId()==317)
					{
						this.player.getInventory().setBoots(istack);
					}

					else{
						this.player.getInventory().addItem(istack);
					}
				}
				
				/* Spout Skin */
				if(p.isSpoutEnabled){
					if(this.dclass.spoutSkinURL!=null){
						try{
							Spout.getServer().getPlayer(player.getName()).setSkin(this.dclass.spoutSkinURL);
						} catch(UnsupportedOperationException exception) {
							p.log(p.language.get("Log_Error_Spout_Skin", this.dclass.name));
							Spout.getServer().getPlayer(player.getName()).resetSkin();
						}
					} else {
						Spout.getServer().getPlayer(player.getName()).resetSkin();
					}
				}
			}
		}
	}

	public void setCheckpoint(GameCheckpoint checkpoint){
		this.checkpoint=checkpoint;
	}

	public void addTreasure(){
		new DLootInventory(this.player,this.treasureInv.getContents());
	}

	//Static	
	public static void remove(DPlayer player){
		players.remove(player);
	}

	public static DPlayer get(Player player){
		for(DPlayer dplayer:players){
			if(dplayer.player.equals(player)){
				return dplayer;
			}
		}
		return null;
	}

	public static DPlayer get(String name) {
		for(DPlayer dplayer:players){
			if(dplayer.player.getName().equalsIgnoreCase(name)){
				return dplayer;
			}
		}
		return null;
	}
	
	public static CopyOnWriteArrayList<DPlayer> get(World world){
		CopyOnWriteArrayList<DPlayer> dplayers = new CopyOnWriteArrayList<DPlayer>();
		
		for(DPlayer dplayer : players){
			if(dplayer.world == world){
				dplayers.add(dplayer);
			}
		}
		
		return dplayers;
	}

	public static void update(boolean updateSecond){
		for(DPlayer dplayer:players){
			if(!updateSecond){
				//Check in World
				if(!dplayer.player.getWorld().equals(dplayer.world)){
					if(dplayer.isEditing){
						EditWorld eworld=EditWorld.get(dplayer.world);
						if(eworld!=null){
							if(eworld.lobby==null){
								dplayer.player.teleport(eworld.world.getSpawnLocation());
							}else{
								dplayer.player.teleport(eworld.lobby);
							}
						}
					}else{
						GameWorld gworld=GameWorld.get(dplayer.world);
						if(gworld!=null){
							if(gworld!=null){

								DGroup dgroup=DGroup.get(dplayer.player);
								if(dplayer.checkpoint==null){
									dplayer.player.teleport(dgroup.getGworld().locStart);
									if(dplayer.wolf!=null){
										dplayer.wolf.teleport(dgroup.getGworld().locStart);
									}
								}else{
									dplayer.player.teleport(dplayer.checkpoint.location);
									if(dplayer.wolf!=null){
										dplayer.wolf.teleport(dplayer.checkpoint.location);
									}
								}


								//Respawn Items
								for(ItemStack istack:dplayer.respawnInventory){
									if(istack!=null){
										dplayer.player.getInventory().addItem(istack);
									}
								}
								dplayer.respawnInventory.clear();
							}
						}
					}
				}
			}else{
				//Update Wolf
				if(dplayer.wolf!=null){
					if(dplayer.wolf.isDead()){
						if(dplayer.wolfRespawnTime<=0){
							dplayer.wolf=(Wolf) dplayer.world.spawnEntity(dplayer.player.getLocation(), EntityType.WOLF);
							dplayer.wolf.setTamed(true);
							dplayer.wolf.setOwner(dplayer.player);
							dplayer.wolfRespawnTime=30;
						}
						dplayer.wolfRespawnTime--;
					}
				}
				
				//Kick offline players
				if(dplayer.offlineTime > 0){
					if(dplayer.offlineTime < System.currentTimeMillis()){
						dplayer.leave();
					}
				}
			}
		}
	}
}
