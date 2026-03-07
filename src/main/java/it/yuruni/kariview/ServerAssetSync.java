package it.yuruni.kariview;

import com.mojang.logging.LogUtils;
import it.yuruni.kariview.packets.PacketHandler;
import it.yuruni.kariview.packets.server2client.AssetSyncCompletePacket;
import it.yuruni.kariview.packets.server2client.AssetSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = Kariview.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerAssetSync {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getServer() == null || !player.getServer().isDedicatedServer()) return;

        File kariviewDir = new File("kariviewlib");
        if (!kariviewDir.exists()) return;

        try (Stream<Path> walk = Files.walk(kariviewDir.toPath())) {
            walk.filter(Files::isRegularFile).forEach(path -> {
                try {
                    String relativePath = kariviewDir.toPath().relativize(path).toString().replace(File.separator, "/");
                    byte[] data = Files.readAllBytes(path);
                    PacketHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new AssetSyncPacket(relativePath, data)
                    );
                } catch (IOException e) {
                    LOGGER.error("Failed to send asset: {}", path, e);
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to walk kariviewlib directory", e);
        }

        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new AssetSyncCompletePacket()
        );
    }
}
