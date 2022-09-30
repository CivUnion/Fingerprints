package com.github.longboyy.fingerprints.model;

import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableBasedBlockChunkMeta;

public class FingerprintsChunkData extends TableBasedBlockChunkMeta<FingerprintContainer> {
	public FingerprintsChunkData(boolean isNew, FPContainerDAO storage) {
		super(isNew, storage);
	}
}
