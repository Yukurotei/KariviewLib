package it.yuruni.kariview.client.listeners;

import it.yuruni.kariview.Kariview;
import it.yuruni.kariview.client.sound.RawAudio;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Kariview.MODID)
public class PlayerLogOutListener {
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        if (!RawAudio.activeSources.isEmpty()) {
            RawAudio.stopAll();
        }
    }
}
