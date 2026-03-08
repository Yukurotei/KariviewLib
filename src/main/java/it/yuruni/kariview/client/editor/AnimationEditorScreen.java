package it.yuruni.kariview.client.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.*;
import java.util.ArrayDeque;
import java.util.Deque;

public class AnimationEditorScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int SIDEBAR_W  = 160;
    private static final int PROPS_W    = 200;
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
    private static final int C_CENTER    = 0xFF1E2E1E;
    private static final int C_BTN_BG    = 0xFF333333;
    private static final int C_BTN_HOV   = 0xFF444444;
    private static final int C_ACCENT    = 0xFF4488CC;

    // Animation state
    private AnimationData currentAnimation = null;
    private String selectedElementId = null;
    private int selectedKeyframeIndex = -1;
    private int selectedActionIndex = -1;
    private long currentTime = 0;
    private boolean isPlaying = false;
    private long playStartMs = 0;
    private boolean isDraggingScrubber = false;
    private boolean unsavedChanges = false;

    // Scrolling
    private int animScrollOffset = 0;
    private int elemScrollOffset = 0;
    private int kfScrollOffset = 0;
    private int actionScrollOffset = 0;
    private int propScrollOffset = 0;

    // Tracked sidebar Y boundaries
    private int rAnimListY, rAnimListBot, rElemListY, rElemListBot;

    // Preview state
    private final Map<String, double[]> previewPos      = new LinkedHashMap<>();
    private final Map<String, ResourceLocation> previewTex  = new LinkedHashMap<>();
    private final Map<String, int[]> previewDims        = new LinkedHashMap<>();

    private List<String> animationKeys = new ArrayList<>();
    private final Set<Integer> playedSoundKeyframes = new HashSet<>();

    // Canvas drag
    private boolean isDraggingElement = false;
    private String dragElementId = null;
    private double dragStartMx, dragStartMy;
    private double dragOrigX, dragOrigY;

    // Action type menu
    private boolean showActionMenu = false;
    private int actionMenuY = 0;

    // Property editing
    private String editingPropKey = null;
    private EditBox propEditBox = null;

    // Tracked Y positions from render pass (used for click handling)
    private int rKfBtnY, rActBtnY, rKfListY, rKfListBot, rActListY, rActListBot, rPropListY;
    // Tracked sidebar button positions
    private int rAddAnimBtnX, rDelAnimBtnX, rAnimBtnY;
    private int rAddElemBtnX, rDelElemBtnX, rElemBtnY;

    // Undo/redo
    private final Deque<String> undoStack = new ArrayDeque<>();
    private final Deque<String> redoStack = new ArrayDeque<>();
    private static final int MAX_UNDO = 50;

    // Modal overlay
    private enum ModalType { NONE, NEW_ANIM, NEW_ELEM }
    private ModalType modal = ModalType.NONE;
    private EditBox modalBoxA = null; // namespace / element id
    private EditBox modalBoxB = null; // animation id / texture path
    private boolean newElemIsSprite = false;

    // Action type registry for the add menu
    private static final String[][] ACTION_TYPES = {
        {"show_element",     "Show Element"},
        {"hide_element",     "Hide Element"},
        {"move_element",     "Move"},
        {"scale_element",    "Scale"},
        {"rotate_element",   "Rotate"},
        {"change_opacity",   "Opacity"},
        {"extend_element",   "Extend"},
        {"play_sound",       "Play Sound"},
    };

    public AnimationEditorScreen() {
        super(Component.literal("KariView Animation Editor"));
    }

    @Override
    protected void init() {
        super.init();
        refreshAnimationList();

        int mid = width / 2;

        addRenderableWidget(Button.builder(Component.literal("Back"),
                btn -> onClose()
        ).bounds(4, 4, 40, 20).build());

        addRenderableWidget(Button.builder(Component.literal("\u25B6"),
                btn -> togglePlay()
        ).bounds(mid - 50, 4, 24, 20).build());

        addRenderableWidget(Button.builder(Component.literal("\u25A0"),
                btn -> { isPlaying = false; currentTime = 0; playedSoundKeyframes.clear(); RawAudio.stopAll(); evaluateAtTime(0); }
        ).bounds(mid - 24, 4, 24, 20).build());

        addRenderableWidget(Button.builder(Component.literal("\u21BB"),
                btn -> { AnimationLoader.loadAllAnimations(); refreshAnimationList(); }
        ).bounds(mid + 2, 4, 24, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Save"),
                btn -> saveCurrentAnimation()
        ).bounds(mid + 28, 4, 36, 20).build());

        // Property edit box (hidden until a property is clicked)
        propEditBox = new EditBox(font, width - PROPS_W + 4, height - TIMELINE_H - 18, PROPS_W - 8, 14, Component.empty());
        propEditBox.setVisible(false);
        propEditBox.setMaxLength(64);
        propEditBox.setResponder(val -> {});
        addRenderableWidget(propEditBox);

        // Modal input boxes (hidden until a modal is opened)
        int mw = 200, mh = 130;
        int mleft = (width - mw) / 2, mtop = (height - mh) / 2;
        modalBoxA = new EditBox(font, mleft + 10, mtop + 34, mw - 20, 14, Component.empty());
        modalBoxA.setVisible(false);
        modalBoxA.setMaxLength(64);
        addRenderableWidget(modalBoxA);

        modalBoxB = new EditBox(font, mleft + 10, mtop + 64, mw - 20, 14, Component.empty());
        modalBoxB.setVisible(false);
        modalBoxB.setMaxLength(128);
        addRenderableWidget(modalBoxB);
    }

    private void togglePlay() {
        if (currentAnimation == null) return;
        if (!isPlaying) {
            isPlaying = true;
            playStartMs = System.currentTimeMillis() - currentTime;
            RawAudio.resumeAll();
        } else {
            isPlaying = false;
            RawAudio.pauseAll();
        }
    }

    private void refreshAnimationList() {
        AnimationLoader.loadAllAnimations();
        animationKeys = new ArrayList<>(AnimationLoader.ANIMATION_CACHE.keySet());
        Collections.sort(animationKeys);
    }

    // ======================== RENDER ========================

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
        // When action menu is open, suppress hover highlights in the panel
        renderPropertiesPanel(g, showActionMenu ? -1 : mx, showActionMenu ? -1 : my);
        renderTimeline(g, mx, my);
        super.render(g, mx, my, partialTick);

        // Action type popup menu — rendered LAST so it's on top of everything
        if (showActionMenu) {
            renderActionMenu(g, mx, my);
        }
        // Modal overlay — rendered after everything else
        if (modal != ModalType.NONE) {
            renderModal(g, mx, my);
        }
    }

    private void renderHeader(GuiGraphics g) {
        g.fill(0, 0, width, HEADER_H, C_HEADER_BG);
        g.fill(0, HEADER_H - 1, width, HEADER_H, C_BORDER);
        int tx = 48;
        g.drawString(font, "KariView Editor", tx, 9, C_TEXT);
        if (currentAnimation != null) {
            String sub = " | " + currentAnimation.getNamespace() + ":" + currentAnimation.getId()
                    + " [" + currentAnimation.getTotalDuration() + "ms]";
            if (unsavedChanges) sub += " *";
            g.drawString(font, sub, tx + font.width("KariView Editor"), 9, C_DIM);
        }
    }

    private void renderSidebar(GuiGraphics g, int mx, int my) {
        int top = HEADER_H, bot = height - TIMELINE_H;
        g.fill(0, top, SIDEBAR_W, bot, C_PANEL_BG);
        g.fill(SIDEBAR_W - 1, top, SIDEBAR_W, bot, C_BORDER);

        int y = top + 4;
        int itemH = 12;

        // Split sidebar: top half animations, bottom half elements
        int midY = top + (bot - top) / 2;

        // --- Animations ---
        g.drawString(font, "ANIMATIONS", 4, y, C_DIM);
        rAddAnimBtnX = SIDEBAR_W - 28; rDelAnimBtnX = SIDEBAR_W - 14; rAnimBtnY = y - 1;
        g.fill(rAddAnimBtnX, rAnimBtnY, rAddAnimBtnX + 12, rAnimBtnY + 10, hovered(mx, my, rAddAnimBtnX, rAnimBtnY, 12, 10) ? C_BTN_HOV : C_BTN_BG);
        g.drawString(font, "+", rAddAnimBtnX + 3, rAnimBtnY + 1, C_TEXT);
        g.fill(rDelAnimBtnX, rAnimBtnY, rDelAnimBtnX + 12, rAnimBtnY + 10, hovered(mx, my, rDelAnimBtnX, rAnimBtnY, 12, 10) ? C_BTN_HOV : C_BTN_BG);
        g.drawString(font, "-", rDelAnimBtnX + 4, rAnimBtnY + 1, C_TEXT);
        y += 12;
        rAnimListY = y;
        rAnimListBot = midY - 2;

        int animVis = Math.max(1, (rAnimListBot - rAnimListY) / itemH);
        animScrollOffset = Math.max(0, Math.min(animScrollOffset, Math.max(0, animationKeys.size() - animVis)));

        for (int i = animScrollOffset; i < animationKeys.size() && y + itemH <= rAnimListBot; i++) {
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

        // --- Divider + Elements ---
        g.fill(0, midY - 1, SIDEBAR_W - 1, midY, C_BORDER);
        y = midY + 3;
        g.drawString(font, "ELEMENTS", 4, y, C_DIM);
        rAddElemBtnX = SIDEBAR_W - 28; rDelElemBtnX = SIDEBAR_W - 14; rElemBtnY = y - 1;
        g.fill(rAddElemBtnX, rElemBtnY, rAddElemBtnX + 12, rElemBtnY + 10, hovered(mx, my, rAddElemBtnX, rElemBtnY, 12, 10) ? C_BTN_HOV : C_BTN_BG);
        g.drawString(font, "+", rAddElemBtnX + 3, rElemBtnY + 1, C_TEXT);
        g.fill(rDelElemBtnX, rElemBtnY, rDelElemBtnX + 12, rElemBtnY + 10, hovered(mx, my, rDelElemBtnX, rElemBtnY, 12, 10) ? C_BTN_HOV : C_BTN_BG);
        g.drawString(font, "-", rDelElemBtnX + 4, rElemBtnY + 1, C_TEXT);
        y += 12;
        rElemListY = y;
        rElemListBot = bot;

        if (currentAnimation != null && currentAnimation.getElements() != null) {
            List<GuiElementData> elems = currentAnimation.getElements();
            int elemVis = Math.max(1, (rElemListBot - rElemListY) / itemH);
            elemScrollOffset = Math.max(0, Math.min(elemScrollOffset, Math.max(0, elems.size() - elemVis)));
            for (int i = elemScrollOffset; i < elems.size() && y + itemH <= rElemListBot; i++) {
                GuiElementData el = elems.get(i);
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

    // ======================== CANVAS ========================

    private int cLeft()  { return SIDEBAR_W; }
    private int cRight() { return width - PROPS_W; }
    private int cTop()   { return HEADER_H; }
    private int cBot()   { return height - TIMELINE_H; }
    private int cW()     { return cRight() - cLeft(); }
    private int cH()     { return cBot() - cTop(); }

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

        g.fill(cl, ct, cr, cb, C_CANVAS_BG);

        int[] pr = previewRect();
        int pl = pr[0], pt = pr[1], pw = pr[2], ph = pr[3];

        g.fill(pl, pt, pl + pw, pt + ph, 0xFF0A0A14);
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

                int[] draw = computeDrawRect(pos, dims, pl, pt, pw, ph);
                int drawX = draw[0], drawY = draw[1], drawW = draw[2], drawH = draw[3];

                RenderSystem.setShaderColor(1f, 1f, 1f, (float) Math.max(0, Math.min(1, pos[5])));

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
                    g.fill(drawX - 1, drawY - 1, drawX + drawW + 1, drawY, sc);
                    g.fill(drawX - 1, drawY + drawH, drawX + drawW + 1, drawY + drawH + 1, sc);
                    g.fill(drawX - 1, drawY, drawX, drawY + drawH, sc);
                    g.fill(drawX + drawW, drawY, drawX + drawW + 1, drawY + drawH, sc);
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

    /** Compute draw rect matching GuiElement.render() exactly. */
    private int[] computeDrawRect(double[] pos, int[] dims, int pl, int pt, int pw, int ph) {
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
        return new int[]{ drawX, drawY, drawW, drawH };
    }

    // ======================== PROPERTIES PANEL ========================

    private void renderPropertiesPanel(GuiGraphics g, int mx, int my) {
        int px = width - PROPS_W, top = HEADER_H, bot = height - TIMELINE_H;
        g.fill(px, top, width, bot, C_PANEL_BG);
        g.fill(px, top, px + 1, bot, C_BORDER);

        int x = px + 4, y = top + 4;

        // --- Element info (compact) ---
        g.drawString(font, "ELEMENT", x, y, C_DIM);
        y += 11;
        if (selectedElementId != null && previewPos.containsKey(selectedElementId)) {
            double[] pos = previewPos.get(selectedElementId);
            String name = selectedElementId.length() > 24 ? selectedElementId.substring(0, 22) + ".." : selectedElementId;
            g.drawString(font, name, x, y, C_TEXT);
            y += 10;
            // Compact two-column layout for element stats
            g.drawString(font, String.format("X:%.2f Y:%.2f", pos[0], pos[1]), x, y, C_DIM); y += 9;
            g.drawString(font, String.format("S:%.2f xM:%.2f yM:%.2f", pos[2], pos[3], pos[4]), x, y, C_DIM); y += 9;
            g.drawString(font, String.format("Op:%.2f Ang:%.1f", pos[5], pos.length > 6 ? pos[6] : 0), x, y, C_DIM); y += 9;
        } else {
            g.drawString(font, "None", x, y, C_DIM);
            y += 10;
        }

        y += 2;
        g.fill(px + 2, y, width - 2, y + 1, C_BORDER);
        y += 3;

        // --- Keyframe section ---
        g.drawString(font, "KEYFRAMES", x, y, C_DIM);
        rKfBtnY = y - 1;
        int addKfX = width - 30, delKfX = width - 16;
        g.fill(addKfX, rKfBtnY, addKfX + 12, rKfBtnY + 10, hovered(mx, my, addKfX, rKfBtnY, 12, 10) ? C_BTN_HOV : C_BTN_BG);
        g.drawString(font, "+", addKfX + 3, rKfBtnY + 1, C_TEXT);
        g.fill(delKfX, rKfBtnY, delKfX + 12, rKfBtnY + 10, hovered(mx, my, delKfX, rKfBtnY, 12, 10) ? C_BTN_HOV : C_BTN_BG);
        g.drawString(font, "-", delKfX + 4, rKfBtnY + 1, C_TEXT);
        y += 11;

        rKfListY = y;
        // Allocate ~1/3 of remaining space to keyframes
        int remainingSpace = bot - y - 20; // 20px for edit box
        int kfMaxH = Math.max(22, remainingSpace / 3);
        rKfListBot = y + kfMaxH;

        if (currentAnimation != null && currentAnimation.getKeyframes() != null) {
            List<Keyframe> kfs = currentAnimation.getKeyframes();
            int maxVis = kfMaxH / 11;
            kfScrollOffset = Math.max(0, Math.min(kfScrollOffset, Math.max(0, kfs.size() - maxVis)));
            for (int i = kfScrollOffset; i < kfs.size() && y + 11 <= rKfListBot; i++) {
                Keyframe kf = kfs.get(i);
                boolean sel = i == selectedKeyframeIndex;
                boolean hov = mx >= px && mx < width && my >= y && my < y + 11;
                if (sel)      g.fill(px, y, width, y + 11, C_SELECTED);
                else if (hov) g.fill(px, y, width, y + 11, C_HOVER);
                int acts = kf.getActions() != null ? kf.getActions().size() : 0;
                g.drawString(font, kf.getTimestamp() + "ms (" + acts + ")", x, y + 2,
                        sel ? 0xFFFFFFFF : C_TEXT);
                y += 11;
            }
        }
        y = rKfListBot + 2;
        g.fill(px + 2, y, width - 2, y + 1, C_BORDER);
        y += 3;

        // --- Actions section ---
        g.drawString(font, "ACTIONS", x, y, C_DIM);
        rActBtnY = y - 1;
        actionMenuY = y + 11; // popup appears just below the label
        int addActX = width - 30, delActX = width - 16;
        g.fill(addActX, rActBtnY, addActX + 12, rActBtnY + 10, hovered(mx, my, addActX, rActBtnY, 12, 10) ? C_BTN_HOV : C_BTN_BG);
        g.drawString(font, "+", addActX + 3, rActBtnY + 1, C_TEXT);
        g.fill(delActX, rActBtnY, delActX + 12, rActBtnY + 10, hovered(mx, my, delActX, rActBtnY, 12, 10) ? C_BTN_HOV : C_BTN_BG);
        g.drawString(font, "-", delActX + 4, rActBtnY + 1, C_TEXT);
        y += 11;

        rActListY = y;
        int actMaxH = Math.max(22, remainingSpace / 3);
        rActListBot = y + actMaxH;

        List<Action> actions = getSelectedKeyframeActions();
        if (actions != null) {
            int maxVis = actMaxH / 11;
            actionScrollOffset = Math.max(0, Math.min(actionScrollOffset, Math.max(0, actions.size() - maxVis)));
            for (int i = actionScrollOffset; i < actions.size() && y + 11 <= rActListBot; i++) {
                Action act = actions.get(i);
                boolean sel = i == selectedActionIndex;
                boolean hov = mx >= px && mx < width && my >= y && my < y + 11;
                if (sel)      g.fill(px, y, width, y + 11, C_SELECTED);
                else if (hov) g.fill(px, y, width, y + 11, C_HOVER);
                String label = getActionLabel(act);
                if (label.length() > 26) label = label.substring(0, 24) + "..";
                g.drawString(font, label, x, y + 2, sel ? 0xFFFFFFFF : C_TEXT);
                y += 11;
            }
        }
        y = rActListBot + 2;
        g.fill(px + 2, y, width - 2, y + 1, C_BORDER);
        y += 3;

        // --- Selected action properties ---
        g.drawString(font, "PROPERTIES", x, y, C_DIM);
        y += 11;
        rPropListY = y;

        Action selAction = getSelectedAction();
        if (selAction != null) {
            List<String[]> props = getActionProperties(selAction);
            for (String[] prop : props) {
                if (y + 10 > bot - 20) break;
                boolean isEditing = prop[0].equals(editingPropKey);
                boolean hov = mx >= px && mx < width && my >= y && my < y + 10;
                if (isEditing) {
                    g.fill(px, y, width, y + 10, 0xFF1A3A5A);
                } else if (hov) {
                    g.fill(px, y, width, y + 10, C_HOVER);
                }
                g.drawString(font, prop[0] + ":", x, y + 1, C_ACCENT);
                g.drawString(font, prop[1], x + font.width(prop[0] + ": "), y + 1, C_TEXT);
                y += 10;
            }
        }

        // Position the edit box at the bottom
        if (editingPropKey != null && propEditBox != null) {
            propEditBox.setPosition(px + 4, bot - 18);
            propEditBox.setWidth(PROPS_W - 8);
            propEditBox.setVisible(true);
        } else if (propEditBox != null) {
            propEditBox.setVisible(false);
        }
    }

    private boolean hovered(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    private void renderActionMenu(GuiGraphics g, int mx, int my) {
        int menuX = width - PROPS_W + 4;
        int menuY = actionMenuY;
        int menuW = PROPS_W - 8;
        int itemH = 13;
        int menuH = ACTION_TYPES.length * itemH;

        // Clamp so menu doesn't go off-screen
        int bot = height - TIMELINE_H;
        if (menuY + menuH > bot) menuY = bot - menuH - 2;

        // Push z-level so menu renders above all panel content
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);

        // Opaque background + border
        g.fill(menuX - 2, menuY - 2, menuX + menuW + 2, menuY + menuH + 2, 0xFF000000);
        g.fill(menuX - 1, menuY - 1, menuX + menuW + 1, menuY + menuH + 1, C_BORDER);
        for (int i = 0; i < ACTION_TYPES.length; i++) {
            int iy = menuY + i * itemH;
            boolean hov = mx >= menuX && mx < menuX + menuW && my >= iy && my < iy + itemH;
            g.fill(menuX, iy, menuX + menuW, iy + itemH, hov ? C_SELECTED : 0xFF1E1E1E);
            g.drawString(font, ACTION_TYPES[i][1], menuX + 4, iy + 3, C_TEXT);
        }

        g.pose().popPose();
    }

    // ======================== TIMELINE ========================

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
            for (int i = 0; i < currentAnimation.getKeyframes().size(); i++) {
                Keyframe kf = currentAnimation.getKeyframes().get(i);
                int kx = trackL + (int)((double) kf.getTimestamp() / total * trackW);
                int col = i == selectedKeyframeIndex ? 0xFFFF8800 : C_KEYFRAME;
                g.fill(kx - 2, trackY - 5, kx + 3, trackY + 9, col);
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

        String status = isPlaying ? "\u25B6 Playing" : "|| Paused";
        g.drawString(font, status, SIDEBAR_W + 8, ty + 56, isPlaying ? 0xFF44CC44 : C_DIM);

        if (currentAnimation != null) {
            String info = "wait".equals(currentAnimation.getEndAction()) ? "[WAIT]" : "[STOP]";
            g.drawString(font, info, SIDEBAR_W + 8 + font.width(status) + 8, ty + 56, C_DIM);
        }

        String hint = "SPACE play/pause  \u25C4\u25BA step  Drag canvas elements";
        g.drawString(font, hint, trackL, ty + 68, C_DIM);
    }

    // ======================== EVALUATION ========================

    private void evaluateAtTime(long time) {
        previewPos.clear();
        previewTex.clear();
        previewDims.clear();
        if (currentAnimation == null || currentAnimation.getKeyframes() == null) return;

        playedSoundKeyframes.removeIf(idx -> idx >= 0 &&
                idx < currentAnimation.getKeyframes().size() &&
                currentAnimation.getKeyframes().get(idx).getTimestamp() >= time);

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
                    if (data != null) {
                        String texPath = data.getTexture();
                        if (texPath == null && data.getTexturePathPattern() != null) {
                            // animated sprite: load the first frame (index 0)
                            texPath = data.getTexturePathPattern().replace("%d", "0").replace("%02d", "00");
                        }
                        if (texPath != null)
                            tex = AssetManager.loadTexture(currentAnimation.getNamespace(), texPath);
                    }
                    float op = sea.getStartOpacity() != null ? sea.getStartOpacity() : 1.0f;
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
                    double startX = pos[3], startY = pos[4];
                    double targetX = startX < sa.getTargetScale() ? sa.getTargetScale() / 2 : sa.getTargetScale();
                    double targetY = startY < sa.getTargetScale() ? sa.getTargetScale() / 2 : sa.getTargetScale();
                    if (prog >= 1.0) { pos[3] = targetX; pos[4] = targetY; }
                    else { pos[3] = startX + (targetX - startX) * prog; pos[4] = startY + (targetY - startY) * prog; }

                } else if (action instanceof ExtendAction ea) {
                    double[] pos = previewPos.get(ea.getElementId());
                    if (pos == null) continue;
                    double prog = progress(elapsed, ea.getDuration(), ea.getEasingType());
                    String dir = ea.getDirection();
                    if ("LEFT".equals(dir) || "RIGHT".equals(dir)) {
                        double s = pos[3];
                        pos[3] = prog >= 1.0 ? ea.getTargetValue() : s + (ea.getTargetValue() - s) * prog;
                    } else {
                        double s = pos[4];
                        pos[4] = prog >= 1.0 ? ea.getTargetValue() : s + (ea.getTargetValue() - s) * prog;
                    }

                } else if (action instanceof RotateAction ra) {
                    double[] pos = previewPos.get(ra.getElementId());
                    if (pos == null) continue;
                    double prog = progress(elapsed, ra.getDuration(), ra.getEasingType());
                    double s = pos[6];
                    pos[6] = prog >= 1.0 ? ra.getTargetAngle() : s + (ra.getTargetAngle() - s) * prog;

                } else if (action instanceof ChangeOpacityAction cao) {
                    double[] pos = previewPos.get(cao.getElementId());
                    if (pos == null) continue;
                    double prog = progress(elapsed, cao.getDuration(), cao.getEasingType());
                    double s = pos[5];
                    pos[5] = prog >= 1.0 ? cao.getTargetOpacity() : s + (cao.getTargetOpacity() - s) * prog;

                } else if (action instanceof PlaySoundAction psa) {
                    if (isPlaying && !playedSoundKeyframes.contains(ki)) {
                        File soundFile = AssetManager.loadSound(currentAnimation.getNamespace(), psa.getSoundId());
                        if (soundFile != null) RawAudio.playOgg(soundFile.getAbsolutePath(), psa.getVolume());
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

    // ======================== INPUT ========================

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // Modal intercepts all clicks
        if (modal != ModalType.NONE) {
            int mw = 200, mh = (modal == ModalType.NEW_ELEM) ? 160 : 130;
            int mleft = (width - mw) / 2, mtop = (height - mh) / 2;
            int bx = mleft + mw / 2 - 25, by = mtop + mh - 22;
            int cx2 = mleft + 4;
            if (hovered((int)mx, (int)my, bx, by, 50, 14)) { confirmModal(); return true; }
            if (hovered((int)mx, (int)my, cx2, by, 40, 14)) { closeModal(); return true; }
            if (modal == ModalType.NEW_ELEM) {
                int tBx = mleft + 10, tBy = mtop + 54;
                if (hovered((int)mx, (int)my, tBx, tBy, mw - 20, 14)) {
                    newElemIsSprite = !newElemIsSprite;
                    return true;
                }
            }
            super.mouseClicked(mx, my, btn); // allow EditBox focus
            return true;
        }

        // Close action menu on click
        if (showActionMenu) {
            int menuX = width - PROPS_W + 4;
            int menuY = actionMenuY;
            int menuW = PROPS_W - 8;
            int itemH = 13;
            int menuH = ACTION_TYPES.length * itemH;
            int bot = height - TIMELINE_H;
            if (menuY + menuH > bot) menuY = bot - menuH - 2;
            if (mx >= menuX && mx < menuX + menuW && my >= menuY && my < menuY + menuH) {
                int idx = (int)((my - menuY) / itemH);
                if (idx >= 0 && idx < ACTION_TYPES.length) {
                    addActionOfType(ACTION_TYPES[idx][0]);
                }
            }
            showActionMenu = false;
            return true;
        }

        if (super.mouseClicked(mx, my, btn)) return true;

        int top = HEADER_H, bot = height - TIMELINE_H;
        int itemH = 12;
        int px = width - PROPS_W;

        // --- Sidebar: Anim [+][-] buttons ---
        if (mx >= 0 && mx < SIDEBAR_W && my >= rAnimBtnY && my < rAnimBtnY + 10) {
            if (hovered((int)mx, (int)my, rAddAnimBtnX, rAnimBtnY, 12, 10)) { openNewAnimModal(); return true; }
            if (hovered((int)mx, (int)my, rDelAnimBtnX, rAnimBtnY, 12, 10)) { deleteCurrentAnimation(); return true; }
        }

        // --- Sidebar: Elem [+][-] buttons ---
        if (mx >= 0 && mx < SIDEBAR_W && my >= rElemBtnY && my < rElemBtnY + 10) {
            if (hovered((int)mx, (int)my, rAddElemBtnX, rElemBtnY, 12, 10)) { openNewElemModal(); return true; }
            if (hovered((int)mx, (int)my, rDelElemBtnX, rElemBtnY, 12, 10)) { deleteSelectedElement(); return true; }
        }

        // --- Sidebar: Animation list ---
        int y = rAnimListY;
        if (mx >= 0 && mx < SIDEBAR_W && my >= rAnimListY && my < rAnimListBot) {
            for (int i = animScrollOffset; i < animationKeys.size() && y + itemH <= rAnimListBot; i++) {
                if (my >= y && my < y + itemH) {
                    AnimationData data = AnimationLoader.ANIMATION_CACHE.get(animationKeys.get(i));
                    if (data != null) {
                        RawAudio.stopAll();
                        playedSoundKeyframes.clear();
                        currentAnimation = data;
                        selectedElementId = null;
                        selectedKeyframeIndex = -1;
                        selectedActionIndex = -1;
                        editingPropKey = null;
                        currentTime = 0;
                        isPlaying = false;
                        unsavedChanges = false;
                        evaluateAtTime(0);
                    }
                    return true;
                }
                y += itemH;
            }
        }

        // --- Sidebar: Element list ---
        y = rElemListY;
        if (mx >= 0 && mx < SIDEBAR_W && my >= rElemListY && my < rElemListBot) {
            if (currentAnimation != null && currentAnimation.getElements() != null) {
                List<GuiElementData> elems = currentAnimation.getElements();
                for (int i = elemScrollOffset; i < elems.size() && y + itemH <= rElemListBot; i++) {
                    if (my >= y && my < y + itemH) {
                        selectedElementId = elems.get(i).getId();
                        return true;
                    }
                    y += itemH;
                }
            }
        }

        // --- Properties panel clicks ---
        if (mx >= px && mx < width && my >= top && my < bot) {
            return handlePropertiesPanelClick(mx, my, px, top, bot);
        }

        // --- Canvas click/drag ---
        int cl = cLeft(), cr = cRight(), ct = cTop(), cb = cBot();
        if (mx >= cl && mx < cr && my >= ct && my < cb) {
            return handleCanvasClick(mx, my);
        }

        // --- Timeline scrub ---
        int ty = height - TIMELINE_H;
        int trackL = SIDEBAR_W + 8, trackR = width - PROPS_W - 8;
        if (my >= ty && my < height && mx >= trackL && mx <= trackR) {
            seekTo(mx, trackL, trackR - trackL);
            isDraggingScrubber = true;
            return true;
        }

        return false;
    }

    private boolean handlePropertiesPanelClick(double mx, double my, int px, int top, int bot) {
        // Use tracked Y positions from the last render pass to stay perfectly in sync

        // --- Keyframe [+] [-] buttons ---
        int addKfX = width - 30, delKfX = width - 16;
        if (hovered((int)mx, (int)my, addKfX, rKfBtnY, 12, 10)) {
            addKeyframeAtCurrentTime();
            return true;
        }
        if (hovered((int)mx, (int)my, delKfX, rKfBtnY, 12, 10)) {
            deleteSelectedKeyframe();
            return true;
        }

        // --- Keyframe list ---
        if (my >= rKfListY && my < rKfListBot && currentAnimation != null && currentAnimation.getKeyframes() != null) {
            List<Keyframe> kfs = currentAnimation.getKeyframes();
            int y = rKfListY;
            for (int i = kfScrollOffset; i < kfs.size() && y + 11 <= rKfListBot; i++) {
                if (my >= y && my < y + 11) {
                    selectedKeyframeIndex = i;
                    selectedActionIndex = -1;
                    editingPropKey = null;
                    currentTime = kfs.get(i).getTimestamp();
                    evaluateAtTime(currentTime);
                    return true;
                }
                y += 11;
            }
        }

        // --- Actions [+] [-] buttons ---
        int addActX = width - 30, delActX = width - 16;
        if (hovered((int)mx, (int)my, addActX, rActBtnY, 12, 10)) {
            if (selectedKeyframeIndex >= 0) showActionMenu = true;
            return true;
        }
        if (hovered((int)mx, (int)my, delActX, rActBtnY, 12, 10)) {
            deleteSelectedAction();
            return true;
        }

        // --- Actions list ---
        if (my >= rActListY && my < rActListBot) {
            List<Action> actions = getSelectedKeyframeActions();
            if (actions != null) {
                int y = rActListY;
                for (int i = actionScrollOffset; i < actions.size() && y + 11 <= rActListBot; i++) {
                    if (my >= y && my < y + 11) {
                        selectedActionIndex = i;
                        editingPropKey = null;
                        return true;
                    }
                    y += 11;
                }
            }
        }

        // --- Properties list ---
        if (my >= rPropListY) {
            Action selAction = getSelectedAction();
            if (selAction != null) {
                List<String[]> props = getActionProperties(selAction);
                int y = rPropListY;
                for (String[] prop : props) {
                    if (y + 10 > bot - 20) break;
                    if (my >= y && my < y + 10) {
                        editingPropKey = prop[0];
                        if (propEditBox != null) {
                            propEditBox.setValue(prop[1]);
                            propEditBox.setVisible(true);
                            propEditBox.setFocused(true);
                            setFocused(propEditBox);
                        }
                        return true;
                    }
                    y += 10;
                }
            }
        }

        return true;
    }

    private boolean handleCanvasClick(double mx, double my) {
        int[] pr = previewRect();
        int pl = pr[0], pt = pr[1], pw = pr[2], ph = pr[3];

        // Try to select/start dragging an element
        selectedElementId = null;
        List<String> ids = new ArrayList<>(previewPos.keySet());
        Collections.reverse(ids);
        for (String id : ids) {
            double[] pos = previewPos.get(id);
            int[] dims = previewDims.getOrDefault(id, new int[]{64, 64});
            int[] draw = computeDrawRect(pos, dims, pl, pt, pw, ph);
            if (mx >= draw[0] && mx < draw[0] + draw[2] && my >= draw[1] && my < draw[1] + draw[3]) {
                selectedElementId = id;
                // Start drag
                isDraggingElement = true;
                dragElementId = id;
                dragStartMx = mx;
                dragStartMy = my;
                dragOrigX = pos[0];
                dragOrigY = pos[1];
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (showActionMenu) return true;
        if (isDraggingScrubber) {
            int trackL = SIDEBAR_W + 8, trackR = width - PROPS_W - 8;
            seekTo(mx, trackL, trackR - trackL);
            return true;
        }
        if (isDraggingElement && dragElementId != null) {
            int[] pr = previewRect();
            int pw = pr[2], ph = pr[3];
            double[] pos = previewPos.get(dragElementId);
            if (pos != null && pw > 0 && ph > 0) {
                // Convert pixel delta to fraction delta
                double deltaFracX = (mx - dragStartMx) / pw;
                double deltaFracY = (my - dragStartMy) / ph;
                pos[0] = dragOrigX + deltaFracX;
                pos[1] = dragOrigY + deltaFracY;
            }
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        // Block release events while action menu is open
        if (showActionMenu) return true;
        if (isDraggingElement && dragElementId != null) {
            double[] pos = previewPos.get(dragElementId);
            if (pos != null && (pos[0] != dragOrigX || pos[1] != dragOrigY)) {
                applyDragToAnimation(dragElementId, pos[0], pos[1]);
            }
            isDraggingElement = false;
            dragElementId = null;
            return true;
        }
        isDraggingScrubber = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (showActionMenu) return true;
        if (mx >= 0 && mx < SIDEBAR_W) {
            if (my >= rAnimListY && my < rAnimListBot) {
                animScrollOffset = Math.max(0, animScrollOffset - (int) delta);
            } else if (my >= rElemListY && my < rElemListBot) {
                elemScrollOffset = Math.max(0, elemScrollOffset - (int) delta);
            }
            return true;
        }
        if (mx >= width - PROPS_W) {
            // Scroll whichever section the mouse is over
            if (my >= rKfListY && my < rKfListBot) {
                kfScrollOffset = Math.max(0, kfScrollOffset - (int) delta);
                return true;
            }
            if (my >= rActListY && my < rActListBot) {
                actionScrollOffset = Math.max(0, actionScrollOffset - (int) delta);
                return true;
            }
            // Don't scroll if mouse isn't over a scrollable section
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // Escape closes modal (before anything else)
        if (key == 256 && modal != ModalType.NONE) { closeModal(); return true; }
        // Enter confirms modal
        if (key == 257 && modal != ModalType.NONE) { confirmModal(); return true; }
        // Block shortcuts while modal is open
        if (modal != ModalType.NONE) return super.keyPressed(key, scan, mods);

        // Enter to confirm property edit
        if (key == 257 && editingPropKey != null && propEditBox != null) { // ENTER
            applyPropertyEdit(editingPropKey, propEditBox.getValue());
            editingPropKey = null;
            propEditBox.setVisible(false);
            evaluateAtTime(currentTime);
            return true;
        }
        // Escape to cancel property edit
        if (key == 256 && editingPropKey != null) {
            editingPropKey = null;
            if (propEditBox != null) propEditBox.setVisible(false);
            return true;
        }
        // Don't handle shortcuts while editing text
        if (propEditBox != null && propEditBox.isFocused()) {
            return super.keyPressed(key, scan, mods);
        }

        // Undo Ctrl+Z
        if (key == 90 && (mods & 2) != 0) { undo(); return true; }
        // Redo Ctrl+Y
        if (key == 89 && (mods & 2) != 0) { redo(); return true; }

        if (key == 32) { togglePlay(); return true; } // SPACE
        if (key == 263 && !isPlaying && currentAnimation != null) { // LEFT
            currentTime = Math.max(0, currentTime - 100);
            evaluateAtTime(currentTime);
            return true;
        }
        if (key == 262 && !isPlaying && currentAnimation != null) { // RIGHT
            currentTime = Math.min(currentAnimation.getTotalDuration(), currentTime + 100);
            evaluateAtTime(currentTime);
            return true;
        }
        if (key == 261 && selectedActionIndex >= 0) { // DELETE
            deleteSelectedAction();
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

    // ======================== EDITING OPERATIONS ========================

    private void addKeyframeAtCurrentTime() {
        if (currentAnimation == null) return;
        pushUndoState();
        List<Keyframe> kfs = currentAnimation.getKeyframes();
        if (kfs == null) {
            kfs = new ArrayList<>();
            currentAnimation.setKeyframes(kfs);
        }
        // Check if a keyframe already exists at this time
        for (int i = 0; i < kfs.size(); i++) {
            if (kfs.get(i).getTimestamp() == currentTime) {
                selectedKeyframeIndex = i;
                return; // already exists
            }
        }
        Keyframe newKf = new Keyframe(currentTime);
        // Insert in sorted order
        int insertIdx = 0;
        for (int i = 0; i < kfs.size(); i++) {
            if (kfs.get(i).getTimestamp() < currentTime) insertIdx = i + 1;
            else break;
        }
        kfs.add(insertIdx, newKf);
        selectedKeyframeIndex = insertIdx;
        selectedActionIndex = -1;
        unsavedChanges = true;
    }

    private void deleteSelectedKeyframe() {
        if (currentAnimation == null || selectedKeyframeIndex < 0) return;
        pushUndoState();
        List<Keyframe> kfs = currentAnimation.getKeyframes();
        if (kfs != null && selectedKeyframeIndex < kfs.size()) {
            kfs.remove(selectedKeyframeIndex);
            if (selectedKeyframeIndex >= kfs.size()) selectedKeyframeIndex = kfs.size() - 1;
            selectedActionIndex = -1;
            unsavedChanges = true;
            evaluateAtTime(currentTime);
        }
    }

    private void addActionOfType(String type) {
        if (currentAnimation == null || selectedKeyframeIndex < 0) return;
        pushUndoState();
        List<Keyframe> kfs = currentAnimation.getKeyframes();
        if (kfs == null || selectedKeyframeIndex >= kfs.size()) return;
        Keyframe kf = kfs.get(selectedKeyframeIndex);
        List<Action> actions = kf.getActions();
        if (actions == null) {
            actions = new ArrayList<>();
            kf.setActions(actions);
        }

        String elemId = selectedElementId != null ? selectedElementId : "element_id";

        Action newAction = switch (type) {
            case "show_element" -> {
                ShowElementAction a = new ShowElementAction();
                a.setElementId(elemId);
                a.setX(0.5); a.setY(0.5); a.setScale(0.2);
                a.setTextureWidth(64); a.setTextureHeight(64);
                yield a;
            }
            case "hide_element" -> {
                HideElementAction a = new HideElementAction();
                a.setElementId(elemId);
                yield a;
            }
            case "move_element" -> {
                MoveAction a = new MoveAction();
                a.setElementId(elemId);
                a.setTargetX(0.5); a.setTargetY(0.5);
                a.setDuration(500); a.setEasingType("linear");
                yield a;
            }
            case "scale_element" -> {
                ScaleAction a = new ScaleAction();
                a.setElementId(elemId);
                a.setTargetScale(1.0);
                a.setDuration(500); a.setEasingType("linear");
                yield a;
            }
            case "rotate_element" -> {
                RotateAction a = new RotateAction();
                a.setElementId(elemId);
                a.setTargetAngle(360);
                a.setDuration(500); a.setEasingType("linear");
                yield a;
            }
            case "change_opacity" -> {
                ChangeOpacityAction a = new ChangeOpacityAction();
                a.setElementId(elemId);
                a.setTargetOpacity(0.0f);
                a.setDuration(500); a.setEasingType("linear");
                yield a;
            }
            case "extend_element" -> {
                ExtendAction a = new ExtendAction();
                a.setElementId(elemId);
                a.setTargetValue(2.0);
                a.setDirection("RIGHT");
                a.setDuration(500); a.setEasingType("linear");
                yield a;
            }
            case "play_sound" -> {
                PlaySoundAction a = new PlaySoundAction();
                a.setSoundId("sound.ogg");
                a.setVolume(1.0f);
                yield a;
            }
            default -> null;
        };

        if (newAction != null) {
            actions.add(newAction);
            selectedActionIndex = actions.size() - 1;
            unsavedChanges = true;
            evaluateAtTime(currentTime);
        }
    }

    private void deleteSelectedAction() {
        List<Action> actions = getSelectedKeyframeActions();
        if (actions != null && selectedActionIndex >= 0 && selectedActionIndex < actions.size()) {
            pushUndoState();
            actions.remove(selectedActionIndex);
            if (selectedActionIndex >= actions.size()) selectedActionIndex = actions.size() - 1;
            editingPropKey = null;
            unsavedChanges = true;
            evaluateAtTime(currentTime);
        }
    }

    private void applyDragToAnimation(String elementId, double newX, double newY) {
        if (currentAnimation == null) return;
        pushUndoState();

        // Find the most recent ShowElementAction or MoveAction for this element
        // at or before currentTime, and update its position
        List<Keyframe> kfs = currentAnimation.getKeyframes();
        if (kfs == null) return;

        // Walk backwards to find what sets this element's position at currentTime
        ShowElementAction lastShow = null;
        MoveAction lastMove = null;
        int lastMoveKfIdx = -1;

        for (int ki = 0; ki < kfs.size(); ki++) {
            Keyframe kf = kfs.get(ki);
            if (kf.getTimestamp() > currentTime) break;
            if (kf.getActions() == null) continue;
            for (Action action : kf.getActions()) {
                if (action instanceof ShowElementAction sea && sea.getElementId().equals(elementId)) {
                    lastShow = sea;
                    lastMove = null; // reset — show overrides previous moves
                } else if (action instanceof MoveAction ma && ma.getElementId().equals(elementId)) {
                    long elapsed = currentTime - kf.getTimestamp();
                    if (elapsed >= ma.getDuration()) {
                        lastMove = ma;
                        lastMoveKfIdx = ki;
                    }
                }
            }
        }

        if (lastMove != null) {
            // Update the completed move's target
            lastMove.setTargetX(newX);
            lastMove.setTargetY(newY);
        } else if (lastShow != null) {
            // Update the show's initial position
            lastShow.setX(newX);
            lastShow.setY(newY);
        }

        unsavedChanges = true;
        evaluateAtTime(currentTime);
    }

    private void applyPropertyEdit(String propKey, String value) {
        Action action = getSelectedAction();
        if (action == null) return;
        pushUndoState();

        try {
            if (action instanceof ShowElementAction a) {
                switch (propKey) {
                    case "element_id" -> a.setElementId(value);
                    case "x" -> a.setX(Double.parseDouble(value));
                    case "y" -> a.setY(Double.parseDouble(value));
                    case "scale" -> a.setScale(Double.parseDouble(value));
                    case "tex_w" -> a.setTextureWidth(Integer.parseInt(value));
                    case "tex_h" -> a.setTextureHeight(Integer.parseInt(value));
                    case "opacity" -> a.setStartOpacity(Float.parseFloat(value));
                }
            } else if (action instanceof HideElementAction a) {
                if ("element_id".equals(propKey)) a.setElementId(value);
            } else if (action instanceof MoveAction a) {
                switch (propKey) {
                    case "element_id" -> a.setElementId(value);
                    case "target_x" -> a.setTargetX(Double.parseDouble(value));
                    case "target_y" -> a.setTargetY(Double.parseDouble(value));
                    case "duration" -> a.setDuration(Long.parseLong(value));
                    case "easing" -> a.setEasingType(value);
                }
            } else if (action instanceof ScaleAction a) {
                switch (propKey) {
                    case "element_id" -> a.setElementId(value);
                    case "target" -> a.setTargetScale(Double.parseDouble(value));
                    case "duration" -> a.setDuration(Long.parseLong(value));
                    case "easing" -> a.setEasingType(value);
                }
            } else if (action instanceof RotateAction a) {
                switch (propKey) {
                    case "element_id" -> a.setElementId(value);
                    case "angle" -> a.setTargetAngle(Double.parseDouble(value));
                    case "duration" -> a.setDuration(Long.parseLong(value));
                    case "easing" -> a.setEasingType(value);
                }
            } else if (action instanceof ChangeOpacityAction a) {
                switch (propKey) {
                    case "element_id" -> a.setElementId(value);
                    case "target" -> a.setTargetOpacity(Float.parseFloat(value));
                    case "duration" -> a.setDuration(Long.parseLong(value));
                    case "easing" -> a.setEasingType(value);
                }
            } else if (action instanceof ExtendAction a) {
                switch (propKey) {
                    case "element_id" -> a.setElementId(value);
                    case "target" -> a.setTargetValue(Double.parseDouble(value));
                    case "direction" -> a.setDirection(value);
                    case "duration" -> a.setDuration(Long.parseLong(value));
                    case "easing" -> a.setEasingType(value);
                }
            } else if (action instanceof PlaySoundAction a) {
                switch (propKey) {
                    case "sound_id" -> a.setSoundId(value);
                    case "volume" -> a.setVolume(Float.parseFloat(value));
                }
            }
            unsavedChanges = true;
        } catch (NumberFormatException e) {
            LOGGER.warn("[Editor] Invalid value for {}: {}", propKey, value);
        }
    }

    // ======================== SAVE ========================

    private void saveCurrentAnimation() {
        if (currentAnimation == null) return;

        String namespace = currentAnimation.getNamespace();
        String animId = currentAnimation.getId();

        // Find the file
        String gameDir = System.getProperty("user.dir");
        File dataPackDir = new File(gameDir, "kariviewlib");
        File namespaceDir = new File(dataPackDir, namespace);
        File animationsDir = new File(namespaceDir, "animations");

        File targetFile = null;
        String[] exts = {".json", ".json5"};
        for (String ext : exts) {
            File f = new File(animationsDir, animId + ext);
            if (f.exists()) { targetFile = f; break; }
        }
        if (targetFile == null) {
            targetFile = new File(animationsDir, animId + ".json");
        }

        try {
            animationsDir.mkdirs();
            // Serialize using the type-adapter-aware Gson, then re-parse and pretty-print
            String json = AnimationLoader.getGson().toJson(currentAnimation);
            com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(json);
            String pretty = new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(parsed);
            try (FileWriter writer = new FileWriter(targetFile)) {
                writer.write(pretty);
            }
            unsavedChanges = false;
            LOGGER.info("[Editor] Saved animation to: {}", targetFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("[Editor] Failed to save animation", e);
        }
    }

    // ======================== HELPERS ========================

    private List<Action> getSelectedKeyframeActions() {
        if (currentAnimation == null || selectedKeyframeIndex < 0) return null;
        List<Keyframe> kfs = currentAnimation.getKeyframes();
        if (kfs == null || selectedKeyframeIndex >= kfs.size()) return null;
        return kfs.get(selectedKeyframeIndex).getActions();
    }

    private Action getSelectedAction() {
        List<Action> actions = getSelectedKeyframeActions();
        if (actions == null || selectedActionIndex < 0 || selectedActionIndex >= actions.size()) return null;
        return actions.get(selectedActionIndex);
    }

    private String getActionLabel(Action action) {
        if (action instanceof ShowElementAction a) return "show: " + a.getElementId();
        if (action instanceof HideElementAction a) return "hide: " + a.getElementId();
        if (action instanceof MoveAction a) return "move: " + a.getElementId();
        if (action instanceof ScaleAction a) return "scale: " + a.getElementId();
        if (action instanceof RotateAction a) return "rotate: " + a.getElementId();
        if (action instanceof ChangeOpacityAction a) return "opacity: " + a.getElementId();
        if (action instanceof ExtendAction a) return "extend: " + a.getElementId();
        if (action instanceof PlaySoundAction a) return "sound: " + a.getSoundId();
        return action.getClass().getSimpleName();
    }

    private List<String[]> getActionProperties(Action action) {
        List<String[]> props = new ArrayList<>();
        if (action instanceof ShowElementAction a) {
            props.add(new String[]{"element_id", a.getElementId()});
            props.add(new String[]{"x", String.format("%.4f", a.getX())});
            props.add(new String[]{"y", String.format("%.4f", a.getY())});
            props.add(new String[]{"scale", String.format("%.4f", a.getWidth())});
            props.add(new String[]{"tex_w", String.valueOf(a.getTextureWidth())});
            props.add(new String[]{"tex_h", String.valueOf(a.getTextureHeight())});
            props.add(new String[]{"opacity", a.getStartOpacity() != null ? String.format("%.2f", a.getStartOpacity()) : "1.00"});
        } else if (action instanceof HideElementAction a) {
            props.add(new String[]{"element_id", a.getElementId()});
        } else if (action instanceof MoveAction a) {
            props.add(new String[]{"element_id", a.getElementId()});
            props.add(new String[]{"target_x", String.format("%.4f", a.getTargetX())});
            props.add(new String[]{"target_y", String.format("%.4f", a.getTargetY())});
            props.add(new String[]{"duration", String.valueOf(a.getDuration())});
            props.add(new String[]{"easing", a.getEasingType() != null ? a.getEasingType() : "linear"});
        } else if (action instanceof ScaleAction a) {
            props.add(new String[]{"element_id", a.getElementId()});
            props.add(new String[]{"target", String.format("%.4f", a.getTargetScale())});
            props.add(new String[]{"duration", String.valueOf(a.getDuration())});
            props.add(new String[]{"easing", a.getEasingType() != null ? a.getEasingType() : "linear"});
        } else if (action instanceof RotateAction a) {
            props.add(new String[]{"element_id", a.getElementId()});
            props.add(new String[]{"angle", String.format("%.2f", a.getTargetAngle())});
            props.add(new String[]{"duration", String.valueOf(a.getDuration())});
            props.add(new String[]{"easing", a.getEasingType() != null ? a.getEasingType() : "linear"});
        } else if (action instanceof ChangeOpacityAction a) {
            props.add(new String[]{"element_id", a.getElementId()});
            props.add(new String[]{"target", String.format("%.2f", a.getTargetOpacity())});
            props.add(new String[]{"duration", String.valueOf(a.getDuration())});
            props.add(new String[]{"easing", a.getEasingType() != null ? a.getEasingType() : "linear"});
        } else if (action instanceof ExtendAction a) {
            props.add(new String[]{"element_id", a.getElementId()});
            props.add(new String[]{"target", String.format("%.4f", a.getTargetValue())});
            props.add(new String[]{"direction", a.getDirection()});
            props.add(new String[]{"duration", String.valueOf(a.getDuration())});
            props.add(new String[]{"easing", a.getEasingType() != null ? a.getEasingType() : "linear"});
        } else if (action instanceof PlaySoundAction a) {
            props.add(new String[]{"sound_id", a.getSoundId()});
            props.add(new String[]{"volume", String.format("%.2f", a.getVolume())});
        }
        return props;
    }

    // ======================== UNDO / REDO ========================

    private void pushUndoState() {
        if (currentAnimation == null) return;
        String snapshot = currentAnimation.getNamespace() + "\n" + AnimationLoader.getGson().toJson(currentAnimation);
        undoStack.push(snapshot);
        while (undoStack.size() > MAX_UNDO) undoStack.removeLast();
        redoStack.clear();
    }

    private void undo() {
        if (undoStack.isEmpty() || currentAnimation == null) return;
        redoStack.push(currentAnimation.getNamespace() + "\n" + AnimationLoader.getGson().toJson(currentAnimation));
        restoreSnapshot(undoStack.pop());
    }

    private void redo() {
        if (redoStack.isEmpty() || currentAnimation == null) return;
        undoStack.push(currentAnimation.getNamespace() + "\n" + AnimationLoader.getGson().toJson(currentAnimation));
        restoreSnapshot(redoStack.pop());
    }

    private void restoreSnapshot(String snapshot) {
        int nl = snapshot.indexOf('\n');
        String ns = snapshot.substring(0, nl);
        String json = snapshot.substring(nl + 1);
        AnimationData data = AnimationLoader.getGson().fromJson(json, AnimationData.class);
        data.namespace = ns;
        AnimationLoader.ANIMATION_CACHE.put(ns + ":" + data.getId(), data);
        currentAnimation = data;
        unsavedChanges = true;
        selectedKeyframeIndex = -1;
        selectedActionIndex = -1;
        editingPropKey = null;
        evaluateAtTime(currentTime);
    }

    // ======================== MODAL ========================

    private void openNewAnimModal() {
        modal = ModalType.NEW_ANIM;
        int mw = 200, mh = 130;
        int mleft = (width - mw) / 2, mtop = (height - mh) / 2;
        modalBoxA.setX(mleft + 10); modalBoxA.setY(mtop + 34); modalBoxA.setWidth(mw - 20);
        modalBoxB.setX(mleft + 10); modalBoxB.setY(mtop + 64); modalBoxB.setWidth(mw - 20);
        modalBoxA.setValue(""); modalBoxA.setVisible(true);
        modalBoxB.setValue(""); modalBoxB.setVisible(true);
        setFocused(modalBoxA); modalBoxA.setFocused(true);
    }

    private void openNewElemModal() {
        if (currentAnimation == null) return;
        modal = ModalType.NEW_ELEM;
        newElemIsSprite = false;
        int mw = 200, mh = 160;
        int mleft = (width - mw) / 2, mtop = (height - mh) / 2;
        modalBoxA.setX(mleft + 10); modalBoxA.setY(mtop + 34); modalBoxA.setWidth(mw - 20);
        modalBoxB.setX(mleft + 10); modalBoxB.setY(mtop + 86); modalBoxB.setWidth(mw - 20);
        modalBoxA.setValue(""); modalBoxA.setVisible(true);
        modalBoxB.setValue(""); modalBoxB.setVisible(true);
        setFocused(modalBoxA); modalBoxA.setFocused(true);
    }

    private void closeModal() {
        modal = ModalType.NONE;
        modalBoxA.setVisible(false);
        modalBoxB.setVisible(false);
    }

    private void confirmModal() {
        if (modal == ModalType.NEW_ANIM) {
            String ns = modalBoxA.getValue().trim();
            String id = modalBoxB.getValue().trim();
            if (!ns.isEmpty() && !id.isEmpty()) createAnimation(ns, id);
        } else if (modal == ModalType.NEW_ELEM) {
            String elemId = modalBoxA.getValue().trim();
            String path   = modalBoxB.getValue().trim();
            if (!elemId.isEmpty() && !path.isEmpty()) createElement(elemId, newElemIsSprite, path);
        }
        closeModal();
    }

    private void renderModal(GuiGraphics g, int mx, int my) {
        int mw = 200;
        int mh = (modal == ModalType.NEW_ELEM) ? 160 : 130;
        int mleft = (width - mw) / 2, mtop = (height - mh) / 2;

        // Dim background
        g.fill(0, 0, width, height, 0x99000000);
        // Panel
        g.fill(mleft, mtop, mleft + mw, mtop + mh, C_PANEL_BG);
        g.fill(mleft - 1, mtop - 1, mleft + mw + 1, mtop + mh + 1, C_BORDER);

        if (modal == ModalType.NEW_ANIM) {
            g.drawString(font, "New Animation", mleft + 4, mtop + 6, C_TEXT);
            g.drawString(font, "Namespace:", mleft + 10, mtop + 22, C_DIM);
            // modalBoxA renders itself at mtop+34
            g.drawString(font, "ID:", mleft + 10, mtop + 52, C_DIM);
            // modalBoxB renders itself at mtop+64
        } else if (modal == ModalType.NEW_ELEM) {
            g.drawString(font, "New Element", mleft + 4, mtop + 6, C_TEXT);
            g.drawString(font, "Element ID:", mleft + 10, mtop + 22, C_DIM);
            // modalBoxA renders itself at mtop+34
            // Type toggle row
            int tBx = mleft + 10, tBy = mtop + 54;
            boolean hovTog = hovered(mx, my, tBx, tBy, mw - 20, 14);
            g.fill(tBx, tBy, tBx + mw - 20, tBy + 14, hovTog ? C_BTN_HOV : C_BTN_BG);
            g.fill(tBx, tBy, tBx + mw - 20, tBy + 14, 0x22FFFFFF); // subtle tint
            g.drawString(font, "Type:  " + (newElemIsSprite ? "\u25C6 Sprite" : "\u25C6 Normal"), tBx + 4, tBy + 3, C_ACCENT);
            // Label above modalBoxB
            g.drawString(font, newElemIsSprite ? "Pattern (in textures/):" : "File (in textures/):", mleft + 10, mtop + 73, C_DIM);
            // modalBoxB renders itself at mtop+86
        }

        // Buttons
        int by = mtop + mh - 22;
        int cancelX = mleft + 4, confirmX = mleft + mw / 2 - 25;
        g.fill(cancelX, by, cancelX + 40, by + 14, hovered(mx, my, cancelX, by, 40, 14) ? C_BTN_HOV : C_BTN_BG);
        g.drawString(font, "Cancel", cancelX + 4, by + 3, C_DIM);
        g.fill(confirmX, by, confirmX + 50, by + 14, hovered(mx, my, confirmX, by, 50, 14) ? C_ACCENT : C_BTN_BG);
        g.drawString(font, "Create", confirmX + 6, by + 3, C_TEXT);
    }

    // ======================== CREATE / DELETE ========================

    private void createAnimation(String namespace, String animId) {
        String gameDir = System.getProperty("user.dir");
        File nsDir = new File(new File(gameDir, "kariviewlib"), namespace);
        File animDir = new File(nsDir, "animations");
        animDir.mkdirs();
        new File(new File(nsDir, "assets"), "textures").mkdirs();
        new File(new File(nsDir, "assets"), "sounds").mkdirs();
        File file = new File(animDir, animId + ".json5");

        AnimationData data = new AnimationData();
        data.setId(animId);
        data.setKeyframes(new ArrayList<>());
        data.setElements(new ArrayList<>());
        data.namespace = namespace;

        try {
            String json = AnimationLoader.getGson().toJson(data);
            com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(json);
            String pretty = new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(parsed);
            try (FileWriter writer = new FileWriter(file)) { writer.write(pretty); }
        } catch (Exception e) {
            LOGGER.error("[Editor] Failed to create animation file", e);
            return;
        }

        String key = namespace + ":" + animId;
        AnimationLoader.ANIMATION_CACHE.put(key, data);
        animationKeys = new ArrayList<>(AnimationLoader.ANIMATION_CACHE.keySet());
        Collections.sort(animationKeys);

        currentAnimation = data;
        selectedElementId = null; selectedKeyframeIndex = -1; selectedActionIndex = -1;
        editingPropKey = null; currentTime = 0; isPlaying = false; unsavedChanges = false;
        previewPos.clear(); previewTex.clear(); previewDims.clear();
        undoStack.clear(); redoStack.clear();
    }

    private void deleteCurrentAnimation() {
        if (currentAnimation == null) return;
        String ns = currentAnimation.getNamespace();
        String id = currentAnimation.getId();

        String gameDir = System.getProperty("user.dir");
        File animDir = new File(new File(new File(gameDir, "kariviewlib"), ns), "animations");
        for (String ext : new String[]{".json5", ".json"}) {
            File f = new File(animDir, id + ext);
            if (f.exists()) { f.delete(); break; }
        }

        AnimationLoader.ANIMATION_CACHE.remove(ns + ":" + id);
        animationKeys = new ArrayList<>(AnimationLoader.ANIMATION_CACHE.keySet());
        Collections.sort(animationKeys);

        currentAnimation = null; selectedElementId = null;
        selectedKeyframeIndex = -1; selectedActionIndex = -1; unsavedChanges = false;
        previewPos.clear(); previewTex.clear(); previewDims.clear();
        undoStack.clear(); redoStack.clear();
    }

    private void createElement(String id, boolean isSprite, String path) {
        if (currentAnimation == null) return;
        pushUndoState();
        List<GuiElementData> elems = currentAnimation.getElements();
        if (elems == null) { elems = new ArrayList<>(); currentAnimation.setElements(elems); }
        GuiElementData elem = new GuiElementData();
        elem.setId(id);
        if (isSprite) elem.setTexturePathPattern(path);
        else elem.setTexturePath(path);
        elems.add(elem);
        selectedElementId = id;
        unsavedChanges = true;
    }

    private void deleteSelectedElement() {
        if (currentAnimation == null || selectedElementId == null) return;
        List<GuiElementData> elems = currentAnimation.getElements();
        if (elems == null) return;
        pushUndoState();
        String toRemove = selectedElementId;
        elems.removeIf(e -> e.getId().equals(toRemove));
        selectedElementId = null;
        unsavedChanges = true;
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
