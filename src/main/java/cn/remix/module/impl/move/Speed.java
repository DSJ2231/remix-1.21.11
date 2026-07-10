package cn.remix.module.impl.move;

import cn.remix.event.base.annotation.EventTarget;
import cn.remix.event.impl.MotionEvent;
import cn.remix.event.impl.PacketEvent;
import cn.remix.module.Category;
import cn.remix.module.Module;
import cn.remix.module.value.impl.BoolValue;
import cn.remix.module.value.impl.ModeValue;
import cn.remix.module.value.impl.NumberValue;
import cn.remix.util.Util;
import cn.remix.util.player.MovementUtil;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class Speed extends Module {
    private final ModeValue mode = new ModeValue("Mode", "Ground", "Ground", "Vulcan");
    private final BoolValue damageBoost = new BoolValue("Damage Boost", false,  () -> mode.is("Vulcan"));
    private final NumberValue vanillaSpeed = new NumberValue("Speed", 1, 1, 5, 0.1f, () -> mode.is("Ground"));
    private final BoolValue lagBackCheck = new BoolValue("LagBack Check", true);

    public Speed() {
        super("Speed", Category.Move);
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (mc.player == null) return;

        setSuffix(mode.getValue());
        if (e.isPre()) {
            switch (mode.getValue()) {
                case "Ground" -> {
                    if (MovementUtil.isMoving() && mc.player.isOnGround()) {
                        MovementUtil.strafe(vanillaSpeed.getValue() / 4);
                    }
                }

                case "Vulcan" -> {
                    if (MovementUtil.isMoving() && mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) {
                        mc.player.jump();
                    }

                    if (damageBoost.getValue() && mc.player.hurtTime == 1) {
                        MovementUtil.strafe(MovementUtil.getSpeed() * 2);
                    }

                    MovementUtil.strafe(MovementUtil.getSpeed());
                }
            }
        }
    }


    @EventTarget
    public void onPacket(PacketEvent event) {
        if (lagBackCheck.getValue() && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            Util.log("Lag detected!");
            toggle();
        }
    }
}
