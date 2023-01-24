package com.github.longboyy.fingerprints.util;

import com.github.longboyy.fingerprints.FingerprintPlugin;
import com.github.longboyy.fingerprints.model.Fingerprint;
import com.github.longboyy.fingerprints.model.FingerprintContainer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import oshi.util.tuples.Pair;
import vg.civcraft.mc.civmodcore.particles.ParticleEffect;

import java.util.*;

public class ParticleUtils {

	private static Random RANDOM = new Random();

	public static void displayParticlesForTicks(List<Pair<ParticleEffect, Location>> effectsAndLocs, long ticks, Player player){
		BukkitRunnable runnable = new BukkitRunnable() {
			int i = 0;

			@Override
			public void run() {
				for(Pair<ParticleEffect, Location> effectLocPair : effectsAndLocs){
					if(player == null){
						effectLocPair.getA().playEffect(effectLocPair.getB());
					}else{
						effectLocPair.getA().playEffect(effectLocPair.getB(), player);
					}
				}

				i++;

				if(i >= ticks){
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(FingerprintPlugin.instance(), 0L, 1L);
	}

	public static void displayParticleForTicks(ParticleEffect effect, Location loc, long ticks, Player player) {
		displayParticlesForTicks(Collections.singletonList(new Pair<>(effect, loc)), ticks, player);
	}

	public static void displayParticleForTicks(ParticleEffect effect, Location loc, long ticks) {
		displayParticleForTicks(effect, loc, ticks, null);
	}






	public static void spawnDustParticle(Location loc){

		Location particleLoc = loc.toCenterLocation().subtract(0, 0.45, 0);
		//ParticleEffect effect = new ParticleEffect(Particle.CRIT_MAGIC, 0, 0, 0, 0.05f, 3);
		BukkitRunnable runnable = new BukkitRunnable() {
			int i = 0;
			//ParticleEffect effect = new ParticleEffect(Particle.CRIT_MAGIC, 0, 0, 0, 0.05f, 3);
			@Override
			public void run() {

				float randX = RANDOM.nextFloat();
				float randZ = RANDOM.nextFloat();
				ParticleEffect effect = new ParticleEffect(Particle.CRIT_MAGIC, randX, 0, randZ, 0.05f, 3);
				effect.playEffect(particleLoc);
				i++;
				if(i >= 30){
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(FingerprintPlugin.instance(), 0L, 1L);
	}

	public static Vector transformToFace(Vector vector, BlockFace face){
		switch(face){
			case NORTH:
			case SOUTH:
				return new Vector(vector.getX(), vector.getZ(), 0);
			case EAST:
			case WEST:
				return new Vector(0, vector.getX(), -(vector.getZ()));
			case UP:
			case DOWN:
				return new Vector(vector.getX(), 0, -(vector.getZ()));
			default:
				return new Vector();
		}
	}

	private static Map<UUID, BukkitRunnable> activeProjections = new HashMap<>();
	public static void projectFingerprints(Player player, FingerprintContainer container, BlockFace face){

		Vector vec = transformToFace(new Vector(0.5f, 0, 0.5f), face);

		Location projectionOrigin = container.getLocation()
				.toCenterLocation()
				.add(face.getDirection().divide(new Vector(2,2,2)))
				.subtract(vec);

		if(activeProjections.containsKey(player.getUniqueId())){
			BukkitRunnable runnable = activeProjections.remove(player.getUniqueId());
			runnable.cancel();
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			int i = 0;
			List<Pair<Fingerprint, Location>> fpLocs = container.getFingerprints().stream()
					.sorted((former, latter) -> {
						if(former.getCreationTime() == latter.getCreationTime()){
							return 0;
						}else if(former.getCreationTime() < latter.getCreationTime()){
							return -1;
						}else {
							return 1;
						}
					})
					.map(fp -> new Pair<>(fp, projectionOrigin.clone().add(transformToFace(fp.getOffset(), face))))
					.toList();
			//ParticleEffect effect = new ParticleEffect(Particle.ELECTRIC_SPARK, 0, 0, 0, 0f, 1);
			@Override
			public void run() {

				/*
				for(int count=0; count<fpLocs.size(); count++){
					ParticleEffect customEffect = new ParticleEffect(Particle.ELECTRIC_SPARK,
							RANDOM.nextFloat()*0.001f,
							0f,
							RANDOM.nextFloat()*0.001f,
							0,
							count+1);
					customEffect.playEffect(fpLocs.get(count), player);
				}
				 */

				//fpLocs.forEach(loc -> effect.playEffect(loc.getB(), player));
				fpLocs.forEach(pair -> pair.getA().getReason().playDisplayParticle(pair.getB(), player));

				i++;

				if(i >= 60){
					activeProjections.remove(player.getUniqueId());
					this.cancel();
				}
			}
		};

		activeProjections.put(player.getUniqueId(), runnable);
		runnable.runTaskTimer(FingerprintPlugin.getInstance(FingerprintPlugin.class), 0L, 1L);
	}
}
