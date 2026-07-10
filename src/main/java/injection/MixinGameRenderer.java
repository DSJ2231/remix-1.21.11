package injection;

import cn.remix.event.impl.Render3DEvent;
import cn.remix.module.impl.render.NoHurtCam;
import cn.remix.util.IMinecraft;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements IMinecraft {

    @Shadow
    public abstract Camera getCamera();

    @Shadow
    @Final
    private BufferBuilderStorage buffers;

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderHand(FZLorg/joml/Matrix4f;)V"))
    private void renderWorld(RenderTickCounter renderTickCounter, CallbackInfo ci, @Local(ordinal = 0) Matrix4f projectionMatrix, @Local(ordinal = 1) Matrix4f modelViewMatrix) {
        MatrixStack matrixStack = new MatrixStack();
        VertexConsumerProvider consumers = this.buffers.getEntityVertexConsumers();
        Render3DEvent event = new Render3DEvent(matrixStack, consumers, renderTickCounter.getTickProgress(true), projectionMatrix, modelViewMatrix);
        Camera camera = this.getCamera();
        RenderSystem.getModelViewStack().pushMatrix().mul(matrixStack.peek().getPositionMatrix());
        Matrix4f entryMatrix = matrixStack.peek().getPositionMatrix();
        entryMatrix.rotateX((float) Math.toRadians(camera.getPitch()));
        entryMatrix.rotateY((float) Math.toRadians(camera.getYaw() + 180));
        instance.getEventManager().call(event);
        RenderSystem.getModelViewStack().popMatrix();
    }

    @Inject(at = @At("HEAD"), method = "tiltViewWhenHurt(Lnet/minecraft/client/util/math/MatrixStack;F)V", cancellable = true)
    private void tiltViewWhenHurt(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
        if (instance.getModuleManager().getModule(NoHurtCam.class).isEnabled()) {
            ci.cancel();
        }
    }
}