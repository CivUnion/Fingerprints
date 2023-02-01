package com.github.longboyy.fingerprints.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;

import com.github.longboyy.fingerprints.FingerprintManager;
import com.github.longboyy.fingerprints.model.Fingerprint;
import com.github.longboyy.fingerprints.model.FingerprintContainer;
import com.github.longboyy.fingerprints.util.FingerprintUtils;
import com.github.longboyy.fingerprints.FingerprintPlugin;
import com.github.longboyy.fingerprints.model.FingerprintReason;
import com.github.longboyy.fingerprints.util.MoreItemUtils;
import com.github.longboyy.fingerprints.util.ParticleUtils;
import com.github.longboyy.fingerprints.util.voronoi.Site;
import com.github.longboyy.fingerprints.util.voronoi.Voronoi;
import com.github.longboyy.fingerprints.listeners.DustListener;

import com.google.common.base.Splitter;
import com.google.common.hash.Hashing;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.apache.commons.lang.WordUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.StringUtil;
import org.bukkit.util.Vector;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.utilities.MoreStringUtils;
import vg.civcraft.mc.civmodcore.nbt.NBTSerialization;
import vg.civcraft.mc.civmodcore.nbt.wrappers.NBTCompound;
import vg.civcraft.mc.civmodcore.utilities.cooldowns.TickCoolDownHandler;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Arrays;

public class CopyPrintCommand extends BaseCommand {

    @CommandAlias("copy")
    @Syntax("[page]")
    @Description("Copies a fingerprint from given page of fingerprint book")
    private void execute(Player player, String[] args){
        if(args.length == 0){
            player.sendMessage(
					Component.text("Missing page number, denote which page of the book you want to copy")
							.color(TextColor.color(255, 0, 0)));
        }

        PlayerInventory inv = player.getInventory();
        ItemStack book = inv.getItemInMainHand();
        
        if (!FingerprintUtils.isFingerprintBook(book)) {
                player.sendMessage(
					Component.text("Copying failed! You are not holding a fingerprint book in your main hand")
							.color(TextColor.color(255, 0, 0)));
        }
        
        NBTCompound mainNBT = NBTSerialization.fromItem(book);
        BookMeta meta = (BookMeta)book.getItemMeta();
        int page = Integer.parseInt(args[0])-1;

        if (page > meta.getPageCount() || page < 0) {
            player.sendMessage(
					Component.text("Copying failed! You denoted a page that doesn't exist")
							.color(TextColor.color(255, 0, 0)));
        }

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
			int inkDamage = (inkItem.getType().getMaxDurability()/FingerprintPlugin.instance().config().getInkUses());
			inkDamageable.setDamage(inkDamageable.getDamage() + inkDamage);
			FingerprintPlugin.log(String.format("Ink damage: %s", inkDamage));
		}else{
			inkMap = new ItemMap(FingerprintPlugin.instance().config().getInkItem());
			inkMap.multiplyContent(FingerprintPlugin.instance().config().getInkUses());
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

        //Everything above is the stuff to make sure the right items are in the inventory
        UUID copyFpUUID = UUID.fromString(mainNBT.getString("FingerprintOwner"));

		String[] locStr = mainNBT.getStringArray("savedLocArr");
		String[] locStrArr = locStr[page].split(",");
		double[] locArr = new double[3];

		for (int i = 0; i<3; i++) {
			locArr[i] = Integer.parseInt(locStrArr[i]);
		}
		Location copyLoc = new Location(player.getWorld(), locArr[0], locArr[1], locArr[2]);


		long[] crtTimeArr = mainNBT.getLongArray("crtTimeArr");
		long fpCreationTime = crtTimeArr[page];


		Location loc = player.getLocation();
		Fingerprint fp = new Fingerprint(copyLoc, fpCreationTime, copyFpUUID, FingerprintReason.PRINTED);
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
				.filter(is -> MoreItemUtils.areItemsSimilarIgnoreDura(is, FingerprintPlugin.instance().config().getInkItem()))
				.toList();

		return inkItems.get(0);
	}
}
