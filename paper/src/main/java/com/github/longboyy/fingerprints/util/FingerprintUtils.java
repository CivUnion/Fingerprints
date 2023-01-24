package com.github.longboyy.fingerprints.util;

import com.github.longboyy.fingerprints.FingerprintManager;
import com.github.longboyy.fingerprints.FingerprintPlugin;
import com.github.longboyy.fingerprints.model.Fingerprint;
import com.github.longboyy.fingerprints.model.FingerprintContainer;
import com.github.longboyy.fingerprints.model.FingerprintReason;
import net.kyori.adventure.text.Component;
import net.minecraft.nbt.CompoundTag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.inventory.items.MetaUtils;
import vg.civcraft.mc.civmodcore.nbt.NBTSerialization;
import vg.civcraft.mc.civmodcore.nbt.wrappers.NBTCompound;
import vg.civcraft.mc.civmodcore.world.WorldUtils;

import java.awt.print.Book;
import java.util.*;
import java.util.stream.Collectors;

public class FingerprintUtils {

	private static Random RANDOM = new Random();

	public static List<Player> getPotentialObservers(Player player, int distance){
		List<Player> players = player.getNearbyEntities(distance, distance, distance).stream()
				.filter(entity -> {
					if(entity.getType() != EntityType.PLAYER){
						return false;
					}

					Player ply = (Player) entity;
					Vector dir = player.getLocation().toVector().subtract(ply.getEyeLocation().toVector()).normalize();

					BlockIterator it = new BlockIterator(ply.getWorld(), ply.getEyeLocation().toVector(), dir, 0, distance);
					boolean canSee = true;
					while(it.hasNext()){
						Block block = it.next();
						if((!block.isPassable() && !block.isLiquid()) || block.getType().isOccluding()){
							canSee = false;
							break;
						}
					}

					return canSee;
				})
				.map(entity -> (Player)entity)
				.collect(Collectors.toList());
		return players;
	}

	/**
	 * Check all sides of a block and return the first solid block found, or the input if none.
	 * @param location
	 * @return non-air block if found, or the block at the location.
	 */
	public static Location getClosestNonAir(Location location){
		return getClosestNonAir(location.getBlock());
	}


	/**
	 * Check all sides of a block and return the first solid block found, or the input if none.
	 * @param block
	 * @return non-air block if found, or the block passed in.
	 */
	public static Location getClosestNonAir(Block block){
		if(block.isSolid()){
			return block.getLocation();
		}

		for(BlockFace face : WorldUtils.ALL_SIDES){
			Block b = block.getRelative(face);
			if(b.isSolid()){
				return b.getLocation();
			}
		}

		return block.getLocation();
	}


	public static ItemStack bookTest(){
		ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta meta = (BookMeta) book.getItemMeta();
		meta.setTitle("Rawr XD");
		meta.addPages(Component.text("i'm losing my mind"));

		meta.setAuthor("Some person");
		book.setItemMeta(meta);

		return book;
	}

	public static final String FP_NBT_TAG_KEY = "fingerprint";
	public static boolean isFingerprint(ItemStack item){
		final NBTCompound tag = NBTSerialization.fromItem(item);
		return tag != null && tag.hasKey(FP_NBT_TAG_KEY) && tag.getBoolean(FP_NBT_TAG_KEY);
	}

	public static final String FP_BOOK_NBT_TAG_KEY = "fingerprint_book";
	public static boolean isFingerprintBook(ItemStack item){
		final NBTCompound tag = NBTSerialization.fromItem(item);
		return tag != null && tag.hasKey(FP_BOOK_NBT_TAG_KEY) && tag.getBoolean(FP_BOOK_NBT_TAG_KEY);
	}

	public static void addFingerprintToBook(ItemStack book, ItemStack item){
		BookMeta meta = (BookMeta) book.getItemMeta();
		Component comps = Component.empty();
		for(Component comp : MetaUtils.getComponentLore(item.getItemMeta())){
			comps = comps.append(comp).append(Component.newline());
		}
		meta.addPages(comps);
		book.setItemMeta(meta);
	}

	public static Fingerprint addFingerprint(Location loc, Player player, FingerprintReason reason){
		return addFingerprint(loc, player, reason, new HashMap<>());
	}

	public static Fingerprint addFingerprint(Location loc, Player player, FingerprintReason reason, Map<String, Object> metadata){
		//FingerprintPlugin.log("addFingerprint - START");
		FingerprintContainer container = FingerprintManager.getInstance().getFingerprintContainer(loc);
		if(container == null){
			FingerprintPlugin.log(String.format("Container was null[%s], creating a new one", loc.toString()));
			container = new FingerprintContainer(loc);
			FingerprintManager.getInstance().addFingerprintContainer(container);
		}
		//FingerprintPlugin.log("Creating the fingerprint");
		Fingerprint fp = new Fingerprint(loc, System.currentTimeMillis(), player.getUniqueId(), reason, metadata);
		container.addFingerprint(fp);
		//FingerprintPlugin.log("Added fingerprint to container");
		//ParticleEffect pe = new ParticleEffect(Particle.BLOCK_DUST, 0, 0 , 0, 0, 3);
		//pe.playEffect(loc);
		reason.playCreationParticle(loc);
		//FingerprintPlugin.log("addFingerprint - FINISH");
		return fp;
	}


	public static boolean checkChance(double chance){
		double rand = RANDOM.nextDouble();
		FingerprintPlugin.log(String.format("Chance check[%s<=%s|%s]", rand, chance, rand <= chance));
		return rand <= chance;
	}

}
