package org.marj4n.smooth_classes.origin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Arrays;
import net.minecraft.server.command.ServerCommandSource;

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
                                            if (type == null || !type.isV1Playable()) {
                                                ctx.getSource().sendFeedback(() -> Text.literal("No Origin selected."), false);
                                            } else if (type == OriginType.HUMAN) {
                                                ctx.getSource().sendFeedback(() -> Text.literal(
                                                        "Origin=Human (vanilla; Class skills only)"), false);
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
                                                        Arrays.stream(OriginType.v1PlayableValues()).map(OriginType::id), builder))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                                    String id = StringArgumentType.getString(ctx, "origin");
                                                    OriginType type = OriginType.byId(id).orElse(null);
                                                    if (type == null || !type.isV1Playable()) {
                                                        ctx.getSource().sendError(Text.literal("Unknown Origin: " + id));
                                                        return 0;
                                                    }
                                                    OriginRuntime.setOrigin(player, type);
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("level")
                                        .executes(ctx -> showLevel(ctx.getSource()))
                                        .then(CommandManager.literal("up")
                                                .requires(source -> source.hasPermissionLevel(2))
                                                .executes(ctx -> grantLevels(ctx.getSource(), 1, false))
                                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 7))
                                                        .executes(ctx -> grantLevels(ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "amount"), false))))
                                        .then(CommandManager.literal("set")
                                                .requires(source -> source.hasPermissionLevel(2))
                                                .then(CommandManager.argument("level", IntegerArgumentType.integer(0, 7))
                                                        .executes(ctx -> grantLevels(ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "level"), true))))
                                        .then(CommandManager.literal("max")
                                                .requires(source -> source.hasPermissionLevel(2))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                                    return grantLevels(ctx.getSource(), OriginDebugLevels.maxLevel(
                                                            OriginRuntime.state(player).origin()), true);
                                                })))
                                .then(CommandManager.literal("grave")
                                        .then(CommandManager.literal("status").executes(ctx -> {
                                            String status = VampireGraveSpawn.status(ctx.getSource().getPlayerOrThrow());
                                            ctx.getSource().sendFeedback(() -> Text.literal(status), false);
                                            return 1;
                                        }))
                                        .then(CommandManager.literal("claim").executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                            return VampireGraveSpawn.awakenOnce(player, OriginRuntime.state(player)) ? 1 : 0;
                                        }))
                                        .then(CommandManager.literal("adopt")
                                                .requires(source -> source.hasPermissionLevel(2))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                                    if (!VampireGraveSpawn.adoptExisting(player)) {
                                                        ctx.getSource().sendError(Text.literal("A shared church already exists, or no intact legacy crypt was found nearby in the Overworld."));
                                                        return 0;
                                                    }
                                                    ctx.getSource().sendFeedback(() -> Text.literal(VampireGraveSpawn.status(player)), false);
                                                    return 1;
                                                }))
                                        .then(CommandManager.literal("test").requires(source -> source.hasPermissionLevel(2)).executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                            if (VampireGraveSpawn.replayForTest(player)) {
                                                ctx.getSource().sendFeedback(() -> Text.literal("[Origin] Replaying the existing Vampire coffin. No structures or loot were regenerated."), false);
                                                return 1;
                                            }
                                            ctx.getSource().sendError(Text.literal("Choose Vampire first; the assigned grave must still contain its coffin. Legacy players can adopt an old church but cannot regenerate an old grave."));
                                            return 0;
                                        })))
                                .then(CommandManager.literal("lord")
                                        .then(CommandManager.literal("teaser")
                                                .then(CommandManager.literal("test")
                                                        .requires(source -> source.hasPermissionLevel(2))
                                                        .executes(ctx -> {
                                                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                                            if (!VampireLordTeaser.replayForTest(player)) {
                                                                ctx.getSource().sendError(Text.literal("Choose Vampire first."));
                                                                return 0;
                                                            }
                                                            ctx.getSource().sendFeedback(() -> Text.literal(
                                                                    "[Origin] Seven Thunders preview started. This does not unlock Vampire Lord."), false);
                                                            return 1;
                                                        }))))
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

    private static int showLevel(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        OriginType origin = OriginRuntime.state(player).origin();
        if (origin == null) {
            source.sendError(Text.literal("Choose an Origin before using Origin levels."));
            return 0;
        }
        if (origin == OriginType.HUMAN) {
            source.sendFeedback(() -> Text.literal("Human has no Origin skill tree or milestones."), false);
            return 0;
        }
        int level = OriginDebugLevels.level(player);
        int max = OriginDebugLevels.maxLevel(origin);
        source.sendFeedback(() -> Text.literal("[Origin] " + origin.displayName() +
                " milestone level: " + level + "/" + max + " (0 = base)."), false);
        return level;
    }

    private static int grantLevels(ServerCommandSource source, int value, boolean absolute)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        OriginType origin = OriginRuntime.state(player).origin();
        if (origin != OriginType.VAMPIRE) {
            source.sendError(Text.literal("Only Vampire has Origin evolution levels. Human has none."));
            return 0;
        }
        int current = OriginDebugLevels.level(player);
        int max = OriginDebugLevels.maxLevel(origin);
        int target = absolute ? value : Math.min(max, current + value);
        if (target < current) {
            source.sendError(Text.literal("Origin milestone levels are permanent. You cannot lower a level with this command."));
            return 0;
        }
        if (!OriginDebugLevels.advanceTo(player, target)) {
            source.sendError(Text.literal("Cannot grant Origin levels. Check the Puffish Skills category and loaded data."));
            return 0;
        }
        int granted = OriginDebugLevels.level(player);
        source.sendFeedback(() -> Text.literal("[Origin] " + origin.displayName() +
                " milestone level: " + granted + "/" + max + ". Unlocked tree nodes saved."), true);
        return Math.max(1, granted - current);
    }
}
