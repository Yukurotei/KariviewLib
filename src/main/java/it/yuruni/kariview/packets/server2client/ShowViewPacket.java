package it.yuruni.kariview.packets.server2client;

import it.yuruni.kariview.client.KariviewRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class ShowViewPacket {
    private static final Logger LOGGER = LogUtils.getLogger();

    public ShowViewPacket() {

    }

    public static void encode(ShowViewPacket msg, FriendlyByteBuf buf) {

    }

    public static ShowViewPacket decode(FriendlyByteBuf buf) {
        return new ShowViewPacket();
    }

    public static void handle(ShowViewPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            //Toggle the GUI's active state
            KariviewRenderer.isGuiActive = true;
        });
        context.setPacketHandled(true);
    }
}
