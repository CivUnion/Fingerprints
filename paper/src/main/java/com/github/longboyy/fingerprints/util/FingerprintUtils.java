package com.github.longboyy.fingerprints.util;

import com.destroystokyo.paper.ParticleBuilder;
import com.github.longboyy.fingerprints.FingerprintManager;
import com.github.longboyy.fingerprints.Fingerprints;
import com.github.longboyy.fingerprints.model.Fingerprint;
import com.github.longboyy.fingerprints.model.FingerprintContainer;
import com.github.longboyy.fingerprints.model.FingerprintReason;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import vg.civcraft.mc.civmodcore.particles.ParticleEffect;
import vg.civcraft.mc.civmodcore.world.WorldUtils;

import java.util.*;
import java.util.stream.Collectors;

public class FingerprintUtils {

	/*
	public static final ImmutableList<Material> HARDEST_MATERIALS = ImmutableList.of(
			Material.CRYING_OBSIDIAN,
			Material.OBSIDIAN,
			Material.RESPAWN_ANCHOR,
			Material.ANCIENT_DEBRIS
	);
	 */

	public static final float MAX_PARTICLE_OFFSET = 0.3f;
	public static final float MAX_PARTICLE_SPEED = 0.1f;

	private static Random RANDOM = new Random();

	public static List<Player> getPotentialObservers(Player player, int distance){
		List<Player> players = player.getNearbyEntities(distance, distance, distance).stream()
				.filter(entity -> {
					if(entity.getType() != EntityType.PLAYER){
						return false;
					}

					Player ply = (Player) entity;
					Vector dir = player.getLocation().toVector().subtract(ply.getEyeLocation().toVector()).normalize();

					BlockIterator it = new BlockIterator(ply.getWorld(), ply.getEyeLocation().toVector(), dir, 0, distance);
					boolean canSee = true;
					while(it.hasNext()){
						Block block = it.next();
						if((!block.isPassable() && !block.isLiquid()) || block.getType().isOccluding()){
							canSee = false;
							break;
						}
					}

					return canSee;
				})
				.map(entity -> (Player)entity)
				.collect(Collectors.toList());
		return players;
	}

	/**
	 * Check all sides of a block and return the first solid block found, or the input if none.
	 * @param location
	 * @return non-air block if found, or the block at the location.
	 */
	public static Location getClosestNonAir(Location location){
		return getClosestNonAir(location.getBlock());
	}


	/**
	 * Check all sides of a block and return the first solid block found, or the input if none.
	 * @param block
	 * @return non-air block if found, or the block passed in.
	 */
	public static Location getClosestNonAir(Block block){
		if(block.isSolid()){
			return block.getLocation();
		}

		for(BlockFace face : WorldUtils.ALL_SIDES){
			Block b = block.getRelative(face);
			if(b.isSolid()){
				return b.getLocation();
			}
		}

		return block.getLocation();
	}

	private final static ParticleEffect DUST_PARTICLE = new ParticleEffect(Particle.FLAME, 0, 0, 0, 0, 3);

	//private final static ParticleBuilder DUST_PARTICLE = new ParticleBuilder(Particle.FLAME)
	//			.count(3);

	public static void spawnDustParticle(Location loc){
		BukkitRunnable runnable = new BukkitRunnable() {
			int i = 0;
			@Override
			public void run() {
				if(i >= 10){
					this.cancel();
				}
				float randX = RANDOM.nextFloat() * 0.5f;
				float randZ = RANDOM.nextFloat() * 0.5f;
				new ParticleEffect(Particle.CRIT_MAGIC, randX, 0, randZ, 0.05f, 3).playEffect(loc.toCenterLocation().subtract(0, 0.5D, 0));
				//DUST_PARTICLE.location(loc.toCenterLocation()).offset(randOffset, randOffset, randOffset).spawn();
				i++;
			}
		};
		runnable.runTaskTimer(Fingerprints.getInstance(Fingerprints.class), 0L, 1L);
		/*
		Bukkit.getScheduler().scheduleSyncRepeatingTask(Fingerprints.getInstance(Fingerprints.class), new Runnable() {
			int i = 0;
			@Override
			public void run() {
				if(i >= 30){

				}
				double randOffset = RANDOM.nextDouble() * 0.5D;
				DUST_PARTICLE.location(loc).offset(randOffset, randOffset, randOffset).spawn();
			}
		}, 0L, 1L);
		 */
		/*
		for(int i=0; i<=3; i++){
			double randOffset = RANDOM.nextDouble() * 0.5D;
			DUST_PARTICLE.location(loc).offset(randOffset, randOffset, randOffset).spawn();
		}
		 */
	}

	public static Fingerprint addFingerprint(Location loc, Player player, FingerprintReason reason){
		return addFingerprint(loc, player, reason, new HashMap<>());
	}

	public static Fingerprint addFingerprint(Location loc, Player player, FingerprintReason reason, Map<String, Object> metadata){
		FingerprintContainer container = FingerprintManager.getInstance().getFingerprintContainer(loc);
		if(container == null){
			container = new FingerprintContainer(loc);
			FingerprintManager.getInstance().addFingerprintContainer(container);
		}
		Fingerprint fp = new Fingerprint(loc, System.currentTimeMillis(), player.getUniqueId(), reason);
		container.addFingerprint(fp);
		//ParticleEffect pe = new ParticleEffect(Particle.BLOCK_DUST, 0, 0 , 0, 0, 3);
		//pe.playEffect(loc);
		reason.playCreationParticle(loc);
		return fp;
	}

	public static void log(String msg){
		Fingerprints.getInstance(Fingerprints.class).getLogger().info(msg);
	}



	public static boolean checkChance(double chance){
		return RANDOM.nextDouble() <= chance;
	}
}
