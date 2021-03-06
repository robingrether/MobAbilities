package de.robingrether.mobabilities;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import de.robingrether.idisguise.disguise.DisguiseType;
import de.robingrether.idisguise.management.VersionHelper;
import de.robingrether.util.ObjectUtil;
import de.robingrether.util.Validate;

public abstract class Abilities {
	
	private static final Map<String, Abilities> fromName = new ConcurrentHashMap<String, Abilities>();
	
	public static Abilities fromName(String name) {
		Validate.notNull(name);
		return fromName.get(name.toLowerCase(Locale.ENGLISH));
	}
	
	public static List<String> values() {
		List<String> abilities = new ArrayList<String>(fromName.keySet());
		abilities.sort(Collator.getInstance(Locale.ENGLISH));
		return abilities;
	}
	
	public static String listAbilities() {
		Iterator<String> iterator = values().iterator();
		StringBuilder builder = new StringBuilder(iterator.next());
		while(iterator.hasNext()) {
			builder.append(", ");
			builder.append(iterator.next());
		}
		return builder.toString();
	}
	
	private String name;
	
	public boolean allowTargetByEntity(EntityType entityType) { return true; }
	
	public void apply(Player player) { applyPotionEffects(player); }
	
	public void applyPotionEffects(Player player) {}
	
	public abstract DisguiseType getDisguiseType();
	
	public final String name() { return name; }
	
	public void handleBowShoot(Player player, ItemStack bow, Entity projectile) {}
	
	public double handleDamage(Player player, double damage, DamageCause cause, Entity damager) { return damage; }
	
	public Vector handleMove(Player player, Vector movement) { return movement; }
	
	public void handleInteract(Player player, ItemStack item) {}
	
	public void handleInteractEntity(Player player, Entity entity) {}
	
	public void handleRightClickedByPlayer(Player player, Player other) {}
	
	public void handleTeleport(Player player, TeleportCause cause) {}
	
	public boolean hasPermission(Player player) { return player.hasPermission("MobAbilities." + name); }
	
	public final Abilities register(String name) { fromName.put(this.name = name, this); return this; }
	
	public void remove(Player player) { removePotionEffects(player); }
	
	public void removePotionEffects(Player player) {}
	
	public static final Abilities BLAZE = new Abilities() {
		
		public boolean allowTargetByEntity(EntityType entityType) {
			return !entityType.equals(EntityType.BLAZE);
		}
		
		public void applyPotionEffects(Player player) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
		}
		
		public DisguiseType getDisguiseType() {
			return DisguiseType.BLAZE;
		}
		
		public void handleInteract(Player player, ItemStack item) {
			if(item != null && item.getType().equals(Material.STICK)) {
				player.launchProjectile(SmallFireball.class);
			}
		}
		
