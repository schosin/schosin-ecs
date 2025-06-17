package de.schosin.ecs.examples.simple.example4_componentSets;

import static de.schosin.ecs.api.components.types.ComponentType.component;

import java.util.Set;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.ComponentSetConfig;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.Relations.EntityRelations;
import de.schosin.ecs.api.components.mappers.ComponentSetMapper;
import de.schosin.ecs.api.components.types.ComponentSetType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.examples.simple.AbstractExample;
import de.schosin.ecs.examples.simple.components.Acceleration;
import de.schosin.ecs.examples.simple.components.Birth;
import de.schosin.ecs.examples.simple.components.FavoriteMedia;
import de.schosin.ecs.examples.simple.components.Location;
import de.schosin.ecs.examples.simple.components.Name;
import de.schosin.ecs.examples.simple.components.Parent;
import de.schosin.ecs.examples.simple.components.Position;
import de.schosin.ecs.examples.simple.components.Velocity;
import de.schosin.ecs.examples.simple.example1_physics.PhysicsExample;
import de.schosin.ecs.examples.simple.example3_regularComponentTypes.RegularComponentTypesExample;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.CompositionData2;
import de.schosin.ecs.plugins.composition.CompositionSet;

/**
 * <b>Note:</b> If this example does not compile, make sure that annotation processing is
 * correctly set up. Consult the documentation for your IDE. 
 * The annotation processor is located in "de.schosin.ecs.buildtools:ecs-build-tools-codegen-apt"
 * 
 * <p>
 * This example will introduce the first {@link ComponentType non-regular ComponentType}: {@link ComponentSetType}
 * 
 * <p>
 * {@link ComponentSetType} describes a {@link ComponentSet}, which has nothing to do with {@link Set the collection type},
 * but is a way to work with multiple different components at the same time.
 */
@SuppressWarnings("unused")
public class ComponentSetExample extends AbstractExample {

    public static void main(String[] args) {
        useFirstComponentSet();
        removeEntities();

        usePhysicsComponentSet();
    }

    /**
     * "A component set is defined by the method that processes it"
     * 
     * <p>
     * This will be our first* component set: {@link PhysicsComponentSet}
     * 
     * <p>
     * But let's ignore the actual type for now, and also the annotation on this method. 
     * Let's first look at the method itself. The method looks exactly like the "systems"
     * from the previous examples, the ones passed to compositions as method references. 
     * 
     * <p>
     * In fact, its signature is almost identical to 
     * {@link PhysicsExample#physicsSystem(int, Position, Velocity, Acceleration) physicsSystem from the previous one}.
     * 
     * It's only missing the {@link Acceleration} parater! Try it, just add an {@link Acceleration} parameter.
     * It might seem like not much is happening, but we will get to that later. 
     * 
     * You can leave the acceleration parameter if you want, or remove it. Up to you.
     */
    @ComponentSetConfig("PhysicsComponentSet")
    private static void physicsSystem(int entityId, Position position, Velocity velocity) {
        // skip this for now, it will be used at a later time

        if (position != null || velocity != null) {
            System.out.println("Method reference - Entity %d: %s / %s".formatted(entityId, position, velocity));
        }
    }

    /**
     * {@link FirstComponentSet} will actually be the first component set we will take a look at.
     * We'll get back to {@link PhysicsComponentSet} later.
     * 
     * <p>
     * It's basically the same as the previous one, just please don't add {@link Acceleration} here though. 
     * It will cause compilation errors.
     * 
     * <p>
     * Now let's take a look at {@link ComponentSetConfig @ComponentSetConfig("FirstComponentSet")}. 
     * This annotation can be added to a method and requires a name. 
     * That name is the reason why there is a type with that very same name. 
     * 
     * <p>
     * What is actually happening here is that an annotation processor analyzes the method and constructs the type
     * from the parameters of the method. There are a few gotchas for this to work though:
     * 
     * <ul>
     *  <li>The first parameter must be an {@code int} (and should be named entityId)</li>
     *  <li>Every following parameter must match the {@code R} of a valid {@link ComponentType ComponentType&lt;T, R&gt;}</li>
     * </ul>
     * 
     * The last point basically translates to "must be what mapper.get(entityId) returns", in this case our simple POJOs we've been
     * working with so far. 
     * 
     * Remember the relations from {@link RegularComponentTypesExample the previous example}? 
     * Those can also be used, like {@code ComponentRelations<Birth, Position>} and {@code EntityRelation<FavoriteMedia>}.
     * Again, don't add, compile errors. 
     * 
     * If you want, you can add them to {@link #physicsSystem(int, Position, Velocity, Acceleration)} though. 
     * Just handle null correctly if you use them.
     */
    @ComponentSetConfig("FirstComponentSet")
    private static void processSimpleComponents(int entityId, Position position, Velocity velocity) {
        // skip this for now, it will be used at a later time
        System.out.println("Method reference - Entity %d: %s / %s".formatted(entityId, position, velocity));
    }

