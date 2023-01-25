package com.github.longboyy.fingerprints.model;

import com.github.longboyy.fingerprints.util.FingerprintUtils;
import com.github.longboyy.fingerprints.FingerprintPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.inventory.items.MetaUtils;
import vg.civcraft.mc.civmodcore.nbt.NBTSerialization;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableBasedDataObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class Fingerprint {

	private static SimpleDateFormat FORMATTER = new SimpleDateFormat("EEE, MMM d, ''yy");

	protected int id = -1;
	protected Vector offset;
	private final Location location;
	private final long createdAt;
	private final UUID playerId;
	private final FingerprintReason reason;
	private final Map<String, Object> metadata;

	public Fingerprint(int id, Location location, long creationTime, UUID playerId, FingerprintReason reason, Vector offset, Map<String, Object> metadata){
		//this(location, creationTime, playerId, true);
		this(location, creationTime, playerId, reason, metadata);
		this.id = id;
		this.offset = offset;
	}

	public Fingerprint(Location location, long creationTime, UUID playerId, FingerprintReason reason){
		//this(location, creationTime, playerId, true);
		this(location, creationTime, playerId, reason, new HashMap<>());
	}

	public Fingerprint(Location location, long creationTime, UUID playerId, FingerprintReason reason, Map<String, Object> metadata){
		this.location = location;
		this.createdAt = creationTime;
		this.playerId = playerId;
		this.reason = reason;
		this.metadata = metadata;
	}

	public int getId(){
		return this.id;
	}

	/*
	public boolean setId(int id){
		if(this.id == -1 && id > 0){
			this.id = id;
			return true;
		}

		return false;
	}
	 */

	public Vector getOffset(){
		return this.offset.clone();
	}

	public Map<String, Object> getMetadata(){
		return this.metadata;
	}

	public <T> T getMetadata(String key){
		return (T) this.metadata.get(key);
	}

	public Location getLocation(){
		return this.location;
	}

	public FingerprintReason getReason(){
		return this.reason;
	}

	public long getCreationTime(){
		return this.createdAt;
	}

	public UUID getPlayerId(){
		return this.playerId;
	}

	public String getVagueTime(){
		long timeSinceMillis = System.currentTimeMillis() - this.createdAt;
		float timeSinceSecs = timeSinceMillis / 1000L;
		// 15 mins
		if(timeSinceSecs <= 900){
			return "Very recent";
		// 1 hour
		}else if(timeSinceSecs <= 3600) {
			return "Recent";
		// 6 hours
		}else if(timeSinceSecs <= 21600) {
			return "Somewhat recent";
		// 24 hours
		}else if(timeSinceSecs <= 86400){
			return "Today";
		}else{
			return "More than a day ago";
		}
	}

	public ItemStack asItem(){
		String collectDate = FORMATTER.format(new Date());
		ItemStack itemStack = new ItemStack(Material.PAPER);
		UUID fpUUID = UUID.nameUUIDFromBytes((getPlayerId().toString() + ".test").getBytes());
		List<Component> lore = new ArrayList<>();
		lore.add(Component.text("Fingerprint"));
		lore.add(Component.text(fpUUID.toString()));
		lore.add(Component.empty());
		lore.add(Component.text("Location:"));
		Location loc = getLocation();
		lore.add(Component.text(String.format("X: %d, Y: %d, Z: %d", new Object[] { Integer.valueOf(loc.getBlockX()), Integer.valueOf(loc.getBlockY()), Integer.valueOf(loc.getBlockZ()) })));
		lore.add(Component.text(String.format("Collected on: %s", new Object[] { collectDate })));
		lore.add(Component.text(String.format("Created: %s", new Object[] { getVagueTime() })));
		lore.add(Component.text(String.format("Reason: %s", new Object[] { getReason().getPrettyName() })));
		ItemUtils.setComponentDisplayName(itemStack, Component.text("Fingerprint").color(TextColor.color(175, 175, 175)));
		ItemUtils.setComponentLore(itemStack, lore);

		Integer fpCustomID = FingerprintPlugin.instance().config().getBookCustomModelData();

		itemStack = ItemMap.enrichWithNBT(itemStack, 1, Map.of(FingerprintUtils.FP_NBT_TAG_KEY, true));
		itemStack = ItemMap.enrichWithNBT(itemStack, 1, Map.of("FingerprintOwner", fpUUID.toString()));
		itemStack = ItemMap.enrichWithNBT(itemStack, 1, Map.of("CustomModelData", fpCustomID));
		return itemStack;
	}
}