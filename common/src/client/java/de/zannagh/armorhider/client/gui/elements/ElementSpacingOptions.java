package de.zannagh.armorhider.client.gui.elements;

import com.mojang.datafixers.util.Pair;
import de.zannagh.armorhider.client.gui.UiConstants;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Options for spacing elements horizontally within a given width.
 * Positions are relative to 0; callers add their own base X offset.
 */
public class ElementSpacingOptions {

    private final int fullWidth;
    private boolean leftAlignment = false;
    private boolean rightAlignment = false;

    private int fixedElementWidth = -1;
    private int totalElementCount = 0;
    private int largeElementCount = 0;
    private int smallElementCount = 0;
    private int primaryPercentage = -1;

    private int groupCount = 0;
    @Nullable private ArrayList<Pair<Integer, Integer>> groups;
    private int @Nullable [] groupSizes;
    private boolean @Nullable [] groupLeftAligned;
    private boolean @Nullable [] groupRightAligned;

    private int gap = UiConstants.DEFAULT_BUTTON_SPACING / 2;
    private static final double SMALL_ELEMENT_WIDTH_PERCENT = 0.1;

    private int @Nullable [] xPositions;
    private int @Nullable [] widths;

    public ElementSpacingOptions(int fullWidth) {
        this.fullWidth = fullWidth;
    }

    public ElementSpacingOptions forEvenElements(int elementWidth, int elementCount) {
        this.fixedElementWidth = elementWidth;
        this.totalElementCount = elementCount;
        invalidate();
        return this;
    }

    public ElementSpacingOptions forVaryingElements(int largeElementCount, int smallElementCount) {
        this.largeElementCount = largeElementCount;
        this.smallElementCount = smallElementCount;
        this.totalElementCount = largeElementCount + smallElementCount;
        invalidate();
        return this;
    }

    /**
     * Configures the options to use a percentage for the first element, making it a primary
     * element. Other elements get spaced via the other configuration options.
     * @param percentage The percentage of the full width to use for the first element.
     * @return this
     */
    public ElementSpacingOptions withPercentageWidthForPrimaryElement(int percentage) {
        this.primaryPercentage = percentage;
        invalidate();
        return this;
    }

    public ElementSpacingOptions withGap(int gap) {
        this.gap = gap;
        invalidate();
        return this;
    }

    public ElementSpacingOptions withLeftAlignment() {
        this.leftAlignment = true;
        this.rightAlignment = false;
        invalidate();
        return this;
    }

    public ElementSpacingOptions withRightAlignment() {
        this.leftAlignment = false;
        this.rightAlignment = true;
        invalidate();
        return this;
    }

    public ElementSpacingOptions withLeftAlignmentForGroup(int groupIndex) {
        ensureGroupAlignmentArrays();
        this.groupLeftAligned[groupIndex] = true;
        this.groupRightAligned[groupIndex] = false;
        invalidate();
        return this;
    }

    public ElementSpacingOptions withRightAlignmentForGroup(int groupIndex) {
        ensureGroupAlignmentArrays();
        this.groupLeftAligned[groupIndex] = false;
        this.groupRightAligned[groupIndex] = true;
        invalidate();
        return this;
    }

    private void ensureGroupAlignmentArrays() {
        if (groupLeftAligned == null) groupLeftAligned = new boolean[groupCount];
        if (groupRightAligned == null) groupRightAligned = new boolean[groupCount];
    }

    /**
     * Configures group spacing with a list of pairs where the first element is the
     * starting index of the group and the second element is the ending index of the group.
     * @param groups the group ranges (inclusive)
     * @return The configured element of the spacing options instance.
     * @throws IllegalArgumentException if the groups are not in ascending order
     * @throws IndexOutOfBoundsException if the groups are out of bounds
     */
    public ElementSpacingOptions withGroups(ArrayList<Pair<Integer, Integer>> groups) {
        for (int i = 1; i < groups.size(); i++) {
            if (groups.get(i).getFirst() <= groups.get(i - 1).getSecond()) {
                throw new IllegalArgumentException("Groups must be non-overlapping and in ascending order");
            }
        }
        for (var group : groups) {
            if (group.getFirst() < 0 || group.getSecond() >= totalElementCount) {
                throw new IndexOutOfBoundsException("Group indices out of bounds: [" +
                        group.getFirst() + ", " + group.getSecond() + "] for " + totalElementCount + " elements");
            }
        }
        this.groupCount = groups.size();
        this.groups = groups;
        this.groupLeftAligned = new boolean[groupCount];
        this.groupRightAligned = new boolean[groupCount];
        invalidate();
        return this;
    }

