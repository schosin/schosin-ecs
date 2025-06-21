package de.schosin.ecs.plugins.experimental.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.Plugin.PluginConfig;
import de.schosin.ecs.api.World;
import de.schosin.ecs.plugins.experimental.system.systems.BaseSystem;
import de.schosin.ecs.utils.ReflectionUtils;
import de.schosin.ecs.utils.collections.Bag;

public final class SystemManager implements SystemPlugin {

    record Unnamed(int id, String display) {
        Unnamed(int id) {
            this(id, "unnamed-group-" + id);
        }

        @Override
        public final String toString() {
            return display;
        }
    }

    enum Root {
        ROOT
    }

    private World world;
    private final Sequential root;

    private final AtomicInteger counter = new AtomicInteger(1);
    private final Map<Object, SystemGroup> groups = new ConcurrentHashMap<>();

    public SystemManager(World world, @Nullable PluginConfig config) {
        this.world = world;

        // cast instead of instanceof to ensure no invalid config passed
        var executorService = config != null
                ? ((SystemConfig) config).executor()
                : Executors.newWorkStealingPool();

        this.root = new Sequential(Root.ROOT, executorService);
    }

    @Override
    public void setProxyWorld(World world) {
        this.world = world;
    }

    @Override
    public void processSystems() {
        root.process();
        world.process();
    }

    @Override
    public SystemGroup getSystemGroup(Object groupId) {
        return groups.get(groupId);
    }

    @Override
    public void enable(Object groupId) {
        var group = groups.get(groupId);
        if (group == null) {
            throw new IllegalArgumentException("No SystemGroup with id '%s' found".formatted(groupId));
        }

        group.enable();
    }

    @Override
    public void disable(Object groupId) {
        var group = groups.get(groupId);
        if (group == null) {
            throw new IllegalArgumentException("No SystemGroup with id '%s' found".formatted(groupId));
        }

        group.disable();
    }

    @Override
    public SystemGroup addSystems(Class<?>... systems) {
        return addSystems(instantiate(systems));
    }

    @Override
    public final SystemGroup addSystems(BaseSystem... systems) {
        return root.add(systems);
    }

    @Override
    public final synchronized SystemGroup addSystemGroup(Object groupId, UnaryOperator<SystemGroup> group) {
        if (groupId == null) {
            throw new IllegalArgumentException("groupId must not be null");
        }

        return root.add(groupId, group);
    }

    @Override
    public final synchronized SystemGroup addParallelSystemGroup(Object groupId, UnaryOperator<SystemGroup> group) {
        return root.addParallel(groupId, group);
    }

    private synchronized Unnamed nextGroupId() {
        return new Unnamed(counter.getAndIncrement());
    }

    private BaseSystem[] instantiate(Class<?>[] classes) {
        var systems = new BaseSystem[classes.length];

        for (int i = 0, s = classes.length; i < s; i++) {
            systems[i] = (BaseSystem) ReflectionUtils.createInstance(world, classes[i]);
        }

        return systems;
    }

    abstract sealed class AbstractSystemGroup implements SystemGroup {

        protected final Object id;
        protected final ExecutorService executor;

        protected final Bag<BaseSystem> systems = new Bag<>(BaseSystem.class, 4);
        protected final Bag<AbstractSystemGroup> groups = new Bag<>(AbstractSystemGroup.class, 4);

        private AbstractSystemGroup current;

        protected boolean disabled;

        public AbstractSystemGroup(Object id, ExecutorService executor) {
            this.id = id;
            this.executor = executor;
        }

        @Override
        public final SystemGroup enable() {
            this.disabled = false;

            return this;
        }

        @Override
        public final SystemGroup disable() {
            this.disabled = true;

            return this;
        }

        @Override
        public final SystemGroup add(Class<?>... systems) {
            return add(instantiate(systems));
        }

        @Override
        public final SystemGroup add(BaseSystem... systems) {
            if (systems.length == 0) {
                throw new IllegalArgumentException("Must pass atleast one system");
            }

            // Add to this group if parallel or no sub-group exists
            if (this instanceof Parallel || current == null) {
                for (var system : systems) {
                    this.systems.add(system);
                }

                changed();
                return this;
            }

            // Add to current group if sequential and unnamed
            if (current instanceof Sequential && current.id instanceof Unnamed) {
                for (var system : systems) {
                    this.current.add(system);
                }

                changed();
                return this;
            }

            // Add to a new unnamed group
            var group = this.current = new Sequential(nextGroupId(), executor);

            this.groups.add(group);
            SystemManager.this.groups.put(group.id, group);

            for (var system : systems) {
                group.add(system);
            }

            changed();
            return this;
        }

