package cn.remix.module.impl.render;

import cn.remix.event.base.annotation.EventTarget;
import cn.remix.event.impl.PacketEvent;
import cn.remix.event.impl.Render3DEvent;
import cn.remix.event.impl.TickEvent;
import cn.remix.event.impl.WorldEvent;
import cn.remix.module.Category;
import cn.remix.module.Module;
import cn.remix.util.render.Render3D;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.WorldChunk;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChestESP extends Module {
    private final List<BlockPos> openedChests = Collections.synchronizedList(new ArrayList<>());
    private final List<BlockEntity> chests = new ArrayList<>();

    public ChestESP() {
        super("ChestESP", Category.Render);
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    @EventTarget
    public void onWorld(WorldEvent event) {
        reset();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getType() == PacketEvent.Type.Received) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof BlockEventS2CPacket blockEvent) {
                if (blockEvent.getType() == 1 && blockEvent.getData() > 0) {
                    BlockPos pos = blockEvent.getPos();
                    if (!openedChests.contains(pos)) {
                        openedChests.add(pos);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        chests.clear();
        int playerChunkX = mc.player.getBlockX() >> 4;
        int playerChunkZ = mc.player.getBlockZ() >> 4;

        for (int x = -16; x <= 16; x++) {
            for (int z = -16; z <= 16; z++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(playerChunkX + x, playerChunkZ + z);
                if (chunk != null) {
                    for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                        if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof EnderChestBlockEntity) {
                            chests.add(blockEntity);
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null || chests.isEmpty()) return;

        for (BlockEntity chest : chests) {
            BlockPos pos = chest.getPos();
            Color color;

            if (chest instanceof EnderChestBlockEntity) {
                color = new Color(255, 0, 255, 60);
            } else if (openedChests.contains(pos)) {
                color = new Color(255, 0, 0, 60);
            } else {
                color = new Color(0, 255, 0, 60);
            }

            Render3D.drawBox(event.getMatrixStack(), pos, color.getRGB());
        }
    }

    private void reset() {
        openedChests.clear();
        chests.clear();
    }
}