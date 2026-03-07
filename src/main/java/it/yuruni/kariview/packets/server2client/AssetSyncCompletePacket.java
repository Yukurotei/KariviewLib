package it.yuruni.kariview.packets.server2client;

import it.yuruni.kariview.client.animation.AnimationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AssetSyncCompletePacket {

    public static AssetSyncCompletePacket decode(FriendlyByteBuf buf) {
        return new AssetSyncCompletePacket();
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(AnimationManager::reload);
            context.setPacketHandled(true);
        }
    }
}
