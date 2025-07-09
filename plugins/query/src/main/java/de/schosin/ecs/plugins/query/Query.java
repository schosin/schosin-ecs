package de.schosin.ecs.plugins.query;

import static de.schosin.ecs.api.components.types.ComponentType.component;
import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;

/*
 * https://ajmmertens.medium.com/why-it-is-time-to-start-thinking-of-games-as-databases-e7971da33ac3
 */
// TODO see what can be translated from that article
public interface Query {

    static Query.Builder has(Class<?> component) {
        return new Builder().has(component);
    }
    
    void process(IntConsumer consumer);

    class Builder {
        
        // TODO query traversal operators: up(ChildOf)
        // TODO requires ChildOf relation as a built-in/plugin relation with special treatment

        final Map<Object, List<Condition>> conditions = new IdentityHashMap<>();

        public Builder has(Class<?> clazz) {
            return has(This.THIS, clazz);
        }

        public Builder has(Object reference, Class<?> clazz) {
            getConditions(reference).add(new ComponentCondition(reference, component(clazz)));
            return this;
        }

        public Builder hasRelation(Class<?> relationship, Object targetReference) {
            return hasRelation(This.THIS, relationship, targetReference);
        }

        // TODO user must be able to reference "THIS"
        // Faction($this), AlliedWith($this, $ally), TradesWith($ally, $this)
        public Builder hasRelation(Object reference, Class<?> relationship, Object targetReference) {
            var relationType = Exclusive.class.isAssignableFrom(relationship)
                    ? exclusiveRelation(Exclusive.class.asSubclass(relationship))
                    : relation(relationship);

            getConditions(reference).add(new EntityRelationCondition(reference, relationType, targetReference));
            return this;
        }

        private List<Condition> getConditions(Object reference) {
            return this.conditions.computeIfAbsent(reference, ignore -> new ArrayList<>());
        }

    }

}

enum This {
    THIS
}

sealed interface Condition {
}

record ComponentCondition(Object reference, ComponentType<?, ?> componentType) implements Condition {
}

record EntityRelationCondition(Object reference, RegularEntityRelationType<?, ?> relationType, Object target) implements Condition {
}
