package it.yuruni.kariview.packets.server2client;

import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.KariviewRenderer;
import it.yuruni.kariview.client.animation.AnimationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class HideGuiPacket {
    private static final Logger LOGGER = LogUtils.getLogger();

    public HideGuiPacket() {

    }

    public static void encode(HideGuiPacket msg, FriendlyByteBuf buf) {

    }

    public static HideGuiPacket decode(FriendlyByteBuf buf) {
        return new HideGuiPacket();
    }

    public static void handle(HideGuiPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Toggle the GUI's active state to false
            //KariviewRenderer.isGuiActive = false;
            AnimationManager.stopAllAnimations();
            LOGGER.info("Received hide GUI packet on client! Toggling GUI state to inactive.");
        });
        context.setPacketHandled(true);
    }
}
