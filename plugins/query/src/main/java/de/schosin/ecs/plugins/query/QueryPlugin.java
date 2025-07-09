package de.schosin.ecs.plugins.query;

import de.schosin.ecs.api.Plugin;

@Plugin(QueryManager.class)
public interface QueryPlugin {
    
    /*
     * record Reference<R>(Object reference, ComponentType<?, R> componentType) {}
     */
    // TODO overloads to extract components
    Query createQuery(Query.Builder builder);

}