    private static void useFirstComponentSet() {
        System.out.println();
        System.out.println("-- FirstComponentSet");

        // Let's create some entities. The only important thing:
        var entity1 = world.createEntity();
        var entity2 = world.createEntity(new Position(2, 2));
        var entity3 = world.createEntity(new Velocity(30, 30));
        var entity4 = world.createEntity(new Position(4, 4), new Velocity(40, 40));

        /*
         * The generated component set contains a ComponentType field TYPE that descibes the generated component set.
         * 
         * Along with the type for the set itself, it also carries a "Processor". This will come into play once we
         * get to compositions.
         */
        ComponentSetType<FirstComponentSet, FirstComponentSet.Processor> componentSetType = FirstComponentSet.TYPE;

        // When used as a component type, the set is assigned to both T (write) and R (read)
        ComponentType<FirstComponentSet, FirstComponentSet> componentType = componentSetType;

        // The mapper for a component set is of type ComponentSetMapper
        ComponentSetMapper<FirstComponentSet> mapper = world.getComponents(FirstComponentSet.TYPE);

        /*
         * Using mapper.get will return the component set for that entity.
         * 
         * Every component set will have a method entity() to return the entityId the components
         * within that set are assigned to. 
         * This might not make much sense yet, but we'll get to EntityRelationFetchType at
         * some point.
         * 
         * Additionally a getter is created for every parameter of the method. The parameter names 
         * will be used as is, so it almost feels like a record.
         * 
         * Fun fact: The first iteration of component sets was based off of records and their
         * record components. But this was scrapped once certain synergies were discovered.
         */
        FirstComponentSet component = mapper.get(entity4);
        System.out.println("Get: - " + component);
        System.out.println("  " + component.entityId());
        System.out.println("  " + component.position());
        System.out.println("  " + component.velocity());

        /*
         * When getting a component set for an entity, it will be non-null as long as the entity
         * has atleast one of the components of the set.
         */
        System.out.println("Get - Entity %d: %s".formatted(entity1, mapper.get(entity1)));
        System.out.println("Get - Entity %d: %s".formatted(entity2, mapper.get(entity2)));
        System.out.println("Get - Entity %d: %s".formatted(entity3, mapper.get(entity3)));
        System.out.println("Get - Entity %d: %s".formatted(entity4, mapper.get(entity4)));

        /*
         * When creating a composition and passing along only a single component set type after the 
         * builder, a CompositionSet will be returned.
         * 
         * Note that its type argument is not the set, but an inner type called Processor. 
         * This functional interface defines how the callbacks for process, inserted and removed work.
         */
        CompositionSet<FirstComponentSet.Processor> composition1 = world.createComposition(Composition.all(), FirstComponentSet.TYPE);

        /*
         * The signature will match the annotated method (processSimpleComponents).
         * 
         * Note that the builder matches all entities, so it will also process entity1, which does not
         * have any of the components.
         */
        composition1.process((entityId, position, velocity) -> {
            System.out.println("Composition1 - Entity %d: %s / %s".formatted(entityId, position, velocity));
        });

        /*
         * A component set type can not be used in Composition.Builder yet. It simply does not carry the necessary information.
         * The component set has all necessary information, but not in a form that is available in API without unsafe reflection.
         * 
         * Just don't use component set types for Composition.Builder. At best it would be a glorified Composition.one(...) anyway
         * to match the behaviour of ComponentSetMapper#get(int).
         * 
         * Note that it is planned to generate "FirstComponentSet.all()" and "FirstComponentSet.one()" though, which will do
         * what one expects: Return a CompositionBuilder that requires entities to have either all or one of the components described
         * by the component set.
         */
        CompositionSet<FirstComponentSet.Processor> composition2 = world.createComposition(Composition.all(componentSetType), FirstComponentSet.TYPE);

        composition2.process((entityId, position, velocity) -> {
            System.out.println("Composition2 - this will not be printed. unless this was actually implemented and this example was forgotten. please raise an issue if that is the case.");
        });

        /*
         * A component set is not limited to its special composition type. It can also be used together with other component types.
         * 
         * If used with CompositionData2 and up, you will instead access an instance of the component set. The "unpacking" is only available for the previous example.
         */
        CompositionData2<Position, FirstComponentSet> composition3 = world.createComposition(Composition.all(Position.class), component(Position.class), FirstComponentSet.TYPE);

        composition3.process((entityId, position, components) -> {
            System.out.println("CompositionData2 - Entity " + entityId);
            System.out.println("  " + position);
            System.out.println("  " + components);
        });
    }

