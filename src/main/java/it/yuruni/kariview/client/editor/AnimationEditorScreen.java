package it.yuruni.kariview.client.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.yuruni.kariview.client.GuiElement;
import it.yuruni.kariview.client.animation.AssetManager;
import it.yuruni.kariview.client.animation.Easing;
import it.yuruni.kariview.client.data.AnimationData;
import it.yuruni.kariview.client.data.AnimationLoader;
import it.yuruni.kariview.client.data.Keyframe;
import it.yuruni.kariview.client.data.actions.*;
import it.yuruni.kariview.client.data.elements.GuiElementData;
import it.yuruni.kariview.client.sound.RawAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.util.*;

public class AnimationEditorScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int SIDEBAR_W  = 160;
    private static final int PROPS_W    = 190;
    private static final int TIMELINE_H = 90;
    private static final int HEADER_H   = 28;

    private static final int C_DARK_BG   = 0xFF1A1A1A;
    private static final int C_PANEL_BG  = 0xFF252526;
    private static final int C_HEADER_BG = 0xFF1E1E1E;
    private static final int C_SELECTED  = 0xFF2A6496;
    private static final int C_HOVER     = 0xFF2D4A5C;
    private static final int C_KEYFRAME  = 0xFFFFD700;
    private static final int C_SCRUBBER  = 0xFFFF4444;
    private static final int C_TEXT      = 0xFFE0E0E0;
    private static final int C_DIM       = 0xFF888888;
    private static final int C_BORDER    = 0xFF404040;
    private static final int C_TIMELINE  = 0xFF3C3C3C;
    private static final int C_CANVAS_BG = 0xFF0D0D0D;
    private static final int C_GRID      = 0xFF1A1A1A;
    private static final int C_CENTER    = 0xFF1E2E1E;

    private AnimationData currentAnimation = null;
    private String selectedElementId = null;
    private int selectedKeyframeIndex = -1;
    private long currentTime = 0;
    private boolean isPlaying = false;
    private long playStartMs = 0;
    private boolean isDraggingScrubber = false;

    private int animScrollOffset = 0;
    private int kfScrollOffset = 0;

    private final Map<String, double[]> previewPos      = new LinkedHashMap<>();
    private final Map<String, ResourceLocation> previewTex  = new LinkedHashMap<>();
    private final Map<String, int[]> previewDims        = new LinkedHashMap<>();

    private List<String> animationKeys = new ArrayList<>();
    private final Set<Integer> playedSoundKeyframes = new HashSet<>();

    public AnimationEditorScreen() {
        super(Component.literal("KariView Animation Editor"));
    }

    @Override
    protected void init() {
        super.init();
        refreshAnimationList();

        int mid = width / 2;

        addRenderableWidget(Button.builder(Component.literal("◀ Back"),
                btn -> onClose()
        ).bounds(4, 4, 55, 20).build());

        addRenderableWidget(Button.builder(Component.literal("▶"),
                btn -> togglePlay()
        ).bounds(mid - 38, 4, 24, 20).build());

        addRenderableWidget(Button.builder(Component.literal("■"),
                btn -> { isPlaying = false; currentTime = 0; playedSoundKeyframes.clear(); RawAudio.stopAll(); evaluateAtTime(0); }
        ).bounds(mid - 12, 4, 24, 20).build());

        addRenderableWidget(Button.builder(Component.literal("↻"),
                btn -> { AnimationLoader.loadAllAnimations(); refreshAnimationList(); }
        ).bounds(mid + 14, 4, 24, 20).build());
    }

    private void togglePlay() {
        if (currentAnimation == null) return;
        if (!isPlaying) {
            isPlaying = true;
            playStartMs = System.currentTimeMillis() - currentTime;
        } else {
            isPlaying = false;
            RawAudio.stopAll();
        }
    }

    private void refreshAnimationList() {
        LOGGER.info("[Editor] Cache before load: {} entries", AnimationLoader.ANIMATION_CACHE.size());
        AnimationLoader.loadAllAnimations();
        LOGGER.info("[Editor] Cache after load: {} entries", AnimationLoader.ANIMATION_CACHE.size());
        animationKeys = new ArrayList<>(AnimationLoader.ANIMATION_CACHE.keySet());
        LOGGER.info("[Editor] Animation keys: {}", animationKeys);
        Collections.sort(animationKeys);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        if (isPlaying && currentAnimation != null) {
            long total = currentAnimation.getTotalDuration();
            currentTime = System.currentTimeMillis() - playStartMs;
            if (total > 0 && currentTime >= total) {
                currentTime = total;
                isPlaying = false;
            }
            evaluateAtTime(currentTime);
        }

        g.fill(0, 0, width, height, C_DARK_BG);
        renderHeader(g);
        renderSidebar(g, mx, my);
        renderCanvas(g, mx, my);
        renderProperties(g, mx, my);
        renderTimeline(g, mx, my);
        super.render(g, mx, my, partialTick);
    }

    private void renderHeader(GuiGraphics g) {
        g.fill(0, 0, width, HEADER_H, C_HEADER_BG);
        g.fill(0, HEADER_H - 1, width, HEADER_H, C_BORDER);
        int tx = 64;
        g.drawString(font, "KariView Animation Editor", tx, 9, C_TEXT);
        if (currentAnimation != null) {
            String sub = "  |  " + currentAnimation.getNamespace() + ":" + currentAnimation.getId()
                    + "  [" + currentAnimation.getTotalDuration() + "ms]";
            g.drawString(font, sub, tx + font.width("KariView Animation Editor"), 9, C_DIM);
        }
    }

    private void renderSidebar(GuiGraphics g, int mx, int my) {
        int top = HEADER_H, bot = height - TIMELINE_H;
        g.fill(0, top, SIDEBAR_W, bot, C_PANEL_BG);
        g.fill(SIDEBAR_W - 1, top, SIDEBAR_W, bot, C_BORDER);

        int y = top + 4;
        g.drawString(font, "ANIMATIONS", 4, y, C_DIM);
        y += 12;

        int itemH = 12;
        int animAreaBot = bot - 80;
        int visCount = Math.max(1, (animAreaBot - y) / itemH);
        LOGGER.debug("[Editor] Sidebar: animationKeys={} animAreaBot={} y={} visCount={}", animationKeys.size(), animAreaBot, y, visCount);
        animScrollOffset = Math.max(0, Math.min(animScrollOffset, Math.max(0, animationKeys.size() - visCount)));

        for (int i = animScrollOffset; i < animationKeys.size() && y + itemH <= animAreaBot; i++) {
            String key = animationKeys.get(i);
            boolean sel = currentAnimation != null &&
                    (currentAnimation.getNamespace() + ":" + currentAnimation.getId()).equals(key);
            boolean hov = mx >= 0 && mx < SIDEBAR_W && my >= y && my < y + itemH;
            if (sel)       g.fill(0, y, SIDEBAR_W - 1, y + itemH, C_SELECTED);
            else if (hov)  g.fill(0, y, SIDEBAR_W - 1, y + itemH, C_HOVER);
            String label = key.length() > 22 ? key.substring(0, 20) + ".." : key;
            g.drawString(font, label, 4, y + 2, sel ? 0xFFFFFFFF : C_TEXT);
            y += itemH;
        }

        g.fill(0, animAreaBot, SIDEBAR_W - 1, animAreaBot + 1, C_BORDER);
        y = animAreaBot + 4;
        g.drawString(font, "ELEMENTS", 4, y, C_DIM);
        y += 12;

        if (currentAnimation != null && currentAnimation.getElements() != null) {
            for (GuiElementData el : currentAnimation.getElements()) {
                if (y + itemH > bot) break;
                boolean sel = el.getId().equals(selectedElementId);
                boolean hov = mx >= 0 && mx < SIDEBAR_W && my >= y && my < y + itemH;
                if (sel)       g.fill(0, y, SIDEBAR_W - 1, y + itemH, C_SELECTED);
                else if (hov)  g.fill(0, y, SIDEBAR_W - 1, y + itemH, C_HOVER);
                boolean active = previewPos.containsKey(el.getId());
                String label = el.getId().length() > 20 ? el.getId().substring(0, 18) + ".." : el.getId();
                g.drawString(font, label, 4, y + 2, active ? C_TEXT : C_DIM);
                y += itemH;
            }
        }
    }

    private int cLeft()  { return SIDEBAR_W; }
    private int cRight() { return width - PROPS_W; }
    private int cTop()   { return HEADER_H; }
    private int cBot()   { return height - TIMELINE_H; }
    private int cW()     { return cRight() - cLeft(); }
    private int cH()     { return cBot() - cTop(); }

    /** Returns {previewLeft, previewTop, previewWidth, previewHeight} for the 16:9 locked preview rect. */
    private int[] previewRect() {
        int cl = cLeft(), ct = cTop(), cw = cW(), ch = cH();
        int pad = 10;
        int aw = cw - pad * 2, ah = ch - pad * 2;
        int pw, ph;
        if ((float) aw / ah > 16f / 9f) {
            ph = ah; pw = ph * 16 / 9;
        } else {
            pw = aw; ph = pw * 9 / 16;
        }
        int pl = cl + (cw - pw) / 2;
        int pt = ct + (ch - ph) / 2;
        return new int[]{ pl, pt, pw, ph };
    }

    private void renderCanvas(GuiGraphics g, int mx, int my) {
        int cl = cLeft(), cr = cRight(), ct = cTop(), cb = cBot();
        int cw = cW(), ch = cH();

        // Outer canvas background
        g.fill(cl, ct, cr, cb, C_CANVAS_BG);

        // 16:9 preview rectangle (acts as the "screen")
        int[] pr = previewRect();
        int pl = pr[0], pt = pr[1], pw = pr[2], ph = pr[3];

        g.fill(pl, pt, pl + pw, pt + ph, 0xFF0A0A14);

        // Center guides
        g.fill(pl + pw / 2, pt, pl + pw / 2 + 1, pt + ph, C_CENTER);
        g.fill(pl, pt + ph / 2, pl + pw, pt + ph / 2 + 1, C_CENTER);

        if (currentAnimation == null) {
            String msg = "Select an animation from the left panel";
            g.drawString(font, msg, cl + (cw - font.width(msg)) / 2, ct + ch / 2 - 4, C_DIM);
        } else {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            for (Map.Entry<String, double[]> entry : previewPos.entrySet()) {
                String id = entry.getKey();
                double[] pos = entry.getValue();
                ResourceLocation tex = previewTex.get(id);
                int[] dims = previewDims.getOrDefault(id, new int[]{64, 64});
                if (tex == null) continue;

                // Match GuiElement.render() exactly:
                // pw = "screenWidth", ph = "screenHeight" for our preview
                int initialW = (int)(pos[2] * pw);
                int initialH = (int)(pos[2] * ph);
                int scaledW = (int)(initialW * pos[3]);
                int scaledH = (int)(initialH * pos[4]);

                // Aspect ratio correction
                if (dims[0] > 0 && dims[1] > 0) {
                    float aspect = (float) dims[0] / dims[1];
                    if (scaledH > scaledW / aspect) {
                        scaledH = (int)(scaledW / aspect);
                    } else {
                        scaledW = (int)(scaledH * aspect);
                    }
                }

                // Apply scale again (matches GuiElement double-apply)
                scaledW = (int)(scaledW * pos[3]);
                scaledH = (int)(scaledH * pos[4]);

                int drawW = Math.max(1, scaledW);
                int drawH = Math.max(1, scaledH);

                // Center offset (matches GuiElement)
                int initialX = pl + (int)(pos[0] * pw);
                int initialY = pt + (int)(pos[1] * ph);
                int drawX = initialX - (drawW - initialW) / 2;
                int drawY = initialY - (drawH - initialH) / 2;

                RenderSystem.setShaderColor(1f, 1f, 1f, (float) Math.max(0, Math.min(1, pos[5])));

                // Rotation support
                double angle = pos.length > 6 ? pos[6] : 0;
                if (angle != 0) {
                    PoseStack pose = g.pose();
                    pose.pushPose();
                    float cx = drawX + drawW / 2f;
                    float cy = drawY + drawH / 2f;
                    pose.translate(cx, cy, 0);
                    pose.mulPose(Axis.ZP.rotationDegrees((float) angle));
                    pose.translate(-cx, -cy, 0);
                    g.blit(tex, drawX, drawY, 0, 0, drawW, drawH, drawW, drawH);
                    pose.popPose();
                } else {
                    g.blit(tex, drawX, drawY, 0, 0, drawW, drawH, drawW, drawH);
                }

                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

                if (id.equals(selectedElementId)) {
                    int sc = 0xFFFFFFFF;
                    g.fill(drawX - 1, drawY - 1, drawX + drawW + 1, drawY,                      sc);
                    g.fill(drawX - 1, drawY + drawH, drawX + drawW + 1, drawY + drawH + 1,      sc);
                    g.fill(drawX - 1, drawY, drawX, drawY + drawH,                              sc);
                    g.fill(drawX + drawW, drawY, drawX + drawW + 1, drawY + drawH,              sc);
                }
            }
            RenderSystem.disableBlend();
        }

        // Preview border
        g.fill(pl - 1, pt - 1, pl + pw + 1, pt,          0xFF555566);
        g.fill(pl - 1, pt + ph, pl + pw + 1, pt + ph + 1, 0xFF555566);
        g.fill(pl - 1, pt - 1, pl,           pt + ph + 1, 0xFF555566);
        g.fill(pl + pw, pt - 1, pl + pw + 1, pt + ph + 1, 0xFF555566);

        g.drawString(font, "16:9", pl + 2, pt + 2, 0xFF333344);

        // Canvas border
        g.fill(cl, ct, cr, ct + 1, C_BORDER);
        g.fill(cl, cb - 1, cr, cb, C_BORDER);
        g.fill(cl, ct, cl + 1, cb, C_BORDER);
        g.fill(cr - 1, ct, cr, cb, C_BORDER);
    }

    private void renderProperties(GuiGraphics g, int mx, int my) {
        int px = width - PROPS_W, top = HEADER_H, bot = height - TIMELINE_H;
        g.fill(px, top, width, bot, C_PANEL_BG);
        g.fill(px, top, px + 1, bot, C_BORDER);

        int x = px + 4, y = top + 6;

        g.drawString(font, "ELEMENT", x, y, C_DIM);
        y += 12;

        if (selectedElementId != null && previewPos.containsKey(selectedElementId)) {
            double[] pos = previewPos.get(selectedElementId);
            g.drawString(font, selectedElementId, x, y, C_TEXT);
            y += 11;
            g.fill(px + 2, y, width - 2, y + 1, C_BORDER);
            y += 4;
            String[] labels = { "X", "Y", "Scale", "xMult", "yMult", "Opacity", "Angle" };
            for (int i = 0; i < labels.length && i < pos.length; i++) {
                g.drawString(font, labels[i] + ": " + String.format("%.3f", pos[i]), x, y, C_TEXT);
                y += 10;
            }
        } else {
            g.drawString(font, "None selected", x, y, C_DIM);
            y += 11;
        }

        y += 6;
        g.fill(px + 2, y, width - 2, y + 1, C_BORDER);
        y += 4;

        g.drawString(font, "KEYFRAMES", x, y, C_DIM);
        y += 12;

        if (currentAnimation != null && currentAnimation.getKeyframes() != null) {
            List<Keyframe> kfs = currentAnimation.getKeyframes();
            int maxKf = Math.max(0, kfs.size() - 1);
            kfScrollOffset = Math.max(0, Math.min(kfScrollOffset, maxKf));
            for (int i = kfScrollOffset; i < kfs.size() && y + 11 <= bot; i++) {
                Keyframe kf = kfs.get(i);
                boolean sel = i == selectedKeyframeIndex;
                boolean hov = mx >= px && mx < width && my >= y && my < y + 11;
                if (sel)      g.fill(px, y, width, y + 11, C_SELECTED);
                else if (hov) g.fill(px, y, width, y + 11, C_HOVER);
                int acts = kf.getActions() != null ? kf.getActions().size() : 0;
                g.drawString(font, kf.getTimestamp() + "ms  (" + acts + ")", x, y + 2,
                        sel ? 0xFFFFFFFF : C_TEXT);
                y += 11;
            }
        }
    }

    private void renderTimeline(GuiGraphics g, int mx, int my) {
        int ty = height - TIMELINE_H;
        g.fill(0, ty, width, height, C_PANEL_BG);
        g.fill(0, ty, width, ty + 1, C_BORDER);

        long total = currentAnimation != null ? Math.max(1, currentAnimation.getTotalDuration()) : 1;
        int trackL = SIDEBAR_W + 8, trackR = width - PROPS_W - 8;
        int trackW = trackR - trackL;
        int trackY = ty + 38;

        g.fill(trackL, trackY, trackR, trackY + 4, C_TIMELINE);

        if (currentAnimation != null && currentAnimation.getKeyframes() != null) {
            for (Keyframe kf : currentAnimation.getKeyframes()) {
                int kx = trackL + (int)((double) kf.getTimestamp() / total * trackW);
                g.fill(kx - 1, trackY - 5, kx + 2, trackY + 9, C_KEYFRAME);
            }
        }

        int sx = trackL + (int)((double) currentTime / total * trackW);
        g.fill(sx - 1, trackY - 8, sx + 2, trackY + 12, C_SCRUBBER);
        g.fill(sx - 4, trackY - 8, sx + 5, trackY - 4, C_SCRUBBER);

        g.drawString(font, "0", trackL, trackY + 8, C_DIM);
        String totalStr = total + "ms";
        g.drawString(font, totalStr, trackR - font.width(totalStr), trackY + 8, C_DIM);

        String curStr = currentTime + "ms";
        int curX = Math.max(trackL, Math.min(sx - font.width(curStr) / 2, trackR - font.width(curStr)));
        g.drawString(font, curStr, curX, ty + 12, C_TEXT);

        String status = isPlaying ? "▶ Playing" : "|| Paused";
        g.drawString(font, status, SIDEBAR_W + 8, ty + 56, isPlaying ? 0xFF44CC44 : C_DIM);

        if (currentAnimation != null) {
            String info = currentAnimation.getEndAction().equals("wait") ? "[WAIT]" : "[STOP]";
            g.drawString(font, info, SIDEBAR_W + 8 + font.width(status) + 8, ty + 56, C_DIM);
        }

        String hint = "SPACE play/pause  ◄► step 100ms  click/drag timeline to scrub";
        g.drawString(font, hint, trackL, ty + 68, C_DIM);
    }

    private void evaluateAtTime(long time) {
        previewPos.clear();
        previewTex.clear();
        previewDims.clear();
        if (currentAnimation == null || currentAnimation.getKeyframes() == null) return;

        // Remove played-sound tracking for keyframes past current time
        playedSoundKeyframes.removeIf(idx -> idx >= 0 &&
                idx < currentAnimation.getKeyframes().size() &&
                currentAnimation.getKeyframes().get(idx).getTimestamp() > time);

        List<Keyframe> keyframes = currentAnimation.getKeyframes();
        for (int ki = 0; ki < keyframes.size(); ki++) {
            Keyframe kf = keyframes.get(ki);
            if (kf.getTimestamp() > time) break;
            if (kf.getActions() == null) continue;
            long elapsed = time - kf.getTimestamp();

            for (Action action : kf.getActions()) {
                if (action instanceof ShowElementAction sea) {
                    GuiElementData data = currentAnimation.getElementById(sea.getElementId());
                    ResourceLocation tex = null;
                    if (data != null && data.getTexture() != null)
                        tex = AssetManager.loadTexture(currentAnimation.getNamespace(), data.getTexture());
                    float op = sea.getStartOpacity() != null ? sea.getStartOpacity() : 1.0f;
                    // pos: {x, y, scale, xScale, yScale, opacity, angle}
                    previewPos.put(sea.getElementId(),
                            new double[]{ sea.getX(), sea.getY(), sea.getWidth(), 1.0, 1.0, op, 0.0 });
                    previewTex.put(sea.getElementId(), tex);
                    previewDims.put(sea.getElementId(),
                            new int[]{ sea.getTextureWidth(), sea.getTextureHeight() });

                } else if (action instanceof HideElementAction hea) {
                    previewPos.remove(hea.getElementId());
                    previewTex.remove(hea.getElementId());
                    previewDims.remove(hea.getElementId());

                } else if (action instanceof MoveAction ma) {
                    double[] pos = previewPos.get(ma.getElementId());
                    if (pos == null) continue;
                    double prog = progress(elapsed, ma.getDuration(), ma.getEasingType());
                    if (prog >= 1.0) {
                        pos[0] = ma.getTargetX();
                        pos[1] = ma.getTargetY();
                    } else {
                        double sx = pos[0], sy = pos[1];
                        pos[0] = sx + (ma.getTargetX() - sx) * prog;
                        pos[1] = sy + (ma.getTargetY() - sy) * prog;
                    }

                } else if (action instanceof ScaleAction sa) {
                    double[] pos = previewPos.get(sa.getElementId());
                    if (pos == null) continue;
                    double prog = progress(elapsed, sa.getDuration(), sa.getEasingType());
                    // ScaleAction targets both xScale and yScale uniformly
                    double startX = pos[3], startY = pos[4];
                    // Matching ScaleAction.execute: target is halved if scaling up
                    double targetX = startX < sa.getTargetScale() ? sa.getTargetScale() / 2 : sa.getTargetScale();
                    double targetY = startY < sa.getTargetScale() ? sa.getTargetScale() / 2 : sa.getTargetScale();
                    if (prog >= 1.0) {
                        pos[3] = targetX;
                        pos[4] = targetY;
                    } else {
                        pos[3] = startX + (targetX - startX) * prog;
                        pos[4] = startY + (targetY - startY) * prog;
                    }

                } else if (action instanceof ExtendAction ea) {
                    double[] pos = previewPos.get(ea.getElementId());
                    if (pos == null) continue;
                    double prog = progress(elapsed, ea.getDuration(), ea.getEasingType());
                    String dir = ea.getDirection();
                    if ("LEFT".equals(dir) || "RIGHT".equals(dir)) {
                        double startX = pos[3];
                        if (prog >= 1.0) {
                            pos[3] = ea.getTargetValue();
                        } else {
                            pos[3] = startX + (ea.getTargetValue() - startX) * prog;
                        }
                    } else { // UP or DOWN
                        double startY = pos[4];
                        if (prog >= 1.0) {
                            pos[4] = ea.getTargetValue();
                        } else {
                            pos[4] = startY + (ea.getTargetValue() - startY) * prog;
                        }
                    }

                } else if (action instanceof RotateAction ra) {
                    double[] pos = previewPos.get(ra.getElementId());
                    if (pos == null) continue;
                    double prog = progress(elapsed, ra.getDuration(), ra.getEasingType());
                    double startAngle = pos[6];
                    if (prog >= 1.0) {
                        pos[6] = ra.getTargetAngle();
                    } else {
                        pos[6] = startAngle + (ra.getTargetAngle() - startAngle) * prog;
                    }

                } else if (action instanceof ChangeOpacityAction cao) {
                    double[] pos = previewPos.get(cao.getElementId());
                    if (pos == null) continue;
                    double prog = progress(elapsed, cao.getDuration(), cao.getEasingType());
                    double startOp = pos[5];
                    if (prog >= 1.0) {
                        pos[5] = cao.getTargetOpacity();
                    } else {
                        pos[5] = startOp + (cao.getTargetOpacity() - startOp) * prog;
                    }

                } else if (action instanceof PlaySoundAction psa) {
                    // Play sound only once per keyframe during playback
                    if (isPlaying && !playedSoundKeyframes.contains(ki)) {
                        File soundFile = AssetManager.loadSound(currentAnimation.getNamespace(), psa.getSoundId());
                        if (soundFile != null) {
                            RawAudio.playOgg(soundFile.getAbsolutePath(), psa.getVolume());
                        }
                        playedSoundKeyframes.add(ki);
                    }
                }
            }
        }
    }

    private double progress(long elapsed, long duration, String easing) {
        if (duration <= 0 || elapsed >= duration) return 1.0;
        return Easing.getEasedProgress(easing != null ? easing : "linear", (double) elapsed / duration);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;

        int top = HEADER_H, bot = height - TIMELINE_H;
        int itemH = 12;

        // Animation list
        int y = top + 16;
        int animAreaBot = bot - 80;
        int visCount = Math.max(1, (animAreaBot - y) / itemH);
        animScrollOffset = Math.max(0, Math.min(animScrollOffset, Math.max(0, animationKeys.size() - visCount)));
        for (int i = animScrollOffset; i < animationKeys.size() && y + itemH <= animAreaBot; i++) {
            if (mx >= 0 && mx < SIDEBAR_W && my >= y && my < y + itemH) {
                AnimationData data = AnimationLoader.ANIMATION_CACHE.get(animationKeys.get(i));
                if (data != null) {
                    RawAudio.stopAll();
                    playedSoundKeyframes.clear();
                    currentAnimation = data;
                    selectedElementId = null;
                    selectedKeyframeIndex = -1;
                    currentTime = 0;
                    isPlaying = false;
                    evaluateAtTime(0);
                }
                return true;
            }
            y += itemH;
        }

        // Element list
        y = animAreaBot + 16;
        if (currentAnimation != null && currentAnimation.getElements() != null) {
            for (GuiElementData el : currentAnimation.getElements()) {
                if (y + itemH > bot) break;
                if (mx >= 0 && mx < SIDEBAR_W && my >= y && my < y + itemH) {
                    selectedElementId = el.getId();
                    return true;
                }
                y += itemH;
            }
        }

        // Keyframe list (right panel)
        int px = width - PROPS_W;
        if (mx >= px && currentAnimation != null && currentAnimation.getKeyframes() != null) {
            int ky = top + 6 + 12;
            if (selectedElementId != null && previewPos.containsKey(selectedElementId))
                ky += 11 + 4 + 6 * 10;
            else
                ky += 11;
            ky += 6 + 4 + 12;
            List<Keyframe> kfs = currentAnimation.getKeyframes();
            for (int i = kfScrollOffset; i < kfs.size() && ky + 11 <= bot; i++) {
                if (my >= ky && my < ky + 11) {
                    selectedKeyframeIndex = i;
                    currentTime = kfs.get(i).getTimestamp();
                    evaluateAtTime(currentTime);
                    return true;
                }
                ky += 11;
            }
        }

        // Canvas element selection
        int cl = cLeft(), cr = cRight(), ct = cTop(), cb = cBot();
        if (mx >= cl && mx < cr && my >= ct && my < cb) {
            int[] pr = previewRect();
            int pl = pr[0], pt = pr[1], pw = pr[2], ph = pr[3];
            selectedElementId = null;
            List<String> ids = new ArrayList<>(previewPos.keySet());
            Collections.reverse(ids);
            for (String id : ids) {
                double[] pos = previewPos.get(id);
                int[] dims = previewDims.getOrDefault(id, new int[]{64, 64});
                // Match GuiElement.render() math
                int initialW = (int)(pos[2] * pw);
                int initialH = (int)(pos[2] * ph);
                int scaledW = (int)(initialW * pos[3]);
                int scaledH = (int)(initialH * pos[4]);
                if (dims[0] > 0 && dims[1] > 0) {
                    float aspect = (float) dims[0] / dims[1];
                    if (scaledH > scaledW / aspect) scaledH = (int)(scaledW / aspect);
                    else scaledW = (int)(scaledH * aspect);
                }
                scaledW = (int)(scaledW * pos[3]);
                scaledH = (int)(scaledH * pos[4]);
                int drawW = Math.max(1, scaledW);
                int drawH = Math.max(1, scaledH);
                int initialX = pl + (int)(pos[0] * pw);
                int initialY = pt + (int)(pos[1] * ph);
                int drawX = initialX - (drawW - initialW) / 2;
                int drawY = initialY - (drawH - initialH) / 2;
                if (mx >= drawX && mx < drawX + drawW && my >= drawY && my < drawY + drawH) {
                    selectedElementId = id;
                    break;
                }
            }
            return true;
        }

        // Timeline scrub
        int ty = height - TIMELINE_H;
        int trackL = SIDEBAR_W + 8, trackR = width - PROPS_W - 8;
        if (my >= ty && my < height && mx >= trackL && mx <= trackR) {
            seekTo(mx, trackL, trackR - trackL);
            isDraggingScrubber = true;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (isDraggingScrubber) {
            int trackL = SIDEBAR_W + 8, trackR = width - PROPS_W - 8;
            seekTo(mx, trackL, trackR - trackL);
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        isDraggingScrubber = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int bot = height - TIMELINE_H;
        if (mx < SIDEBAR_W) {
            animScrollOffset = Math.max(0, animScrollOffset - (int) delta);
            return true;
        }
        if (mx >= width - PROPS_W) {
            kfScrollOffset = Math.max(0, kfScrollOffset - (int) delta);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 32) { togglePlay(); return true; }
        if (key == 263 && !isPlaying && currentAnimation != null) {
            currentTime = Math.max(0, currentTime - 100);
            evaluateAtTime(currentTime);
            return true;
        }
        if (key == 262 && !isPlaying && currentAnimation != null) {
            currentTime = Math.min(currentAnimation.getTotalDuration(), currentTime + 100);
            evaluateAtTime(currentTime);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void seekTo(double mx, int trackL, int trackW) {
        long total = currentAnimation != null ? Math.max(1, currentAnimation.getTotalDuration()) : 1;
        currentTime = (long)(((mx - trackL) / trackW) * total);
        currentTime = Math.max(0, Math.min(currentTime, total));
        if (isPlaying) playStartMs = System.currentTimeMillis() - currentTime;
        evaluateAtTime(currentTime);
    }

    @Override
    public void onClose() {
        RawAudio.stopAll();
        Minecraft.getInstance().setScreen(new TitleScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
