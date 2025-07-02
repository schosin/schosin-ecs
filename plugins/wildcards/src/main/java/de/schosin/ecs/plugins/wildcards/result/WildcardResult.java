package de.schosin.ecs.plugins.wildcards.result;

import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.components.Result;
import de.schosin.ecs.plugins.wildcards.types.WildcardClassType;

/**
 * Specilized type used by {@link WildcardClassType} that allows accessing a component
 * by its {@link Class}
 * 
 * @param <T> type of component
 */
public interface WildcardResult<T> extends Result<T> {

    /**
     * Retrieves a component that has the given {@code clazz}. The components
     * will be checked by {@code component.getClass() == clazz}.
     *  
     * @param <R> type of component
     * @param clazz class of component
     * @return matching component, or null if not present
     */
    @Nullable
    <R extends T> R get(Class<R> clazz);

}
