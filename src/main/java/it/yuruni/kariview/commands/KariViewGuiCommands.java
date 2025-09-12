package it.yuruni.kariview.commands;

import com.mojang.brigadier.context.CommandContext;
import it.yuruni.kariview.Kariview;
import it.yuruni.kariview.packets.PacketHandler;
import it.yuruni.kariview.packets.server2client.HideGuiPacket;
import it.yuruni.kariview.packets.server2client.ShowGuiPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid= Kariview.MODID)
public class KariViewGuiCommands {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("kariview")
                        .then(Commands.literal("showGui")
                                .requires(source -> source.hasPermission(2))
                                .executes(KariViewGuiCommands::showGui)
                        ).then(Commands.literal("hideGui")
                                .executes(KariViewGuiCommands::hideGui)
                        )
        );
    }

    private static int showGui(CommandContext<CommandSourceStack> ctx) {
        LOGGER.info("Command '/kariview showGui' was executed!");
        ctx.getSource().sendSuccess(() -> Component.literal("Displaying custom GUI..."), false);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new ShowGuiPacket());
        return 1;
    }

    private static int hideGui(CommandContext<CommandSourceStack> ctx) {
        LOGGER.info("Command '/kariview hideGui' was executed!");
        ctx.getSource().sendSuccess(() -> Component.literal("Hiding custom GUI..."), false);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new HideGuiPacket());
        return 1;
    }
}
