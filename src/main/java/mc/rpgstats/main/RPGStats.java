package mc.rpgstats.main;

import mc.rpgstats.advancemnents.AdvancementHelper;
import mc.rpgstats.command.StatsCommand;
import mc.rpgstats.component.*;
import nerdhub.cardinal.components.api.ComponentRegistry;
import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.component.ComponentProvider;
import nerdhub.cardinal.components.api.event.EntityComponentCallback;
import nerdhub.cardinal.components.api.util.EntityComponents;
import nerdhub.cardinal.components.api.util.RespawnCopyStrategy;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.server.PlayerStream;
import net.minecraft.advancement.Advancement;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class RPGStats implements ModInitializer {
    public static final String MOD_ID = "rpgstats";
    
    // Stat components
    public static final ComponentType<MeleeComponent> MELEE_COMPONENT = ComponentRegistry.INSTANCE.registerIfAbsent(new Identifier("rpgstats:melee"), MeleeComponent.class);
    public static final ComponentType<RangedComponent> RANGED_COMPONENT = ComponentRegistry.INSTANCE.registerIfAbsent(new Identifier("rpgstats:ranged"), RangedComponent.class);
    public static final ComponentType<DefenseComponent> DEFENSE_COMPONENT = ComponentRegistry.INSTANCE.registerIfAbsent(new Identifier("rpgstats:defence"), DefenseComponent.class);
    public static final ComponentType<FarmingComponent> FARMING_COMPONENT = ComponentRegistry.INSTANCE.registerIfAbsent(new Identifier("rpgstats:farming"), FarmingComponent.class);
    public static final ComponentType<MagicComponent> MAGIC_COMPONENT = ComponentRegistry.INSTANCE.registerIfAbsent(new Identifier("rpgstats:magic"), MagicComponent.class);
    public static final ComponentType<MiningComponent> MINING_COMPONENT = ComponentRegistry.INSTANCE.registerIfAbsent(new Identifier("rpgstats:mining"), MiningComponent.class);
    public static final ComponentType<FishingComponent> FISHING_COMPONENT = ComponentRegistry.INSTANCE.registerIfAbsent(new Identifier("rpgstats:fishing"), FishingComponent.class);
    
    public static ArrayList<ServerPlayerEntity> needsStatFix = new ArrayList<>();
    
    private int tickCount = 0;
    
    @Override
    public void onInitialize() {
        // Init components on players
        EntityComponentCallback.event(PlayerEntity.class).register((player, components) -> {
            components.put(MAGIC_COMPONENT, new MagicComponent(player));
            components.put(MINING_COMPONENT, new MiningComponent(player));
            components.put(FISHING_COMPONENT, new FishingComponent(player));
            components.put(FARMING_COMPONENT, new FarmingComponent(player));
            components.put(DEFENSE_COMPONENT, new DefenseComponent(player));
            components.put(RANGED_COMPONENT, new RangedComponent(player));
            components.put(MELEE_COMPONENT, new MeleeComponent(player));
        });
        
        // Keeps stats always
        EntityComponents.setRespawnCopyStrategy(MELEE_COMPONENT, RespawnCopyStrategy.ALWAYS_COPY);
        EntityComponents.setRespawnCopyStrategy(RANGED_COMPONENT, RespawnCopyStrategy.ALWAYS_COPY);
        EntityComponents.setRespawnCopyStrategy(DEFENSE_COMPONENT, RespawnCopyStrategy.ALWAYS_COPY);
        EntityComponents.setRespawnCopyStrategy(FARMING_COMPONENT, RespawnCopyStrategy.ALWAYS_COPY);
        EntityComponents.setRespawnCopyStrategy(MAGIC_COMPONENT, RespawnCopyStrategy.ALWAYS_COPY);
        EntityComponents.setRespawnCopyStrategy(MINING_COMPONENT, RespawnCopyStrategy.ALWAYS_COPY);
        
        // Command
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> StatsCommand.register(dispatcher));
        
        // Syncing and advancements
        ServerTickEvents.END_SERVER_TICK.register((MinecraftServer server) -> {
            tickCount++;
            if (tickCount >= 10) {
                Collection<Advancement> collection = server.getAdvancementLoader().getAdvancements();
                PlayerStream.all(server).forEach(
                    (player) -> {
                        if (needsStatFix.contains(player) && player.isAlive()) {
                            softLevelUp(DEFENSE_COMPONENT, player);
                            softLevelUp(FARMING_COMPONENT, player);
                            softLevelUp(MAGIC_COMPONENT, player);
                            softLevelUp(MELEE_COMPONENT, player);
                            softLevelUp(MINING_COMPONENT, player);
                            softLevelUp(RANGED_COMPONENT, player);
                            softLevelUp(FISHING_COMPONENT, player);
                            needsStatFix.remove(player);
                        }
                        
                        for (Advancement advancement : collection) {
                            if (advancement.getId().getNamespace().equals("rpgstats")) {
                                if (!player.getAdvancementTracker().getProgress(advancement).isDone()) {
                                    if (AdvancementHelper.shouldGrant(advancement.getId(), player)) {
                                        player.getAdvancementTracker().grantCriterion(advancement, "trigger");
                                    }
                                }
                            }
                        }
                        
                        if (player.getBlockPos().getY() <= 40 && getComponentLevel(RPGStats.MINING_COMPONENT, ComponentProvider.fromEntity(player)) >= 50) {
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 13 * 20));
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 2 * 20, 1));
                        }
                    }
                );
                tickCount = 0;
            }
        });
    }
    
    // Helper methods for components
    public static void setComponentXP(ComponentType<? extends IStatComponent> type, ComponentProvider provider, int newValue) {
        type.get(provider).setXP(newValue);
    }
    
    public static int getComponentXP(ComponentType<? extends IStatComponent> type, ComponentProvider provider) {
        return type.get(provider).getXP();
    }
    
    public static void setComponentLevel(ComponentType<? extends IStatComponent> type, ComponentProvider provider, int newValue) {
        type.get(provider).setLevel(newValue);
    }
    
    public static int getComponentLevel(ComponentType<? extends IStatComponent> type, ComponentProvider provider) {
        return type.get(provider).getLevel();
    }
    
    public static int calculateXpNeededToReachLevel(int level) {
        return (int)Math.floor(Math.pow(level, 2) * 0.3) + 100;
    }
    
    public static void addXpAndLevelUp(ComponentType<? extends IStatComponent> type, ServerPlayerEntity entity, int addedXP) {
        ComponentProvider provider = ComponentProvider.fromEntity(entity);
        int nextXP = getComponentXP(type, provider) + addedXP;
        int currentLevel = getComponentLevel(type, provider);
        
        if (currentLevel <= 50) {
            // Enough to level up
            int nextXPForLevelUp = calculateXpNeededToReachLevel(currentLevel + 1);
            if (nextXP >= nextXPForLevelUp) {
                nextXP -= nextXPForLevelUp;
                setComponentLevel(type, provider, currentLevel + 1);
                ((PlayerEntity)type.get(provider).getEntity()).sendMessage(new LiteralText("§aRPGStats >§r You gained a §6" + type.get(provider).getName() + "§r level! You are now level §6" + type.get(provider).getLevel()), false);
                type.get(provider).onLevelUp(false);
            }
            setComponentXP(type, provider, nextXP);
        }
    }
    
    public static String getFormattedLevelData(ComponentType<? extends IStatComponent> type, ComponentProvider provider) {
        int currentLevel = getComponentLevel(type, provider);
        int xp = getComponentXP(type, provider);
        if (currentLevel < 50) {
            int nextXP = calculateXpNeededToReachLevel(currentLevel + 1);
            return "§6" + type.get(provider).getCapName() + "§r - Level: " + currentLevel + " XP: " + xp + "/" + nextXP;
        } else {
            return "§6" + type.get(provider).getCapName() + "§r - Level: " + currentLevel;
        }
    }
    
    public static String getNotFormattedLevelData(ComponentType<? extends IStatComponent> type, ComponentProvider provider) {
        int currentLevel = getComponentLevel(type, provider);
        int xp = getComponentXP(type, provider);
        if (currentLevel < 50) {
            int nextXP = calculateXpNeededToReachLevel(currentLevel + 1);
            return type.get(provider).getCapName() + " - Level: " + currentLevel + " XP: " + xp + "/" + nextXP;
        } else {
            return "" + type.get(provider).getCapName() + " - Level: " + currentLevel;
        }
    }
    
    public static int getHighestLevel(ComponentProvider provider) {
        return Collections.max(
            Arrays.asList(
                getComponentLevel(MELEE_COMPONENT, provider),
                getComponentLevel(RANGED_COMPONENT, provider),
                getComponentLevel(MINING_COMPONENT, provider),
                getComponentLevel(DEFENSE_COMPONENT, provider),
                getComponentLevel(MAGIC_COMPONENT, provider),
                getComponentLevel(FARMING_COMPONENT, provider),
                getComponentLevel(FISHING_COMPONENT, provider)
            )
        );
    }
    
    public static int getLowestLevel(ComponentProvider provider) {
        return Collections.min(
            Arrays.asList(
                getComponentLevel(MELEE_COMPONENT, provider),
                getComponentLevel(RANGED_COMPONENT, provider),
                getComponentLevel(MINING_COMPONENT, provider),
                getComponentLevel(DEFENSE_COMPONENT, provider),
                getComponentLevel(MAGIC_COMPONENT, provider),
                getComponentLevel(FARMING_COMPONENT, provider),
                getComponentLevel(FISHING_COMPONENT, provider)
            )
        );
    }
    
    public static void softLevelUp(ComponentType<? extends IStatComponent> type, ServerPlayerEntity player) {
        int savedLevel = getComponentLevel(type, ComponentProvider.fromEntity(player));
        for (int i = 1; i <= savedLevel; i++) {
            setComponentLevel(type, ComponentProvider.fromEntity(player), i);
            type.get(ComponentProvider.fromEntity(player)).onLevelUp(true);
        }
    }
}
