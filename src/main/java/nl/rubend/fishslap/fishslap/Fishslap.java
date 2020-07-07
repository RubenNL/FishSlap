package nl.rubend.fishslap.fishslap;

import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;

public final class Fishslap extends JavaPlugin implements Listener {
	private String worldName;
	private YamlConfiguration config;
	private ItemStack fish=new ItemStack(Material.COD);
	private ItemStack hoe=new ItemStack(Material.IRON_HOE,2);
	private ItemStack feather=new ItemStack(Material.FEATHER);
	private ItemStack sugar=new ItemStack(Material.SUGAR);
	@Override public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);
		fish.addUnsafeEnchantment(Enchantment.KNOCKBACK,6);
		this.saveResource("config.yml", false);
		try {
			config = YamlConfiguration.loadConfiguration(new File(this.getDataFolder(), "config.yml"));
		} catch (IllegalArgumentException ex) {
			ex.printStackTrace();
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		worldName = config.getString("worldName","fs");
	}
	private void onJoin(Player player) {
		if(player.getWorld()!=getWorld()) return;
		player.getInventory().clear();
		player.getInventory().setItem(0,fish);
		player.getInventory().setItem(1,hoe);
		player.getInventory().setItem(2,feather);
		player.getInventory().setItem(3,sugar);
		player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Integer.MAX_VALUE,1));
		player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP,Integer.MAX_VALUE,1));
		player.teleport(getWorld().getSpawnLocation());
	}
	private World getWorld() {
		World world= Bukkit.getWorld(worldName);
		if(world!=null) return world;
		WorldCreator wc = new WorldCreator(worldName);
		world=Bukkit.getServer().createWorld(wc);
		world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);
		world.setFullTime(6000);
		return world;
	}

	@EventHandler private void onPlayerDeath(PlayerDeathEvent event) {
		if (event.getEntity().getWorld() != getWorld()) return;
		event.getDrops().clear();
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> event.getEntity().spigot().respawn(), 0L);
	}

	@EventHandler private void onDamage(EntityDamageEvent event) {
		if (event.getEntity().getWorld() != getWorld()) return;
		if (event.getCause() == EntityDamageEvent.DamageCause.VOID) event.setDamage(((Player) event.getEntity()).getHealth());
		else event.setCancelled(true);
	}
	@EventHandler private void onInventoryClick (InventoryClickEvent event){
		if (event.getWhoClicked().getGameMode()==GameMode.CREATIVE) return;
		if (event.getWhoClicked().getWorld() == getWorld()) event.setCancelled(true);
	}
	@EventHandler private void onItemDrop(PlayerDropItemEvent event) {
		if(event.getPlayer().getWorld()==getWorld()) event.setCancelled(true);
	}
	@EventHandler private void onItemConsume(PlayerItemConsumeEvent event) {
		if(event.getPlayer().getWorld()==getWorld()) event.setCancelled(true);
	}
	@EventHandler private void onInteract(PlayerInteractEvent event) {
		if(event.getPlayer().getWorld()!=getWorld()) return;
		ItemStack item=event.getItem();
		if(item==null) return;
		Material type=item.getType();
		Player player=event.getPlayer();
		if (player.getGameMode()==GameMode.CREATIVE) return;
		if(type==Material.COD) return;
		if(type==Material.IRON_HOE) event.getPlayer().setVelocity(event.getPlayer().getVelocity().add(event.getPlayer().getLocation().getDirection().multiply(3)));
		if(type==Material.FEATHER) {
			player.setAllowFlight(true);
			player.setFlying(true);
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
				player.setFlying(false);
				player.setAllowFlight(false);
			}, 100L);
		}
		if(type==Material.SUGAR) player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,200,4));
		item.subtract();
	}
	@EventHandler private void onChangedWorld(PlayerChangedWorldEvent event) {onJoin(event.getPlayer());}
	@EventHandler private void onRespawn(PlayerRespawnEvent event) {onJoin(event.getPlayer());}
	@EventHandler private void onPlayerJoin(PlayerJoinEvent event) {onJoin(event.getPlayer());}

	@EventHandler private void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (event.getPlayer().getWorld() != getWorld()) return;
		if (player.getGameMode()==GameMode.CREATIVE) return;
		if (player.getLocation().getY() < 40) onJoin(player);
	}
}
