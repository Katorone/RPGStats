package mc.rpgstats.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import mc.rpgstats.main.RPGStats;
import nerdhub.cardinal.components.api.component.ComponentProvider;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

public class StatsCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        //noinspection unchecked
        dispatcher.register(
                (LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager.literal("rpgstats"))
                        .executes(
                                (commandContext) -> execute(
                                        (ServerCommandSource) commandContext.getSource(), (ServerPlayerEntity) ((ServerCommandSource) commandContext.getSource()).getEntity()
                                )
                        ).then(
                                CommandManager.argument(
                                        "targets", EntityArgumentType.player()
                                )
                                        .executes(
                                                (commandContext) -> execute(
                                                        commandContext.getSource(), EntityArgumentType.getPlayer(commandContext, "targets")
                                                )
                                        )
                        )
        );
    }

    private static int execute(ServerCommandSource source, ServerPlayerEntity target) {
        if (source.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity spe = (ServerPlayerEntity) source.getEntity();
            ComponentProvider provider = ComponentProvider.fromEntity(spe);
            spe.sendMessage(new LiteralText("§aRPGStats >§r Stats for " + target.getEntityName()), false);

            spe.sendMessage(new LiteralText(RPGStats.getFormattedLevelData(RPGStats.MELEE_COMPONENT, provider)), false);
            spe.sendMessage(new LiteralText(RPGStats.getFormattedLevelData(RPGStats.RANGED_COMPONENT, provider)), false);
            spe.sendMessage(new LiteralText(RPGStats.getFormattedLevelData(RPGStats.DEFENSE_COMPONENT, provider)), false);
            spe.sendMessage(new LiteralText(RPGStats.getFormattedLevelData(RPGStats.MAGIC_COMPONENT, provider)), false);
            spe.sendMessage(new LiteralText(RPGStats.getFormattedLevelData(RPGStats.FARMING_COMPONENT, provider)), false);
            spe.sendMessage(new LiteralText(RPGStats.getFormattedLevelData(RPGStats.MINING_COMPONENT, provider)), false);
            spe.sendMessage(new LiteralText(RPGStats.getFormattedLevelData(RPGStats.FISHING_COMPONENT, provider)), false);
        } else if (target != null) {
            ComponentProvider provider = ComponentProvider.fromEntity(target);
            source.sendFeedback(new LiteralText("Stats for " + target.getEntityName()), false);
    
            source.sendFeedback(new LiteralText(RPGStats.getNotFormattedLevelData(RPGStats.MELEE_COMPONENT, provider)), false);
            source.sendFeedback(new LiteralText(RPGStats.getNotFormattedLevelData(RPGStats.RANGED_COMPONENT, provider)), false);
            source.sendFeedback(new LiteralText(RPGStats.getNotFormattedLevelData(RPGStats.DEFENSE_COMPONENT, provider)), false);
            source.sendFeedback(new LiteralText(RPGStats.getNotFormattedLevelData(RPGStats.MAGIC_COMPONENT, provider)), false);
            source.sendFeedback(new LiteralText(RPGStats.getNotFormattedLevelData(RPGStats.FARMING_COMPONENT, provider)), false);
            source.sendFeedback(new LiteralText(RPGStats.getNotFormattedLevelData(RPGStats.MINING_COMPONENT, provider)), false);
            source.sendFeedback(new LiteralText(RPGStats.getNotFormattedLevelData(RPGStats.FISHING_COMPONENT, provider)), false);
        } else {
            source.sendError(new LiteralText("A player must be passed when execute from the console"));
        }
        return 1;
    }
}