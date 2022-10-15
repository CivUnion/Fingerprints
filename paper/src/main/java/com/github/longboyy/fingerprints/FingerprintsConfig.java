package com.github.longboyy.fingerprints;

import com.github.longboyy.fingerprints.model.FingerprintReason;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import oshi.util.tuples.Pair;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.config.ConfigHelper;
import vg.civcraft.mc.civmodcore.config.ConfigParser;
import vg.civcraft.mc.civmodcore.dao.DatabaseCredentials;
import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;
import vg.civcraft.mc.civmodcore.utilities.creative.CivCreativeManager;

import java.util.List;

public class FingerprintsConfig extends ConfigParser {

	private ManagedDatasource database;

	private int maxPrintsPerBlock = 5;
	private Pair<ItemStack, Integer> dusterItem = new Pair<>(new ItemStack(Material.WOODEN_SWORD), 1);
	private Pair<ItemStack, Integer> inkItem = new Pair<>(new ItemStack(Material.WOODEN_SWORD), 1);
	private ItemStack magnifyingGlassItem = new ItemStack(new ItemStack(Material.STICK));

	public FingerprintsConfig(FingerprintPlugin plugin) {
		super(plugin);
	}

	@Override
	protected boolean parseInternal(ConfigurationSection config) {
		if(this.database == null) {
			this.database = ManagedDatasource.construct((ACivMod) plugin, (DatabaseCredentials) config.get("database"));
		}


		this.maxPrintsPerBlock = config.getInt("max_prints_per_block", 5);

		ConfigurationSection reasonSection = config.getConfigurationSection("reason_settings");
		for(FingerprintReason reason : FingerprintReason.values()){
			ConfigurationSection section = null;
			if(reasonSection != null && reasonSection.isConfigurationSection(reason.getConfigKey())){
				section = reasonSection.getConfigurationSection(reason.getConfigKey());
			}
			reason.parseConfig(section);
		}

		//inkItem.getItemMeta()
		if(config.isConfigurationSection("items")){
			ConfigurationSection current = config.getConfigurationSection("items");

			ConfigurationSection dusterSection = current.getConfigurationSection("duster");
			List<ItemStack> dusterItems = ConfigHelper.parseItemMapDirectly(dusterSection).getItemStackRepresentation();
			if(dusterItems.size() != 0) {
				int uses = dusterSection.getInt("uses", 1);
				dusterItem = new Pair<>(dusterItems.get(0), uses);
			}

			ConfigurationSection inkSection = current.getConfigurationSection("ink");
			List<ItemStack> inkItems = ConfigHelper.parseItemMapDirectly(inkSection).getItemStackRepresentation();
			if(inkItems.size() != 0){
				int uses = inkSection.getInt("uses", 1);
				inkItem = new Pair<>(inkItems.get(0), uses);
			}

			ConfigurationSection magnifyingGlassSection = current.getConfigurationSection("magnifying_glass");
			List<ItemStack> magnifyingGlassItems = ConfigHelper.parseItemMapDirectly(magnifyingGlassSection).getItemStackRepresentation();
			if(magnifyingGlassItems.size() != 0){
				magnifyingGlassItem = magnifyingGlassItems.get(0);
			}
		}

		CivCreativeManager.register(
				NamespacedKey.fromString("fingerprint_duster", this.plugin),
				dusterItem.getA()
		);

		CivCreativeManager.register(
				NamespacedKey.fromString("inkwell", this.plugin),
				inkItem.getA()
		);

		CivCreativeManager.register(
				NamespacedKey.fromString("magnifying_glass", this.plugin),
				magnifyingGlassItem
		);

		return true;
	}

	public ManagedDatasource getDatabase() {
		return this.database;
	}

	public int getMaxPrintsPerBlock(){
		return this.maxPrintsPerBlock;
	}

	public ItemStack getDusterItem() {
		return this.dusterItem.getA();
	}

	public int getDusterUses(){
		return this.dusterItem.getB();
	}

	public ItemStack getInkItem() {
		return this.inkItem.getA();
	}

	public int getInkUses(){
		return this.inkItem.getB();
	}

	public ItemStack getMagnifyingGlassItem(){
		return this.magnifyingGlassItem;
	}
}
