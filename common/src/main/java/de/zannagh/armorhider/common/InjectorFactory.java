package de.zannagh.armorhider.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class InjectorFactory<TEvent, THandler extends PrioritizedHandler<TEvent>> {
    private final ArrayList<THandler> instances = new ArrayList<>();

    public InjectorFactory(THandler defaultHandler) {
        addHandler(defaultHandler);
    }

    public void addHandler(THandler injector) {
        instances.add(injector);
        instances.sort(Comparator.comparingInt(PrioritizedHandler::getPriority));
    }

    public List<THandler> getInstancesFor(TEvent event) {
        var handlers = instances.stream().filter(injector -> injector.shouldHandle(event)).toList();
        if (handlers.stream().anyMatch(handler -> handler.shouldHandleExclusively(event))) {
            var exclusiveHandler = handlers.stream().filter(handler -> handler.shouldHandleExclusively(event)).findFirst();
            if (exclusiveHandler.isPresent()) {
                return List.of(exclusiveHandler.get());
            }
        }
        return handlers;
    }

    public THandler getPreferredHandler(){
        return instances.get(0);
    }

    public ArrayList<THandler> getInstances(){
        return instances;
    }

    public Optional<THandler> findHandler(Predicate<THandler> predicate) {
        return instances.stream().filter(predicate).findFirst();
    }

    public boolean anyHandler(Predicate<THandler> predicate) {
        return instances.stream().anyMatch(predicate);
    }

    public void handle(TEvent event){
        var mutatedEvent = event;
        for (var handler : getInstancesFor(mutatedEvent)) {
            mutatedEvent = handler.handle(mutatedEvent);
        }
    }
}
