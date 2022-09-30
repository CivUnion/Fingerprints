package com.github.longboyy.fingerprints.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import com.github.longboyy.fingerprints.FingerprintManager;
import com.github.longboyy.fingerprints.model.Fingerprint;
import com.github.longboyy.fingerprints.model.FingerprintContainer;
import com.google.common.base.Splitter;
import com.google.common.hash.Hashing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
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
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.utilities.MoreStringUtils;

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

public class DustCommand extends BaseCommand {

	private static SimpleDateFormat FORMATTER = new SimpleDateFormat("EEE, MMM d, ''yy");



	@CommandAlias("dust")
	@Description("Dusts an area around the player for fingerprints")
	public void execute(Player player){
		List<Block> blocks = getBlocks(player.getLocation().getBlock(), 4);
		List<Fingerprint> fingerprints = new ArrayList<>();
		for(Block block : blocks){
			FingerprintContainer fpContainer = FingerprintManager.getInstance().getFingerprintContainer(block);

			if(fpContainer == null){
				continue;
			}
			/*
			if(fp == null || fp.getPlayerId() == null){
				continue;
			}
			 */
			fingerprints.addAll(fpContainer.getFingerprints());

			//fingerprints.add(fp);
		}

		//Date collectDate = new Date();

		String collectDate = FORMATTER.format(new Date());

		for(Fingerprint fingerprint : fingerprints){
			ItemStack itemStack = new ItemStack(Material.PAPER);
			//String hashString = Hashing.sha256().hashString(fingerprint.getPlayerId().toString()+".test", StandardCharsets.UTF_8).toString();
			UUID fpUUID = UUID.nameUUIDFromBytes((fingerprint.getPlayerId().toString() + ".test").getBytes());
			List<Component> lore = new ArrayList<>();
			//Splitter.fixedLength(16).split(hashString).forEach(str -> lore.add(Component.text(str)));
			lore.add(Component.text(fpUUID.toString()));
			lore.add(Component.empty());
			lore.add(Component.text("Location:"));
			Location loc = fingerprint.getLocation();
			lore.add(Component.text(String.format("X: %d, Y: %d, Z: %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())));
			//Instant.now();
			lore.add(Component.text(String.format("Collected on: %s", collectDate)));
			lore.add(Component.text(String.format("Created: %s", fingerprint.getVagueTime())));
			lore.add(Component.text(String.format("Reason: %s", fingerprint.getReason().getPrettyName())));
			ItemUtils.setComponentDisplayName(itemStack, Component.text("Fingerprint").color(TextColor.color(175,175,175)));
			ItemUtils.setComponentLore(itemStack, lore);
			player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
		}
	}

	private static List<Block> getBlocks(Block start, int radius){
		if (radius < 0) {
			return new ArrayList<Block>(0);
		}
		int iterations = (radius * 2) + 1;
		List<Block> blocks = new ArrayList<Block>(iterations * iterations * iterations);
		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				for (int z = -radius; z <= radius; z++) {
					blocks.add(start.getRelative(x, y, z));
				}
			}
		}
		return blocks;
	}

}