        @Override
        public final synchronized SystemGroup add(Object groupId, UnaryOperator<SystemGroup> operator) {
            if (SystemManager.this.groups.containsKey(groupId)) {
                throw new IllegalArgumentException("Cannot reuse groupId '%s'".formatted(groupId));
            }

            // Add a new named sequential group
            var group = this.current = new Sequential(groupId, executor);
            operator.apply(group);

            this.groups.add(group);
            SystemManager.this.groups.put(group.id, group);

            changed();
            return this;
        }

        @Override
        public SystemGroup addParallel(Class<?>... systems) {
            return addParallel(instantiate(systems));
        }

        @Override
        public SystemGroup addParallel(BaseSystem... systems) {
            if (systems.length == 0) {
                throw new IllegalArgumentException("Must pass atleast one system");
            }

            // Add to this group if parallel and unnamed
            if (this instanceof Parallel && this.id instanceof Unnamed) {
                for (var system : systems) {
                    this.systems.add(system);
                }

                changed();
                return this;
            }

            // Add to current group if parallel and unnamed
            if (current instanceof Parallel && current.id instanceof Unnamed) {
                for (var system : systems) {
                    this.current.add(system);
                }

                changed();
                return this;
            }

            // Add to a new unnamed group
            var group = this.current = new Sequential(nextGroupId(), executor);

            this.groups.add(group);
            SystemManager.this.groups.put(group.id, group);

            for (var system : systems) {
                group.add(system);
            }

            changed();
            return this;
        }

        @Override
        public final synchronized SystemGroup addParallel(Object groupId, UnaryOperator<SystemGroup> operator) {
            if (SystemManager.this.groups.containsKey(groupId)) {
                throw new IllegalArgumentException("Cannot reuse groupId '%s'".formatted(groupId));
            }

            // Add a new named parallel group
            var group = this.current = new Parallel(groupId, executor);
            operator.apply(group);

            this.groups.add(group);
            SystemManager.this.groups.put(groupId, group);

            changed();
            return this;
        }

        protected void changed() {
        }

        @Override
        public final String toString() {
            var builder = new StringBuilder().append(this.getClass().getSimpleName()).append("(id = ").append(id);

            var systemsSize = systems.getSize();
            if (systemsSize > 0) {
                builder.append(", systems = ").append(systemsSize);
            }

            var groupsSize = groups.getSize();
            if (groupsSize > 0) {
                builder.append(", groups = ").append(groupsSize);
            }

            return builder.append(")").toString();
        }

    }

    private final class Sequential extends AbstractSystemGroup {

        private Sequential(Object groupId, ExecutorService executor) {
            super(groupId, executor);
        }

        @Override
        public void process() {
            if (disabled) {
                return;
            }

            var systemsData = systems.getData();
            for (int i = 0, s = systems.getSize(); i < s; i++) {
                systemsData[i].process();
            }

            var groupsData = groups.getData();
            for (int i = 0, s = groups.getSize(); i < s; i++) {
                groupsData[i].process();
            }
        }

    }

    private final class Parallel extends AbstractSystemGroup {

        private final List<Callable<Void>> callables = new ArrayList<>();

        private Parallel(Object groupId, ExecutorService executor) {
            super(groupId, executor);
        }

        @Override
        protected void changed() {
            this.callables.clear();

            var systemsData = systems.getData();
            for (int i = 0, s = systems.getSize(); i < s; i++) {
                var system = systemsData[i];
                this.callables.add(system);
            }

            var groupsData = groups.getData();
            for (int i = 0, s = groups.getSize(); i < s; i++) {
                var group = groupsData[i];
                this.callables.add(group);
            }
        }

        @Override
        public void process() {
            if (disabled) {
                return;
            }

            try {
                var futures = new Future<?>[callables.size()];
                for (int i = 0, s = futures.length; i < s; i++) {
                    futures[i] = executor.submit(callables.get(i));
                }

                for (int i = 0, s = futures.length; i < s; i++) {
                    try {
                        futures[i].get();
                    } catch (ExecutionException ex) {
                        var target = i < systems.getSize() ? systems.get(i) : groups.get(i - systems.getSize());

                        throw new SystemInvocationException(id, target, ex.getCause());
                    }
                }
            } catch (InterruptedException ex) {
                throw new SystemInvocationException(id, null, ex);
            }
        }

    }

}
