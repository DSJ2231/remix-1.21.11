package cn.remix.ui.font;

import cn.remix.ui.font.base.FontData;
import cn.remix.ui.font.base.FontTexture;
import cn.remix.util.IMinecraft;
import cn.remix.util.misc.StringUtil;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TrueTypeFont implements IMinecraft {
    private final FontTexture fontTexture;
    private final float fontHeight;
    private final float scale;

    private final Map<String, GlyphLayout> cache = new LinkedHashMap<>(512, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, GlyphLayout> eldest) {
            return size() > 8964;
        }
    };

    public TrueTypeFont(Font font, List<Font> fallbackFont, float scale) {
        this.fontTexture = new FontTexture(font, fallbackFont);
        this.scale = scale;
        this.fontHeight = fontTexture.getFontHeight() / scale;
    }

    public void drawStringWithShadow(DrawContext drawContext, String text, float x, float y, int color) {
        drawString(drawContext, text, x, y, color, true);
    }

    public void drawString(DrawContext drawContext, String text, float x, float y, int color) {
        drawString(drawContext, text, x, y, color, false);
    }

    public void drawString(DrawContext drawContext, String text, float x, float y, int color, boolean shadow) {
        drawParsed(drawContext, text, x, y, color, shadow);
    }

    private void drawParsed(DrawContext drawContext, String text, float x, float y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) return;

        int length = text.length();
        int[] colors = new int[length];
        StringBuilder stripped = new StringBuilder(length);
        int baseColor = color, alpha = color & 0xFF000000;

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < length) {
                char code = text.charAt(++i);
                color = (code == 'r' || code == 'R') ? baseColor : (StringUtil.parseColorCode(code, color) & 0x00FFFFFF) | alpha;
            } else {
                colors[stripped.length()] = color;
                stripped.append(c);
            }
        }

        GlyphLayout layout = cache.computeIfAbsent(stripped.toString(), this::shape);
        if (shadow) renderLayout(drawContext, layout, colors, x + 0.5f, y + 0.5f, true);
        renderLayout(drawContext, layout, colors, x, y, false);
    }

    private GlyphLayout shape(String text) {
        if (text.isEmpty()) {
            return new GlyphLayout(new int[0], new Font[0], new float[0], new float[0], new int[0], 0, new FontData[0]);
        }

        char[] chars = text.toCharArray();
        int len = chars.length;

        int[] glyphs = new int[len * 2];
        Font[] fonts = new Font[len * 2];
        float[] glyphX = new float[len * 2];
        float[] glyphY = new float[len * 2];
        int[] indices = new int[len * 2];

        int count = 0, start = 0;
        float cursorX = 0;
        Font currentFont = fontTexture.getFont(Character.codePointAt(chars, 0));

        for (int i = 0; i < len; ) {
            int codePoint = Character.codePointAt(chars, i);
            int nextI = i + Character.charCount(codePoint);
            Font nextFont = (nextI < len) ? fontTexture.getFont(Character.codePointAt(chars, nextI)) : null;

            if (nextFont != currentFont || nextI == len) {
                GlyphVector gv = currentFont.layoutGlyphVector(
                        fontTexture.getContext(), chars, start, nextI, Font.LAYOUT_LEFT_TO_RIGHT
                );

                int numGlyphs = gv.getNumGlyphs();
                for (int j = 0; j < numGlyphs; j++) {
                    int glyphCode = gv.getGlyphCode(j);
                    if (glyphCode == 0) continue;

                    glyphs[count] = glyphCode;
                    fonts[count] = currentFont;
                    glyphX[count] = cursorX + (float) gv.getGlyphPosition(j).getX();
                    glyphY[count] = (float) gv.getGlyphPosition(j).getY();
                    indices[count] = start + gv.getGlyphCharIndex(j);
                    count++;
                }
                cursorX += (float) gv.getLogicalBounds().getWidth();
                currentFont = nextFont;
                start = nextI;
            }
            i = nextI;
        }

        FontData[] finalData = new FontData[count];
        for (int i = 0; i < count; i++) {
            finalData[i] = fontTexture.getGlyphTexture(fonts[i], glyphs[i]);
        }
        fontTexture.flush();

        return new GlyphLayout(
                Arrays.copyOf(glyphs, count),
                Arrays.copyOf(fonts, count),
                Arrays.copyOf(glyphX, count),
                Arrays.copyOf(glyphY, count),
                Arrays.copyOf(indices, count),
                cursorX,
                finalData
        );
    }

    private void renderLayout(DrawContext drawContext, GlyphLayout layout, int[] colors, float x, float y, boolean shadow) {
        int count = layout.count();
        if (count == 0) return;

        float[] glyphX = layout.xs();
        float[] glyphY = layout.ys();
        int[] indices = layout.indices();
        FontData[] layoutData = layout.data();

        Matrix3x2fStack matrices = drawContext.getMatrices();
        matrices.pushMatrix();
        matrices.scale(1f / scale, 1f / scale);

        float scaledX = x * scale;
        float scaledY = y * scale + fontTexture.getFontAscent();

        for (int i = 0; i < count; i++) {
            FontData glyph = layoutData[i];
            if (glyph.width() <= 0) continue;

            int renderColor = colors[indices[i]];
            if (shadow) {
                renderColor = (renderColor & 0xFCFCFC) >> 2 | (renderColor & 0xFF000000);
            }

            drawContext.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    glyph.atlasId(),
                    Math.round(scaledX + glyphX[i] + glyph.offsetX()),
                    Math.round(scaledY + glyphY[i] + glyph.offsetY()),
                    glyph.u0() * 4096f,
                    glyph.v0() * 4096f,
                    glyph.width(),
                    glyph.height(),
                    4096,
                    4096,
                    renderColor
            );
        }
        matrices.popMatrix();
    }

    public float getStringWidth(String text) {
        if (text == null || text.isEmpty()) return 0;

        int length = text.length();
        StringBuilder stripped = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            if (text.charAt(i) == '§' && i + 1 < length) {
                i++;
            } else {
                stripped.append(text.charAt(i));
            }
        }
        return cache.computeIfAbsent(stripped.toString(), this::shape).width / scale;
    }

    public float getHeight() {
        return fontHeight;
    }

    private record GlyphLayout(int[] glyphs, Font[] fonts, float[] xs, float[] ys, int[] indices, float width, int count, FontData[] data) {
        GlyphLayout(int[] glyphs, Font[] fonts, float[] xs, float[] ys, int[] indices, float width, FontData[] data) {
            this(glyphs, fonts, xs, ys, indices, width, glyphs.length, data);
        }
    }
}