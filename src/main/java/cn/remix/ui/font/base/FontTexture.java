package cn.remix.ui.font.base;

import cn.remix.util.IMinecraft;
import injection.accessor.NativeImageAccessor;
import lombok.Getter;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FontTexture implements IMinecraft {
    private static final int atlasSize = 4096, maxGlyph = 512;
    private static final AtomicInteger pageCounter = new AtomicInteger(0);
    private final Map<Long, FontData> glyphCache = new HashMap<>(512, 0.5f);
    private final IdentityHashMap<Font, Integer> fontIdMap = new IdentityHashMap<>();
    private final List<TexturePage> pages = new ArrayList<>();
    private final Map<Integer, Font> charFontCache = new HashMap<>();
    private final Font primaryFont;
    private final List<Font> fallbackFonts;
    private int nextFontId;
    private TexturePage page;

    @Getter
    private final FontRenderContext context;
    @Getter
    private final int fontHeight, fontAscent;
    private final int[] pixels;
    private final Graphics2D graphics;

    public FontTexture(Font primaryFont, List<Font> fallbackFonts) {
        Map<TextAttribute, Object> attr = new HashMap<>();
        attr.put(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
        attr.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        this.primaryFont = primaryFont.deriveFont(attr);
        this.fallbackFonts = fallbackFonts.stream().map(f -> f.deriveFont(attr)).toList();

        BufferedImage buf = new BufferedImage(maxGlyph, maxGlyph, BufferedImage.TYPE_INT_ARGB);
        this.pixels = ((DataBufferInt) buf.getRaster().getDataBuffer()).getData();
        this.graphics = buf.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        this.context = graphics.getFontRenderContext();
        FontMetrics fm = graphics.getFontMetrics(this.primaryFont);
        this.fontHeight = fm.getAscent() + fm.getDescent();
        this.fontAscent = fm.getAscent();
        this.pages.add(this.page = new TexturePage());
    }

    public Font getFont(int character) {
        Font cached = charFontCache.get(character);
        if (cached != null) return cached;
        Font result = primaryFont;

        if (!primaryFont.canDisplay(character)) {
            for (Font f : fallbackFonts) {
                if (f.canDisplay(character)) {
                    result = f;
                    break;
                }
            }
        }
        charFontCache.put(character, result);
        return result;
    }

    public FontData getGlyphTexture(Font font, int glyphCode) {
        long key = ((long) fontIdMap.computeIfAbsent(font, ignored -> nextFontId++) << 32) | (glyphCode & 0xFFFFFFFFL);
        FontData existing = glyphCache.get(key);
        if (existing != null) return existing;

        GlyphVector gv = font.createGlyphVector(context, new int[]{glyphCode});
        Shape outline = gv.getGlyphOutline(0);
        Rectangle2D bounds = outline.getBounds2D();
        float adv = gv.getGlyphMetrics(0).getAdvance();

        if (bounds.isEmpty() || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            FontData empty = new FontData(page.id, 0, 0, 0, 0, 0, 0, adv, 0, 0);
            glyphCache.put(key, empty);
            return empty;
        }

        float offX = (float) bounds.getX() - 2, offY = (float) bounds.getY() - 2;
        int w = (int) Math.ceil(bounds.getWidth()) + 4, h = (int) Math.ceil(bounds.getHeight()) + 4;

        if (page.x + w > atlasSize) { page.x = 1; page.y += page.h + 1; page.h = 0; }
        if (page.y + h > atlasSize) pages.add(page = new TexturePage());

        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, w, h);
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setColor(Color.WHITE);
        graphics.translate(-offX, -offY);
        graphics.fill(outline);
        graphics.translate(offX, offY);

        long ptr = ((NativeImageAccessor) (Object) page.image).getPointer();
        for (int r = 0; r < h; r++) {
            int rowOff = r * maxGlyph, last = 0xFFFFFF;
            for (int c = 0; c < w; c++) {
                int p = pixels[rowOff + c];
                if ((p >>> 24) > 5) last = p & 0xFFFFFF;
                else pixels[rowOff + c] = (p & 0xFF000000) | last;
            }
            long rowPtr = ptr + ((long) (page.y + r) * atlasSize + page.x) * 4;
            last = 0xFFFFFF;
            for (int c = w - 1; c >= 0; c--) {
                int p = pixels[rowOff + c];
                if ((p >>> 24) > 5) last = p & 0xFFFFFF;
                else p = (p & 0xFF000000) | last;
                MemoryUtil.memPutInt(rowPtr + (c * 4L), (p & 0xFF00FF00) | ((p & 0xFF) << 16) | ((p >> 16) & 0xFF));
            }
        }

        float inv = 1f / atlasSize;
        FontData data = new FontData(page.id, page.x * inv, page.y * inv, (page.x + w) * inv, (page.y + h) * inv, w, h, adv, offX, offY);
        glyphCache.put(key, data);
        page.x += w + 1;
        page.h = Math.max(page.h, h);
        page.dirty = true;
        return data;
    }

    public void flush() {
        pages.forEach(p -> {
            if (p.dirty) {
                p.texture.upload();
                p.dirty = false;
            }
        });
    }

    private static class TexturePage {
        final NativeImage image = new NativeImage(atlasSize, atlasSize, false);
        final Identifier id = Identifier.of("flux", "_" + pageCounter.incrementAndGet());
        final NativeImageBackedTexture texture;
        int x = 1, y = 1, h = 0;
        boolean dirty;

        TexturePage() {
            texture = new NativeImageBackedTexture(id::toString, image);
            mc.getTextureManager().registerTexture(id, texture);
        }
    }
}