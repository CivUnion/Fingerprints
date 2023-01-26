package com.github.longboyy.fingerprints;

import com.github.longboyy.fingerprints.model.FingerprintReason;
import com.github.longboyy.fingerprints.util.FingerprintUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import oshi.util.tuples.Pair;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.config.ConfigHelper;
import vg.civcraft.mc.civmodcore.config.ConfigParser;
import vg.civcraft.mc.civmodcore.dao.DatabaseCredentials;
import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;
import vg.civcraft.mc.civmodcore.utilities.ItemManager;

import java.util.List;

public class FingerprintsConfig extends ConfigParser {

	private ManagedDatasource database;

	private int maxPrintsPerBlock = 5;
	private Pair<ItemStack, Integer> dusterItem = new Pair<>(new ItemStack(Material.WOODEN_SWORD), 1);
	private Pair<ItemStack, Integer> inkItem = new Pair<>(new ItemStack(Material.WOODEN_SWORD), 1);
	private ItemStack magnifyingGlassItem = new ItemStack(new ItemStack(Material.STICK));

	private int bookCustomModelData = -1;
	private int fingerprintCustomModelData = -1;

	private Particle volunEffect;
	private Particle trespassEffect;
	private Particle rummageEffect;
	private Particle theftEffect;
	private Particle assaultEffect;
	private Particle murderEffect;
	private Particle vandaEffect;

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

		if(config.isConfigurationSection("custom_model_ids")){
			ConfigurationSection modelIDs = config.getConfigurationSection("custom_model_ids");
			Integer fpID = modelIDs.getInt("fingerprint");
			Integer bookID = modelIDs.getInt("fingerprint_book");
			if(fpID > -1){
				this.fingerprintCustomModelData = fpID;
			}
			if(bookID > -1){
				this.bookCustomModelData = bookID;
			}
		}

		if(config.isConfigurationSection("effects")){
			ConfigurationSection effectSect = config.getConfigurationSection("effects");

			Integer volunEffectNum = effectSect.getInt("volunEffect");
			Particle volunEffect = Particle.values()[volunEffectNum];
			
			Integer trespassEffectNum = effectSect.getInt("trespassEffect");
			Particle trespassEffect = Particle.values()[trespassEffectNum];

			Integer rummageEffectNum = effectSect.getInt("rummageEffect");
			Particle rummageEffect = Particle.values()[rummageEffectNum];

			Integer theftEffectNum = effectSect.getInt("theftEffect");
			Particle theftEffect = Particle.values()[theftEffectNum];

			Integer assaultEffectNum = effectSect.getInt("assaultEffect");
			Particle assaultEffect = Particle.values()[assaultEffectNum];

			Integer murderEffectNum = effectSect.getInt("murderEffect");
			Particle murderEffect = Particle.values()[murderEffectNum];

			Integer vandaEffectNum = effectSect.getInt("vandaEffect");
			Particle vandaEffect = Particle.values()[vandaEffectNum];

			this.volunEffect = volunEffect;
			this.trespassEffect = trespassEffect;
			this.rummageEffect = rummageEffect;
			this.theftEffect = theftEffect;
			this.assaultEffect = assaultEffect;
			this.murderEffect = murderEffect;
			this.vandaEffect = vandaEffect;
		}

		ItemManager.register(
				NamespacedKey.fromString("fingerprint_duster", this.plugin),
				dusterItem.getA()
		);

		ItemManager.register(
				NamespacedKey.fromString("inkwell", this.plugin),
				inkItem.getA()
		);

		ItemManager.register(
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

	public int getBookCustomModelData(){
		return this.bookCustomModelData;
	}

	public int getFingerprintCustomModelData(){
		return this.fingerprintCustomModelData;
	}

	public Particle getVolunEffect(){
		return this.volunEffect;
	}

	public Particle getTrespassEffect(){
		return this.trespassEffect;
	}

	public Particle getRummageEffect(){
		return this.rummageEffect;
	}

	public Particle getTheftEffect(){
		return this.theftEffect;
	}

	public Particle getAssaultEffect(){
		return this.assaultEffect;
	}

	public Particle getMurderEffect(){
		return this.murderEffect;
	}

	public Particle getVandaEffect(){
		return this.vandaEffect;
	}
}
