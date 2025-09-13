package it.yuruni.kariview;

import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.data.AnimationLoader;
import it.yuruni.kariview.packets.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Kariview.MODID)
public class Kariview {
    public static final String MODID = "kariviewlib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Kariview() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        //Client event for registering GUI renderers
        modEventBus.addListener(this::onClientSetup);
        MinecraftForge.EVENT_BUS.register(this);

        PacketHandler.register();

        LOGGER.info("Kariview mod initialized!");
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Kariview client setup complete!");
        AnimationLoader.ensureMainDirectoryExists();
    }
}