    /**
     * Configures the element spacing to use fixed sizes for each group.
     * Best effort: if the sum of the sizes will exceed the full width, the sizes get adjusted.
     * @param sizes width for each group
     * @return this
     * @throws IllegalArgumentException if the sizes array is not the same length as the number of groups
     */
    public ElementSpacingOptions withSizesForGroups(int[] sizes) {
        if (sizes.length != groupCount) {
            throw new IllegalArgumentException("Sizes array length (" + sizes.length +
                    ") must match number of groups (" + groupCount + ")");
        }
        this.groupSizes = sizes.clone();
        int total = 0;
        for (int s : this.groupSizes) total += s;
        int totalWithGaps = total + Math.max(groupCount - 1, 0) * gap;
        if (totalWithGaps > fullWidth) {
            int availableForContent = fullWidth - Math.max(groupCount - 1, 0) * gap;
            double scale = (double) availableForContent / total;
            for (int i = 0; i < this.groupSizes.length; i++) {
                this.groupSizes[i] = (int) (this.groupSizes[i] * scale);
            }
        }
        invalidate();
        return this;
    }

    private void invalidate() {
        xPositions = null;
        widths = null;
    }

    private void compute() {
        if (xPositions != null) return;

        xPositions = new int[totalElementCount];
        widths = new int[totalElementCount];

        if (groups != null && groupSizes != null) {
            computeGroupLayout();
        } else if (fixedElementWidth > 0) {
            computeEvenLayout();
        } else if (totalElementCount > 0) {
            computeVaryingLayout();
        }
    }

    private void computeEvenLayout() {
        int n = totalElementCount;
        if (n == 0) return;

        if (leftAlignment) {
            for (int i = 0; i < n; i++) {
                widths[i] = fixedElementWidth;
                xPositions[i] = i * (fixedElementWidth + gap);
            }
        } else if (rightAlignment) {
            int totalUsed = n * fixedElementWidth + (n - 1) * gap;
            int startX = fullWidth - totalUsed;
            for (int i = 0; i < n; i++) {
                widths[i] = fixedElementWidth;
                xPositions[i] = startX + i * (fixedElementWidth + gap);
            }
        } else {
            int totalSpace = fullWidth - n * fixedElementWidth;
            int gap = totalSpace / (n + 1);
            for (int i = 0; i < n; i++) {
                widths[i] = fixedElementWidth;
                xPositions[i] = gap + i * (fixedElementWidth + gap);
            }
        }
    }

    private void computeVaryingLayout() {
        if (totalElementCount == 0) return;

        int primaryWidth;
        int secondaryWidth;

        if (smallElementCount == 0) {
            primaryWidth = fullWidth;
            secondaryWidth = 0;
        } else if (primaryPercentage > 0) {
            primaryWidth = (int) (fullWidth * primaryPercentage / 100.0) - gap;
            int remaining = fullWidth - primaryWidth - gap;
            secondaryWidth = (remaining - (smallElementCount - 1) * gap) / smallElementCount;
        } else {
            primaryWidth = (int) (fullWidth * (1 - smallElementCount * SMALL_ELEMENT_WIDTH_PERCENT)) - gap;
            secondaryWidth = (int) (fullWidth * SMALL_ELEMENT_WIDTH_PERCENT) - gap;
        }

        int x = 0;
        for (int i = 0; i < largeElementCount; i++) {
            xPositions[i] = x;
            widths[i] = primaryWidth;
            x += primaryWidth + gap;
        }
        for (int i = 0; i < smallElementCount; i++) {
            int idx = largeElementCount + i;
            xPositions[idx] = x;
            widths[idx] = secondaryWidth;
            x += secondaryWidth + gap;
        }
    }

    private void computeGroupLayout() {
        int x = 0;
        for (int g = 0; g < groupCount; g++) {
            int groupStart = groups.get(g).getFirst();
            int groupEnd = groups.get(g).getSecond();
            int groupWidth = groupSizes[g];
            int elemCount = groupEnd - groupStart + 1;
            int elemWidth = elemCount > 1
                    ? (groupWidth - (elemCount - 1) * gap) / elemCount
                    : groupWidth;

            boolean gLeft = groupLeftAligned != null && groupLeftAligned[g];
            boolean gRight = groupRightAligned != null && groupRightAligned[g];

            int totalUsed = elemCount * elemWidth + (elemCount - 1) * gap;
            int offsetInGroup;
            if (gRight) {
                offsetInGroup = groupWidth - totalUsed;
            } else if (gLeft) {
                offsetInGroup = 0;
            } else {
                offsetInGroup = (groupWidth - totalUsed) / 2;
            }

            for (int i = 0; i < elemCount; i++) {
                xPositions[groupStart + i] = x + offsetInGroup + i * (elemWidth + gap);
                widths[groupStart + i] = elemWidth;
            }

            x += groupWidth + gap;
        }
    }

    public int getX(int index) {
        compute();
        return xPositions[index];
    }

    public int getWidth(int index) {
        compute();
        return widths[index];
    }
}
