# Disguise Lib

A server-side library that allows disguising entities as other ones.
~~Features built-in `/disguise` command as well.~~
[`/disguise` command with some other features has been moved to a separate mod.](https://github.com/samolego/MobDisguises)

## Command options

- `/disguise option player-nameplate`: 플레이어 위장 이름표 옵션 현재 상태 확인
- `/disguise option player-nameplate on`: 플레이어가 위장했을 때 현재 표시 이름을 위장 엔티티 이름표로 노출
- `/disguise option player-nameplate off`: 기본 동작으로 복귀
- `/disguise option player-sneak`: 플레이어 위장 웅크리기 옵션 현재 상태 확인
- `/disguise option player-sneak on`: 플레이어가 웅크릴 때 위장 엔티티도 같이 웅크리기
- `/disguise option player-sneak off`: 플레이어 웅크림을 위장 엔티티에 반영하지 않기

이 옵션은 기본값이 `off`이고 `config/disguiselib.json`에 저장되어 서버 재시작 후에도 유지됨.

## Dependecy
```gradle
repositories {
	maven {
		url 'https://maven.nucleoid.xyz'
	}
	// OR
	maven {
        url 'https://jitpack.io'
    }
}

dependencies {
    // Common module
    modImplementation "xyz.nucleoid:DisguiseLib:${project.disguiselib_version}"
  
    // Fabric
    modImplementation "xyz.nucleoid:disguiselib-fabric:${project.disguiselib_version}"
    
    // Forge
    implementation fg.deobf "com.github.NucleoidMC.DisguiseLib:disguiselib-forge:${project.disguiselib_version}"
    
}
```
# API

Use the provided interface `EntityDisguise` on any class extending `net.minecraft.entity.Entity`.

```java
import xyz.nucleoid.disguiselib.api.EntityDisguise;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

public class MyDisguises {
    public static void disguise() {
        // Make sure you are executing disguise on the server side
        if (world.isClient)
            return;

        // Disguises as creeper
        ((EntityDisguise) entityToDisguise).disguiseAs(EntityType.CREEPER);

        // Disguise as aCustomEntity (net.minecraft.entity)
        ((EntityDisguise) entityToDisguise).disguiseAs(aCustomEntity);

        // If you disguise it as EntityType.PLAYER, you can apply custom GameProfile as well
        ((EntityDisguise) entityToDisguise).setGameProfile(aCustomGameProfile);

        ((EntityDisguise) entityToDisguise).isDisguised(); // Tells whether entity is disguised or not
        ((EntityDisguise) entityToDisguise).removeDisguise(); // Clears the disguise


        // Not that useful (mainly for internal use)
        ((EntityDisguise) entityToDisguise).getDisguiseType(); // Gets the EntityType of the disguise
        ((EntityDisguise) entityToDisguise).disguiseAlive(); // Whether the entity from the disguise is an instance of LivingEntity
    }
}

```
