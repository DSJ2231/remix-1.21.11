package cn.remix.ui.clickgui;

import cn.remix.module.Category;
import cn.remix.module.impl.render.HUD;
import cn.remix.util.IMinecraft;
import cn.remix.util.animation.Easing;
import cn.remix.util.animation.EasingAnimation;
import cn.remix.util.render.ColorUtil;
import cn.remix.util.render.Render2D;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public final class Panel implements IMinecraft {
    public static final float width = 110, headerHeight = 18, maxHeight = 350;
    private final Category category;
    private final List<ModuleButton> buttons = new ArrayList<>();
    private final EasingAnimation scrollAnimation = new EasingAnimation(Easing.EASE_OUT_CUBIC, 200);
    private final EasingAnimation barAlphaAnimation = new EasingAnimation(Easing.EASE_OUT_CUBIC, 250);
    private float x, y, dragOffsetX, dragOffsetY;
    private double targetScrollY;
    private boolean dragging;
    private long lastScrollTime;

    public Panel(Category category, float x, float y) {
        this.category = category;
        this.x = x;
        this.y = y;
        instance.getModuleManager().getModuleMap().values().stream()
                .filter(m -> m.getCategory() == category)
                .forEach(m -> buttons.add(new ModuleButton(this, m)));
    }

    public int getAccent() {
        return instance.getModuleManager().getModule(HUD.class).getColor();
    }

    public void render(DrawContext context, int mouseX, int mouseY, float globalAlpha) {
        if (dragging) {
            x = mouseX - dragOffsetX;
            y = mouseY - dragOffsetY;
        }
        var font = instance.getFontManager().getBoldFont(18);
        int alphaInt = (int) (255 * globalAlpha);

        Render2D.drawRect(context, x, y, width, headerHeight, ColorUtil.applyAlpha(new Color(26, 26, 30).getRGB(), alphaInt));
        Render2D.drawRect(context, x, y + headerHeight - 1, width, 1, ColorUtil.applyAlpha(getAccent(), alphaInt));
        font.drawString(context, category.getName(), x + 7, y + (headerHeight - font.getHeight()) / 2.0f + 0.5f, ColorUtil.applyAlpha(Color.WHITE.getRGB(), alphaInt));

        float totalH = totalHeight(), bodyH = Math.min(totalH, maxHeight);
        Render2D.drawRect(context, x, y + headerHeight, width, bodyH, ColorUtil.applyAlpha(new Color(22, 22, 25).getRGB(), alphaInt));

        boolean scroll = totalH > maxHeight;
        targetScrollY = scroll ? Math.max(maxHeight - totalH, Math.min(0, targetScrollY)) : 0;
        scrollAnimation.run(targetScrollY);

        Render2D.beginScissor(context, x, y + headerHeight, width, bodyH);
        float buttonY = y + headerHeight + scrollAnimation.getValue().floatValue();
        for (ModuleButton b : buttons) buttonY += b.render(context, x, buttonY, width, mouseX, mouseY, globalAlpha);
        Render2D.endScissor(context);

        boolean active = scroll && (mouseX >= x && mouseX <= x + width && mouseY >= y + headerHeight && mouseY <= y + headerHeight + bodyH || (System.currentTimeMillis() - lastScrollTime < 1000) || Math.abs(targetScrollY - scrollAnimation.getValue()) > 1.0);
        barAlphaAnimation.run(active ? 1 : 0);

        float barAlpha = barAlphaAnimation.getValue().floatValue() * globalAlpha;
        if (barAlpha > 0.01f) {
            float barH = (maxHeight / totalH) * bodyH;
            float barY = y + headerHeight + (float) (-scrollAnimation.getValue() / totalH * bodyH);
            Render2D.drawRect(context, x + width - 3, barY + 1, 2, barH - 2, ColorUtil.applyAlpha(new Color(128, 128, 128).getRGB(), (int) (120 * barAlpha)));
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight) {
            if (button == 0) {
                dragging = true;
                dragOffsetX = (float) (mouseX - x);
                dragOffsetY = (float) (mouseY - y);
            }
            return;
        }
        if (mouseX >= x && mouseX <= x + width && mouseY >= y + headerHeight && mouseY <= y + headerHeight + Math.min(totalHeight(), maxHeight)) {
            float buttonY = y + headerHeight + scrollAnimation.getValue().floatValue();
            for (ModuleButton mb : buttons) {
                mb.mouseClicked(mouseX, mouseY, button, x, buttonY, width);
                buttonY += mb.getRenderHeight();
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        buttons.forEach(mb -> mb.mouseReleased(mouseX, mouseY, button));
    }

    public void mouseScroll(double mouseX, double mouseY, double amount) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y) {
            targetScrollY += amount * 18;
            lastScrollTime = System.currentTimeMillis();
        }
    }

    public boolean keyTyped(int keyCode) {
        return buttons.stream().anyMatch(mb -> mb.keyTyped(keyCode));
    }

    private float totalHeight() {
        return (float) buttons.stream().mapToDouble(b -> b.getRenderHeight() == 0 ? ModuleButton.height : b.getRenderHeight()).sum();
    }
}