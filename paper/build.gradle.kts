plugins {
	`java-library`
	id("io.papermc.paperweight.userdev") version "1.3.1"
}

dependencies {
	paperDevBundle("1.18.2-R0.1-SNAPSHOT")

	compileOnly("net.civmc.civmodcore:CivModCore:2.4.1:dev-all")
	compileOnly("net.civmc.citadel:Citadel:5.1.0:dev")
	compileOnly("net.civmc.bastion:Bastion:3.1.0:dev")
	compileOnly("net.civmc.namelayer:NameLayer:3.1.0:dev")


	//compileOnly("net.civmc.namelayer:paper:3.0.0-SNAPSHOT:dev")
	//compileOnly("net.civmc.citadel:paper:5.0.0-SNAPSHOT:dev")
}
