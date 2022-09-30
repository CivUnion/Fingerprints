package com.github.longboyy.fingerprints;

import com.github.longboyy.fingerprints.commands.FingerprintCommandManager;
import com.github.longboyy.fingerprints.listeners.PlayerListener;
import com.github.longboyy.fingerprints.model.Fingerprint;
import com.github.longboyy.fingerprints.model.FingerprintContainer;
import com.github.longboyy.fingerprints.model.FingerprintsChunkData;
import com.github.longboyy.fingerprints.model.FPContainerDAO;
import org.bukkit.event.HandlerList;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.api.BlockBasedChunkMetaView;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.api.ChunkMetaAPI;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableBasedDataObject;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableStorageEngine;

import java.util.logging.Logger;

public class Fingerprints extends ACivMod {

	private Logger logger;
	private FPContainerDAO dao;
	private FingerprintsConfig config;
	private FingerprintManager fingerprintManager;

	@Override
	public void onEnable() {
		super.onEnable();
		this.logger = this.getLogger();
		this.config = new FingerprintsConfig(this);
		this.saveDefaultConfig();
		if(!this.config.parse()){
			this.disable();
			return;
		}
		this.dao = new FPContainerDAO(this.getLogger(), this.config.getDatabase());
		if(!dao.updateDatabase()){
			logger.severe("Errors setting up database");
			this.disable();
			return;
		}
		BlockBasedChunkMetaView<FingerprintsChunkData, TableBasedDataObject, TableStorageEngine<FingerprintContainer>> chunkMetaData =
				ChunkMetaAPI.registerBlockBasedPlugin(this, () -> new FingerprintsChunkData(false, dao), dao, true);
		if(chunkMetaData == null){
			logger.severe("Errors setting up chunk metadata API");
			this.disable();
			return;
		}
		fingerprintManager = new FingerprintManager(chunkMetaData);
		this.registerListener(new PlayerListener(this));

		new FingerprintCommandManager(this);
		//Hashing.sha256().hashString("", StandardCharsets.UTF_8).toString();
	}

	@Override
	public void onDisable() {
		super.onDisable();
		dao.setBatchMode(true);
		fingerprintManager.shutDown();
		dao.cleanupBatches();
		HandlerList.unregisterAll(this);
	}

	public FingerprintsConfig config(){
		return this.config;
	}
}