		public void removePotionEffects(Player player) {
			player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
		}
		
	}.register("blaze");
	
	public static final Abilities CHICKEN = new Abilities() {
		
		public DisguiseType getDisguiseType() {
			return DisguiseType.CHICKEN;
		}
		
		public Vector handleMove(Player player, Vector movement) {
			if(movement.getY() < 0) {
				player.setVelocity(player.getVelocity().multiply(new Vector(1.0, 0.5, 1.0)));
				player.setFallDistance(0.0F);
			}
			return movement;
		}
		
	}.register("chicken");
	
	public static final Abilities CREEPER = new Abilities() {
		
		public boolean allowTargetByEntity(EntityType entityType) {
			return !entityType.equals(EntityType.CREEPER);
		}
		
		public DisguiseType getDisguiseType() {
			return DisguiseType.CREEPER;
		}
		
		public void handleInteract(Player player, ItemStack item) {
			if(item != null && item.getType().equals(Material.STICK) && player.isSneaking()) {
				player.getWorld().createExplosion(player.getLocation(), 2.0F);
			}
		}
		
	}.register("creeper");
	
	public static final Abilities ENDER_DRAGON = new Abilities() {
		
		public void apply(Player player) {
			player.setAllowFlight(true);
			player.setFlying(true);
			applyPotionEffects(player);
		}
		
		public DisguiseType getDisguiseType() {
			return DisguiseType.ENDER_DRAGON;
		}
		
		public Vector handleMove(Player player, Vector movement) {
			Location base = player.getLocation().subtract(4, 1, 4);
			for(int x = 0; x < 9; x++) {
				for(int y = 0; y < 5; y++) {
					for(int z = 0; z < 9; z++) {
						Block block = base.clone().add(x, y, z).getBlock();
						if(!ObjectUtil.equals(block.getType(), Material.AIR, Material.BEDROCK, Material.ENDER_STONE, Material.OBSIDIAN)) {
							block.setType(Material.AIR);
						}
					}
				}
			}
			return movement;
		}
		
		public void remove(Player player) {
			player.setFlying(false);
			player.setAllowFlight(false);
			removePotionEffects(player);
		}
		
	}.register("ender_dragon");
	
	public static final Abilities ENDERMAN = new Abilities() {
		
		public boolean allowTargetByEntity(EntityType entityType) {
			return !ObjectUtil.equals(entityType, EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.ENDER_DRAGON);
		}
		
		public void apply(Player player) {
			if(MobAbilities.instance.configuration.ENABLE_DISABILITIES) {
				DamageRunnable runnable = new DamageRunnable(player);
				runnable.runTaskTimer(MobAbilities.instance, 5L, 5L);
				damageRunnables.put(player, runnable);
			}
		}
		
		public DisguiseType getDisguiseType() {
			return DisguiseType.ENDERMAN;
		}
		
		public double handleDamage(Player player, double damage, DamageCause cause, Entity damager) {
			if(damager instanceof EnderPearl) {
				return 0.0;
			}
			return damage;
		}
		
		public void remove(Player player) {
			if(MobAbilities.instance.configuration.ENABLE_DISABILITIES) {
				DamageRunnable runnable = damageRunnables.remove(player);
				runnable.cancel();
			}
		}
		
		private Map<Player, DamageRunnable> damageRunnables = new ConcurrentHashMap<Player, DamageRunnable>();
		
		class DamageRunnable extends BukkitRunnable {
			
			private Player player;
			
			private DamageRunnable(Player player) {
				this.player = player;
			}
			
			public void run() {
				if(ObjectUtil.equals(player.getLocation().getBlock().getType(), Material.WATER, Material.STATIONARY_WATER) || ObjectUtil.equals(player.getLocation().getBlock().getRelative(BlockFace.UP).getType(), Material.WATER, Material.STATIONARY_WATER)) {
					player.damage(1.0);
				}
			}
			
		}
		
		public void handleTeleport(final Player player, TeleportCause cause) {
			if(cause.equals(TeleportCause.ENDER_PEARL)) {
				Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MobAbilities"), new Runnable() {
					
					public void run() {
						if(player.getInventory().contains(Material.ENDER_PEARL)) {
							ItemStack itemStack = player.getInventory().getItem(player.getInventory().first(Material.ENDER_PEARL));
							itemStack.setAmount(itemStack.getAmount() + 1);
						} else {
							player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 1));
						}
						player.updateInventory();
					}
					
				}, 10L);
			}
		}
		
	}.register("enderman");
	
	public static final Abilities GHAST = new Abilities() {
		
		public boolean allowTargetByEntity(EntityType entityType) {
			return !entityType.equals(EntityType.GHAST);
		}
		
		public void apply(Player player) {
			player.setAllowFlight(true);
			player.setFlying(true);
			applyPotionEffects(player);
		}
		
		public void applyPotionEffects(Player player) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
		}
		
		public DisguiseType getDisguiseType() {
			return DisguiseType.GHAST;
		}
		
		public void handleInteract(Player player, ItemStack item) {
			if(item != null && item.getType().equals(Material.STICK)) {
				player.launchProjectile(LargeFireball.class);
			}
		}
		
		public void remove(Player player) {
			player.setFlying(false);
			player.setAllowFlight(false);
			removePotionEffects(player);
		}
		
		public void removePotionEffects(Player player) {
			player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
		}
		
	}.register("ghast");
	
	public static final Abilities HORSE = new Abilities() {
		
		public void applyPotionEffects(Player player) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 3));
			player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
		}
		
		public DisguiseType getDisguiseType() {
			return DisguiseType.HORSE;
		}
		
		public void handleRightClickedByPlayer(Player player, Player other) {
			if(VersionHelper.require1_9())
				other.sendMessage(ChatColor.RED + "Riding is disabled due to bugs...");
			else
				player.setPassenger(other);
			
		}
		
		public void remove(Player player) {
			player.eject();
			removePotionEffects(player);
		}
		
		public void removePotionEffects(Player player) {
			player.removePotionEffect(PotionEffectType.JUMP);
			player.removePotionEffect(PotionEffectType.SPEED);
		}
		
	}.register("horse");
	
	public static final Abilities PIG_ZOMBIE = new Abilities() {
		
		public boolean allowTargetByEntity(EntityType entityType) {
			return !ObjectUtil.equals(entityType, EntityType.PIG_ZOMBIE, EntityType.WITHER);
		}
		
		public void applyPotionEffects(Player player) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
		}
		
		public DisguiseType getDisguiseType() {
			return DisguiseType.PIG_ZOMBIE;
		}
		
		public void removePotionEffects(Player player) {
			player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
		}
		
	}.register("pig_zombie");
	
	public static final Abilities SKELETON = new Abilities() {
		
		public boolean allowTargetByEntity(EntityType entityType) {
			return VersionHelper.require1_11() ? !ObjectUtil.equals(entityType, EntityType.CAVE_SPIDER, EntityType.SKELETON, EntityType.SPIDER, EntityType.WITHER, EntityType.WITHER_SKELETON) : !ObjectUtil.equals(entityType, EntityType.CAVE_SPIDER, EntityType.SKELETON, EntityType.SPIDER, EntityType.WITHER);
		}
		
		public void apply(Player player) {
			if(MobAbilities.instance.configuration.ENABLE_DISABILITIES) {
				FireTickRunnable runnable = new FireTickRunnable(player);
				runnable.runTaskTimer(MobAbilities.instance, 10L, 10L);
				fireTickRunnables.put(player, runnable);
			}
			applyPotionEffects(player);
		}
		
		public void applyPotionEffects(Player player) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 0));
		}
		
		public DisguiseType getDisguiseType() {
			return DisguiseType.SKELETON;
		}
		
		public void handleBowShoot(final Player player, ItemStack bow, Entity projectile) {
			if(bow != null && !bow.containsEnchantment(Enchantment.ARROW_INFINITE) && projectile.getType().equals(EntityType.ARROW)) {
				if(VersionHelper.require1_11()) {
					((Arrow)projectile).setPickupStatus(Arrow.PickupStatus.DISALLOWED);
				}
				Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MobAbilities"), new Runnable() {
					
					public void run() {
						if(player.getInventory().contains(Material.ARROW)) {
							ItemStack itemStack = player.getInventory().getItem(player.getInventory().first(Material.ARROW));
							itemStack.setAmount(itemStack.getAmount() + 1);
						} else {
							player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
						}
						player.updateInventory();
					}
					
				}, 10L);
			}
		}
		
		public void handleInteractEntity(Player player, Entity entity) {
			if(ObjectUtil.equals(entity.getType(), EntityType.CAVE_SPIDER, EntityType.SPIDER)) {
				if(!player.isInsideVehicle() && player.getLocation().distance(entity.getLocation()) < 5.0) {
					entity.setPassenger(player);
				}
			}
		}
		
		public void remove(Player player) {
			if(MobAbilities.instance.configuration.ENABLE_DISABILITIES) {
				FireTickRunnable runnable = fireTickRunnables.remove(player);
				runnable.cancel();
			}
			removePotionEffects(player);
		}
		
		public void removePotionEffects(Player player) {
			player.removePotionEffect(PotionEffectType.SATURATION);
		}
		
		private Map<Player, FireTickRunnable> fireTickRunnables = new ConcurrentHashMap<Player, FireTickRunnable>();
		
		class FireTickRunnable extends BukkitRunnable {
			
			private Player player;
			
			private FireTickRunnable(Player player) {
				this.player = player;
			}
			
			public void run() {
				if(player.getWorld().getHighestBlockYAt(player.getLocation()) <= player.getLocation().getY() && player.getWorld().getFullTime() < 12000 && !player.getWorld().hasStorm()) {
					player.setFireTicks(40);
				}
			}
			
		}
		
	}.register("skeleton");
	
	public static final Abilities SPIDER = new Abilities() {
		
		public boolean allowTargetByEntity(EntityType entityType) {
			return !ObjectUtil.equals(entityType, EntityType.CAVE_SPIDER, EntityType.SPIDER);
		}
		
		public DisguiseType getDisguiseType() {
			return DisguiseType.SPIDER;
		}
		
		public Vector handleMove(Player player, Vector movement) {
			Block block = player.getLocation().getBlock();
			if(block.isEmpty()) {
				Block blockDown = block.getRelative(BlockFace.DOWN);
				Block blockUp = block.getRelative(BlockFace.UP);
				List<BlockFace> blockFaces = new ArrayList<BlockFace>(Arrays.asList(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST));
				for(int i = 0; i < blockFaces.size(); i++) {
					if(!block.getRelative(blockFaces.get(i)).getType().isSolid()) {
						blockFaces.remove(i);
						i--;
					}
				}
				for(int i = 0; i < blockFaces.size(); i++) {
					if(!(blockUp.getRelative(blockFaces.get(i)).getType().isSolid() || (blockDown.getRelative(blockFaces.get(i)).getType().isSolid() && blockDown.isEmpty()))) {
						blockFaces.remove(i);
						i--;
					}
				}
				if(!blockFaces.isEmpty()) {
					byte blockData;
					switch(blockFaces.get(0)) {
						case NORTH:
							blockData = (byte)0b0001;
							break;
						case EAST:
							blockData = (byte)0b0010;
							break;
						case SOUTH:
							blockData = (byte)0b0100;
							break;
						case WEST:
							blockData = (byte)0b1000;
							break;
						default:
							blockData = (byte)0;
							break;
					}
					player.sendBlockChange(block.getLocation(), Material.VINE, blockData);
					Bukkit.getScheduler().runTaskLater(MobAbilities.instance, new BlockUpdate(player, block), 20L);
					if(blockUp.isEmpty()) {
						player.sendBlockChange(blockUp.getLocation(), Material.VINE, blockData);
						Bukkit.getScheduler().runTaskLater(MobAbilities.instance, new BlockUpdate(player, blockUp), 20L);
					}
					player.setFallDistance(0.0F);
				}
			}
			return movement;
		}
		
		class BlockUpdate implements Runnable {
			
			private Player player;
			private Block block;
			
			private BlockUpdate(Player player, Block block) {
				this.player = player;
				this.block = block;
			}
			
			public void run() {
				if(player == null) {
					return;
				}
				if(!player.getLocation().getBlock().equals(block)) {
					player.sendBlockChange(block.getLocation(), block.getType(), block.getData());
				} else {
					Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MobAbilities"), new BlockUpdate(player, block), 20L);
				}
			}
			
		}
		
	}.register("spider");
	
	public static final Abilities SQUID = new Abilities() {
		
		public void apply(Player player) {
			if(MobAbilities.instance.configuration.ENABLE_DISABILITIES) {
				DamageRunnable runnable = new DamageRunnable(player);
				runnable.runTaskTimer(MobAbilities.instance, 5L, 5L);
				damageRunnables.put(player, runnable);
			}
			applyPotionEffects(player);
		}
		
		public void applyPotionEffects(Player player) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0));
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0));
		}
		
		public DisguiseType getDisguiseType() {
			return DisguiseType.SQUID;
		}
		
		public void remove(Player player) {
			if(MobAbilities.instance.configuration.ENABLE_DISABILITIES) {
				DamageRunnable runnable = damageRunnables.remove(player);
				runnable.cancel();
			}
			removePotionEffects(player);
		}
		
		private Map<Player, DamageRunnable> damageRunnables = new ConcurrentHashMap<Player, DamageRunnable>();
		
		class DamageRunnable extends BukkitRunnable {
			
			private Player player;
			
			private DamageRunnable(Player player) {
				this.player = player;
			}
			
			public void run() {
				if(!(ObjectUtil.equals(player.getLocation().getBlock().getType(), Material.WATER, Material.STATIONARY_WATER) || ObjectUtil.equals(player.getLocation().getBlock().getRelative(BlockFace.UP).getType(), Material.WATER, Material.STATIONARY_WATER))) {
					player.damage(1.0);
				}
			}
			
		}
		
		public void removePotionEffects(Player player) {
			player.removePotionEffect(PotionEffectType.WATER_BREATHING);
			player.removePotionEffect(PotionEffectType.NIGHT_VISION);
		}
		
	}.register("squid");
	
	public static final Abilities WITHER = new Abilities() {
		
		public void apply(Player player) {
			player.setAllowFlight(true);
			player.setFlying(true);
			applyPotionEffects(player);
		}
		
		public DisguiseType getDisguiseType() {
			return DisguiseType.WITHER;
		}
		
		public void handleInteract(Player player, ItemStack item) {
			if(item != null && item.getType().equals(Material.STICK)) {
				player.launchProjectile(WitherSkull.class);
			}
		}
		
		public void remove(Player player) {
			player.setFlying(false);
			player.setAllowFlight(false);
			removePotionEffects(player);
		}
		
	}.register("wither");
	
	public static final Abilities ZOMBIE = new Abilities() {
		
		public boolean allowTargetByEntity(EntityType entityType) {
			return VersionHelper.require1_11() ? !ObjectUtil.equals(entityType, EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER) : !ObjectUtil.equals(entityType, EntityType.ZOMBIE);
		}
		
		public void apply(Player player) {
			if(MobAbilities.instance.configuration.ENABLE_DISABILITIES) {
				FireTickRunnable runnable = new FireTickRunnable(player);
				runnable.runTaskTimer(MobAbilities.instance, 10L, 10L);
				fireTickRunnables.put(player, runnable);
			}
			applyPotionEffects(player);
		}
		
		public void applyPotionEffects(Player player) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0));
			player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 0));
		}
		
		public DisguiseType getDisguiseType() {
			return DisguiseType.ZOMBIE;
		}
		
		public void remove(Player player) {
			if(MobAbilities.instance.configuration.ENABLE_DISABILITIES) {
				FireTickRunnable runnable = fireTickRunnables.remove(player);
				runnable.cancel();
			}
			removePotionEffects(player);
		}
		
		public void removePotionEffects(Player player) {
			player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
			player.removePotionEffect(PotionEffectType.SATURATION);
		}
		
		private Map<Player, FireTickRunnable> fireTickRunnables = new ConcurrentHashMap<Player, FireTickRunnable>();
		
		class FireTickRunnable extends BukkitRunnable {
			
			private Player player;
			
			private FireTickRunnable(Player player) {
				this.player = player;
			}
			
			public void run() {
				if(player.getWorld().getHighestBlockYAt(player.getLocation()) <= player.getLocation().getY() && player.getWorld().getFullTime() < 12000 && !player.getWorld().hasStorm()) {
					player.setFireTicks(40);
				}
			}
			
		}
		
	}.register("zombie");
	
}