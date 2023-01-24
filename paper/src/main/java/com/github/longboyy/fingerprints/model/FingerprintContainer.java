package com.github.longboyy.fingerprints.model;

import com.github.longboyy.fingerprints.FingerprintPlugin;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import oshi.util.tuples.Pair;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.CacheState;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableBasedDataObject;

import java.util.*;

public class FingerprintContainer extends TableBasedDataObject {

	protected int id = -1;

	protected List<Fingerprint> fingerprints;

	protected Queue<Fingerprint> deletions = new ArrayDeque<>();

	protected Queue<Fingerprint> inserts = new ArrayDeque<>();

	public FingerprintContainer(Location location){
		this(-1, location, true, new ArrayList<>());
		//this.state = CacheState.MODIFIED
		//this.setCacheState();
	}

	public FingerprintContainer(int id, Location location, boolean isNew){
		this(id, location, isNew, new ArrayList<>());
	}

	public FingerprintContainer(int id, Location location, boolean isNew, List<Fingerprint> fingerprints) {
		super(location, isNew);
		this.id = id;
		this.fingerprints = fingerprints;
		//this.setDirty();
	}

	public List<Fingerprint> getFingerprints(){
		return fingerprints;
	}

	public Queue<Fingerprint> getDeletions(){
		return deletions;
	}


	public void addFingerprint(Fingerprint fingerprint){
		if(fingerprint == null){
			FingerprintPlugin.log("FingerprintContainer::addFingerprint - Fingerprint was null");
			return;
		}

		//FingerprintPlugin.log("FingerprintContainer::addFingerprint - START");

		int maxPrints = FingerprintPlugin.getInstance(FingerprintPlugin.class).config().getMaxPrintsPerBlock();
		FingerprintPlugin.log(String.format("FingerprintContainer::addFingerprint - Size: %s, Max Size: %s, Result: %s", fingerprints.size(), maxPrints, fingerprints.size() >= maxPrints));
		if(fingerprints.size() >= maxPrints){
			Fingerprint fp = fingerprints.remove(0);
			FingerprintPlugin.log(String.format("FingerprintContainer::addFingerprint - Queueing fingerprint with id %s for deletion", fp.id));
			deletions.add(fp);
		}


		{
			Vector generatedOffset = null;

			double safetyPadding = Math.pow((1D / Math.pow(maxPrints, 1.5D)), 1.2D);

			List<Pair<Vector, Integer>> rejections = new ArrayList<>();
			for (int iterations = 0; iterations <= 20; iterations++) {
				Vector newVec = Vector.getRandom();
				int conflicts = (int) this.fingerprints.stream()
						.filter(fp -> fp.getOffset() != null && fp.getOffset().distanceSquared(newVec) <= safetyPadding)
						.count();
				if (conflicts == 0) {
					generatedOffset = newVec;
					break;
				} else {
					rejections.add(new Pair<>(newVec, conflicts));
				}
			}

			if (generatedOffset == null) {
				rejections.sort((former, latter) -> {
					if (former.getB() < latter.getB()) {
						return -1;
					} else if (former.getB().equals(latter.getB())) {
						return 0;
					} else {
						return 1;
					}
				});
				fingerprint.offset = rejections.get(0).getA();
			} else {
				fingerprint.offset = generatedOffset;
			}
		}

		inserts.add(fingerprint);
		//FingerprintPlugin.log("FingerprintContainer::addFingerprint - Added fingerprint to inserts");
		fingerprints.add(fingerprint);
		//FingerprintPlugin.log("FingerprintContainer::addFingerprint - Added fingerprint to container");
		this.setCacheState(CacheState.MODIFIED);
		this.getOwningCache().insert();
		//this.getOwningCache().insert();
		//FingerprintPlugin.log("FingerprintContainer::addFingerprint - FINISH");
	}

	public void removeFingerprint(Fingerprint fingerprint){
		if(fingerprints.contains(fingerprint)){
			fingerprints.remove(fingerprint);
			deletions.add(fingerprint);
			if(fingerprints.size() == 0){
				this.setCacheState(CacheState.DELETED);
			}else{
				this.setCacheState(CacheState.MODIFIED);
			}
			//this.setDirty();
		}
	}

}
