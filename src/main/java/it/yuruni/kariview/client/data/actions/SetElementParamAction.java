package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import it.yuruni.kariview.client.GuiElement;
import it.yuruni.kariview.client.animation.AssetManager;
import it.yuruni.kariview.client.animation.SpriteState;
import it.yuruni.kariview.client.animation.states.AnimationContext;
import it.yuruni.kariview.client.data.VariableManager;
import net.minecraft.resources.ResourceLocation;

public class SetElementParamAction implements Action {
    @SerializedName("element_id")
    private String elementId;
    private String param;
    private String namespace;
    private String variable;

    @Override
    public void execute(AnimationContext ctx) {
        String value = VariableManager.get(namespace, variable);
        if (value == null) return;
        GuiElement element = ctx.activeElements.get(elementId);
        if (element == null && !"texture".equals(param) && !"sprite_index".equals(param)) return;

        try {
            switch (param) {
                case "x" -> { if (element != null) element.setX(Double.parseDouble(value)); }
                case "y" -> { if (element != null) element.setY(Double.parseDouble(value)); }
                case "scale" -> {
                    if (element != null) {
                        double s = Double.parseDouble(value);
                        element.setXScale(s);
                        element.setYScale(s);
                    }
                }
                case "opacity" -> { if (element != null) element.setOpacity(Float.parseFloat(value)); }
                case "angle" -> { if (element != null) element.setAngle(Double.parseDouble(value)); }
                case "sprite_index" -> {
                    SpriteState ss = ctx.spriteStates.get(elementId);
                    if (ss != null && element != null) {
                        int idx = Math.max(0, Math.min(Integer.parseInt(value), ss.getSprites().size() - 1));
                        ss.setCurrentIndex(idx);
                        element.setTexture(ss.getCurrentSprite());
                    }
                }
                case "texture" -> {
                    if (element != null) {
                        String ns = namespace != null ? namespace : ctx.currentAnimation.getNamespace();
                        ResourceLocation loc = AssetManager.loadTexture(ns, value);
                        if (loc != null) element.setTexture(loc);
                    }
                }
            }
        } catch (NumberFormatException ignored) {}
    }
}
