package com.github.longboyy.fingerprints.model;

import com.github.longboyy.fingerprints.Fingerprints;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.bukkit.Location;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.CacheState;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableBasedDataObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

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
			return;
		}

		int maxPrints = Fingerprints.getInstance(Fingerprints.class).config().getMaxPrintsPerBlock();
		if(fingerprints.size() >= maxPrints){
			Fingerprint fp = fingerprints.remove(0);
			deletions.add(fp);
		}

		fingerprints.add(fingerprint);
		inserts.add(fingerprint);
		this.setCacheState(CacheState.MODIFIED);
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
			this.setDirty();
		}
	}

}
