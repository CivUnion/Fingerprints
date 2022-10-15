package com.github.longboyy.fingerprints.util.voronoi;

import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Voronoi<T> {
	private final Set<Site<T>> sites = new HashSet<>();

	public Voronoi(){
		this(null);
	}

	public Voronoi(Collection<Site<T>> sites){
		if(sites != null){
			this.sites.addAll(sites);
		}
	}

	public void addSite(Site<T> site){
		if(site == null){
			return;
		}

		this.sites.add(site);
	}

	public void addSites(Collection<Site<T>> sites){
		if(sites == null){
			return;
		}

		this.sites.addAll(sites.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
	}

	public Site<T> getSiteAt(Vector pos){

		Site<T> closestSite = null;
		double closestDistance = -1D;
		for(Site<T> site : this.sites){
			if(closestSite == null){
				closestSite = site;
				closestDistance = site.getPos().distanceSquared(pos);
				continue;
			}

			double dist = site.getPos().distanceSquared(pos);
			if(closestDistance > dist){
				closestSite = site;
				closestDistance = dist;
			}
		}

		return closestSite;
	}

	public T getValueAt(Vector pos){
		Site<T> site = this.getSiteAt(pos);
		return site != null ? site.getValue() : null;
	}
}
