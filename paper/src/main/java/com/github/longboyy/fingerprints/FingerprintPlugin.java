package com.github.longboyy.fingerprints;

import com.github.longboyy.fingerprints.commands.FingerprintCommandManager;
import com.github.longboyy.fingerprints.listeners.DustListener;
import com.github.longboyy.fingerprints.listeners.FingerprintListener;
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

public class FingerprintPlugin extends ACivMod {

	public static FingerprintPlugin instance(){
		return FingerprintPlugin.getInstance(FingerprintPlugin.class);
	}

	//private Logger logger;
	private FPContainerDAO dao;
	private FingerprintsConfig config;
	private FingerprintManager fingerprintManager;

	public static void log(String msg){
		instance().debug(msg);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		//this.logger = this.getLogger();
		this.config = new FingerprintsConfig(this);
		if(!this.config.parse()){
			this.disable();
			return;
		}
		this.dao = new FPContainerDAO(this.getLogger(), this.config.getDatabase());
		if(!dao.updateDatabase()){
			this.severe("Errors setting up database");
			this.disable();
			return;
		}
		dao.setBatchMode(true);
		BlockBasedChunkMetaView<FingerprintsChunkData, TableBasedDataObject, TableStorageEngine<FingerprintContainer>> chunkMetaData =
				ChunkMetaAPI.registerBlockBasedPlugin(this, () -> new FingerprintsChunkData(false, dao), dao, true);
		if(chunkMetaData == null){
			this.severe("Errors setting up chunk metadata API");
			this.disable();
			return;
		}
		fingerprintManager = new FingerprintManager(chunkMetaData);
		this.registerListener(new FingerprintListener(this));
		this.registerListener(new DustListener(this, fingerprintManager));

		new FingerprintCommandManager(this);
	}

	@Override
	public void onDisable() {
		super.onDisable();
		//dao.setBatchMode(true);
		fingerprintManager.shutDown();
		dao.cleanupBatches();
		HandlerList.unregisterAll(this);
	}

	public FingerprintsConfig config(){
		return this.config;
	}
}
