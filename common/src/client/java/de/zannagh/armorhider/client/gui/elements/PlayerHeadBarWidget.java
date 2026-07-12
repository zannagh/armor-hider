package de.zannagh.armorhider.client.gui.elements;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

//? if >= 1.21.6
import net.minecraft.client.renderer.RenderPipelines;
//? if > 1.21.8
import net.minecraft.client.input.MouseButtonEvent;

/**
 * A horizontally scrollable bar of player head icons. Each cell renders a player's face (with the hat
 * overlay) blitted from their skin; hovering a cell shows the player's name as a tooltip and clicking it
 * selects that player. Scroll horizontally with the mouse wheel when the heads overflow the bar width.
 */
public class PlayerHeadBarWidget extends AbstractWidget {

    /**
     * One selectable entry in the bar. Player entries render a skin face+hat (texture resolved lazily so it
     * updates as skins load); the special "global" entry renders a full icon and is flagged {@link #global}.
     */
    public static final class Entry {
        public final UUID id;
        public final String name;
        public final boolean global;
        private final boolean fullIcon;
        private final Supplier<Identifier> texture;

        public Entry(UUID id, String name, Supplier<Identifier> faceTexture) {
            this(id, name, faceTexture, false, false);
        }

        private Entry(UUID id, String name, Supplier<Identifier> texture, boolean fullIcon, boolean global) {
            this.id = id;
            this.name = name;
            this.texture = texture;
            this.fullIcon = fullIcon;
            this.global = global;
        }

        /** Creates the special "global configuration" entry, rendered as a full icon rather than a face. */
        public static Entry global(UUID sentinelId, String name, Supplier<Identifier> icon) {
            return new Entry(sentinelId, name, icon, true, true);
        }

        Identifier texture() {
            return texture.get();
        }

        boolean fullIcon() {
            return fullIcon;
        }
    }

    private static final int CELL_GAP = 4;
    private static final int FACE_INSET = 3;

    // Scroll-affordance arrows shown at the bar edges when there is more content to scroll to. Both
    // textures are 23x13 and are drawn at native size, vertically centered, overlaid on top of the cells.
    private static final int ARROW_W = 23;
    private static final int ARROW_H = 13;
    private static final int ARROW_EDGE_PAD = 2;
    private static final Identifier ARROW_BACK = sprite("textures/gui/sprites/arrow_back.png");
    private static final Identifier ARROW_FORWARD = sprite("textures/gui/sprites/arrow_forward.png");

    private static Identifier sprite(String path) {
        //? if >= 1.21 {
        return Identifier.fromNamespaceAndPath("armor-hider", path);
        //?}
        //? if < 1.21 {
        /*return new Identifier("armor-hider", path);
        *///?}
    }

    private final List<Entry> entries;
    private final Consumer<Entry> onSelect;
    private int scrollOffset = 0;
    private int selectedIndex = -1;

    public PlayerHeadBarWidget(int x, int y, int width, int height, List<Entry> entries, Consumer<Entry> onSelect) {
        super(x, y, width, height, Component.empty());
        this.entries = entries;
        this.onSelect = onSelect;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
    }

    public List<Entry> entries() {
        return entries;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int offset) {
        this.scrollOffset = offset;
        clampScroll();
    }

    private int cellSize() {
        return this.height;
    }

    private int stride() {
        return cellSize() + CELL_GAP;
    }

    private int contentWidth() {
        return entries.isEmpty() ? 0 : entries.size() * stride() - CELL_GAP;
    }

    private int maxScroll() {
        return Math.max(0, contentWidth() - this.width);
    }

