package de.schosin.ecs.api;

import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;

/**
 * Marker interface for pooled objects and components. 
 * 
 * <p>
 * When a component implements this interface, its instances will
 * be pooled by the world and can be obtained from its {@link PooledComponentMapper}.
 * Some plugins provide additional ways to retrieve an instance of a component.
 * </p>
 * 
 * <p>
 * To support pooling, a component must have a public default constructor. A common
 * pattern is to provide an {@code init} method that acts in place of a regular constructor.
 * 
 * {@snippet:
 * class Position implements Pooled {
 *    
 *   private int x;
 *   private int y;
 *        
 *   public Position init(int x, int y) {
 *     this.x = x;
 *     this.y = =y;
 *        
 *     return this;
 *   }
 *   
 * } 
 * 
 * class SomeSystem {
 *     final PooledComponentMapper<Position> posM;
 *     
 *     void someMethod() {
 *         var pos = posM.getInstance().init(0, 0);
 *         // add to entity
 *     }
 * }
 * }
 * </p>
 * 
 * <p>
 * Accessing a pooled component only through its component mapper or plugins
 * allows the ECS to minimize object allocations and reduce GC pressure.
 * </p>
 */
public interface Pooled {

    /**
     * Reset the state of the object.
     */
    default void reset() {
    }

}
