package com.github.longboyy.fingerprints;

import com.github.longboyy.fingerprints.model.FingerprintReason;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.config.ConfigParser;
import vg.civcraft.mc.civmodcore.dao.DatabaseCredentials;
import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;
import vg.civcraft.mc.civmodcore.particles.ParticleEffect;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class FingerprintsConfig extends ConfigParser {

	private static final String DATABASE_KEY = "database";

	//private final Map<String, Object> configOptions = new HashMap<>();

	private ManagedDatasource database;

	private int maxPrintsPerBlock = 5;

	public FingerprintsConfig(Fingerprints plugin) {
		super(plugin);
	}

	/*
	public <T> T getOption(String key){
		if(!this.configOptions.containsKey(key)){
			return null;
		}

		Object rawValue = this.configOptions.get(key);

		try {
			@SuppressWarnings("unchecked")
			T value = (T) rawValue;
			return value;
		}catch(Exception e){
			return null;
		}
	}

	public <T> T getOption(String key, T defaultValue){
		T value = this.getOption(key);
		return value == null ? defaultValue : value;
	}
	 */

	@Override
	protected boolean parseInternal(ConfigurationSection config) {
		if(this.database == null) {
			this.database = ManagedDatasource.construct((ACivMod) plugin, (DatabaseCredentials) config.get(DATABASE_KEY));
		}


		this.maxPrintsPerBlock = config.getInt("max_prints_per_block", 5);

		ConfigurationSection reasonSection = config.getConfigurationSection("reason_settings");
		for(FingerprintReason reason : FingerprintReason.values()){
			ConfigurationSection section = null;
			if(reasonSection != null && reasonSection.isConfigurationSection(reason.getConfigKey())){
				section = reasonSection.getConfigurationSection(reason.getConfigKey());
			}
			reason.parseConfig(section);
		}
		return true;
	}

	public ManagedDatasource getDatabase() {
		return this.database;
	}

	public int getMaxPrintsPerBlock(){
		return this.maxPrintsPerBlock;
	}
}
