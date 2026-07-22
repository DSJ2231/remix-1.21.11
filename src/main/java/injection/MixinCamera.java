package injection;

import cn.remix.module.impl.render.MotionCamera;
import cn.remix.util.IMinecraft;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera implements IMinecraft {

    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    public abstract Vec3d getCameraPos();

    @Unique
    private double smoothX;
    @Unique
    private double smoothY;
    @Unique
    private double smoothZ;
    @Unique
    private boolean isFirstFrame = true;

    @Inject(method = "update", at = @At("RETURN"))
    private void onUpdate(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        MotionCamera motionCamera = instance.getModuleManager().getModule(MotionCamera.class);

        if (motionCamera.isEnabled()) {
            Vec3d targetPos = this.getCameraPos();

            if (isFirstFrame) {
                smoothX = targetPos.x;
                smoothY = targetPos.y;
                smoothZ = targetPos.z;
                isFirstFrame = false;
            }

            if (thirdPerson) {
                double interpolation = motionCamera.interpolation.getValue().doubleValue();

                smoothX += (targetPos.x - smoothX) * interpolation;
                smoothY += (targetPos.y - smoothY) * interpolation;
                smoothZ += (targetPos.z - smoothZ) * interpolation;

                this.setPos(smoothX, smoothY, smoothZ);
            } else {
                smoothX = targetPos.x;
                smoothY = targetPos.y;
                smoothZ = targetPos.z;
            }
        } else {
            isFirstFrame = true;
        }
    }
}