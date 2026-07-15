package cn.remix.module.impl.render;

import cn.remix.module.impl.combat.Aura;
import cn.remix.module.impl.render.targethud.Exhibition;
import cn.remix.module.impl.render.targethud.Novoline;
import cn.remix.module.impl.render.targethud.Remix;
import cn.remix.module.value.impl.ModeValue;
import cn.remix.ui.hud.Drag;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.LivingEntity;

public class TargetHUD extends Drag {
    private final ModeValue mode = new ModeValue("Mode", "Novoline", "Novoline", "Remix", "Exhibition");

    public TargetHUD() {
        super("TargetHUD");
        this.percentX = .5f;
        this.percentY = .8f;
    }

    @Override
    public void render(DrawContext context) {
        if (mc.player == null || mc.world == null) return;
        setSuffix(mode.getValue());

        LivingEntity target = getTarget();
        if (target != null) {
            width = switch (mode.getValue()) {
                case "Exhibition" -> Exhibition.getWidth(target);
                case "Remix" -> Remix.getWidth(target);
                default -> Novoline.getWidth(target);
            };

            height = switch (mode.getValue()) {
                case "Exhibition" -> Exhibition.getHeight();
                case "Remix" -> Remix.getHeight();
                default -> Novoline.getHeight();
            };

            switch (mode.getValue()) {
                case "Exhibition" -> Exhibition.render(context, target, renderX, renderY);
                case "Remix" -> Remix.render(context, target, renderX, renderY);
                default -> Novoline.render(context, target, renderX, renderY);
            }
        }
    }

    public LivingEntity getTarget() {
        if (mc.currentScreen instanceof ChatScreen) return mc.player;

        Aura aura = getModule(Aura.class);
        return aura.isEnabled() ? aura.getTarget() : null;
    }
}