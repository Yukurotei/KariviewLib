package it.yuruni.kariview.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import it.yuruni.kariview.Kariview;
import it.yuruni.kariview.client.KariviewRenderer;
import it.yuruni.kariview.client.animation.AnimationManager;
import it.yuruni.kariview.client.camera.CameraController;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = Kariview.MODID)
public class KariViewClientCommands {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("kariview")
                        .then(Commands.literal("displayElement")
                                .then(Commands.argument("elementId", StringArgumentType.string())
                                        .then(Commands.argument("namespace", StringArgumentType.string())
                                                .then(Commands.argument("texturePath", StringArgumentType.string())
                                                        .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                                                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                                                        .then(Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                                                .then(Commands.argument("textureWidth", IntegerArgumentType.integer())
                                                                                        .then(Commands.argument("textureHeight", IntegerArgumentType.integer())
                                                                                                .executes(KariViewClientCommands::executeDisplayElementClient)
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("hideAllElements")
                                .executes(KariViewClientCommands::executeHideAllElementsClient)
                        )
                        .then(Commands.literal("testcam")
                                .executes(ctx -> {
                                    var player = Minecraft.getInstance().player;
                                    if (player == null) return 0;
                                    if (CameraController.isActive()) {
                                        CameraController.exit();
                                        ctx.getSource().sendSuccess(() -> Component.translatable("[Kariview] Camera exit"), false);
                                    } else {
                                        CameraController.enter(new CameraController.View()
                                                .position(player.position().add(0, 6, 0))
                                                .pitch(90f));
                                        ctx.getSource().sendSuccess(() -> Component.translatable("[Kariview] Camera enter"), false);
                                    }
                                    return 1;
                                })
                        )
        );
    }

    private static int executeDisplayElementClient(CommandContext<CommandSourceStack> ctx) {
        String elementId = StringArgumentType.getString(ctx, "elementId");
        String texturePath = StringArgumentType.getString(ctx, "texturePath");
        String namespace = StringArgumentType.getString(ctx, "namespace");
        double x = DoubleArgumentType.getDouble(ctx, "x");
        double y = DoubleArgumentType.getDouble(ctx, "y");
        double scale = DoubleArgumentType.getDouble(ctx, "scale");
        int textureWidth = IntegerArgumentType.getInteger(ctx, "textureWidth");
        int textureHeight = IntegerArgumentType.getInteger(ctx, "textureHeight");

        boolean isSuccess = AnimationManager.displayTemporaryElement(elementId, namespace, texturePath, x, y, scale, textureWidth, textureHeight);
        if (isSuccess) {
            KariviewRenderer.isGuiActive = true;
            ctx.getSource().sendSuccess(() -> Component.translatable("Displaying temporary element: " + elementId), false);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.translatable("An error occurred while trying to display that image. Did you spell out everything correctly? Make sure to include file extensions!"));
            return 0;
        }
    }

    private static int executeHideAllElementsClient(CommandContext<CommandSourceStack> ctx) {
        AnimationManager.hideAllTemporaryElements();
        KariviewRenderer.isGuiActive = false;

        ctx.getSource().sendSuccess(() -> Component.translatable("Hiding all temporary elements."), false);
        return 1;
    }
}
