package de.schosin.ecs.examples.simple.example6_singletons;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Supplier;

import de.schosin.ecs.api.World;

public class SingletonsExample {

    public static void main(String[] args) throws InterruptedException {
        singletons();
        singletonConstruction();
    }

    /*
     * Some frameworks introduce the concept of singletons, sometimes as "singleton components".
     * 
     * In this library, singletons are managed by the world. If you're familiar with Spring, think
     * of the work to act as a very limited ApplicationContext, allowing systems to receive singletons
     * from the world.
     * 
     * This allows you to instantiate systems just by passing in the world and receive all dependendies
     * through the singleton mechanism.
     */
    private static void singletons() throws InterruptedException {
        System.out.println();
        System.out.println("-- Singletons");

        /*
         * Some frameworks introduce the concept of singletons, sometimes as components themselves.
         * In this library, singletons are managed by the world. One way to provide singletons is
         * to pass them to the builder. 
         */
        World world = World.builder()
                .singletons(new PolygonSpriteBatch("shared batch for rendering"), new QuadTree("shared quadtree for collisions"))
                .build();

        /*
         * You can access singletons with World.getSingleton(Class). 
         * 
         * <p>Note:
         * This must be an exact type match for the time being. So no fetching a singleton by an interface just yet.
         */
        System.out.println("Singletons - %s".formatted(world.getSingleton(PolygonSpriteBatch.class)));
        System.out.println("Singletons - %s".formatted(world.getSingleton(QuadTree.class)));

        /*
         * Singletons can also be added afterwards. This is useful if you cannot provide the singleton
         * beforehand. Maybe a system provides some functionality. 
         */
        new SharedResourceManager(world);
        var manager = world.getSingleton(SharedResourceManager.class);

        manager.writeLock.lock();
        System.out.println("Locks (Main) - Got write lock");

        /*
         * Let's create a small "system" that maybe runs on another thread.
         * 
         * It wants to work on the resource managed by the singleton, so it
         * acquires the read lock, works with the resource, and then releases
         * it again.
         */
        new Thread(new Runnable() {
            private final SharedResourceManager threadManager = world.getSingleton(SharedResourceManager.class);

            @Override
            public void run() {
                try {
                    System.out.println("Locks (Thread) - Acquiring read lock ...");
                    threadManager.readLock.lock();

                    var connection = threadManager.dataSource.get();
                    System.out.println("Locks (Thread) - Got read lock, accessing connection: " + connection);
                } finally {
                    threadManager.readLock.unlock();
                }
            }
        }, "read-lock-example-thread").start();

        /*
         * Meanwhile the main thread still holds the write lock and takes a nap.
         * Afterwards it releases the write lock, which will allow the thread to continue.
         */
        System.out.println("Locks (Main) - Gonna release the write lock after a short nap");
        Thread.sleep(1000L);

        System.out.println("Locks (Main) - Okay, releasing now!");
        manager.writeLock.unlock();
    }

    private static void singletonConstruction() {
        System.out.println();
        System.out.println("-- Singleton construction");

        /*
         * Let's create a fresh world. 
         * 
         * Let's also print the identityHashCode and world itself. We'll get to that in a moment.
         */
        World world = World.builder().build();
        System.out.println("World2 - %d / %s".formatted(System.identityHashCode(world), world));

        /*
         * Unlike with the world in the previous method, we haven't provided a PolygonSpriteBatch singleton. 
         * 
         * When accessing a singleton that does not exist, the world will try to instantiate one using the
         * default constructor. 
         * 
         * If the implicit construction is used, that constructor has to be accessible.
         * That means both the class and constructor have to be public.
         */
        System.out.println("Singleton construction #1 - %s".formatted(world.getSingleton(PolygonSpriteBatch.class)));
        System.out.println("Singleton construction #2 - %s".formatted(world.getSingleton(PolygonSpriteBatch.class)));

        /*
         * Additionally the constructor of a singleton can also accept a World (or sub interface like DefaultWorld).
         * This allows to create singletons without having to explicitly add them to the world. They will be instantiated
         * on first access.
         * 
         * In this case the constructor will print the identity hash code and world instance. It should match the println
         * at the start of this method.
         */
        System.out.println("Singleton construction #3 - %s".formatted(world.getSingleton(WorldAccessingSingleton.class)));

        System.out.println();
        System.out.println("- Ordering #1");

        /*
         * When singletons are used to implement dependencies between system, you have will have
         * to ensure the correct order of instatiation.
         * 
         * In this example System1 will add itself as a singleton to the world and System2 will
         * retrieve it and accesses a field.
         */
        new System1(world);
        new System2(world);

        System.out.println();
        System.out.println("- Ordering #1");

        /*
         * Here we change the order in which the systems are created.
         * 
         * When System2 retrieves an instance of System1, the world will instantiate an instance.
         * Afterwards System1 is constructed, which tries to add itself to the world. 
         * This will cause an Exception to be thrown because there is already a singleton of that type 
         * present in the world:
         * 
         *     Exception in thread "main" java.lang.IllegalArgumentException: This world already contains a singleton of type class de.schosin.ecs.examples.simple.example6_singletons.SingletonsExample$System1
         * 
         * When using singletons in such a way, make sure to initialize the dependencies before they
         * are used. Luckily this will crash right at startup and tell you which singleton is created
         * twice, making it easy to fix.
         */
        try {
            var world2 = World.builder().build();

            new System2(world2);
            new System1(world2);
        } catch (Exception ex) {
            System.out.println("Singleton ordering - Incorrect order causes a singleton to be added twice");
            ex.printStackTrace();
        }
    }

    public static class SharedResourceManager {

        final ReadLock readLock;
        final WriteLock writeLock;

        final Supplier<String> dataSource;

        public SharedResourceManager(World world) {
            world.addSingleton(this);

            var lock = new ReentrantReadWriteLock();
            this.readLock = lock.readLock();
            this.writeLock = lock.writeLock();

            this.dataSource = () -> "shared connection";
        }

    }

    /*
     * Imagine this is the LibGDX class of the same name 
     */
    public record PolygonSpriteBatch(String name) {
        static final AtomicInteger counter = new AtomicInteger(0);

        public PolygonSpriteBatch() {
            this("this is the default sprite batch.");

            System.out.println("Singleton construction - PolygonSpriteBatch default constructor called %d times".formatted(counter.incrementAndGet()));
        }
    }

    /*
     * Maybe you are building a 2D game and use a QuadTree for the broadphase, but need it in
     * several systems.
     */
    record QuadTree(String name) {
    }

    public static class WorldAccessingSingleton {
        public WorldAccessingSingleton(World world) {
            System.out.println("Singleton construction - WorldAccessor default constructor called, World (%d): %s".formatted(System.identityHashCode(world), world));
        }
    }

    public static class System1 {

        final String data = "System 1 data";

        public System1(World world) {
            System.out.println("Singleton ordering - System1: Constructor called");

            world.addSingleton(this);
        }
    }

    static class System2 {

        public System2(World world) {
            System.out.println("Singleton ordering - System2: Constructor called");

            var system1 = world.getSingleton(System1.class);
            System.out.println("Singleton ordering - System2: " + system1.data);
        }

    }

}