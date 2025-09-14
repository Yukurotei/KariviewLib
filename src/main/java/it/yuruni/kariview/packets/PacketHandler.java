package it.yuruni.kariview.packets;

import it.yuruni.kariview.Kariview;
import it.yuruni.kariview.packets.server2client.StopViewPacket;
import it.yuruni.kariview.packets.server2client.PlayAnimationPacket;
import it.yuruni.kariview.packets.server2client.ShowViewPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Kariview.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int packetId = 0;

    public static void register() {
        // Register our packet to be sent from the server to the client.
        INSTANCE.registerMessage(packetId++,
                ShowViewPacket.class,
                ShowViewPacket::encode,
                ShowViewPacket::decode,
                ShowViewPacket::handle);
        INSTANCE.registerMessage(packetId++,
                StopViewPacket.class,
                StopViewPacket::encode,
                StopViewPacket::decode,
                StopViewPacket::handle);
        INSTANCE.registerMessage(packetId++,
                PlayAnimationPacket.class,
                PlayAnimationPacket::encode,
                PlayAnimationPacket::decode,
                PlayAnimationPacket::handle);
    }

    public static <MSG> void sendToAll(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.with(() -> null), message);
    }
}
