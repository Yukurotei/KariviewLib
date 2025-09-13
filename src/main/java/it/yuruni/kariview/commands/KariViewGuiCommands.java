package it.yuruni.kariview.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import it.yuruni.kariview.Kariview;
import it.yuruni.kariview.client.data.AnimationData;
import it.yuruni.kariview.client.data.AnimationLoader;
import it.yuruni.kariview.packets.PacketHandler;
import it.yuruni.kariview.packets.server2client.HideGuiPacket;
import it.yuruni.kariview.packets.server2client.PlayAnimationPacket;
import it.yuruni.kariview.packets.server2client.ShowGuiPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = Kariview.MODID)
public class KariViewGuiCommands {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("kariview")
                        .then(Commands.literal("showGui")
                                .requires(source -> source.hasPermission(2))
                                .executes(KariViewGuiCommands::executeShowGui)
                        )
                        .then(Commands.literal("hideGui")
                                .requires(source -> source.hasPermission(2))
                                .executes(KariViewGuiCommands::executeHideGui)
                        )
                        .then(Commands.literal("playAnimation")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("namespace", StringArgumentType.string())
                                        .then(Commands.argument("animationId", StringArgumentType.string())
                                                .executes(KariViewGuiCommands::executePlayAnimation)
                                        )
                                )
                        )
        );
    }

    private static int executeShowGui(CommandContext<CommandSourceStack> ctx) {
        LOGGER.info("Command '/kariview showGui' was executed!");
        ctx.getSource().sendSuccess(() -> Component.literal("Displaying custom GUI..."), false);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new ShowGuiPacket());
        return 1;
    }

    private static int executeHideGui(CommandContext<CommandSourceStack> ctx) {
        LOGGER.info("Command '/kariview hideGui' was executed!");
        ctx.getSource().sendSuccess(() -> Component.literal("Hiding custom GUI..."), false);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new HideGuiPacket());
        return 1;
    }

    private static int executePlayAnimation(CommandContext<CommandSourceStack> ctx) {
        String namespace = StringArgumentType.getString(ctx, "namespace");
        String animationId = StringArgumentType.getString(ctx, "animationId");

        try {
            AnimationData data = AnimationLoader.loadAnimation(namespace, animationId);
            if (data != null) {
                PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PlayAnimationPacket(namespace, animationId));
                ctx.getSource().sendSuccess(() -> Component.literal("Playing animation: " + namespace + ":" + animationId), false);
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