package com.github.longboyy.fingerprints.util.voronoi;

import net.minecraft.world.phys.Vec2;
import org.bukkit.util.Vector;

public class Site<T> {

	private final Vector pos;

	private final T value;

	public Site(Vector pos, T value){
		this.pos = pos;
		this.value = value;
	}

	public Vector getPos() {
		return pos;
	}

	public T getValue() {
		return value;
	}
}
