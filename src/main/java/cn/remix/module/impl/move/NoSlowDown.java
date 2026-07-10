package cn.remix.module.impl.move;

import cn.remix.event.base.annotation.EventTarget;
import cn.remix.event.impl.SlowEvent;
import cn.remix.event.impl.UpdateEvent;
import cn.remix.module.Category;
import cn.remix.module.Module;
import cn.remix.module.value.impl.BoolValue;
import cn.remix.module.value.impl.ModeValue;

public class NoSlowDown extends Module {
    private final ModeValue mode = new ModeValue("Mode", "Vanilla", "Vanilla");
    private final BoolValue keepSprint = new BoolValue("Keep Sprint", true);

    public NoSlowDown() {
        super("NoSlowDown", Category.Move);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        setSuffix(mode.getValue());
    }

    @EventTarget
    public void onSlow(SlowEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isUsingItem() && !mc.player.getActiveItem().isEmpty()) {
            if (mode.is("Vanilla")) {
                event.setCancelled(true);
            }

            if (keepSprint.getValue()) {
                mc.player.setSprinting(true);
            }
        }
    }
}