package cn.remix.ui.clickgui;

import cn.remix.module.Category;
import cn.remix.util.IMinecraft;
import cn.remix.util.animation.Easing;
import cn.remix.util.animation.EasingAnimation;
import cn.remix.util.render.Render2D;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class ClickGuiScreen extends Screen implements IMinecraft {
    private final EasingAnimation openAnimation = new EasingAnimation(Easing.EASE_OUT_CUBIC, 250);
    private final List<Panel> panels = new ArrayList<>();
    private boolean closing;

    public ClickGuiScreen() {
        super(Text.literal("ClickGUI"));
        float panelX = 20;
        for (Category c : Category.values()) {
            panels.add(new Panel(c, panelX, 20));
            panelX += Panel.width + 12;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        openAnimation.run(closing ? 1.5 : 1.0);

        float p = openAnimation.getValue().floatValue();
        int alpha = closing ? (int) Math.max(0, 100 * (1 - (p - 1) / 0.5f)) : (int) (100 * p);
        if (alpha > 0) Render2D.drawRect(context, 0, 0, width, height, new Color(0, 0, 0, alpha).getRGB());

        if (closing && openAnimation.isFinished()) { super.close(); return; }

        float panelAlpha = closing ? Math.max(0.0f, 1.0f - (p - 1.0f) / 0.5f) : p;
        if (panelAlpha < 0.01f) return;

        panels.forEach(panel -> panel.render(context, mouseX, mouseY, panelAlpha));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (closing) return false;
        for (int i = panels.size() - 1; i >= 0; i--) {
            panels.get(i).mouseClicked(click.x(), click.y(), click.button());
        }
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        panels.forEach(p -> p.mouseReleased(click.x(), click.y(), click.button()));
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (closing) return false;
        panels.forEach(p -> p.mouseScroll(mouseX, mouseY, verticalAmount));
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            openAnimation.setDuration(200);
            openAnimation.setEasing(Easing.EASE_IN_CUBIC);
            openAnimation.reset();
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (closing) return false;
        if (panels.stream().anyMatch(p -> p.keyTyped(input.key()))) return true;
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() { return false; }
}