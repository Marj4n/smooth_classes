package org.marj4n.smooth_classes.origin;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Arrays;

/** Small V1 test surface so every Origin can be previewed without making twelve worlds. */
public final class OriginCommands {
    private OriginCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("smoothclasses")
                        .then(CommandManager.literal("origin")
                                .then(CommandManager.literal("choose")
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                            OriginRuntime.openSelection(player);
                                            return 1;
                                        }))
                                .then(CommandManager.literal("status")
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                            OriginState state = OriginRuntime.state(player);
                                            OriginType type = state.origin();
                                            if (type == null) {
                                                ctx.getSource().sendFeedback(() -> Text.literal("No Origin selected."), false);
                                            } else {
                                                ctx.getSource().sendFeedback(() -> Text.literal(
                                                        "Origin=" + type.displayName()
                                                                + " Blood=" + state.blood()
                                                                + " Sun=" + state.sunExposure()
                                                                + " Wetness=" + state.wetnessTicks() / 20
                                                                + " Soul=" + state.soul()
                                                                + " Bone=" + state.boneMass()
                                                                + " Carbon=" + state.carbonLayer()
                                                                + " LivingMass=" + state.livingMass()), false);
                                            }
                                            return 1;
                                        }))
                                .then(CommandManager.literal("set")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.argument("origin", StringArgumentType.word())
                                                .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                                        Arrays.stream(OriginType.values()).map(OriginType::id), builder))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                                    String id = StringArgumentType.getString(ctx, "origin");
                                                    OriginType type = OriginType.byId(id).orElse(null);
                                                    if (type == null) {
                                                        ctx.getSource().sendError(Text.literal("Unknown Origin: " + id));
                                                        return 0;
                                                    }
                                                    OriginRuntime.setOrigin(player, type);
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("clear")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                            OriginRuntime.clearOrigin(player);
                                            OriginRuntime.openSelection(player);
                                            return 1;
                                        }))
                        )
        ));
    }
}
