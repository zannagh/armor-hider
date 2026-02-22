//? if >= 1.20.5 {
package de.zannagh.armorhider.net;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Marker wrapper class to prevent infinite recursion when other mods (like Carpet) also mixin
 * to the CustomPacketPayload.codec() method and call it recursively.
 * <p>
 * The instanceof check against this class is used to detect if we've already processed the list,
 * similar to how Carpet uses their CarpetTaintedList.
 */
public class ArmorHiderPayloadList<E> extends ArrayList<E> {

    public ArmorHiderPayloadList(Collection<? extends E> c) {
        super(c);
    }
}
//?}
