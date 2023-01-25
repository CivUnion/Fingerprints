package com.github.longboyy.fingerprints.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import com.github.longboyy.fingerprints.util.FingerprintUtils;
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import vg.civcraft.mc.civmodcore.nbt.NBTSerialization;
import vg.civcraft.mc.civmodcore.nbt.wrappers.NBTCompound;

public class ComparePrintsCommand extends BaseCommand {
    @CommandAlias("compare")
    @Description("compare a fingerprint to either another fingerprint or a fingerprint book")
    public void execute(Player player) {
        PlayerInventory playerInv = player.getInventory();
        ItemStack mainItem = playerInv.getItemInMainHand();
        ItemStack offItem = playerInv.getItemInOffHand();

        if (FingerprintUtils.isFingerprint(offItem) && FingerprintUtils.isFingerprintBook(mainItem)) {
            NBTCompound printNBT = NBTSerialization.fromItem(offItem);
            String printUUID = printNBT.getString("FingerprintOwner");
            NBTCompound bookNBT = NBTSerialization.fromItem(mainItem);
            String[] fpOwners = bookNBT.getStringArray("FingerprintOwners");
            int placeInBook = Arrays.<String>asList(fpOwners).indexOf(printUUID) + 1;

            if (placeInBook > 0) {
                player.sendMessage(
                    Component.text("The fingerprint is in the book! It's on page " + Integer.toString(placeInBook))
                    .color(TextColor.color(0, 255, 0)));
            } else {
                player.sendMessage(
                    Component.text("The given fingerprint isn't in the fingerprint book!")
                    .color(TextColor.color(255, 0, 0)));
            } 
        } else if (FingerprintUtils.isFingerprint(mainItem) && FingerprintUtils.isFingerprint(offItem)) {
            NBTCompound mainNBT = NBTSerialization.fromItem(mainItem);
            NBTCompound offNBT = NBTSerialization.fromItem(offItem);
            String mainUUID = mainNBT.getString("FingerprintOwner");
            String offUUID = offNBT.getString("FingerprintOwner");
            if (mainUUID.equals(offUUID)) {
                player.sendMessage(
                    Component.text("These two fingerprints are the same!")
                    .color(TextColor.color(0, 255, 0)));
            } else {
                player.sendMessage(
                    Component.text("These two fingerprints aren't the same!")
                    .color(TextColor.color(255, 0, 50)));
            } 
        } else {
        player.sendMessage(
            Component.text("Comparison failed, you're not holding the correct items")
            .color(TextColor.color(255, 0, 0)));
        } 
    }
}

