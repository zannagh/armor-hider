package de.zannagh.armorhider.client.common;

import de.zannagh.armorhider.client.api.AhRenderer;
import de.zannagh.armorhider.common.ItemInfo;
import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.Nullable;

/**
 * The decision an {@link AhRenderer} returns to its caller after looking at a render call.
 * <p>
 * Three meaningful states:
 * <ul>
 *   <li><b>{@code shouldIntercept = false}</b>: the renderer has no interest in this call —
 *       caller proceeds with vanilla rendering. Returned by
 *       {@link #ignore()}.</li>
 *   <li><b>{@code shouldIntercept = true}, {@code shouldCancel = false}</b>: the caller should
 *       {@link de.zannagh.armorhider.client.api.AhRenderManagementApi#enterScope(RenderInterceptionResult) enter the scope}
 *       so downstream WrapOps see the modification; the underlying render still runs.</li>
 *   <li><b>{@code shouldIntercept = true}, {@code shouldCancel = true}</b>: the renderer already
 *       cancelled the mixin callback; the caller should bail out without entering the scope.</li>
 * </ul>
 *
 * @param shouldIntercept whether the caller should hand control to the scope-management API.
 * @param shouldCancel    whether the underlying mixin callback was cancelled by the renderer.
 * @param scope           the scope this decision applies to; {@link RenderScope#NONE} when
 *                        {@code shouldIntercept} is {@code false}.
 * @param carrier         identity carrier the decision was made against, or {@code null} when
 *                        none was available.
 * @param modification    the resolved modification, or {@code null} for the ignore case.
 * @since 0.12.0
 */
public record RenderInterceptionResult(
        boolean shouldIntercept,
        boolean shouldCancel,
        RenderScope scope,
        @Nullable IdentityCarrier carrier,
        @Nullable SlotModification modification
) {

    /**
     * The canonical "renderer has nothing to say" result. {@code shouldIntercept} and
     * {@code shouldCancel} are both {@code false}; the scope is {@link RenderScope#NONE}.
     */
    public static RenderInterceptionResult ignore() {
        return new RenderInterceptionResult(false, false, RenderScope.NONE, null, null);
    }

    /**
     * @return the equipment slot the resolved modification applies to, or {@code null} when the
     * result carries no modification (the {@link #ignore()} case).
     */
    public @Nullable EquipmentSlot getSlot() {
        if (modification == null) {
            return null;
        }
        return modification.slot();
    }

    /**
     * @return information about the item the modification applies to, or
     * {@link ItemInfo#empty()} when the result carries no modification.
     */
    public ItemInfo getItemInfo() {
        if (modification == null) {
            return ItemInfo.empty();
        }
        return modification.itemInfo();
    }
}
