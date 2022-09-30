package com.github.longboyy.fingerprints;

import com.github.longboyy.fingerprints.model.Fingerprint;
import com.github.longboyy.fingerprints.model.FingerprintContainer;
import com.github.longboyy.fingerprints.model.FingerprintsChunkData;
import org.bukkit.Location;
import org.bukkit.block.Block;
import vg.civcraft.mc.civmodcore.utilities.MoreClassUtils;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.api.BlockBasedChunkMetaView;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableBasedDataObject;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableStorageEngine;

public class FingerprintManager {

	private static FingerprintManager instance = null;

	private BlockBasedChunkMetaView<FingerprintsChunkData, TableBasedDataObject, TableStorageEngine<FingerprintContainer>> chunkMetaData;

	public FingerprintManager(BlockBasedChunkMetaView<FingerprintsChunkData, TableBasedDataObject, TableStorageEngine<FingerprintContainer>> chunkMetaData){
		if(instance != null){
			throw new RuntimeException("Failed to instantiate a new FingerprintManager as a singleton already exists!");
		}
		instance = this;
		this.chunkMetaData = chunkMetaData;
	}

	public static FingerprintManager getInstance() {
		return instance;
	}

	public FingerprintContainer getFingerprintContainer(Location location){
		return MoreClassUtils.castOrNull(FingerprintContainer.class, chunkMetaData.get(location));
	}

	public FingerprintContainer getFingerprintContainer(Block block){
		return MoreClassUtils.castOrNull(FingerprintContainer.class, chunkMetaData.get(block));
	}

	public void addFingerprintContainer(FingerprintContainer container){
		chunkMetaData.put(container);
	}

	public void removeFingerprintContainer(FingerprintContainer container){
		chunkMetaData.remove(container);
	}

	public void shutDown(){
		this.chunkMetaData.disable();
	}
}
