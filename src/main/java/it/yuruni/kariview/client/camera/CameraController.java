package it.yuruni.kariview.client.camera;

import it.yuruni.kariview.client.animation.Easing;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Smoothly transitions the player camera to an arbitrary world position and pitch,
 * then back to the player's current eye on exit.
 *
 * <p>Usage:
 * <pre>{@code
 * CameraController.enter(new CameraController.View()
 *     .position(workbenchPos.add(0, 6, 0))
 *     .pitch(90f)
 *     .enterDuration(800)
 *     .exitDuration(500));
 * }</pre>
 *
 * Call {@link #exit()} to reverse. The exit animation always targets the player's
 * current eye position at the time of the call, not the original entry point.
 *
 * {@link #tick()} must be called every render frame (e.g. from a camera angle event).
 */
public final class CameraController {

    private CameraController() {}

    // -------------------------------------------------------------------------
    // Public builder

    public static final class View {
        Vec3  position    = Vec3.ZERO;
        float pitch       = 90f;
        long  enterMs     = 800;
        long  exitMs      = 500;
        java.util.function.DoubleUnaryOperator easing = Easing::easeInOutCubic;
        boolean fadeOnExit                 = true;
        boolean hideLocalPlayer            = true;
        boolean hidePlayerDuringTransition = false;
        long    hidePlayerForMs            = 0;

        public View position(Vec3 pos)                                    { this.position                = pos;    return this; }
        public View pitch(float pitch)                                    { this.pitch                   = pitch;  return this; }
        public View enterDuration(long ms)                                { this.enterMs                 = ms;     return this; }
        public View exitDuration(long ms)                                 { this.exitMs                  = ms;     return this; }
        public View easing(java.util.function.DoubleUnaryOperator easing) { this.easing                  = easing; return this; }
        /** If false, the camera snaps back instantly on exit instead of animating. */
        public View fadeOnExit(boolean fade)                              { this.fadeOnExit              = fade;   return this; }
        /** If true (default), suppresses local player rendering in all camera states. */
        public View hideLocalPlayer(boolean hide)                         { this.hideLocalPlayer         = hide;   return this; }
        /** If true, suppresses local player rendering only during enter/exit transitions.
         *  Useful when hideLocalPlayer is false but you still want a clean transition. */
        public View hidePlayerDuringTransition(boolean hide)              { this.hidePlayerDuringTransition = hide; return this; }
        /** Hides the local player model for the given number of milliseconds after entering,
         *  regardless of transition state. 0 = disabled. */
        public View hidePlayerForMs(long ms)                              { this.hidePlayerForMs         = ms;     return this; }

        boolean hideHead      = false;
        long    hideHeadForMs = 0;

        /** If true, suppresses only the head and hat model parts (not the full body). */
        public View hideHead(boolean hide)                                { this.hideHead      = hide; return this; }
        /** Hides only the head and hat model parts for the given milliseconds after entering. */
        public View hideHeadForMs(long ms)                                { this.hideHeadForMs = ms;   return this; }

        boolean lockPitch = true;  // default true — preserves existing top-down behaviour
        boolean lockYaw   = false;

        /** If true (default), prevents the player from changing camera pitch while active. */
        public View lockPitch(boolean lock) { this.lockPitch = lock; return this; }
        /** If true, captures yaw when the camera becomes active and holds it there. */
        public View lockYaw(boolean lock)   { this.lockYaw   = lock; return this; }

        float toYaw = Float.NaN; // NaN = no yaw animation

        /** Animates yaw to the given angle (degrees). Use with lockYaw(true) to also hold it.
         *  On exit, yaw animates back to the player's yaw at the time exit() is called. */
        public View yaw(float yaw) { this.toYaw = yaw; return this; }
    }

    // -------------------------------------------------------------------------

    private enum State { IDLE, ENTERING, ACTIVE, EXITING }

    private static State state = State.IDLE;

    private static Vec3  fromPos      = Vec3.ZERO;
    private static float fromPitch    = 0f;
    private static float fromYaw      = 0f;   // player yaw at enter (or current yaw at exit)
    private static Vec3  toPos        = Vec3.ZERO;
    private static float toPitch      = 90f;
    private static float toYawTarget  = Float.NaN; // NaN = no yaw animation

    private static long  transitionStartMs = 0;
    private static float exitFromProgress  = 1f;
    private static long  enterDurationMs   = 800;
    private static long  exitDurationMs    = 500;
    private static java.util.function.DoubleUnaryOperator activeEasing = Easing::easeInOutCubic;
    private static boolean fadeOnExit                 = true;
    private static boolean hideLocalPlayer            = true;
    private static boolean hidePlayerDuringTransition = false;
    private static long    hidePlayerUntilMs          = 0;
    private static boolean hideHead                   = false;
    private static long    hideHeadUntilMs            = 0;
    private static long    hidePlayerForMsDuration    = 0;
    private static long    hideHeadForMsDuration      = 0;
    private static boolean lockPitchActive            = true;
    private static boolean lockYawActive              = false;
    private static float   capturedYaw                = 0f;

    private static CameraType savedCameraType = CameraType.FIRST_PERSON;

    // -------------------------------------------------------------------------

    public static void enter(View view) {
        Minecraft mc = Minecraft.getInstance();
        var cam = mc.gameRenderer.getMainCamera();
        fromPos      = cam.getPosition();
        fromPitch    = cam.getXRot();
        fromYaw      = cam.getYRot();
        toPos        = view.position;
        toPitch      = view.pitch;
        toYawTarget  = view.toYaw;
        enterDurationMs = view.enterMs;
        exitDurationMs  = view.exitMs;
        activeEasing    = view.easing;
        fadeOnExit                 = view.fadeOnExit;
        hideLocalPlayer            = view.hideLocalPlayer;
        hidePlayerDuringTransition = view.hidePlayerDuringTransition;
        hidePlayerForMsDuration    = view.hidePlayerForMs;
        hidePlayerUntilMs          = view.hidePlayerForMs > 0
                ? System.currentTimeMillis() + view.hidePlayerForMs : 0;
        hideHead                   = view.hideHead;
        hideHeadForMsDuration      = view.hideHeadForMs;
        lockPitchActive            = view.lockPitch;
        lockYawActive              = view.lockYaw;
        hideHeadUntilMs            = view.hideHeadForMs > 0
                ? System.currentTimeMillis() + view.hideHeadForMs : 0;

        savedCameraType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);

        transitionStartMs = System.currentTimeMillis();
        state = State.ENTERING;
    }

    public static void exit() {
        exitImpl(Float.NaN);
    }

    /**
     * Same as {@link #exit()} but overrides the exit-animation yaw start point.
     * Use this when player rotation is frozen (e.g. by InputController) so that
     * the animation targets the pre-lock yaw rather than the frozen camera yaw.
     *
     * @param exitYaw the yaw the exit animation should start from (degrees)
     */
    public static void exit(float exitYaw) {
        exitImpl(exitYaw);
    }

    private static void exitImpl(float exitYawOverride) {
        if (state == State.IDLE) return;

        if (hidePlayerForMsDuration > 0)
            hidePlayerUntilMs = System.currentTimeMillis() + hidePlayerForMsDuration;
        if (hideHeadForMsDuration > 0)
            hideHeadUntilMs = System.currentTimeMillis() + hideHeadForMsDuration;

        if (!fadeOnExit) {
            Minecraft.getInstance().options.setCameraType(savedCameraType);
            state = State.IDLE;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            fromPos = mc.player.getEyePosition();
            if (!Float.isNaN(toYawTarget)) {
                // Use the override if provided; otherwise fall back to the live camera yaw
                fromYaw = Float.isNaN(exitYawOverride)
                        ? mc.gameRenderer.getMainCamera().getYRot()
                        : exitYawOverride;
            }
        }

        exitFromProgress  = getRawProgress();
        transitionStartMs = System.currentTimeMillis();
        state = State.EXITING;
    }

    public static void tick() {
        if (state == State.ENTERING && getRawProgress() >= 1f) {
            state = State.ACTIVE;
            if (lockYawActive) {
                capturedYaw = Minecraft.getInstance().gameRenderer.getMainCamera().getYRot();
            }
        } else if (state == State.EXITING && getRawProgress() <= 0f) {
            Minecraft.getInstance().options.setCameraType(savedCameraType);
            state = State.IDLE;
        }
    }

    public static boolean isActive() {
        return state != State.IDLE;
    }

    public static boolean isInTransition() {
        return state == State.ENTERING || state == State.EXITING;
    }

    public static boolean isPitchLocked()  { return lockPitchActive; }
    public static boolean isYawLocked()   { return lockYawActive; }
    public static float   getLockedYaw()  { return capturedYaw; }
    public static boolean hasYawTarget()  { return !Float.isNaN(toYawTarget); }

    /** Returns the interpolated yaw for the current animation progress.
     *  Only meaningful when {@link #hasYawTarget()} is true. */
    public static float getYaw() {
        float diff = toYawTarget - fromYaw;
        // Normalize to [-180, 180] so we always take the short arc
        while (diff >  180f) diff -= 360f;
        while (diff < -180f) diff += 360f;
        return fromYaw + diff * getEasedProgress();
    }

    public static boolean isHidingHead() {
        if (state == State.IDLE) return false;
        if (hideHead) return true;
        if (hideHeadUntilMs > 0 && System.currentTimeMillis() < hideHeadUntilMs) return true;
        return false;
    }

    public static boolean isHidingLocalPlayer() {
        if (state == State.IDLE) return false;
        if (hideLocalPlayer) return true;
        if (hidePlayerDuringTransition && (state == State.ENTERING || state == State.EXITING)) return true;
        if (hidePlayerUntilMs > 0 && System.currentTimeMillis() < hidePlayerUntilMs) return true;
        return false;
    }

    // -------------------------------------------------------------------------

    private static float getRawProgress() {
        long elapsed = System.currentTimeMillis() - transitionStartMs;
        return switch (state) {
            case ENTERING -> Math.min(1f, (float) elapsed / enterDurationMs);
            case ACTIVE   -> 1f;
            case EXITING  -> exitFromProgress * (1f - Math.min(1f, (float) elapsed / exitDurationMs));
            default       -> 0f;
        };
    }

    private static float getEasedProgress() {
        return (float) activeEasing.applyAsDouble(getRawProgress());
    }

    public static Vec3 getPosition() {
        return fromPos.lerp(toPos, getEasedProgress());
    }

    public static float getPitch() {
        return fromPitch + (toPitch - fromPitch) * getEasedProgress();
    }
}
