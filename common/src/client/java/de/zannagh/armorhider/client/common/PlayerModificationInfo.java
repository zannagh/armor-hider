package de.zannagh.armorhider.client.common;

/**
 * Represents the information related to player modification for different equipment slots.
 *
 * @param head The {@link SlotModification} instance representing modifications for the head slot.
 * @param chest The {@link SlotModification} instance representing modifications for the chest slot.
 * @param legs The {@link SlotModification} instance representing modifications for the legs slot.
 * @param feet The {@link SlotModification} instance representing modifications for the feet slot.
 */
public record PlayerModificationInfo(
        SlotModification head,
        SlotModification chest,
        SlotModification legs,
        SlotModification feet
) {

    public boolean allHidden(){
        return head.shouldHide() && chest.shouldHide() && legs.shouldHide() && feet.shouldHide();
    }
}
