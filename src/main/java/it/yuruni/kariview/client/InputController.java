package it.yuruni.kariview.client;

import it.yuruni.kariview.Kariview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Controls player input and head-rotation locking independently of the camera.
 *
 * <p>Call {@link #lockMovement()} to suppress WASD, jump, and head rotation.
 * The player's yaw and pitch at lock time are saved and can be retrieved with
 * {@link #getSavedYRot()} / {@link #getSavedXRot()} so the camera exit animation
 * can smoothly return to the pre-lock orientation.
 *
 * <pre>{@code
 * InputController.lockMovement();
 * CameraController.enter(view);
 * // ...
 * CameraController.exit(InputController.getSavedYRot());
 * InputController.unlockMovement();
 * }</pre>
 */
@Mod.EventBusSubscriber(modid = Kariview.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class InputController {

    private InputController() {}

    private static boolean movementLocked = false;
    private static float   savedYRot      = 0f;
    private static float   savedXRot      = 0f;

    /** Locks WASD, jump, and head rotation. Saves current yaw/pitch for exit animation. */
    public static void lockMovement() {
        movementLocked = true;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            savedYRot = player.getYRot();
            savedXRot = player.getXRot();
        }
    }

    public static void unlockMovement()      { movementLocked = false; }
    public static boolean isMovementLocked() { return movementLocked; }

    /** Player yaw at the time movement was locked — use as the exit animation's yaw target. */
    public static float getSavedYRot() { return savedYRot; }
    /** Player pitch at the time movement was locked. */
    public static float getSavedXRot() { return savedXRot; }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!movementLocked) return;
        event.getInput().forwardImpulse = 0f;
        event.getInput().leftImpulse   = 0f;
        event.getInput().jumping       = false;
        // Freeze head rotation so the player model doesn't spin visually
        if (event.getEntity() instanceof LocalPlayer player) {
            player.setYRot(savedYRot);
            player.setXRot(savedXRot);
        }
    }
}
