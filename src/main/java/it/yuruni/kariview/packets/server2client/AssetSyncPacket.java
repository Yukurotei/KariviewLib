package it.yuruni.kariview.packets.server2client;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Supplier;

public class AssetSyncPacket {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final String relativePath;
    private final byte[] data;

    public AssetSyncPacket(String relativePath, byte[] data) {
        this.relativePath = relativePath;
        this.data = data;
    }

    public static AssetSyncPacket decode(FriendlyByteBuf buf) {
        String path = buf.readUtf();
        byte[] data = buf.readByteArray();
        return new AssetSyncPacket(path, data);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(relativePath);
        buf.writeByteArray(data);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                File target = new File("kariviewlib/" + relativePath);
                target.getParentFile().mkdirs();
                try {
                    Files.write(target.toPath(), data);
                } catch (IOException e) {
                    LOGGER.error("Failed to write synced asset: {}", relativePath, e);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
