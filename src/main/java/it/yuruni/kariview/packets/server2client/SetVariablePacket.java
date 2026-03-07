package it.yuruni.kariview.packets.server2client;

import it.yuruni.kariview.client.data.VariableManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetVariablePacket {
    private final String namespace;
    private final String name;
    private final String value;

    public SetVariablePacket(String namespace, String name, String value) {
        this.namespace = namespace;
        this.name = name;
        this.value = value;
    }

    public static SetVariablePacket decode(FriendlyByteBuf buf) {
        return new SetVariablePacket(buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(namespace);
        buf.writeUtf(name);
        buf.writeUtf(value);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> VariableManager.set(namespace, name, value));
            context.setPacketHandled(true);
        }
    }
}
