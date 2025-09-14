package it.yuruni.kariview.packets.server2client;

import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.animation.AnimationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class StopViewPacket {
    private static final Logger LOGGER = LogUtils.getLogger();

    public StopViewPacket() {

    }

    public static void encode(StopViewPacket msg, FriendlyByteBuf buf) {

    }

    public static StopViewPacket decode(FriendlyByteBuf buf) {
        return new StopViewPacket();
    }

    public static void handle(StopViewPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(AnimationManager::stopAllAnimations);
        context.setPacketHandled(true);
    }
}
