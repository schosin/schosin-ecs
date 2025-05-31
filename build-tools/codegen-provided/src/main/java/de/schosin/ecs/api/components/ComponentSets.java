package de.schosin.ecs.api.components;

import de.schosin.ecs.api.components.ComponentSet.ComponentSetData;

/**
 * Placeholder instance providing the API for the core module to work with
 * generated component set instances.
 * 
 * <p>
 * The annotation processor "{@code de.schosin.ecs.buildtools:codegen-apt}"
 * will generate the actual type in user code that will implement the same
 * methods.
 * </p>
 */
@SuppressWarnings("unused")
public class ComponentSets {

    public static <S extends ComponentSet<?>> ComponentSetData<S> getData(Class<S> componentSet) {
        return null;
    }

    private ComponentSets() {
    }

}
