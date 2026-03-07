package it.yuruni.kariview.client.editor;

import it.yuruni.kariview.Kariview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Kariview.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TitleScreenInjector {
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen screen)) return;
        Button button = Button.builder(
                Component.literal("Animation Editor"),
                btn -> Minecraft.getInstance().setScreen(new AnimationEditorScreen())
        ).bounds(screen.width - 112, 8, 104, 20).build();
        event.addListener(button);
    }
}
