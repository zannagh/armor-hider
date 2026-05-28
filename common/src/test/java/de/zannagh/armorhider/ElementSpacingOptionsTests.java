package de.zannagh.armorhider;

import com.mojang.datafixers.util.Pair;
import de.zannagh.armorhider.client.gui.elements.ElementSpacingOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ElementSpacingOptionsTests {

    @Nested
    @DisplayName("Even elements")
    class EvenElements {

        @Test
        @DisplayName("centered elements are evenly spaced with equal edge gaps")
        void centered() {
            var spacing = new ElementSpacingOptions(200)
                    .forEvenElements(20, 4);

            int gap = (200 - 4 * 20) / (4 + 1); // = 24
            for (int i = 0; i < 4; i++) {
                assertEquals(gap + i * (20 + gap), spacing.getX(i));
                assertEquals(20, spacing.getWidth(i));
            }
        }

        @Test
        @DisplayName("single element is centered")
        void singleElement() {
            var spacing = new ElementSpacingOptions(200)
                    .forEvenElements(20, 1);

            int gap = (200 - 20) / 2; // = 90
            assertEquals(gap, spacing.getX(0));
            assertEquals(20, spacing.getWidth(0));
        }

        @Test
        @DisplayName("left-aligned elements start at 0")
        void leftAligned() {
            var spacing = new ElementSpacingOptions(200)
                    .forEvenElements(20, 3)
                    .withLeftAlignment();

            assertEquals(0, spacing.getX(0));
            assertTrue(spacing.getX(1) > spacing.getX(0) + spacing.getWidth(0));
        }

        @Test
        @DisplayName("no elements overlap when centered")
        void noOverlapCentered() {
            var spacing = new ElementSpacingOptions(200)
                    .forEvenElements(20, 5);

            for (int i = 1; i < 5; i++) {
                int prevEnd = spacing.getX(i - 1) + spacing.getWidth(i - 1);
                assertTrue(spacing.getX(i) >= prevEnd,
                        "Element " + i + " overlaps with element " + (i - 1));
            }
        }

        @Test
        @DisplayName("all elements fit within full width")
        void fitsWithinWidth() {
            var spacing = new ElementSpacingOptions(300)
                    .forEvenElements(25, 8);

            for (int i = 0; i < 8; i++) {
                assertTrue(spacing.getX(i) >= 0, "Element " + i + " starts before 0");
                assertTrue(spacing.getX(i) + spacing.getWidth(i) <= 300,
                        "Element " + i + " extends beyond full width");
            }
        }

        @Test
        @DisplayName("left and right aligned produce mirrored layouts")
        void leftRightMirror() {
            var left = new ElementSpacingOptions(200)
                    .forEvenElements(20, 3)
                    .withLeftAlignment();
            var right = new ElementSpacingOptions(200)
                    .forEvenElements(20, 3)
                    .withRightAlignment();

            int lastRight = right.getX(2) + right.getWidth(2);
            assertTrue(lastRight <= 200, "Right-aligned extends beyond width");
            assertEquals(0, left.getX(0), "Left-aligned should start at 0");
        }

        @Test
        @DisplayName("right-aligned last element ends at full width")
        void rightAlignedEndsAtWidth() {
            int fullWidth = 200;
            var spacing = new ElementSpacingOptions(fullWidth)
                    .forEvenElements(20, 3)
                    .withRightAlignment();

            int lastEnd = spacing.getX(2) + spacing.getWidth(2);
            assertEquals(fullWidth, lastEnd,
                    "Right-aligned last element should end at full width");
        }
    }

    @Nested
    @DisplayName("Varying elements")
    class VaryingElements {

        @Test
        @DisplayName("default percentage matches legacy 10% per small element formula")
        void defaultPercentageMatchesLegacy() {
            int width = 310;
            int smallCount = 3;
            var spacing = new ElementSpacingOptions(width)
                    .forVaryingElements(1, smallCount);

            int expectedPrimary = (int) (width * (1 - smallCount * 0.1)) - 4;
            int expectedSecondary = (int) (width * 0.1) - 4;

            assertEquals(expectedPrimary, spacing.getWidth(0));
            assertEquals(expectedSecondary, spacing.getWidth(1));
            assertEquals(expectedSecondary, spacing.getWidth(2));
            assertEquals(expectedSecondary, spacing.getWidth(3));
        }

        @Test
        @DisplayName("explicit percentage overrides default")
        void explicitPercentage() {
            int width = 400;
            var spacing = new ElementSpacingOptions(width)
                    .forVaryingElements(1, 2)
                    .withPercentageWidthForPrimaryElement(70);

            int expectedPrimary = (int) (400 * 70 / 100.0) - 4;
            assertEquals(expectedPrimary, spacing.getWidth(0));

            int remaining = width - expectedPrimary - 4;
            int expectedSecondary = (remaining - (2 - 1) * 4) / 2;
            assertEquals(expectedSecondary, spacing.getWidth(1));
            assertEquals(expectedSecondary, spacing.getWidth(2));
        }

        @Test
        @DisplayName("primary element starts at 0")
        void primaryStartsAtZero() {
            var spacing = new ElementSpacingOptions(400)
                    .forVaryingElements(1, 2);

            assertEquals(0, spacing.getX(0));
        }

        @Test
        @DisplayName("no elements overlap")
        void noOverlap() {
            var spacing = new ElementSpacingOptions(300)
                    .forVaryingElements(1, 3);

            for (int i = 1; i < 4; i++) {
                int prevEnd = spacing.getX(i - 1) + spacing.getWidth(i - 1);
                assertTrue(spacing.getX(i) >= prevEnd,
                        "Element " + i + " overlaps with element " + (i - 1));
            }
        }

        @Test
        @DisplayName("all elements fit within full width")
        void fitsWithinWidth() {
            var spacing = new ElementSpacingOptions(300)
                    .forVaryingElements(1, 3);

            for (int i = 0; i < 4; i++) {
                assertTrue(spacing.getX(i) >= 0);
                assertTrue(spacing.getX(i) + spacing.getWidth(i) <= 300,
                        "Element " + i + " extends beyond full width");
            }
        }

        @Test
        @DisplayName("single large element with no small elements uses full width")
        void singleLargeElement() {
            var spacing = new ElementSpacingOptions(400)
                    .forVaryingElements(1, 0)
                    .withPercentageWidthForPrimaryElement(60)
                    .withGap(0);

            assertEquals(400, spacing.getWidth(0));
        }
    }

    @Nested
    @DisplayName("Panel/preview layout (60/40 split)")
    class PanelPreviewLayout {

        @Test
        @DisplayName("60/40 split matches legacy integer arithmetic")
        void splitMatchesLegacyFormula() {
            int[] testWidths = {310, 400, 800, 1920};

            for (int width : testWidths) {
                var spacing = new ElementSpacingOptions(width)
                        .forVaryingElements(1, 1)
                        .withPercentageWidthForPrimaryElement(60)
                        .withGap(0);

                int expectedPanel = (width * 3) / 5;
                int expectedPreview = width - expectedPanel;

                assertEquals(expectedPanel, spacing.getWidth(0),
                        "Panel width mismatch for full width " + width);
                assertEquals(expectedPreview, spacing.getWidth(1),
                        "Preview width mismatch for full width " + width);
            }
        }

        @Test
        @DisplayName("preview X equals panel width when gap is 0")
        void previewStartsAfterPanel() {
            var spacing = new ElementSpacingOptions(1000)
                    .forVaryingElements(1, 1)
                    .withPercentageWidthForPrimaryElement(60)
                    .withGap(0);

            assertEquals(spacing.getWidth(0), spacing.getX(1));
        }

        @Test
        @DisplayName("panel and preview together fill full width")
        void panelPlusPreviewEqualsFullWidth() {
            int fullWidth = 1920;
            var spacing = new ElementSpacingOptions(fullWidth)
                    .forVaryingElements(1, 1)
                    .withPercentageWidthForPrimaryElement(60)
                    .withGap(0);

            assertEquals(fullWidth, spacing.getWidth(0) + spacing.getWidth(1));
        }

        @Test
        @DisplayName("not in game: single element at full width")
        void notInGame() {
            int fullWidth = 1920;
            var spacing = new ElementSpacingOptions(fullWidth)
                    .forVaryingElements(1, 0)
                    .withPercentageWidthForPrimaryElement(60)
                    .withGap(0);

            assertEquals(fullWidth, spacing.getWidth(0));
        }
    }

    @Nested
    @DisplayName("Custom gap")
    class CustomGap {

        @Test
        @DisplayName("gap of 0 produces tightly packed elements")
        void zeroGap() {
            var spacing = new ElementSpacingOptions(100)
                    .forVaryingElements(1, 2)
                    .withPercentageWidthForPrimaryElement(60)
                    .withGap(0);

            assertEquals(0, spacing.getX(0));
            assertEquals(spacing.getWidth(0), spacing.getX(1));
            assertEquals(spacing.getX(1) + spacing.getWidth(1), spacing.getX(2));
        }

        @Test
        @DisplayName("custom gap is applied between elements")
        void customGapApplied() {
            int customGap = 10;
            var spacing = new ElementSpacingOptions(200)
                    .forEvenElements(20, 3)
                    .withGap(customGap)
                    .withLeftAlignment();

            assertEquals(0, spacing.getX(0));
            assertEquals(20 + customGap, spacing.getX(1));
            assertEquals(2 * (20 + customGap), spacing.getX(2));
        }
    }

    @Nested
    @DisplayName("Groups")
    class Groups {

        @Test
        @DisplayName("groups with fixed sizes distribute elements within each group")
        void groupsWithFixedSizes() {
            var groups = new ArrayList<Pair<Integer, Integer>>();
            groups.add(new Pair<>(0, 0));
            groups.add(new Pair<>(1, 3));

            var spacing = new ElementSpacingOptions(400)
                    .forEvenElements(20, 4)
                    .withGroups(groups)
                    .withSizesForGroups(new int[]{200, 190});

            assertTrue(spacing.getX(0) >= 0);
            assertTrue(spacing.getX(0) + spacing.getWidth(0) <= 200);

            for (int i = 1; i <= 3; i++) {
                assertTrue(spacing.getX(i) >= 200,
                        "Group 2 element " + i + " should start after group 1");
            }
        }

        @Test
        @DisplayName("group sizes scale down when exceeding full width")
        void groupSizesScaleDown() {
            var groups = new ArrayList<Pair<Integer, Integer>>();
            groups.add(new Pair<>(0, 0));
            groups.add(new Pair<>(1, 1));

            var spacing = new ElementSpacingOptions(100)
                    .forEvenElements(20, 2)
                    .withGroups(groups)
                    .withSizesForGroups(new int[]{200, 200});

            for (int i = 0; i < 2; i++) {
                assertTrue(spacing.getX(i) + spacing.getWidth(i) <= 100,
                        "Scaled-down element " + i + " should fit within width");
            }
        }

        @Test
        @DisplayName("groups must be non-overlapping and ascending")
        void groupValidation() {
            var overlapping = new ArrayList<Pair<Integer, Integer>>();
            overlapping.add(new Pair<>(0, 2));
            overlapping.add(new Pair<>(1, 3));

            assertThrows(IllegalArgumentException.class, () ->
                    new ElementSpacingOptions(400)
                            .forEvenElements(20, 4)
                            .withGroups(overlapping));
        }

        @Test
        @DisplayName("out-of-bounds groups throw IndexOutOfBoundsException")
        void groupBoundsValidation() {
            var outOfBounds = new ArrayList<Pair<Integer, Integer>>();
            outOfBounds.add(new Pair<>(0, 5));

            assertThrows(IndexOutOfBoundsException.class, () ->
                    new ElementSpacingOptions(400)
                            .forEvenElements(20, 4)
                            .withGroups(outOfBounds));
        }

        @Test
        @DisplayName("sizes array length must match group count")
        void sizesMismatch() {
            var groups = new ArrayList<Pair<Integer, Integer>>();
            groups.add(new Pair<>(0, 1));

            assertThrows(IllegalArgumentException.class, () ->
                    new ElementSpacingOptions(400)
                            .forEvenElements(20, 2)
                            .withGroups(groups)
                            .withSizesForGroups(new int[]{100, 200}));
        }

        @Test
        @DisplayName("no elements overlap within groups")
        void noOverlapWithinGroups() {
            var groups = new ArrayList<Pair<Integer, Integer>>();
            groups.add(new Pair<>(0, 1));
            groups.add(new Pair<>(2, 4));

            var spacing = new ElementSpacingOptions(400)
                    .forEvenElements(20, 5)
                    .withGroups(groups)
                    .withSizesForGroups(new int[]{100, 290});

            for (int i = 1; i < 5; i++) {
                int prevEnd = spacing.getX(i - 1) + spacing.getWidth(i - 1);
                assertTrue(spacing.getX(i) >= prevEnd,
                        "Element " + i + " overlaps with element " + (i - 1));
            }
        }
    }

    @Nested
    @DisplayName("CompoundOptionWidget compatibility")
    class CompoundOptionWidgetCompat {

        @Test
        @DisplayName("primary width matches legacy getPrimaryWidth")
        void primaryWidthCompat() {
            int[] testWidths = {200, 310, 400};
            int[] smallCounts = {1, 2, 3};

            for (int width : testWidths) {
                for (int sc : smallCounts) {
                    var spacing = new ElementSpacingOptions(width)
                            .forVaryingElements(1, sc);

                    int legacyPrimary = (int) (width * (1 - sc * 0.1)) - 4;
                    assertEquals(legacyPrimary, spacing.getWidth(0),
                            "Primary width mismatch for width=" + width + ", smallCount=" + sc);
                }
            }
        }

        @Test
        @DisplayName("secondary width matches legacy getAdditionalElementWidth")
        void secondaryWidthCompat() {
            int[] testWidths = {200, 310, 400};
            int[] smallCounts = {1, 2, 3};

            for (int width : testWidths) {
                for (int sc : smallCounts) {
                    var spacing = new ElementSpacingOptions(width)
                            .forVaryingElements(1, sc);

                    int legacySecondary = (int) (width * 0.1) - 4;
                    for (int i = 1; i <= sc; i++) {
                        assertEquals(legacySecondary, spacing.getWidth(i),
                                "Secondary width mismatch for width=" + width + ", smallCount=" + sc + ", index=" + i);
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("CompoundButtonWidget compatibility")
    class CompoundButtonWidgetCompat {

        @Test
        @DisplayName("centered square buttons match legacy layout")
        void centeredButtonsCompat() {
            int fullWidth = 310;
            int buttonSize = 20;
            int n = 4;

            var spacing = new ElementSpacingOptions(fullWidth)
                    .forEvenElements(buttonSize, n);

            int totalSpace = fullWidth - n * buttonSize;
            int gap = totalSpace / (n + 1);

            for (int i = 0; i < n; i++) {
                int expected = gap + i * (buttonSize + gap);
                assertEquals(expected, spacing.getX(i),
                        "Button " + i + " X mismatch");
                assertEquals(buttonSize, spacing.getWidth(i));
            }
        }
    }
}
