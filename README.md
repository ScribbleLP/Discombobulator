# Discombobulator
A java preprocessor gradle plugin for Minecraft mod development.

This project seeks to improve the [ReplayMod preprocessor](https://github.com/ReplayMod/preprocessor) by Johni0702 in several ways

## Comparison
### Syntax
The ReplayMod preprocessor uses the following syntax:
```java
    //#if MC>=11200
    // This is the block for MC >= 1.12.0
    category.addDetail(name, callable::call);
    //#else
    //$$ // This is the block for MC < 1.12.0
    //$$ category.setDetail(name, callable::call);
    //#endif
```
While the syntax is powerful, in practice, a lot of it is simply unnecessary and bothersome to write.  
We noticed how we *always* use the "greater equals" syntax paired with else.  
Furthermore, we dislike the version numbering that was chosen (11200), as it does not support snapshots versions  
and when reading it, you always need a second untangle the version in your head, especially with 12001, 11202 or 12101

Hence we simplified the syntax by making it the default operator:
```java
    //# 1.12
    // This is the block for MC >= 1.12.0
    category.addDetail(name, callable::call);
    //# def
    //$$ // This is the block for MC < 1.12.0
    //$$ category.setDetail(name, callable::call);
    //# end
```
With `# def` being the default lowest version

### Nesting
```java
    //#if MC>=10904
    public CPacketResourcePackStatus makeStatusPacket(String hash, Action action) {
        //#if MC>=11002
        return new CPacketResourcePackStatus(action);
        //#else
        //$$ return new CPacketResourcePackStatus(hash, action);
        //#endif
    }
    //#else
    //$$ public C19PacketResourcePackStatus makeStatusPacket(String hash, Action action) {
    //$$     return new C19PacketResourcePackStatus(hash, action);
    //$$ }
    //#endif
```
Nesting is also supported by adding # to the version:
```java
    //# 1.9.4
    public CPacketResourcePackStatus makeStatusPacket(String hash, Action action) {
        //## 1.10.2
        return new CPacketResourcePackStatus(action);
        //## def
        //$$ return new CPacketResourcePackStatus(hash, action);
        //## end
    }
    //# def
    //$$ public C19PacketResourcePackStatus makeStatusPacket(String hash, Action action) {
    //$$     return new C19PacketResourcePackStatus(hash, action);
    //$$ }
    //# end
```
With `## def` being the version of the parent block, in this case 1.9.4

### Patterns
The ReplayMod patterns are quite involved, requiring an annotation and a [class](https://github.com/ReplayMod/ReplayMod/blob/193e51b3c2023e1d8382384aedebb9f046a82436/src/main/java/com/replaymod/core/versions/Patterns.java).  
While that may be superior, we opted to apply patterns on a line by line basis:

```java
System.out.println(mc.window); // @GetWindow;
```
Multiple patterns being applied like so:
```java
Minecraft.getMinecraft().setWindow(mc.window); // @GetWindow,GetMinecraft;
```
and all of them being registered in the build.gradle
```groovy
discombobulator {
	patterns = [
		GetWindow: [
			"def": "mc.window",
			"1.15.2": "mc.getWindow()"
		],
		GetMinecraft: [
			//etc...
		]
	]
}
```
### Workflow
When editing code in ReplayMod, you can only edit and run one mod version at a time,  
slowing things down considerably.

With Discombobulator, all versions are open at the same time  
and you can edit the code in one version, which then gets copied to every other version after saving.

The main src folder is updated as well, which then gets committed via git. No more "switching to the newest version" for committing!

## Setup
More can be found via the [Discombobulator-Template](https://github.com/MinecraftTAS/Discombobulator-Template)