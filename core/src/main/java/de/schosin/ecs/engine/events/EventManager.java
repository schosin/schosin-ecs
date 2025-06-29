package de.schosin.ecs.engine.events;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.schosin.ecs.engine.events.builtin.Event;
import de.schosin.ecs.engine.utils.exceptions.EcsEventHandlerException;
import de.schosin.ecs.storage.api.events.StorageEvent;
import de.schosin.ecs.utils.collections.Bag;

public class EventManager {

    private final Set<Class<?>> noHandlers = new HashSet<>();
    private final Set<Class<?>> processed = new HashSet<>();

    private final Map<Class<?>, Bag<EventHandler<?>>> handlers = new HashMap<>();

    /**
     * Dispatch the event.
     * 
     * @param event event to dispatch
     * @return true if handlers invoked for event
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean dispatchEvent(Object event) {
        // Dispatch event
        var handlers = getHandlers(event.getClass());
        if (handlers != null) {
            var data = handlers.getData();
            for (int i = 0, s = handlers.getSize(); i < s; i++) {
                EventHandler handler = data[i];
                handler.handle(event);
            }
        }

        // Free builtin and storage events
        if (event instanceof Event builtin) {
            builtin.free();
        }

        if (event instanceof StorageEvent storageEvent) {
            storageEvent.free();
        }

        return handlers != null && !handlers.isEmpty();
    }

    private Bag<EventHandler<?>> getHandlers(Class<?> clazz) {
        if (this.noHandlers.contains(clazz)) {
            return null;
        }

        var handlers = this.handlers.get(clazz);
        if (handlers == null) {
            synchronized (this.handlers) {
                handlers = this.handlers.get(clazz);
                if (handlers == null) {

                    var existingHandlers = this.handlers.entrySet().stream()
                            .filter(entry -> entry.getKey().isAssignableFrom(clazz))
                            .map(Map.Entry::getValue)
                            .flatMap(existing -> Arrays.stream(existing.getData(), 0, existing.getSize()))
                            .distinct()
                            .toList();

                    if (existingHandlers.isEmpty()) {
                        this.noHandlers.add(clazz);
                        return null;
                    }

                    handlers = new Bag<>(EventHandler.class, 8);
                    for (var handler : existingHandlers) {
                        handlers.add(handler);
                    }

                    this.handlers.put(clazz, handlers);
                }
            }
        }

        synchronized (this.processed) {
            if (!this.processed.add(clazz)) {
                return handlers;
            }
        }

        var currentHandlers = handlers;

        var matchingHandlers = this.handlers.entrySet().stream()
                .filter(entry -> entry.getKey().isAssignableFrom(clazz))
                .map(Map.Entry::getValue)
                .flatMap(existing -> Arrays.stream(existing.getData(), 0, existing.getSize()))
                .filter(handler -> !currentHandlers.contains(handler))
                .toList();

        if (!matchingHandlers.isEmpty()) {
            for (var handler : matchingHandlers) {
                handlers.add(handler);
            }
        }

        return handlers;
    }

    public <T> void registerEventHandler(EventHandler<T> handler) {
        registerEventHandler(detectEventClass(handler), handler);
    }

    public synchronized <T> void registerEventHandler(Class<T> eventType, EventHandler<T> handler) {
        var bag = this.handlers.get(eventType);
        if (bag == null) {
            bag = new Bag<>(EventHandler.class, 8);
            this.handlers.put(eventType, bag);
        }

        bag.add(handler);

        noHandlers.clear();
        processed.clear();
    }

    private <T> Class<T> detectEventClass(EventHandler<T> handler) {
        var eventType = detectEventClass(handler.getClass(), handler);
        if (eventType == null) {
            throw new EcsEventHandlerException(handler,
                    "Could not determine event type for handler. Implement EventHandler without generic types, or use registerHandler(Class, EventHandler) instead.");
        }

        return eventType;
    }

    private <T> Class<T> detectEventClass(Class<?> handlerClass, EventHandler<T> handler) {
        if (handlerClass.isSynthetic()) {
            throw new EcsEventHandlerException(handler, "Cannot determine event type for synthetic handlers (method reference, lambda). Use registerHandler(Class, EventHandler) instead.");
        }

        var genericInterfaces = handlerClass.getGenericInterfaces();
        for (var genericInterface : genericInterfaces) {
            var eventType = detectEventClass(genericInterface, handler);
            if (eventType != null) {
                return eventType;
            }
        }

        var genericSuperclass = handlerClass.getGenericSuperclass();
        if (genericSuperclass != null) {
            var eventType = detectEventClass(genericSuperclass, handler);
            if (eventType != null) {
                return eventType;
            }
        }

        return null;
    }

    private <T> Class<T> detectEventClass(Type handlerType, EventHandler<T> handler) {
        return switch (handlerType) {
            case Class<?> clazz -> detectEventClass(clazz, handler);
            case ParameterizedType type -> detectEventClass(type, handler);
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> detectEventClass(ParameterizedType type, EventHandler<T> handler) {
        if (EventHandler.class == type.getRawType() && type.getActualTypeArguments()[0] instanceof Class<?> eventType) {
            return (Class<T>) eventType;
        }

        return detectEventClass(type.getRawType(), handler);
    }

}
