package nl.rubend.fishslap.fishslap;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.*;

public final class Fishslap extends JavaPlugin implements Listener {
	private String worldName;
	private ItemStack fish = new ItemStack(Material.COD);
	private ItemStack hoe = new ItemStack(Material.IRON_HOE, 2);
	private ItemStack feather = new ItemStack(Material.FEATHER);
	private ItemStack sugar = new ItemStack(Material.SUGAR);
	private Map<String, Location> maps = new HashMap<>();
	private String currentMap;
	private Map<Player,Player> lastDamage=new HashMap<>();
	private Map<Player,Integer> lastDamageTime=new HashMap<>();

	@Override
	public void onEnable() {
		saveDefaultConfig();
		Bukkit.getPluginManager().registerEvents(this, this);
		fish.addUnsafeEnchantment(Enchantment.KNOCKBACK, 6);
		this.saveResource("config.yml", false);
		worldName = getConfig().getString("worldName", "fs");
		maps.put("Archill", new Location(null,-240, 100, 64));
		maps.put("Birds", new Location(null, -315, 177, 416));
		maps.put("IceVillage", new Location(null, -84, 160, 332));
		maps.put("Balloons", new Location(null, -102, 120, 65));
		maps.put("UFO", new Location(null, -217, 112, 187));
		maps.put("Prody", new Location(null, -436, 165, 121));
		maps.put("End", new Location(null, -354, 146, 268));
		maps.put("SpaceStation", new Location(null, -466, 143, 280));
		maps.put("Frozen", new Location(null, -175, 123, 482));
		maps.put("RottenRotation", new Location(null, -92, 110, 188));
		maps.put("FastFood", new Location(null, -225, 134, 322));
		maps.put("Sculpture", new Location(null, -464, 123, 408));
		nextMap();
	}

	@Override
	public void onDisable() {
		Bukkit.unloadWorld(worldName, false);
	}

	private void nextMap() {
		ArrayList<String> keys = new ArrayList<>(maps.keySet());
		currentMap = keys.get(new Random().nextInt(keys.size()));
		if(getWorld()==null) return;
		for (Player player : getWorld().getPlayers()) onJoin(player);
	}
	private void sendToPlayers(String message) {
		for(Player player:getWorld().getPlayers()) player.sendMessage("§b[FS]:"+message);
	}
	private Location getSpawn() {
		Location location=maps.get(currentMap);
		location.setWorld(getWorld());
		return location;
	}
	private void onJoin(Player player) {
		lastDamage.remove(player);
		lastDamageTime.remove(player);
		if (player.getWorld() != getWorld()) return;
		player.getInventory().clear();
		player.getInventory().setItem(0, fish);
		player.getInventory().setItem(1, hoe);
		player.getInventory().setItem(2, feather);
		player.getInventory().setItem(3, sugar);
		player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
		player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 1));
		player.teleport(getSpawn());
		player.setCollidable(false);
		player.sendMessage("§ocurrently on map \""+currentMap+"\"!");
	}

	private World getWorld() {
		return Bukkit.getWorld(worldName);
	}

	@EventHandler
	private void onPlayerDeath(PlayerDeathEvent event) {
		if (event.getEntity().getWorld() != getWorld()) return;
		event.getDrops().clear();
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> event.getEntity().spigot().respawn(), 0L);
	}

	@EventHandler
	private void onDamage(EntityDamageEvent event) {
		if (event.getEntity().getWorld() != getWorld()) return;
		if (event.getCause() == EntityDamageEvent.DamageCause.VOID)
			event.setDamage(((Player) event.getEntity()).getHealth());
		else if(event.getCause()==EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
		else event.setCancelled(true);
	}

	@EventHandler
	private void onInventoryClick(InventoryClickEvent event) {
		if (event.getWhoClicked().getGameMode() == GameMode.CREATIVE) return;
		if (event.getWhoClicked().getWorld() == getWorld()) event.setCancelled(true);
	}

	@EventHandler
	private void onItemDrop(PlayerDropItemEvent event) {
		if (event.getPlayer().getWorld() == getWorld()) event.setCancelled(true);
	}

	@EventHandler
	private void onItemConsume(PlayerItemConsumeEvent event) {
		if (event.getPlayer().getWorld() == getWorld()) event.setCancelled(true);
	}

	@EventHandler
	private void onInteract(PlayerInteractEvent event) {
		if (event.getPlayer().getWorld() != getWorld()) return;
		ItemStack item = event.getItem();
		if (item == null) return;
		Material type = item.getType();
		Player player = event.getPlayer();
		if (player.getGameMode() == GameMode.CREATIVE) return;
		if (type == Material.COD) return;
		if (type == Material.IRON_HOE)
			event.getPlayer().setVelocity(event.getPlayer().getVelocity().add(event.getPlayer().getLocation().getDirection().multiply(3)));
		if (type == Material.FEATHER) {
			player.setAllowFlight(true);
			player.setFlying(true);
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
				player.setFlying(false);
				player.setAllowFlight(false);
			}, 100L);
		}
		if (type == Material.SUGAR) player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 4));
		item.subtract();
	}

	@EventHandler
	private void onChangedWorld(PlayerChangedWorldEvent event) {
		onJoin(event.getPlayer());
	}

	@EventHandler
	private void onRespawn(PlayerRespawnEvent event) {
		onJoin(event.getPlayer());
	}

	@EventHandler
	private void onPlayerJoin(PlayerJoinEvent event) {
		onJoin(event.getPlayer());
	}

	@EventHandler
	private void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (event.getPlayer().getWorld() != getWorld()) return;
		if (player.getGameMode() == GameMode.CREATIVE) return;
		if (player.getLocation().getY() > 80) return;
		if (lastDamage.containsKey(player)) {
			if(Bukkit.getCurrentTick()-lastDamageTime.get(player)<60) sendToPlayers(lastDamage.get(player).getName()+" slapped "+player.getName()+" out of the world.");
			else sendToPlayers(player.getName()+" thought suicide was smart, while trying to escape "+lastDamage.get(player).getName());
		}
		else sendToPlayers(player.getName()+" thought suicide was smart.");
		onJoin(player);
	}
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (cmd.getName().equalsIgnoreCase("next")) nextMap();
		return true;
	}
	@EventHandler private void onEntityDamage(EntityDamageByEntityEvent e) {
		if(e.getDamager().getWorld()!=getWorld()) return;
		if(!(e.getEntity() instanceof Player) || !(e.getDamager() instanceof Player)) return;
		if(e.getEntity().getLocation().distance(getSpawn())<10) {
			e.setCancelled(true);
			return;
		}
		Player slapped= (Player) e.getEntity();
		Player slapper= (Player) e.getDamager();
		lastDamage.put(slapped,slapper);
		lastDamageTime.put(slapped,Bukkit.getCurrentTick());
	}
}
