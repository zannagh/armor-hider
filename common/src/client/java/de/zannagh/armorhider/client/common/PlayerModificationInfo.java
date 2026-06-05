package de.zannagh.armorhider.client.common;

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
