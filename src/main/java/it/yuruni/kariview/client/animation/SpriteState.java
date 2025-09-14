package it.yuruni.kariview.client.animation;


import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class SpriteState {
    private final List<ResourceLocation> sprites;
    private int currentIndex;
    private long lastUpdateTime;

    public SpriteState(List<ResourceLocation> sprites) {
        this.sprites = sprites;
        this.currentIndex = 0;
        this.lastUpdateTime = 0;
    }

    public List<ResourceLocation> getSprites() {
        return sprites;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public ResourceLocation getCurrentSprite() {
        if (sprites.isEmpty()) {
            return null;
        }
        return sprites.get(currentIndex);
    }
}