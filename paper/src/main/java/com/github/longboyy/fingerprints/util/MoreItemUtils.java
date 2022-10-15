package com.github.longboyy.fingerprints.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import vg.civcraft.mc.civmodcore.inventory.items.MetaUtils;

public class MoreItemUtils {

	//public static Damageable damageOrConsume

	public static Damageable getDamageable(ItemStack item){
		return item.hasItemMeta() && item.getItemMeta() instanceof Damageable damageable ? damageable : null;
	}

	public static boolean areItemsSimilarIgnoreDura(ItemStack former, ItemStack latter){
		if (former == latter) {
			return true;
		}
		if ((former == null || latter == null)
				|| former.getType() != latter.getType()
				|| former.hasItemMeta() != latter.hasItemMeta()) {
			return false;
		}

		ItemMeta formerMeta = former.getItemMeta().clone();
		ItemMeta latterMeta = latter.getItemMeta().clone();
		if(formerMeta instanceof Damageable damageable){
			damageable.setDamage(0);
		}
		if(latterMeta instanceof Damageable damageable){
			damageable.setDamage(0);
		}

		return MetaUtils.areMetasEqual(formerMeta, latterMeta);
	}
}
