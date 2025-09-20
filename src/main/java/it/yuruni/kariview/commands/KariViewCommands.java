package it.yuruni.kariview.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.yuruni.kariview.Kariview;
import it.yuruni.kariview.client.animation.AnimationManager;
import it.yuruni.kariview.client.data.AnimationData;
import it.yuruni.kariview.client.data.AnimationLoader;
import it.yuruni.kariview.packets.PacketHandler;
import it.yuruni.kariview.packets.server2client.StopViewPacket;
import it.yuruni.kariview.packets.server2client.PlayAnimationPacket;
import it.yuruni.kariview.packets.server2client.ShowViewPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = Kariview.MODID)
public class KariViewCommands {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("kariview")
                        .then(Commands.literal("enableView")
                                .requires(source -> source.hasPermission(2))
                                .executes(KariViewCommands::executeShowView)
                        )
                        .then(Commands.literal("stopView")
                                .requires(source -> source.hasPermission(2))
                                .executes(KariViewCommands::executeStopView)
                        )
                        .then(Commands.literal("playAnimation")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("namespace", StringArgumentType.string())
                                                .then(Commands.argument("animationId", StringArgumentType.string())
                                                        .executes(KariViewCommands::executePlayAnimation)
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("reload")
                                .requires(source -> source.hasPermission(2))
                                .executes(KariViewCommands::reloadAnimations)
                        )
        );
    }


    private static int reloadAnimations(CommandContext<CommandSourceStack> context) {
        AnimationManager.reload();
        context.getSource().sendSuccess(() -> Component.literal("Animations reloaded."), false);
        return 1;
    }

    private static int executeShowView(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("Displaying custom view..."), false);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new ShowViewPacket());
        return 1;
    }

    private static int executeStopView(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("Hiding custom view..."), false);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new StopViewPacket());
        return 1;
    }

    private static int executePlayAnimation(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String namespace = StringArgumentType.getString(ctx, "namespace");
        String animationId = StringArgumentType.getString(ctx, "animationId");
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");

        try {
            AnimationData data = AnimationLoader.loadAnimation(namespace, animationId);
            if (data != null) {
                for (ServerPlayer player : players) {
                    PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PlayAnimationPacket(namespace, animationId));
                }
                ctx.getSource().sendSuccess(() -> Component.literal("Playing animation: " + namespace + ":" + animationId + " for " + players.size() + " players."), false);
            } else {
                ctx.getSource().sendFailure(Component.literal("Unknown animation: " + namespace + ":" + animationId));
            }
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed to play animation: " + e.getMessage()));
            e.printStackTrace();
        }

        return 1;
    }
}