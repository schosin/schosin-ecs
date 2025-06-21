package de.schosin.ecs.plugins.experimental.system;

import java.util.concurrent.ExecutorService;
import java.util.function.UnaryOperator;

import de.schosin.ecs.api.Plugin;
import de.schosin.ecs.api.Plugin.ProxyPlugin;
import de.schosin.ecs.api.World;
import de.schosin.ecs.plugins.experimental.system.systems.BaseSystem;

@Plugin(SystemManager.class)
public interface SystemPlugin extends ProxyPlugin {

    /**
     * Creates a standalone instance of the plugin.
     * 
     * @param world world to bind to
     * @return standalone instance
     */
    static SystemPlugin standalone(World world) {
        return new SystemManager(world, null);
    }

    /**
     * Creates a standalone instance of the plugin.
     * 
     * @param world world to bind to
     * @param executor executor to use for parallel execution
     * @return standalone instance
     */
    static SystemPlugin standalone(World world, ExecutorService executor) {
        return new SystemManager(world, new SystemConfig(executor));
    }

    /**
     * Processes the systems and invokes {@link World#process()} afterwards.
     */
    void processSystems();

    /**
     * Retrieves a group by its id or null if not found.
     */
    SystemGroup getSystemGroup(Object groupId);

    /**
     * Enables the {@link SystemGroup} with that id.
     */
    void enable(Object groupId);

    /**
     * Disables the {@link SystemGroup} with that id.
     */
    void disable(Object groupId);

    /**
     * Adds the systems to the root system group.
     * These systems will be run sequentially after all previously added systems and groups.
     * 
     * <p>
     * The systems will be instantiated using a public constructor that accepts a single parameter
     * of type {@link World} or a subtype of {@link World}. If no such constructor exists, the
     * default constructor (no arguments) will be used.
     * 
     * @return root {@link SystemGroup}
     * @throws ClassCastException if any system does not implement {@link BaseSystem}
     */
    SystemGroup addSystems(Class<?>... systems);

    /**
     * Adds the systems to the root system group.
     * These systems will be run sequentially after all previously added systems and groups.
     * 
     * @return root {@link SystemGroup}
     */
    SystemGroup addSystems(BaseSystem... systems);

    /**
     * Creates a new parallel {@link SystemGroup} and adds it to the root. Systems and groups added to the new system group will be run in sequence.
     * 
     * <p>
     * This example adds three groups of systems that will be run sequentially.
     * 
     * <ul>
     * <li>InputSystem, AiSystem in parallel</l>
     * <li>MovementSystem, CollisionSystem, CameraSystem in sequence</li>
     * <li>SpriteRenderSystem, HudRenderSystem in sequence</li>
     * <li>EntityIdRenderSystem, HitboxRenderSystem in sequence if enabled (e.g. via keybind)</li>
     * </ul>
     * 
     * {@snippet:
     * enum Systems { Logic, Render, DebugRender }
     * 
     * world
     *     .addSystemGroup(Systems.Logic, logic -> logic
     *         .addParallel(InputSystem.class, AiSystem.class)
     *         .add(MovementSystem.class, CollisionSystem.class, CameraSystem.class))
     *     .addSystemGroup(Systems.Render, render -> render
     *         .add(SpriteRenderSystem.class, HudRenderSystem.class))
     *     .addSystemGroup(Systems.DebugRender, debug -> debug
     *         .disable() // disabled by default
     *         .add(EntityIdRenderSystem.class, HitboxRenderSystem.class))
     * }
     * 
     * <p>
     * The created group can be {@link #getSystemGroup(Object) accessed}, {@link #enable(Object) enabled} and
     * {@link #disable(Object) disabled} using the assigned groupId.
     * 
     * @param id of the group, will be used as a map key. Use an {@link Enum enum} or a String for easier lookup later.
     * @param group initialization operator
     * @return root {@link SystemGroup}
     */
    SystemGroup addSystemGroup(Object groupId, UnaryOperator<SystemGroup> group);

    /**
     * Creates a new parallel {@link SystemGroup} and adds it to the root. Systems and groups added to the new system group will be run in parallel.
     * 
     * <p>
     * {@snippet:
     * enum Systems { Logic, Render, DebugRender }
     * 
     * world
     *     .addSystemGroup(Systems.Logic, logic -> logic
     *         .addParallel(InputSystem.class, AiSystem.class)
     *         .add(MovementSystem.class, CollisionSystem.class, CameraSystem.class))
     *     .addSystemGroup(Systems.Render, render -> render
     *         .add(SpriteRenderSystem.class, HudRenderSystem.class))
     *     .addSystemGroup(Systems.DebugRender, debug -> debug
     *         .add(EntityIdRenderSystem.class, HitboxRenderSystem.class))
     * }
     * 
     * <p>
     * The created group can be {@link #getSystemGroup(Object) accessed}, {@link #enable(Object) enabled} and
     * {@link #disable(Object) disabled} using the assigned groupId.
     * 
     * @param id of the group, will be used as a map key. Use an {@link Enum enum} for easier lookup later.
     * @param group initialization operator
     * @return root {@link SystemGroup}
     */
    SystemGroup addParallelSystemGroup(Object groupId, UnaryOperator<SystemGroup> group);

}
