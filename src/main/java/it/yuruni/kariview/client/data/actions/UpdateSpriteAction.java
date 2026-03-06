package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.animation.states.AnimationContext;
import it.yuruni.kariview.client.animation.SpriteManager;
import it.yuruni.kariview.client.animation.SpriteState;
import it.yuruni.kariview.client.data.elements.GuiElementData;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.List;

public class UpdateSpriteAction implements Action {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SerializedName("element_id")
    private String elementId;

    @SerializedName("update_interval")
    private long updateInterval;

    @SerializedName("loop")
    private boolean shouldLoop = true;

    public String getElementId() {
        return elementId;
    }

    public long getUpdateInterval() {
        return updateInterval;
    }

    public boolean shouldLoop() {
        return shouldLoop;
    }

    @Override
    public void execute(AnimationContext ctx) {
        GuiElementData elementData = ctx.currentAnimation.getElementById(elementId);
        if (elementData != null && elementData.getTexturePathPattern() != null) {
            String eId = elementData.getId();
            List<ResourceLocation> sprites = SpriteManager.loadSprites(ctx.currentAnimation.getNamespace(), elementData.getTexturePathPattern());
            if (!sprites.isEmpty()) {
                ctx.spriteStates.put(eId, new SpriteState(sprites));
                ctx.spriteUpdateIntervals.put(eId, updateInterval);
            } else {
                LOGGER.error("UpdateSpriteAction: No sprites found for pattern: " + elementData.getTexturePathPattern());
            }
        } else {
            LOGGER.error("UpdateSpriteAction: Element with ID '" + elementId + "' not found or has no texture_path_pattern.");
        }
    }
}
