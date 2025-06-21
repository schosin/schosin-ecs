package de.schosin.ecs.worlds;

import de.schosin.ecs.api.World;
import de.schosin.ecs.plugins.archetype.ArchetypePlugin;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.CompositionPlugin;

/**
 * A custom {@link World} that includes multiple plugins to extend the functionality.
 * Use {@link DefaultWorld#create} to create an instance, or {@link DefaultWorld#builder()}
 * to obtain a builder for further customization.
 * 
 * <ol>
 * <li><b>StatePlugin:</b> Provides ways to attach non-component state to entities</li>
 * <li><b>ArchetypePlugin:</b> Provides ways to create predefined entities</li>
 * <li><b>TransmuterPlugin:</b> Provides ways to mutate the component composition of entities</li>
 * <li><b>CompositionPlugin:</b> Provides ways to query for entities matching a {@link Composition.Builder composition} and extracting components</li>
 * </ol>
 */
public interface DefaultWorld extends World, ArchetypePlugin, CompositionPlugin {

    /**
     * Creates an instance of {@link DefaultWorld}.
     * 
     * @return instance
     */
    static DefaultWorld create() {
        return builder().build();
    }

    /**
     * Creates a builder for {@link DefaultWorld}.
     * 
     * @return builder instance
     */
    static World.Builder<DefaultWorld> builder() {
        return World.builder(DefaultWorld.class);
    }

}
