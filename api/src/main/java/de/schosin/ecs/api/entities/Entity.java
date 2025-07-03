package de.schosin.ecs.api.entities;

import static de.schosin.ecs.api.components.types.ComponentType.component;
import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.Relations.EntityRelations;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;

public interface Entity {

    int id();

    boolean isAlive();

    default <R> R get(Class<R> clazz) {
        return get(component(clazz));
    }

    default <R, T> ComponentRelations<R, T> getRelations(Class<R> relationship, Class<T> target) {
        return get(relation(relationship, target));
    }

    default <R extends Exclusive, T> ComponentRelation<R, T> getRelation(Class<R> relationship, Class<T> target) {
        return get(exclusiveRelation(relationship, target));
    }

    default <R> EntityRelations<R> getEntityRelations(Class<R> relationship) {
        return get(relation(relationship));
    }

    default <R extends Exclusive> EntityRelation<R> getEntityRelation(Class<R> relationship) {
        return get(exclusiveRelation(relationship));
    }

    <R> R get(RegularComponentType<?, R> componentType);

}
