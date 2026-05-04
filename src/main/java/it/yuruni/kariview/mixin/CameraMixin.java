package it.yuruni.kariview.mixin;

import it.yuruni.kariview.client.camera.CameraController;
import net.minecraft.client.Camera;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Invoker("setPosition")
    protected abstract void kariview$invokeSetPosition(Vec3 pos);

    @Inject(method = "setup", at = @At("RETURN"))
    private void kariview$overridePosition(BlockGetter level, Entity entity,
                                           boolean detached, boolean thirdPersonReverse,
                                           float partialTick, CallbackInfo ci) {
        if (CameraController.isActive()) {
            kariview$invokeSetPosition(CameraController.getPosition());
        }
    }
}
