package pl.craftserve.radiation.nms;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import pl.craftserve.radiation.LugolsIodinePotion;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class V1_19_R2NmsBridge implements RadiationNmsBridge {
    static final Logger logger = Logger.getLogger(V1_19_R2NmsBridge.class.getName());

    private final Class<?> itemClass;
    private final Class<?> iRegistryClass;
    private final Class<?> mobEffectClass;
    private final Class<?> potionRegistryClass;
    private final Class<?> potionBrewerClass;

    private final Field isRegistryMaterialsFrozen;

    private final Method getItem;
    private final Method newMinecraftKey;
    private final Method getPotion;
    private final Method minHeightMethod;

    private final Object potionRegistry;

    private final Map<UUID, Integer> minWorldHeightMap = new HashMap<>();

    //REMOVEME:
    private void shoutout(String str, Object sth)
    {
      if (sth != null)
      {
        logger.log(Level.SEVERE, str + " exists!");
      }
      else
      {
        logger.log(Level.SEVERE, str + " IS NULL!");
      }
    }

    public V1_19_R2NmsBridge(String version) {
        Objects.requireNonNull(version, "version");

        try {
            this.itemClass = Class.forName("net.minecraft.world.item.Item"); // Item -> Item

            //REMOVEME:
            shoutout("itemClass", itemClass);

            this.iRegistryClass = Class.forName("net.minecraft.core.IRegistry"); // IRegistry -> Registry

            //REMOVEME:
            shoutout("iRegistryClass", iRegistryClass);

            this.mobEffectClass = Class.forName("net.minecraft.world.effect.MobEffect"); // MobEffect -> MobEffectInstance

            //REMOVEME:
            shoutout("mobEffectClass", mobEffectClass);

            this.potionRegistryClass = Class.forName("net.minecraft.world.item.alchemy.PotionRegistry"); // PotionRegistry -> Potion

            //REMOVEME:
            shoutout("potionRegistryClass", potionRegistryClass);

            this.potionBrewerClass = Class.forName("net.minecraft.world.item.alchemy.PotionBrewer$a"); // PotionBrewer -> PotionBrewing


            //REMOVEME:
            shoutout("potionBrewerClass", potionBrewerClass);

            Class<?> registryMaterialsClass = Class.forName("net.minecraft.core.RegistryMaterials"); // RegistryMaterials -> MappedRegistry

            //REMOVEME:
            shoutout("registryMaterialsClass", registryMaterialsClass);

            this.isRegistryMaterialsFrozen = registryMaterialsClass.getDeclaredField("l"); // l -> frozen

            //REMOVEME:
            shoutout("isRegistryMaterialsFrozen", isRegistryMaterialsFrozen);

            //Class<?> craftMagicNumbers = Class.forName("org.bukkit.craftbukkit." + version + ".util.CraftMagicNumbers");
            Class<?> craftMagicNumbers = Class.forName("org.bukkit.craftbukkit.util.CraftMagicNumbers");

            //REMOVEME:
            shoutout("craftMagicNumbers", craftMagicNumbers);

            this.getItem = craftMagicNumbers.getMethod("getItem", Material.class);

            //REMOVEME:
            shoutout("this.getItem", this.getItem);

            this.minHeightMethod = Class.forName("org.bukkit.generator.WorldInfo").getMethod("getMinHeight");

            //REMOVEME:
            shoutout("this.minHeightMethod", this.minHeightMethod);


            Class<?> minecraftKey = Class.forName("net.minecraft.resources.MinecraftKey"); // MinecraftKey -> ResourceLocation

            //REMOVEME:
            shoutout("minecraftKey", minecraftKey);

            this.newMinecraftKey = minecraftKey.getMethod("a", String.class); // a -> tryParse

            //REMOVEME:
            shoutout("this.newMinecraftKey", this.newMinecraftKey);

            Class<?> builtInRegistries = Class.forName("net.minecraft.core.registries.BuiltInRegistries"); // RegistryGeneration -> BuiltInRegistries

            //REMOVEME:
            shoutout("builtInRegistries", builtInRegistries);

            //this.potionRegistry = builtInRegistries.getDeclaredField("j").get(null); // j -> POTION

            //https://mappings.cephx.dev/1.20.4/net/minecraft/core/registries/BuiltInRegistries.html
            //DeclaredField może się różnić między wersjami, dla 1.21.1 jest to "h", wcześniej było "j"
            this.potionRegistry = builtInRegistries.getDeclaredField("h").get(null); // j -> POTION


            //REMOVEME:
            shoutout("this.potionRegistry", this.potionRegistry);

            this.getPotion = this.potionRegistry.getClass().getMethod("a", minecraftKey); // a -> get

            //REMOVEME:
            shoutout("this.getPotion", this.getPotion);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize 1.19.3 bridge", e);
        }
    }

    @Override
    public void registerLugolsIodinePotion(NamespacedKey potionKey, LugolsIodinePotion.Config.Recipe config) {
        Objects.requireNonNull(potionKey, "potionKey");
        Objects.requireNonNull(config, "config");

        try {
            String basePotionName = config.basePotion().name().toLowerCase(Locale.ROOT);

            //REMOVEME:
            shoutout("basePotionName", basePotionName);
            logger.log(Level.SEVERE, "basePotionName is " + basePotionName);

            //FIXME: Jak mam to opakować w klasę implementującą Holder?
            Object basePotion = this.getPotion.invoke(this.potionRegistry, this.newMinecraftKey.invoke(null, basePotionName));
            Objects.requireNonNull(basePotion, "basePotion not found");

            logger.log(Level.SEVERE, "ingredient is " + config.ingredient());

            Object ingredient = this.getItem.invoke(null, config.ingredient());
            Objects.requireNonNull(ingredient, "ingredient not found");

            Object mobEffectArray = Array.newInstance(this.mobEffectClass, 0);

            //REMOVEME:
            shoutout("mobEffectArray", mobEffectArray);

            Object newPotion = this.potionRegistryClass.getConstructor(mobEffectArray.getClass()).newInstance(mobEffectArray);

            //REMOVEME:
            shoutout("newPotion", newPotion);

            Method registerMethod = this.iRegistryClass.getDeclaredMethod("a", this.iRegistryClass, String.class, Object.class); // a -> register

            //REMOVEME:
            shoutout("registerMethod", registerMethod);

            this.isRegistryMaterialsFrozen.setAccessible(true);
            this.isRegistryMaterialsFrozen.set(this.potionRegistry, false);
            Object potion = registerMethod.invoke(null, this.potionRegistry, potionKey.getKey(), newPotion);

            //REMOVEME:
            shoutout("potion", potion);

            //Method registerBrewingRecipe = this.potionBrewerClass.getDeclaredMethod("a", this.potionRegistryClass, this.itemClass, this.potionRegistryClass); // a -> addMix
            Class<?> holder = Class.forName("net.minecraft.core.Holder");
            Method registerBrewingRecipe = this.potionBrewerClass.getDeclaredMethod("a", holder, this.itemClass, holder); // a -> addMix

            //REMOVEME:
            shoutout("registerBrewingRecipe", registerBrewingRecipe);

            registerBrewingRecipe.setAccessible(true);

            //REMOVEME:
            shoutout("basePotion", basePotion);
            //REMOVEME:
            shoutout("ingredient", ingredient);
            //REMOVEME:
            shoutout("potion", potion);

            registerBrewingRecipe.invoke(null, basePotion, ingredient, potion);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not handle reflective operation.", e);
        }
    }

    @Override
    public void unregisterLugolsIodinePotion(NamespacedKey potionKey) {
        // todo unregister potion and brewing recipe
    }

    @Override
    public int getMinWorldHeight(World bukkitWorld) {
        Objects.requireNonNull(bukkitWorld, "bukkitWorld");

        return this.minWorldHeightMap.computeIfAbsent(bukkitWorld.getUID(), worldId -> {
            try {
                return (int) this.minHeightMethod.invoke(bukkitWorld);
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.log(Level.SEVERE, "Could not handle min world height on world '" + bukkitWorld.getName() + "' ('" + worldId + "').", e);
                return 0;
            }
        });
    }
}
