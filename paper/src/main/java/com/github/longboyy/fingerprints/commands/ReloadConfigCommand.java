package com.github.longboyy.fingerprints.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import com.github.longboyy.fingerprints.FingerprintPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class ReloadConfigCommand extends BaseCommand {

	@CommandAlias("fpreload")
	@Description("Reloads the config for fingerprints")
	@CommandPermission("fingerprints.reload")
	public void execute(Player player){
		if(player.isOp()){
			FingerprintPlugin plugin = FingerprintPlugin.getInstance(FingerprintPlugin.class);

			plugin.reloadConfig();
			if(plugin.config().parse()){
				player.sendMessage(Component.text("Successfully reloaded fingerprint config"));
			}else{
				player.sendMessage(Component.text("Failed to reload fingerprint config"));
			}
		}
	}
}
