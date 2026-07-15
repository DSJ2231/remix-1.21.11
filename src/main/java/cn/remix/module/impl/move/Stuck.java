package cn.remix.module.impl.move;

import cn.remix.event.base.annotation.EventTarget;
import cn.remix.event.impl.LivingUpdateEvent;
import cn.remix.event.impl.MoveInputEvent;
import cn.remix.event.impl.MoveMathEvent;
import cn.remix.event.impl.WorldEvent;
import cn.remix.module.Category;
import cn.remix.module.Module;
import cn.remix.module.value.impl.BoolValue;
import cn.remix.module.value.impl.ModeValue;
import cn.remix.module.value.impl.NumberValue;

public class Stuck extends Module {
    private final ModeValue mode = new ModeValue("Mode", "Freeze", "Freeze", "Cancel");
    private final NumberValue freezeTick = new NumberValue("Freeze Tick", 20, 1, 20, 1, () -> mode.is("Freeze"));
    private final BoolValue noMove = new BoolValue("No Move", true);
    private int stuckTick;

    public Stuck() {
        super("Stuck", Category.Move);
    }

    @Override
    public void onEnable() {
        stuckTick = 0;
    }

    @Override
    public void onDisable() {
        stuckTick = 0;
    }

    @EventTarget
    private void onWorld(WorldEvent event) {
        toggle();
    }

    @EventTarget
    private void onMathEvent(MoveMathEvent event) {
        if (mc.player == null || mc.world == null) return;

        switch (mode.getValue()) {
            case "Freeze" -> {
                if (stuckTick >= freezeTick.getValue().intValue()) {
                    stuckTick = 0;
                    event.setCancelled(false);
                } else {
                    event.setCancelled(true);
                }
            }

            case "Cancel" -> event.setCancelled(true);
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (noMove.getValue()) {
            event.setForward(0);
            event.setStrafe(0);
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        setSuffix(mode.getValue());
        stuckTick++;
    }
}
