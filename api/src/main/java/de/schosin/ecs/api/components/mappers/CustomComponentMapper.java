package de.schosin.ecs.api.components.mappers;

import de.schosin.ecs.api.components.types.CustomComponentType;

public non-sealed interface CustomComponentMapper<T, R> extends Components<T, R> {

    @FunctionalInterface
    interface Factory {

        <T, R, C extends CustomComponentMapper<T, R>> C createComponents(CustomComponentType<T, R, C> type);

    }

    @FunctionalInterface
    @SuppressWarnings({ "unchecked", "rawtypes" })
    interface FactoryAdapter<T extends CustomComponentType, C extends CustomComponentMapper> extends Factory {

        @Override
        default <TT, R, CC extends CustomComponentMapper<TT, R>> CC createComponents(CustomComponentType<TT, R, CC> type) {
            return (CC) create((T) type);
        }

        C create(T type);

    }

}
