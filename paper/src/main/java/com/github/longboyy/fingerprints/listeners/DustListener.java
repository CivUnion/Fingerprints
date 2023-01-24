package com.github.longboyy.fingerprints.listeners;

import com.github.longboyy.fingerprints.FingerprintManager;
import com.github.longboyy.fingerprints.FingerprintPlugin;
import com.github.longboyy.fingerprints.model.Fingerprint;
import com.github.longboyy.fingerprints.model.FingerprintContainer;
import com.github.longboyy.fingerprints.model.FingerprintReason;
import com.github.longboyy.fingerprints.util.MoreItemUtils;
import com.github.longboyy.fingerprints.util.ParticleUtils;
import com.github.longboyy.fingerprints.util.voronoi.Site;
import com.github.longboyy.fingerprints.util.voronoi.Voronoi;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.utilities.cooldowns.TickCoolDownHandler;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DustListener implements Listener {


	private final FingerprintPlugin plugin;
	private final FingerprintManager fingerprintManager;
	private final TickCoolDownHandler<UUID> dustCooldowns;

	public DustListener(FingerprintPlugin plugin, FingerprintManager fingerprintManager){
		this.plugin = plugin;
		this.fingerprintManager = fingerprintManager;
		this.dustCooldowns = new TickCoolDownHandler<>(plugin, 20L);
	}

	private static final ItemStack PAPER_ITEM = new ItemStack(Material.PAPER);
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDust(PlayerInteractEvent event){
		if(event.getAction() != Action.RIGHT_CLICK_BLOCK){
			return;
		}

		Block block = event.getClickedBlock();
		Player player = event.getPlayer();

		if(this.dustCooldowns.onCoolDown(player.getUniqueId())){
			return;
		}

		PlayerInventory playerInv = player.getInventory();
		ItemStack usedItem = player.getHandRaised() == EquipmentSlot.HAND ? playerInv.getItemInMainHand() : playerInv.getItemInOffHand();

		if(MoreItemUtils.areItemsSimilarIgnoreDura(usedItem, this.plugin.config().getDusterItem())){
			handleDust(player, usedItem, block);
			event.setCancelled(true);
		}else if(ItemUtils.areItemsSimilar(usedItem, this.plugin.config().getMagnifyingGlassItem())){
			handleMagnifyingGlass(player);
		}else if(ItemUtils.areItemsSimilar(usedItem, PAPER_ITEM)){
			handleSelfPrint(player);
		}

	}

	private void handleSelfPrint(Player player){
		PlayerInventory inv = player.getInventory();

		ItemMap paperMap = new ItemMap(new ItemStack(Material.PAPER));
		if(!paperMap.isContainedIn(inv)){
			player.sendMessage(
					Component.text("You don't have any paper to dust for fingerprints")
							.color(TextColor.color(120, 130, 255))
			);
			return;
		}

		ItemStack inkItem = getInkItem(player);
		Damageable inkDamageable = MoreItemUtils.getDamageable(inkItem);

		ItemMap inkMap = null;

		if(inkDamageable != null){
			int inkDamage = (inkItem.getType().getMaxDurability()/this.plugin.config().getInkUses());
			inkDamageable.setDamage(inkDamageable.getDamage() + inkDamage);
			FingerprintPlugin.log(String.format("Ink damage: %s", inkDamage));
		}else{
			inkMap = new ItemMap(this.plugin.config().getInkItem());
			inkMap.multiplyContent(this.plugin.config().getInkUses());
		}

		if(inkDamageable != null){
			if(inkDamageable.getDamage() > inkItem.getType().getMaxDurability()){
				inv.removeItemAnySlot(inkItem);
				player.sendMessage(
						Component.text("Your inkwell runs dry...")
								.color(TextColor.color(150, 150, 150))
								.decorate(TextDecoration.ITALIC)
				);
				//inv.remove(inkItem);
			}else {
				inkItem.setItemMeta(inkDamageable);
			}
			FingerprintPlugin.log("Ink damage should have been applied?");
		}else{
			inkMap.removeSafelyFrom(inv);
		}

		if(!paperMap.removeSafelyFrom(inv)){
			return;
		}

		Location loc = player.getLocation();
		Fingerprint fp = new Fingerprint(loc, System.currentTimeMillis(), player.getUniqueId(), FingerprintReason.PRINTED);
		ItemStack fpItem = fp.asItem();
		ItemMap map = new ItemMap(fp.asItem());
		if(!map.fitsIn(inv)){
			loc.getWorld().dropItemNaturally(loc, fpItem);
		}else{
			map.addToInventory(inv);
		}
	}


	private ItemStack getInkItem(Player player){
		PlayerInventory inv = player.getInventory();

		List<ItemStack> inkItems = Arrays.stream(inv.getContents())
				.filter(is -> MoreItemUtils.areItemsSimilarIgnoreDura(is, this.plugin.config().getInkItem()))
				.toList();

		return inkItems.get(0);
		/*
		Damageable inkDamageable = MoreItemUtils.getDamageable(inkItem);

		ItemMap inkMap = null;

		if(inkDamageable != null){
			int inkDamage = (inkItem.getType().getMaxDurability()/this.plugin.config().getInkUses());
			inkDamageable.setDamage(inkDamageable.getDamage() + inkDamage);
			FingerprintPlugin.log(String.format("Ink damage: %s", inkDamage));
		}else{
			inkMap = new ItemMap(this.plugin.config().getInkItem());
			inkMap.multiplyContent(this.plugin.config().getInkUses());
		}

		if(inkDamageable != null){
			if(inkDamageable.getDamage() > inkItem.getType().getMaxDurability()){
				inv.removeItemAnySlot(inkItem);
				player.sendMessage(
						Component.text("Your inkwell runs dry...")
								.color(TextColor.color(150, 150, 150))
								.decorate(TextDecoration.ITALIC)
				);
				//inv.remove(inkItem);
			}else {
				inkItem.setItemMeta(inkDamageable);
			}
			FingerprintPlugin.log("Ink damage should have been applied?");
		}else{
			inkMap.removeSafelyFrom(inv);
		}
		 */
	}

	private void handleDust(Player player, ItemStack duster, Block block){
		FingerprintContainer container = this.fingerprintManager.getFingerprintContainer(block);
		if(container == null){
			return;
		}

		if(this.dustCooldowns.onCoolDown(player.getUniqueId())){
			return;
		}

		this.dustCooldowns.putOnCoolDown(player.getUniqueId());

		PlayerInventory inv = player.getInventory();

		Location eyeLoc = player.getEyeLocation();
		RayTraceResult result = player.getWorld().rayTraceBlocks(eyeLoc, eyeLoc.getDirection(), 5);
		if(result != null && result.getHitBlockFace() != null) {
			BlockFace face = result.getHitBlockFace();
			Location projectionOrigin = container.getLocation()
					.toCenterLocation()
					.add(face.getDirection().divide(new Vector(2,2,2)))
					.subtract(ParticleUtils.transformToFace(new Vector(0.5f, 0, 0.5f), face));

			List<Fingerprint> fingerprints = container.getFingerprints();
			List<Site<Fingerprint>> sites = fingerprints.stream()
					.map(fp -> new Site<>(
						projectionOrigin.clone().add(ParticleUtils.transformToFace(fp.getOffset(), face)).toVector(),
						fp
					)).toList();

			Voronoi<Fingerprint> voronoi = new Voronoi<>(sites);
			Fingerprint fp = voronoi.getValueAt(result.getHitPosition());
			if(fp == null){
				return;
			}

			List<ItemStack> inkItems = Arrays.stream(inv.getContents())
					.filter(is -> MoreItemUtils.areItemsSimilarIgnoreDura(is, this.plugin.config().getInkItem()))
					.toList();

			if(inkItems.isEmpty()){
				// no ink
				player.sendMessage(
						Component.text("You don't have enough ink to dust for fingerprints")
								.color(TextColor.color(120, 130, 255))
				);
				return;
			}

			ItemStack inkItem = inkItems.get(0);
			Damageable inkDamageable = MoreItemUtils.getDamageable(inkItem);
			Damageable dusterDamageable = MoreItemUtils.getDamageable(duster);

			ItemMap inkMap = null;

			if(inkDamageable != null){
				int inkDamage = (inkItem.getType().getMaxDurability()/this.plugin.config().getInkUses());
				inkDamageable.setDamage(inkDamageable.getDamage() + inkDamage);
				FingerprintPlugin.log(String.format("Ink damage: %s", inkDamage));
			}else{
				inkMap = new ItemMap(this.plugin.config().getInkItem());
				inkMap.multiplyContent(this.plugin.config().getInkUses());
			}

			if(dusterDamageable != null){
				int dusterDamage = (duster.getType().getMaxDurability()/this.plugin.config().getDusterUses());
				dusterDamageable.setDamage(dusterDamageable.getDamage() + dusterDamage);
				FingerprintPlugin.log(String.format("Duster damage: %s", dusterDamage));
			}


			if(inkMap != null && inkMap.isContainedIn(inv)){
				// not enough ink
				player.sendMessage(
						Component.text("You don't have enough ink to dust for fingerprints")
								.color(TextColor.color(120, 130, 255))
				);
				return;
			}

			ItemMap paperMap = new ItemMap(new ItemStack(Material.PAPER));
			if(!paperMap.removeSafelyFrom(inv)){
				// no paper
				player.sendMessage(
						Component.text("You don't have any paper to dust for fingerprints")
								.color(TextColor.color(120, 130, 255))
				);
				return;
			}

			if(dusterDamageable != null){
				if(dusterDamageable.getDamage() > duster.getType().getMaxDurability()){
					inv.remove(duster);
					player.sendMessage(
							Component.text("Your duster falls apart...")
									.color(TextColor.color(150, 150, 150))
									.decorate(TextDecoration.ITALIC)
					);
				}else{
					duster.setItemMeta(dusterDamageable);
				}
				FingerprintPlugin.log("Duster damage should have been applied?");
			}else{
				inv.remove(duster);
			}

			if(inkDamageable != null){
				if(inkDamageable.getDamage() > inkItem.getType().getMaxDurability()){
					inv.removeItemAnySlot(inkItem);
					player.sendMessage(
							Component.text("Your inkwell runs dry...")
									.color(TextColor.color(150, 150, 150))
									.decorate(TextDecoration.ITALIC)
					);
					//inv.remove(inkItem);
				}else {
					inkItem.setItemMeta(inkDamageable);
				}
				FingerprintPlugin.log("Ink damage should have been applied?");
			}else{
				inkMap.removeSafelyFrom(inv);
			}
			

			player.getWorld().dropItemNaturally(player.getLocation(), fp.asItem());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event){
		Player player = event.getPlayer();

		PlayerInventory playerInv = player.getInventory();

		if(ItemUtils.areItemsSimilar(playerInv.getItemInMainHand(), this.plugin.config().getMagnifyingGlassItem())){
			handleMagnifyingGlass(player);
		}

		if(ItemUtils.areItemsSimilar(playerInv.getItemInOffHand(), this.plugin.config().getMagnifyingGlassItem())){
			handleMagnifyingGlass(player);
		}
	}

	private void handleMagnifyingGlass(Player player){
		Location eyeLoc = player.getEyeLocation();
		RayTraceResult result = player.getWorld().rayTraceBlocks(eyeLoc, eyeLoc.getDirection(), 6);
		if(result != null && result.getHitBlockFace() != null) {
			Block block = result.getHitBlock();
			if(block == null){
				return;
			}
			FingerprintContainer container = this.fingerprintManager.getFingerprintContainer(block);
			if(container == null){
				return;
			}

			ParticleUtils.projectFingerprints(player, container, result.getHitBlockFace());
		}
	}

}
