package com.github.longboyy.fingerprints.listeners;

import com.github.longboyy.fingerprints.FingerprintManager;
import com.github.longboyy.fingerprints.Fingerprints;
import com.github.longboyy.fingerprints.model.Fingerprint;
import com.github.longboyy.fingerprints.model.FingerprintContainer;
import com.github.longboyy.fingerprints.model.FingerprintReason;
import com.github.longboyy.fingerprints.util.FingerprintUtils;
import isaac.bastion.Bastion;
import isaac.bastion.BastionBlock;
import isaac.bastion.event.BastionDamageEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.CitadelPermissionHandler;
import vg.civcraft.mc.citadel.model.Reinforcement;
import vg.civcraft.mc.civmodcore.events.PlayerMoveBlockEvent;
import vg.civcraft.mc.namelayer.permission.PermissionType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.*;

public class PlayerListener implements Listener {

	private final Fingerprints plugin;

	public PlayerListener(Fingerprints plugin){
		this.plugin = plugin;
	}

	private static void createMurderAssaultFingerprint(FingerprintReason reason, Location loc, Player player, LivingEntity victim){
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("victim_id", victim.getUniqueId());
		metadata.put("victim_name", String.format("%1.64s", victim.getName()));
		FingerprintUtils.addFingerprint(loc, player, reason, metadata);
	}

	private static void handleMurderAssault(FingerprintReason reason, Player player, LivingEntity victim){
		if(reason == null || player == null || victim == null){
			FingerprintUtils.log("MURDER/ASSAULT - REASON, PLAYER, OR VICTIM IS NULL");
			return;
		}

		double baseChance;

		if(victim instanceof Mob){
			baseChance = reason.getSetting("pve_chance", 0.02D);
		}else if(victim instanceof Player){
			baseChance = reason.getSetting("pvp_chance", 0.1D);
		}else{
			FingerprintUtils.log("MURDER/ASSAULT - NO VALID VICTIM");
			return;
		}

		Location loc = FingerprintUtils.getClosestNonAir(victim.getLocation());
		Set<BastionBlock> bastions = Bastion.getBastionManager().getBlockingBastions(loc);
		double allowedOnBastionMult = reason.getSetting("player_allowed_on_bastion_multiplier", 0.8D);

		if(bastions.size() > 0) {
			for (BastionBlock bastion : bastions) {
				if (bastion.canPlace(player)){
					double chance = baseChance;
					if(victim.getType() == EntityType.PLAYER){
						if(!reason.getSetting("ignore_bastion_for_pvp", true)){
							chance *= allowedOnBastionMult;
						}
					}
					if(FingerprintUtils.checkChance(chance)) {
						createMurderAssaultFingerprint(reason, loc, player, victim);
					}else{
						FingerprintUtils.log("MURDER - FAILED CHANCE");
					}
				}
			}
		}else if(FingerprintUtils.checkChance(baseChance)){
			createMurderAssaultFingerprint(reason, loc, player, victim);
		}else{
			FingerprintUtils.log("MURDER/ASSAULT - FAILED CHANCE");
		}
	}

