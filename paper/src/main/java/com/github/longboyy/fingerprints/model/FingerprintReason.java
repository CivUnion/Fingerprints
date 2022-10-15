package com.github.longboyy.fingerprints.model;

import com.github.longboyy.fingerprints.FingerprintPlugin;
import com.github.longboyy.fingerprints.util.ParticleUtils;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public enum FingerprintReason {

	// Created when someone voluntarily prints a fingerprint
	PRINTED("Printed", conf -> {
		Map<String, Object> result = new HashMap<>();
		if(conf != null){
		}

		return result;
	}, ParticleUtils::spawnDustParticle),

	// Trespassing in a vault bastion field may result in trespassing.
	TRESPASSING("Trespassing", conf -> {
		Map<String, Object> result = new HashMap<>();
		if(conf != null){
			result.put("bastion_types", conf.getStringList("bastion_types"));
			result.put("chance", conf.getDouble("chance", 0.05));
		}

		return result;
	}, ParticleUtils::spawnDustParticle),

	// Attempting to open a container (regardless of any locks) may result in rummaging
	/**
	 * NOTE: Perhaps instead there should be two configurable chances for if a container is locked or if it is unlocked?
	 */
	RUMMAGING("Rummaging", conf -> {
		Map<String, Object> result = new HashMap<>();
		if(conf != null){
			result.put("locked_chance", conf.getDouble("locked_chance", 0.07));
			result.put("unlocked_chance", conf.getDouble("unlocked_chance", 0.02));
		}

		return result;
	}, ParticleUtils::spawnDustParticle),

	// Attempting to take an item from a container, or take an item from an item frame or armor stand may result in theft.
	THEFT("Theft", conf -> {
		Map<String, Object> result = new HashMap<>();
		if(conf != null){
			result.put("locked_chance", conf.getDouble("locked_chance", 0.07));
			result.put("unlocked_chance", conf.getDouble("unlocked_chance", 0.02));
		}

		return result;
	}, ParticleUtils::spawnDustParticle),

	// Hitting another player or their pet may result in assault.
	ASSAULT("Assault", conf -> {
		Map<String, Object> result = new HashMap<>();
		if(conf != null){
			result.put("pvp_chance", conf.getDouble("pvp_chance", 0.1));
			result.put("pve_chance", conf.getDouble("pve_chance", 0.02));
			result.put("player_allowed_on_bastion_multiplier", conf.getDouble("player_allowed_on_bastion_multiplier", 0.8));
			result.put("ignore_bastion_for_pvp", conf.getBoolean("ignore_bastion_for_pvp", true));
		}

		return result;
	}, ParticleUtils::spawnDustParticle),

	// Killing another player or their pet may result in murder. (Always apply murder when player killed)
	MURDER("Murder", conf -> {
		Map<String, Object> result = new HashMap<>();
		if(conf != null){
			result.put("pvp_chance", conf.getDouble("pvp_chance", 1));
			result.put("pve_chance", conf.getDouble("pve_chance", 0.1));
			result.put("player_allowed_on_bastion_multiplier", conf.getDouble("player_allowed_on_bastion_multiplier", 0.8));
			result.put("ignore_bastion_for_pvp", conf.getBoolean("ignore_bastion_for_pvp", true));
		}

		return result;
	}, ParticleUtils::spawnDustParticle),

	// Breaking any block or bastion may result in vandalism.
	VANDALISM("Vandalism", conf -> {
		Map<String, Object> result = new HashMap<>();
		if(conf != null){
			result.put("bastion_break_chance", conf.getDouble("bastion_break_chance", 0.33));
			result.put("block_break_chance", conf.getDouble("block_break_chance", 0.05));
			result.put("ignore_break_if_on_bastion", conf.getBoolean("ignore_if_on_bastion", true));
		}

		return result;
	}, ParticleUtils::spawnDustParticle);

	protected final String prettyName;

	private final Function<ConfigurationSection, Map<String, Object>> settingFunc;
	private final Consumer<Location> creationParticleFunc;

	protected final String configKey;

	protected final Map<String, Object> settings = new HashMap<>();

	FingerprintReason(String prettyName, Function<ConfigurationSection, Map<String, Object>> settingFunc, Consumer<Location> creationParticleFunc){
		this.prettyName = prettyName;
		this.configKey = this.name().toLowerCase();
		this.settingFunc = settingFunc;
		this.creationParticleFunc = creationParticleFunc;
	}

	public void parseConfig(ConfigurationSection section){
		this.settings.clear();
		this.settings.putAll(this.settingFunc.apply(section));
	}

	public void playCreationParticle(Location loc){
		creationParticleFunc.accept(loc);
	}

	public <T> T getSetting(String key){
		if(!this.settings.containsKey(key)){
			return null;
		}

		Object rawValue = this.settings.get(key);

		try {
			@SuppressWarnings("unchecked")
			T value = (T) rawValue;
			return value;
		}catch(Exception e){
			return null;
		}
	}

	public <T> T getSetting(String key, T defaultValue){
		T value = this.getSetting(key);
		FingerprintPlugin.log(String.format("SETTING %s[%s]", key, value == null ? null : value.toString()));
		return value == null ? defaultValue : value;
	}

	public String getConfigKey(){
		return this.configKey;
	}

	public String getPrettyName(){
		return this.prettyName;
	}

}
