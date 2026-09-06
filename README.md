# OpenComputers: Modernized

![A showcase of various blocks from the mod](.github/banner.png)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/5ohMkyqo?logo=modrinth)](https://modrinth.com/project/5ohMkyqo) [![CurseForge Downloads](https://img.shields.io/curseforge/dt/1646866?logo=curseforge)](https://www.curseforge.com/minecraft/mc-mods/opencomputers-modernized) [![MIT License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

OpenComputers: Modernized (OC:M) is a port of the original OpenComputers mod for modern Minecraft.\
Currently, the mod is available for 1.21.1, but versions for 1.20.1 and 26.x are also in the works!\
This version of the mod works on both NeoForge and Fabric, so you can play with any other mods while not having to leave OpenComputers out!

## Features

OpenComputers: Modernized (OC:M) is fully feature-complete with the original OpenComputers mod, you can find everything you remember from the original, for example:
- Modular, persistent computers with customizable components
- Customizable drones and robots, which can be made using the assembler with multiple tiers of components
- Tablets for computing on the go, or server racks if you need to fit a lot more computing in a lot less space
- Loot disks hidden in vanilla structures that contain pre-written programs for your computers and robots
- Extensive support for other mods, so you can use OpenComputers to automate your entire base
- An API for developers to extend the mod even further, adding more new content for you to enjoy

## Support

If you run into any issue or find yourself stuck, first check the ingame "Manual" item, it might just happen to have the answers you're looking for.\
You can also check the original mod's wiki pages for information, most of it applies here too!

Think you've found a bug? Report it on the issue tracker so it can be fixed!:  https://github.com/habzg/OpenComputers-Modernized/issues

## Modpacks
Feel free to use the mod in any modpack you make/publish, crediting/linking back to this page is not required.

## Useful Links
Source code: https://github.com/habzg/OpenComputers-Modernized \
OpenComputers Wiki: https://ocdoc.cil.li/ \
Original OpenComputers source: https://github.com/MightyPirates/OpenComputers

## For Developers
If you're a developer and want to make an addon for OpenComputers: Modernized (OC:M), or even just add integrations into your own mod, an API is provided for exactly that purpose.

You can find API artifacts in the Maven repository, as shown below:
```
repositories {
    maven {
        name = 'OpenComputers: Modernized'
        url = 'https://habzg.github.io/OpenComputers-Modernized/'
    }
}
```

After having added the repository to your project, you will have to include the following artifacts, depending on the modloader(s) your mod targets. In all cases, you will need the common API (replace `<VERSION>` with the version of the mod you're targeting):
```
dependencies {
    compileOnly("li.cil.oc:opencomputers-api:<VERSION>")
}
```

<details>
<summary><b>Fabric</b></summary>

```
dependencies {
    modCompileOnly("li.cil.oc:opencomputers-fabric-api:<VERSION>")
}
```
Or, if you need the full mod (not just the API):
```
dependencies {
    modImplementation("li.cil.oc:opencomputers-fabric:<VERSION>")
}
```
</details>

<details>
<summary><b>NeoForge</b></summary>

```
dependencies {
    compileOnly("li.cil.oc:opencomputers-neoforge-api:<VERSION>")
}
```
Or, if you need the full mod (not just the API):
```
dependencies {
    implementation("li.cil.oc:opencomputers-neoforge:<VERSION>")
}
```
</details>

Some examples of how to make use of the API are available [here](https://github.com/habzg/OpenComputers-Modernized/blob/main/api/src/main/java/li/cil/oc/api/README.md)