    private void clampScroll() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll()));
    }

    /**
     * X of the first cell. When all cells fit, they are centered within the bar (and scrolling is disabled);
     * when they overflow, they are left-aligned and shifted by the scroll offset.
     */
    private int contentStartX() {
        if (contentWidth() <= this.width) {
            return getX() + (this.width - contentWidth()) / 2;
        }
        return getX() - scrollOffset;
    }

    /** Index of the cell under (mouseX, mouseY), or -1 if the pointer is over a gap / outside the bar. */
    private int cellAt(double mouseX, double mouseY) {
        if (mouseX < getX() || mouseX > getX() + this.width || mouseY < getY() || mouseY > getY() + this.height) {
            return -1;
        }
        int relative = (int) (mouseX - contentStartX());
        if (relative < 0) {
            return -1;
        }
        if (relative % stride() > cellSize()) {
            return -1;
        }
        int index = relative / stride();
        return (index >= 0 && index < entries.size()) ? index : -1;
    }

    private boolean selectAt(double mouseX, double mouseY) {
        int index = cellAt(mouseX, mouseY);
        if (index < 0) {
            return false;
        }
        selectedIndex = index;
        onSelect.accept(entries.get(index));
        return true;
    }

    private void scrollBy(double scrollY) {
        scrollOffset -= (int) (scrollY * stride());
        clampScroll();
    }

    private void renderBar(net.minecraft.client.gui.GuiGraphicsExtractor context, int mouseX, int mouseY) {
        clampScroll();
        int hovered = cellAt(mouseX, mouseY);

        int startX = contentStartX();
        context.enableScissor(getX(), getY(), getX() + this.width, getY() + this.height);
        for (int i = 0; i < entries.size(); i++) {
            int cellX = startX + i * stride();
            if (cellX + cellSize() < getX() || cellX > getX() + this.width) {
                continue;
            }
            int cellY = getY();
            boolean selected = i == selectedIndex;
            boolean hover = i == hovered;

            int background = selected ? 0xFF3B6EA5 : (hover ? 0x55FFFFFF : 0x33000000);
            context.fill(cellX, cellY, cellX + cellSize(), cellY + cellSize(), background);
            drawCell(context, entries.get(i), cellX + FACE_INSET, cellY + FACE_INSET, cellSize() - FACE_INSET * 2);
            if (selected) {
                drawBorder(context, cellX, cellY, cellSize(), cellSize(), 0xFFFFFFFF);
            }
        }
        context.disableScissor();

        // Scroll affordances: forward arrow on the right when more entries lie to the right, back arrow on
        // the left when more lie to the left. Drawn on top of the (clipped) cells at the bar edges.
        int arrowY = getY() + (this.height - ARROW_H) / 2;
        if (scrollOffset > 0) {
            drawArrow(context, ARROW_BACK, getX() + ARROW_EDGE_PAD, arrowY);
        }
        if (scrollOffset < maxScroll()) {
            drawArrow(context, ARROW_FORWARD, getX() + this.width - ARROW_W - ARROW_EDGE_PAD, arrowY);
        }

        setTooltip(hovered >= 0 ? Tooltip.create(Component.literal(entries.get(hovered).name)) : null);
    }

    private void drawArrow(net.minecraft.client.gui.GuiGraphicsExtractor context, Identifier arrow, int x, int y) {
        // Subtle dark backdrop so the arrow stays legible over face icons behind it.
        context.fill(x - 1, y - 1, x + ARROW_W + 1, y + ARROW_H + 1, 0x99000000);
        drawTextureRegion(context, arrow, x, y, ARROW_W, ARROW_H, 0.0F, 0.0F, ARROW_W, ARROW_H, ARROW_W, ARROW_H);
    }

    private void drawBorder(net.minecraft.client.gui.GuiGraphicsExtractor context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawCell(net.minecraft.client.gui.GuiGraphicsExtractor context, Entry entry, int x, int y, int size) {
        if (entry.fullIcon()) {
            // Full 16x16 icon (e.g. the global-configuration icon), scaled into the cell.
            drawTextureRegion(context, entry.texture(), x, y, size, size, 0.0F, 0.0F, 16, 16, 16, 16);
        } else {
            // Face layer (skin UV 8,8) and hat overlay (skin UV 40,8), each an 8x8 region of the 64x64 skin.
            drawTextureRegion(context, entry.texture(), x, y, size, size, 8.0F, 8.0F, 8, 8, 64, 64);
            drawTextureRegion(context, entry.texture(), x, y, size, size, 40.0F, 8.0F, 8, 8, 64, 64);
        }
    }

    /** Blits a texture region scaled into a drawW x drawH rect. The blit overload differs per rendering epoch. */
    private void drawTextureRegion(net.minecraft.client.gui.GuiGraphicsExtractor context, Identifier texture,
                                   int x, int y, int drawW, int drawH, float u, float v, int regionW, int regionH, int texW, int texH) {
        //? if >= 1.21.6 {
        context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, drawW, drawH, regionW, regionH, texW, texH);
        //?}
        //? if >= 1.21.4 && < 1.21.6 {
        /*context.blit((t) -> net.minecraft.client.renderer.rendertype.RenderType.guiTextured(t), texture, x, y, u, v, drawW, drawH, regionW, regionH, texW, texH);
        *///?}
        //? if < 1.21.4 {
        /*context.blit(texture, x, y, drawW, drawH, u, v, regionW, regionH, texW, texH);
        *///?}
    }

    //? if >= 26.1-1.pre.1 {
    @Override
    protected void extractWidgetRenderState(net.minecraft.client.gui.GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        renderBar(context, mouseX, mouseY);
    }
    //?}
    //? if < 26.1-1.pre.1 {
    /*@Override
    protected void renderWidget(net.minecraft.client.gui.GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        renderBar(context, mouseX, mouseY);
    }
    *///?}

    //? if > 1.21.8 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.active || !this.visible || event.button() != 0) {
            return false;
        }
        return selectAt(event.x(), event.y());
    }
    //?}
    //? if <= 1.21.8 {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || button != 0) {
            return false;
        }
        return selectAt(mouseX, mouseY);
    }
    *///?}

    //? if >= 1.21 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollBy(scrollY);
        return true;
    }
    //?}
    //? if < 1.21 {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        scrollBy(scrollY);
        return true;
    }
    *///?}

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // No narration needed
    }
}
