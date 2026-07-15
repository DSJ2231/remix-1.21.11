package cn.remix.util.render;

import cn.remix.util.IMinecraft;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@UtilityClass
public final class Render3D implements IMinecraft {

    public void drawBox(MatrixStack stack, BlockPos pos, int color) {
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        Vec3d target = new Vec3d(pos.getX(), pos.getY(), pos.getZ());

        Box box = new Box(
                target.x - cam.x,
                target.y - cam.y,
                target.z - cam.z,
                target.x + 1 - cam.x,
                target.y + 1 - cam.y,
                target.z + 1 - cam.z
        );

        fill(stack, box, color);
    }

    private void fill(MatrixStack stack, Box box, int color) {
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f mat = stack.peek().getPositionMatrix();
        float x0 = (float) box.minX, y0 = (float) box.minY, z0 = (float) box.minZ;
        float x1 = (float) box.maxX, y1 = (float) box.maxY, z1 = (float) box.maxZ;

        // bottom
        buf.vertex(mat, x0, y0, z0).color(color);
        buf.vertex(mat, x1, y0, z0).color(color);
        buf.vertex(mat, x1, y0, z1).color(color);
        buf.vertex(mat, x0, y0, z1).color(color);

        // top
        buf.vertex(mat, x0, y1, z0).color(color);
        buf.vertex(mat, x0, y1, z1).color(color);
        buf.vertex(mat, x1, y1, z1).color(color);
        buf.vertex(mat, x1, y1, z0).color(color);

        // north
        buf.vertex(mat, x0, y0, z0).color(color);
        buf.vertex(mat, x0, y1, z0).color(color);
        buf.vertex(mat, x1, y1, z0).color(color);
        buf.vertex(mat, x1, y0, z0).color(color);

        // south
        buf.vertex(mat, x0, y0, z1).color(color);
        buf.vertex(mat, x1, y0, z1).color(color);
        buf.vertex(mat, x1, y1, z1).color(color);
        buf.vertex(mat, x0, y1, z1).color(color);

        // west
        buf.vertex(mat, x0, y0, z0).color(color);
        buf.vertex(mat, x0, y0, z1).color(color);
        buf.vertex(mat, x0, y1, z1).color(color);
        buf.vertex(mat, x0, y1, z0).color(color);

        // east
        buf.vertex(mat, x1, y0, z0).color(color);
        buf.vertex(mat, x1, y1, z0).color(color);
        buf.vertex(mat, x1, y1, z1).color(color);
        buf.vertex(mat, x1, y0, z1).color(color);

        Pipelines.box.draw(buf.end());
    }
}