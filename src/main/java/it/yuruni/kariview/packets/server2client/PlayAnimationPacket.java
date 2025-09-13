package it.yuruni.kariview.packets.server2client;

import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.animation.AnimationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class PlayAnimationPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final String namespace;
    private final String animationId;

    public PlayAnimationPacket(String namespace, String animationId) {
        this.namespace = namespace;
        this.animationId = animationId;
    }

    public static PlayAnimationPacket decode(FriendlyByteBuf buf) {
        return new PlayAnimationPacket(buf.readUtf(), buf.readUtf());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.namespace);
        buf.writeUtf(this.animationId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                AnimationManager.playAnimation(this.namespace, this.animationId);
            });
            context.setPacketHandled(true);
        }
    }

    public String getNamespace() {
        return namespace;
    }

    public String getAnimationId() {
        return animationId;
    }
}