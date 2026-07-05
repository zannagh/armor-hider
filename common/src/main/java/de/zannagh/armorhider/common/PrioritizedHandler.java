package de.zannagh.armorhider.common;

/**
 * Represents a handler with an associated priority for processing events.
 * This interface allows defining logic to determine if an event should be handled,
 * whether it should be handled exclusively, and the actual handling behavior.<br><br>
 * Handlers implementing this interface can be prioritized by comparing their priority values.
 * The priority system facilitates control over the order in which handlers are invoked.
 *
 * @param <TEvent> The type of event that this handler processes.
 */
public interface PrioritizedHandler<TEvent> {

    int DEFAULT_PRIORITY = 100;

    default int getPriority() { return DEFAULT_PRIORITY; }

    /**
     * Determines whether a given event should be handled by this handler.
     *
     * @param event The event to evaluate for handling.
     * @return {@code true} if the event should be handled by this handler;
     *         {@code false} otherwise.
     */
    boolean shouldHandle(TEvent event);

    /**
     * Determines whether the given event should be handled exclusively by this handler.
     * If the event is to be handled exclusively, no other handlers will process the event.
     *
     * @param event The event to evaluate for exclusive handling.
     * @return {@code true} if the event should be handled exclusively by this handler;
     *         {@code false} otherwise.
     */
    boolean shouldHandleExclusively(TEvent event);

    /**
     * Handles the event. The handler can decide internally to mutate the event for following handlers.
     * @param event The event to handle.
     * @return The potentially mutated event for subsequent handlers.
     */
    TEvent handle(TEvent event);
}
