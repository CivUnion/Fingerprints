package com.github.longboyy.fingerprints.listeners;

import com.github.longboyy.fingerprints.FingerprintPlugin;
import com.github.longboyy.fingerprints.model.FingerprintReason;
import com.github.longboyy.fingerprints.util.FingerprintUtils;
import com.google.common.collect.ImmutableSet;
import isaac.bastion.Bastion;
import isaac.bastion.BastionBlock;
import isaac.bastion.event.BastionDamageEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.BlockIterator;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.CitadelPermissionHandler;
import vg.civcraft.mc.citadel.model.Reinforcement;
import vg.civcraft.mc.civmodcore.events.PlayerMoveBlockEvent;

import java.util.*;
import java.util.stream.Collectors;

public class FingerprintListener implements Listener {

	private final FingerprintPlugin plugin;

	public FingerprintListener(FingerprintPlugin plugin){
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
			FingerprintPlugin.log("MURDER/ASSAULT - REASON, PLAYER, OR VICTIM IS NULL");
			return;
		}

		double baseChance;

		if(victim instanceof Mob){
			baseChance = reason.getSetting("pve_chance", 0.02D);
		}else if(victim instanceof Player){
			baseChance = reason.getSetting("pvp_chance", 0.1D);
		}else{
			FingerprintPlugin.log("MURDER/ASSAULT - NO VALID VICTIM");
			return;
		}

		Location loc = FingerprintUtils.getClosestNonAir(victim.getLocation());
		Set<BastionBlock> bastions = Bastion.getBastionManager().getBlockingBastions(loc);

		if(bastions.size() > 0) {
			double allowedOnBastionMult = reason.getSetting("player_allowed_on_bastion_multiplier", 0.8D);
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
						FingerprintPlugin.log("MURDER - FAILED CHANCE");
					}
				}
			}
		}else if(FingerprintUtils.checkChance(baseChance)){
			createMurderAssaultFingerprint(reason, loc, player, victim);
		}else{
			FingerprintPlugin.log("MURDER/ASSAULT - FAILED CHANCE");
		}
	}

	private static final Set<InventoryType> VALID_CONTAINERS = ImmutableSet.of(
			InventoryType.BARREL,
			InventoryType.SMOKER,
			InventoryType.BLAST_FURNACE,
			InventoryType.BREWING,
			InventoryType.CHEST,
			InventoryType.DISPENSER,
			InventoryType.DROPPER,
			InventoryType.HOPPER,
			InventoryType.LECTERN,
			InventoryType.FURNACE
	);

	private static final Set<InventoryAction> VALID_INV_ACTIONS = ImmutableSet.of(
			InventoryAction.PICKUP_ONE,
			InventoryAction.PICKUP_SOME,
			InventoryAction.PICKUP_HALF,
			InventoryAction.PICKUP_ALL,
			InventoryAction.COLLECT_TO_CURSOR,
			InventoryAction.DROP_ONE_CURSOR,
			InventoryAction.DROP_ALL_CURSOR,
			InventoryAction.DROP_ONE_SLOT,
			InventoryAction.DROP_ALL_SLOT,
			InventoryAction.SWAP_WITH_CURSOR,
			InventoryAction.HOTBAR_SWAP,
			InventoryAction.HOTBAR_MOVE_AND_READD,
			InventoryAction.MOVE_TO_OTHER_INVENTORY
	);

	/**
	 * THEFT
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerTheft(InventoryClickEvent event){
		/*
		FingerprintPlugin.log(String.format("Main inv: %s | Clicked inv: %s | Action: %s",
				event.getInventory().getType(),
				event.getClickedInventory() != null ? event.getClickedInventory().getType() : "NONE",
				event.getAction()
		));
		 */

		Inventory mainInv = event.getInventory();
		if(!VALID_CONTAINERS.contains(mainInv.getType()) || !VALID_INV_ACTIONS.contains(event.getAction()) || mainInv.getType() != event.getClickedInventory().getType()){
			return;
		}

		ItemStack itemStack = event.getCurrentItem();
		if(itemStack == null){
			return;
		}

		if(mainInv.getLocation() == null){
			return;
		}

		Block block = mainInv.getLocation().getBlock();
		if(!(block.getState() instanceof Container)){
			FingerprintPlugin.log("Attempted to do theft but wasn't container");
			return;
		}

		Player player = (Player) event.getWhoClicked();
		Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(block);
		double chance = -1D;
		if(rein == null){
			// no reinforcement, just apply the chance here
			chance = FingerprintReason.THEFT.getSetting("unlocked_chance", 0.05D);
		}else if(!rein.hasPermission(player, CitadelPermissionHandler.getChests())){
			chance = FingerprintReason.THEFT.getSetting("locked_chance", 0.1D);
		}

		//double stackSize = item.getMaxStackSize() == 1D ? 1D : item.getMaxStackSize();
		chance *= (double) itemStack.getAmount() / itemStack.getMaxStackSize();
		FingerprintPlugin.log("Chance for theft: " + chance);

		if(FingerprintUtils.checkChance(chance)) {
			FingerprintUtils.addFingerprint(mainInv.getLocation(), player, FingerprintReason.THEFT);
		}


		//FingerprintPlugin.log(String.format("ItemStack: %s", itemStack));

		//FingerprintPlugin.log(event.getInventory().getType().toString());
	}

	/**
	 * TRESPASSING
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveBlockEvent event){
		Player player = event.getPlayer();
		Location loc = event.getTo();
		List<String> validBastionTypes = FingerprintReason.TRESPASSING.getSetting("bastion_types", new ArrayList<String>());
		Set<BastionBlock> bastions = Bastion.getBastionManager().getBlockingBastions(loc).stream().filter(bastionBlock -> validBastionTypes.contains(bastionBlock.getType().getName())).collect(Collectors.toSet());
		if(bastions.isEmpty()){
			return;
		}

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
			FingerprintPlugin.log("ASSAULT - NO DAMAGED OR DAMAGER");
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
			FingerprintPlugin.log("MURDER - NO KILLER");
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
			FingerprintPlugin.log("RUMMAGE CONTAINER - FAILED CHANCE: " + chance);
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
	@EventHandler(priority = EventPriority.MONITOR)
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

		Block block = loc.getBlock();
		BlockIterator blockIterator = new BlockIterator(loc, 0, 1);
		do{
			Block b = blockIterator.next();
			if(b.isEmpty()){
				block = b;
				break;
			}
		}while(blockIterator.hasNext());

		double chance = FingerprintReason.VANDALISM.getSetting("block_break_chance", 0.05D);

		if(FingerprintUtils.checkChance(chance)){
			FingerprintUtils.addFingerprint(block.getLocation(), player, FingerprintReason.VANDALISM);
		}
	}

}
