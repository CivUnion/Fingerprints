package com.github.longboyy.fingerprints.listeners;

import com.github.longboyy.fingerprints.util.FingerprintUtils;
import com.github.longboyy.fingerprints.util.MoreItemUtils;
import net.minecraft.world.item.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;

import java.util.Map;

public class BookListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void OnBookCreation(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
    	return; 

    Player player = event.getPlayer();
    if (!player.isSneaking())
		return;

    PlayerInventory playerInv = player.getInventory();
    ItemStack mainItem = playerInv.getItemInMainHand();
    ItemStack offItem = playerInv.getItemInOffHand();

    if (!FingerprintUtils.isFingerprint(offItem) || (!FingerprintUtils.isFingerprintBook(mainItem) && mainItem.getType() != Material.BOOK))
		return;

    if (mainItem.getAmount() > 1)
		return; 

    if (mainItem.getType() == Material.BOOK) {
		mainItem.setType(Material.WRITTEN_BOOK);
		BookMeta meta = (BookMeta)mainItem.getItemMeta();
		String[] fpOwners = new String[0];
		meta.setTitle("Fingerprint Compendium");
		meta.setAuthor(player.getName());
		mainItem.setItemMeta(meta);
		ItemStack enriched = ItemMap.enrichWithNBT(mainItem, 1, Map.of(FingerprintUtils.FP_BOOK_NBT_TAG_KEY, true));
		ItemStack enriched2 = ItemMap.enrichWithNBT(enriched, 1, Map.of("FingerprintOwners", fpOwners));
		ItemStack enriched3 = ItemMap.enrichWithNBT(enriched2, 1, Map.of("CustomModelData", 10001));
		mainItem.setItemMeta(enriched3.getItemMeta());
    } else {
		BookMeta meta = (BookMeta)mainItem.getItemMeta();
		if (meta.getPageCount() == 100)
		return; 
    } 

    FingerprintUtils.addFingerprintToBook(mainItem, offItem);
    ItemMap map = new ItemMap(offItem.asOne());
    map.removeSafelyFrom(playerInv);
    event.setCancelled(true);

	}

}
