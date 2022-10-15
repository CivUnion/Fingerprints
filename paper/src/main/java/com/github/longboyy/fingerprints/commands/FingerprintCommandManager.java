package com.github.longboyy.fingerprints.commands;

import com.github.longboyy.fingerprints.FingerprintPlugin;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.commands.CommandManager;

public class FingerprintCommandManager extends CommandManager {
	/**
	 * Creates a new command manager for Aikar based commands and tab completions.
	 *
	 * @param plugin The plugin to bind this manager to.
	 */
	public FingerprintCommandManager(@NotNull FingerprintPlugin plugin) {
		super(plugin);
		//this.registerContexts(this.getCommandContexts());
		//this.registerCompletions(this.getCommandCompletions());
		this.registerCommands();
	}

	@Override
	public void registerCommands() {
		super.registerCommands();
		this.registerCommand(new DustCommand());
		this.registerCommand(new ReloadConfigCommand());
	}
}
