package it.yuruni.kariview.packets.server2client;

import it.yuruni.kariview.client.KariviewRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class ShowGuiPacket {
    private static final Logger LOGGER = LogUtils.getLogger();

    public ShowGuiPacket() {
        // This packet currently holds no data.
    }

    public static void encode(ShowGuiPacket msg, FriendlyByteBuf buf) {
        // No data to encode for this simple packet.
    }

    public static ShowGuiPacket decode(FriendlyByteBuf buf) {
        return new ShowGuiPacket();
    }

    public static void handle(ShowGuiPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            //Toggle the GUI's active state
            KariviewRenderer.isGuiActive = true;
            LOGGER.info("Received show GUI packet on client! Toggling GUI state to active.");
        });
        context.setPacketHandled(true);
    }
}
