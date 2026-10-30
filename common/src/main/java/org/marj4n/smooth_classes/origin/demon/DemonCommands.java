package org.marj4n.smooth_classes.origin.demon;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import org.marj4n.smooth_classes.origin.*;

public final class DemonCommands {
    private DemonCommands(){}
    public static void register(){
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(CommandManager.literal("smoothclasses")
            .then(CommandManager.literal("demon")
                .then(CommandManager.literal("path").executes(ctx->{var p=ctx.getSource().getPlayerOrThrow();if(!DemonProgression.demon(p))return 0;DemonNetworking.openPath(p);return 1;})
                    .then(CommandManager.argument("choice",StringArgumentType.word()).suggests((ctx,b)->{b.suggest("world_ender");return b.buildFuture();}).executes(ctx->DemonProgression.choose(ctx.getSource().getPlayerOrThrow(),StringArgumentType.getString(ctx,"choice"))?1:0)))
                .then(CommandManager.literal("status").executes(ctx->{var p=ctx.getSource().getPlayerOrThrow();var s=OriginRuntime.state(p);
                    ctx.getSource().sendFeedback(()->Text.literal("Demon: path="+(s.hasFlag("demon.world_ender")?"World-Ender":"unselected")+", decay="+s.progress("demon.decay")/20+"s, tier="+DemonProgression.tier(p)+", throne cooldown="+s.progress("demon.throne_cd")/20+"s"),false);
                    for(String boss:DemonRules.BOSSES){String id=DemonConfig.INSTANCE.bossEntities.getOrDefault(boss,"");var key=net.minecraft.util.Identifier.tryParse(id);boolean registered=key!=null&&org.marj4n.smooth_classes.platform.PlatformRegistries.ENTITY_TYPE.containsId(key);ctx.getSource().sendFeedback(()->Text.literal(boss+": "+(s.hasFlag("demon.boss."+boss)?"defeated":registered?"required":"dependency not registered ("+id+")")),false);}return 1;}))
                .then(CommandManager.literal("order").then(CommandManager.argument("command",StringArgumentType.word()).suggests((ctx,b)->{for(String c:DemonRules.COMMANDS)b.suggest(c.toLowerCase(java.util.Locale.ROOT));return b.buildFuture();}).executes(ctx->{String name=StringArgumentType.getString(ctx,"command");for(int i=0;i<DemonRules.COMMANDS.length;i++)if(DemonRules.COMMANDS[i].equalsIgnoreCase(name)){var r=DemonArmy.command(ctx.getSource().getPlayerOrThrow(),i);ctx.getSource().sendFeedback(()->Text.literal(r.message()),false);return r.success()?1:0;}return 0;})))
            )));
    }
}