    private static void usePhysicsComponentSet() {
        System.out.println();
        System.out.println("-- PhysicsComponentSet");

        // Let's create some entities. The only important thing:
        world.createEntity(new Position(2, 2));
        world.createEntity(new Velocity(30, 30));
        world.createEntity(new Position(4, 4), new Velocity(40, 40));
        world.createEntity(Relation.create(Location.Start, new Position(0, 0)), Relation.create(Location.End, new Position(100, 100)));
        world.createEntity(Relation.create(new Birth(1782), new Position(50_7, 8_41)));

        var mother = world.createEntity(new Name("Chi-Chi"));
        var father = world.createEntity(new Name("Goku"));
        world.createEntity(Relation.create(Parent.Mother, mother), Relation.create(Parent.Father, father));

        var oldbody = world.createEntity(new Name("Oldboy"));
        world.createEntity(Relation.create(new FavoriteMedia("Laugh and the world laughs with you. Weep and you weep alone."), oldbody));

        /*
         * Same as before, we create a composition by passing in the regenerated TYPE.
         */
        var composition = world.createComposition(Composition.all(), PhysicsComponentSet.TYPE);

        /*
         * "A component set is defined by the method that processes it"
         * 
         * Yeah, this is the reason. The method responsible for creating a component set can be used 
         * with the composition created for that component set.
         * 
         * If you haven't already, play around with the signature of that method. Remove a component, 
         * add a component, maybe a ComponentRelations<Location, Position> or EntityRelation<FavoriteMedia>.
         */
        composition.process(ComponentSetExample::physicsSystem);
    }

    /**
     * Or just move the annotation from {@link #physicsSystem(int, Position, Velocity)} to this method.
     * 
     * Make sure to move it, not copy it.
     */
    private static void physicsSystem(int entityId, Name name, Position position, Velocity velocity, Acceleration acceleration,
            ComponentRelations<Location, Position> locations,
            ComponentRelation<Birth, Position> birthplace,
            EntityRelations<Parent> parents,
            EntityRelation<FavoriteMedia> favoriteMedia) {

        System.out.println("Entity " + entityId);
        if (name != null) {
            System.out.println("  " + name.name);
        }
        if (position != null) {
            System.out.println("  " + position);
        }
        if (velocity != null) {
            System.out.println("  " + velocity);
        }
        if (acceleration != null) {
            System.out.println("  " + acceleration);
        }
        if (locations != null) {
            System.out.println("  " + locations);
        }
        if (birthplace != null) {
            System.out.println("  " + birthplace);
        }
        if (parents != null) {
            System.out.println("  " + parents);
        }
        if (favoriteMedia != null) {
            System.out.println("  " + favoriteMedia);
        }
    }

}
