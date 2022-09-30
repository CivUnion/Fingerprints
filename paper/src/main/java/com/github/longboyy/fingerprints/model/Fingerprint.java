package com.github.longboyy.fingerprints.model;

import org.bukkit.Location;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableBasedDataObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Fingerprint {

	protected int id = -1;
	private final Location location;
	private final long createdAt;
	private final UUID playerId;
	private final FingerprintReason reason;
	private final Map<String, Object> metadata;

	public Fingerprint(int id, Location location, long creationTime, UUID playerId, FingerprintReason reason, Map<String, Object> metadata){
		//this(location, creationTime, playerId, true);
		this(location, creationTime, playerId, reason, metadata);
		this.id = id;
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
		long timeSinceMillis = this.createdAt - System.currentTimeMillis();
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
}
