package de.schosin.ecs.plugins.experimental.system;

import java.util.function.UnaryOperator;

import de.schosin.ecs.plugins.experimental.system.systems.BaseSystem;

public sealed interface SystemGroup extends BaseSystem permits SystemManager.AbstractSystemGroup {

    SystemGroup enable();

    SystemGroup disable();

    SystemGroup add(Class<?>... systems);

    SystemGroup add(BaseSystem... systems);

    SystemGroup addParallel(Class<?>... systems);

    SystemGroup addParallel(BaseSystem... systems);

    SystemGroup add(Object groupId, UnaryOperator<SystemGroup> group);

    SystemGroup addParallel(Object groupId, UnaryOperator<SystemGroup> group);

}

class Test {

    public static void main(String[] args) {
        var systems = SystemPlugin.standalone(null);

        systems.addSystemGroup("main", group -> group
                .add(Sys1.INSTANCE, Sys2.INSTANCE)
                .add(Sys3.class, Sys3.class)
                .addParallel(Systems.Logic, stuff -> stuff
                        .add(Sys1.INSTANCE, Sys2.INSTANCE)
                        .add(Systems.Logic, sequential -> sequential
                                .add(Sys1.INSTANCE, Sys2.INSTANCE)))
                .add(Systems.Logic, render -> render
                        .add(Sys1.INSTANCE, Sys2.INSTANCE)));
    }

    enum Systems {
        Logic, Render, DebugRender
    }

    enum Sys1 implements BaseSystem {
        INSTANCE;

        @Override
        public void process() {
        }
    }

    enum Sys2 implements BaseSystem {
        INSTANCE;

        @Override
        public void process() {
        }
    }

    public class Sys3 implements BaseSystem {

        @Override
        public void process() {
        }
    }

}