	/**
	 * THEFT
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryMove(InventoryMoveItemEvent event){
		if(!(event.getDestination() instanceof PlayerInventory to)){
			return;
		}

		Inventory from = event.getInitiator();
		Location loc = from.getLocation();
		if(loc == null){
			return;
		}

		Block block = loc.getBlock();
		if(!(block.getState() instanceof Container)){
			return;
		}

		Player player = (Player)to.getHolder();
		Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(block);
		double chance = -1D;
		if(rein == null){
			// no reinforcement, just apply the chance here
			chance = FingerprintReason.THEFT.getSetting("unlocked_chance", 0.05D);
		}else if(!rein.hasPermission(player, CitadelPermissionHandler.getChests())){
			chance = FingerprintReason.THEFT.getSetting("locked_chance", 0.1D);
		}

		ItemStack item = event.getItem();
		double stackSize = item.getMaxStackSize() == 1D ? 1D : item.getMaxStackSize() / 8D;
		chance *= item.getAmount() / stackSize;

		if(FingerprintUtils.checkChance(chance)) {
			FingerprintUtils.addFingerprint(loc, player, FingerprintReason.THEFT);
		}
	}

	/**
	 * TRESPASSING
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveBlockEvent event){
		Player player = event.getPlayer();
		Location loc = event.getTo();
		Set<BastionBlock> bastions = Bastion.getBastionManager().getBlockingBastions(loc);
		if(bastions == null || bastions.isEmpty()){
			return;
		}

		List<String> validBastionTypes = FingerprintReason.TRESPASSING.getSetting("bastion_types", new ArrayList<String>());
		double chance = FingerprintReason.TRESPASSING.getSetting("chance", 0.05D);

		for(BastionBlock bastion : bastions){
			if(!validBastionTypes.contains(bastion.getType().getName())){
				continue;
			}

			if(bastion.canPlace(player) || bastion.canPearl(player)){
				continue;
			}

			if(FingerprintUtils.checkChance(chance)){
				FingerprintUtils.addFingerprint(loc, player, FingerprintReason.TRESPASSING);
			}
		}
	}

	/**
	 * ASSAULT - PvP & PvE
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageByEntityEvent event){

		if(!(event.getEntity() instanceof LivingEntity damaged) ||
				!(event.getDamager() instanceof Player damager)){
			FingerprintUtils.log("ASSAULT - NO DAMAGED OR DAMAGER");
			return;
		}

		handleMurderAssault(FingerprintReason.ASSAULT, damager, damaged);
	}


	/**
	 * MURDER - PvP & PvE
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityKilled(EntityDeathEvent event){
		LivingEntity killed = event.getEntity();
		Player killer = event.getEntity().getKiller();
		if(killer == null){
			FingerprintUtils.log("MURDER - NO KILLER");
			return;
		}

		handleMurderAssault(FingerprintReason.MURDER, killer, killed);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onContainerOpen(InventoryOpenEvent event){
		Inventory inv = event.getInventory();
		Location loc = inv.getLocation();
		if(loc == null){
			return;
		}

		Block block = loc.getBlock();
		if(!(block.getState() instanceof Container)){
			return;
		}

		Player player = (Player)event.getPlayer();

		Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(block);
		double chance = -1D;
		if(rein == null){
			// no reinforcement, just apply the chance here
			chance = FingerprintReason.RUMMAGING.getSetting("unlocked_chance", 0.05D);
		}else if(!rein.hasPermission(player, CitadelPermissionHandler.getChests())){
			chance = FingerprintReason.RUMMAGING.getSetting("locked_chance", 0.1D);
		}

		if(FingerprintUtils.checkChance(chance)) {
			FingerprintUtils.addFingerprint(loc, player, FingerprintReason.RUMMAGING);
		}else{
			FingerprintUtils.log("RUMMAGE CONTAINER - FAILED CHANCE: " + chance);
		}
	}

	/**
	 * RUMMAGING - ANY CONTAINERS
	 */
	/*
	@EventHandler(priority = EventPriority.MONITOR)
	public void onContainerOpen(PlayerInteractEvent event){
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();

		if(block == null){
			FingerprintUtils.log("RUMMAGE CONTAINER - NO BLOCK");
			return;
		}

		BlockState state = block.getState();

		if(!(state instanceof Container)){
			FingerprintUtils.log("RUMMAGE CONTAINER - NO CONTAINER");
			return;
		}

		Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(block);
		Location loc = event.getClickedBlock().getLocation();
		double chance = -1D;
		if(rein == null){
			// no reinforcement, just apply the chance here
			chance = FingerprintReason.RUMMAGING.getSetting("unlocked_chance", 0.05D);
		}else if(!rein.hasPermission(event.getPlayer(), CitadelPermissionHandler.getChests())){
			chance = FingerprintReason.RUMMAGING.getSetting("locked_chance", 0.1D);
		}

		if(FingerprintUtils.checkChance(chance)) {
			FingerprintUtils.addFingerprint(loc, player, FingerprintReason.RUMMAGING);
		}else{
			FingerprintUtils.log("RUMMAGE CONTAINER - FAILED CHANCE: " + chance);
		}
	}
	 */


	/**
	 * VANDALISM - BASTION BREAK
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBastionDamage(BastionDamageEvent event){
		Player player = event.getPlayer();
		Location loc = FingerprintUtils.getClosestNonAir(player.getLocation());

		double chance = FingerprintReason.VANDALISM.getSetting("bastion_break_chance", 0.11D);

		if(FingerprintUtils.checkChance(chance)){
			FingerprintUtils.addFingerprint(loc, player, FingerprintReason.VANDALISM);
		}
	}

	/**
	 * VANDALISM - BLOCK BREAK
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockBreak(BlockBreakEvent event){
		Player player = event.getPlayer();
		Location loc = event.getBlock().getLocation();

		double chance = FingerprintReason.VANDALISM.getSetting("block_break_chance", 0.05D);

		if(FingerprintUtils.checkChance(chance)){
			FingerprintUtils.addFingerprint(loc, player, FingerprintReason.VANDALISM);
		}
	}

}
