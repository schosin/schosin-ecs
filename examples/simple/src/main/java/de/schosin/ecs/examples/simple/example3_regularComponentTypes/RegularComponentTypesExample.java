package de.schosin.ecs.examples.simple.example3_regularComponentTypes;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.Relations.EntityRelations;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers.ComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.Components.RegularComponents;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.EntityRelationMapper;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.ExclusiveEntityRelationMapper;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.examples.simple.AbstractExample;
import de.schosin.ecs.examples.simple.components.Birth;
import de.schosin.ecs.examples.simple.components.Birthplace;
import de.schosin.ecs.examples.simple.components.FavoriteMedia;
import de.schosin.ecs.examples.simple.components.FavoritePlushy;
import de.schosin.ecs.examples.simple.components.Location;
import de.schosin.ecs.examples.simple.components.Parent;
import de.schosin.ecs.examples.simple.components.Position;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.CompositionData1;
import de.schosin.ecs.plugins.composition.CompositionData2;
import de.schosin.ecs.worlds.DefaultWorld;

/**
 * Up until now we have only dealth with {@link Class Class&lt;?&gt;} when working with components.
 * This example will explain the concept of {@link ComponentType} and in particular
 * {@link RegularComponentType}.
 * 
 * <p>
 * When we used {@link DefaultWorld#createComposition(Composition.Builder, Class) world.createComposition(Composition.all(), Position.class)}
 * in the previous example (see composition "all") it is actually forwarded to
 * {@link DefaultWorld#createComposition(Composition.Builder, ComponentType)}.
 * 
 * <p>
 * A {@link ComponentType} describes the "shapes" of a component. This example will explain all {@link RegularComponentType RegularComponentTypes}
 * in detail. <b>Spoiler:</b> {@link Class Class&lt;Position&gt;} gets converted to {@link ClassType ClassType&lt;Position&gt;} 
 * 
 * <p>
 * <b>Notes:</b> 
 * 
 * <p>
 * This example makes use of block statements to create a scope
 * within a method. Do not use this pattern in your own code, it's just used
 * in examples so no new names for variables have to be invented. 
 * 
 * <p>
 * These examples are very explicit with their types, which can be harder to
 * read when actually using the library. In most cases it is advised to make use
 * of {@code var} when dealing with local variables to hide the longer type
 * signatures.
 */
@SuppressWarnings("unused")
public class RegularComponentTypesExample extends AbstractExample {

    /**
     * The library offers a number of component types that are used in the API to provide
     * type-safe access to the components.
     * 
     * <p>
     * While component types carry a lot of information through its type, a user will rarely
     * have to deal directly with crafting the correct type signature. Most common use cases
     * will just deal with {@link Class} directly or one of the factory methods provided by 
     * component type API.
     * 
     * <p>
     * The main type is {@link ComponentType}, which carries two type arguments. 
     * The first type argument {@code T} describes a single instance of that component
     * and is used mainly in writing operations in the API, e.g. when adding a component.
     * 
     * </p>
     * The second type argument {@code R} describes how the components are read and can
     * differ from the {@code T} when a component type describes components that can be
     * assigned multiple times, or it describes multiple components.
     * 
     * <p>
     * The following methods describe the {@link RegularComponentTypes} implemented in this
     * library. These describe the components that can be directly assigned to entities.
     */
    public static void main(String[] args) {
        ClassTypeExample.run();
        removeEntities();
        System.out.println();

        ComponentRelationExample.run();
        removeEntities();
        System.out.println();

        EntityRelationExample.run();
        removeEntities();
        System.out.println();
    }

    /**
     * The most common component type is {@link ClassType}. This component type describes a
     * simple POJO based component and can include regular java classes, records and enums.
     */
    static class ClassTypeExample {

        static void run() {
            System.out.println("-- ClassType examples");

            componentType();
            removeEntities();

            componentMapper();
            removeEntities();

            composition();
            removeEntities();

            compositionData();
            removeEntities();
        }

        static void componentType() {
            System.out.println("---- RegularComponent type");

            // The class type has only one type argument
            ClassType<Position> classType = ComponentType.component(Position.class);
            System.out.println(classType);

            // When used as a component type, the type argument is assigned to both T (write) and R (read)
            ComponentType<Position, Position> componentType = classType;

            // As it is a regular component type, it can also be assigned to that. The type parameters don't differ from ComponentType.
            RegularComponentType<Position, Position> regualarComponentType = classType;

            /*
             * When creating a new entity, the library will analyze its components and detect the correct
             * component type. For the simple POJO Position, ClassType<Position> will be detected and used.
             */
            world.createEntity(new Position(10, 20));
        }

        static void componentMapper() {
            System.out.println("---- RegularComponent mappers");

            ClassType<Position> classType = ComponentType.component(Position.class);
            ComponentType<Position, Position> componentType = classType;

            int entityId = world.createEntity(new Position(10, 20));

            /*
             * When retrieving the mapper for a ClassType, a ComponentMapper<T> will be returned.
             */
            ComponentMapper<Position> mapper = world.getComponents(classType);
            {
                Position position = mapper.get(entityId);
                System.out.println("Position - (%2d, %2d)".formatted(position.x, position.y));
            }

            /*
             * The base type for component mappers is Components, which also carries matching type
             * arguments T and R. Same as with ClassType and ComponentType, a ComponentMapper<Position>
             * maps to Components<Position, Position>
             */
            Components<Position, Position> components = mapper;
            {
                Position position = mapper.get(entityId);
                System.out.println("Position - (%2d, %2d)".formatted(position.x, position.y));
            }

            /*
             * There's also RegularComponents which match regular component types.
             * 
             * These also allow adding components to entities, which is not supported on Components.
             */
            RegularComponents<Position, Position> regularComponents = mapper;
            {
                int otherEntityId = world.createEntity();

                // Entity does not have the component, so it will be null
                System.out.println("Before add: %s".formatted(regularComponents.get(otherEntityId)));

                regularComponents.add(otherEntityId, new Position(10, 20));

                // The component will be available immediately, the "delay" from previous examples (almost) only affects callback methods of composition
                System.out.println("After add: %s".formatted(regularComponents.get(otherEntityId)));
            }

            /*
             * When only having access to a matching ComponentType of a ClassType, 
             * the returned mapper will match the signature and return the same instance.
             */
            components = world.getComponents(componentType);
        }

        static void composition() {
            System.out.println("---- Composition");

            ClassType<Position> classType = ComponentType.component(Position.class);
            ComponentMapper<Position> mapper = world.getComponents(classType);

            int entity1 = world.createEntity(new Position(10, 100));
            int entity2 = world.createEntity(new Position(20, 200));

            /*
             * When using a ClassType with Composition.all(...), it will match all entities that 
             * have that type of component.
             */
            Composition.Builder positioned = Composition.all(classType);

            /*
             * Creating a composition with that predicate will allow to process all entities matching
             * it.
             */
            Composition composition = world.createComposition(positioned);
            composition.process(entityId -> {
                // composition ensures that only entities with positions are passed, no null check required
                var pos = mapper.get(entityId);

                System.out.println("Process - Entity %d: (%2d, %2d)".formatted(entityId, pos.x, pos.y));
            });

            /*
             * For simple class based components, Composition supports passing the Class instances
             * as well.
             */
            Composition.Builder positionShortcut = Composition.all(Position.class);

            Composition composition2 = world.createComposition(positionShortcut);
            composition2.process(entityId -> {
                // composition ensures that only entities with positions are passed, no null check required
                var pos = mapper.get(entityId);

                System.out.println("Shortcut - Entity %d: (%2d, %2d)".formatted(entityId, pos.x, pos.y));
            });
        }

        static void compositionData() {
            System.out.println("---- CompositionData");

            ClassType<Position> classType = ComponentType.component(Position.class);
            ComponentMapper<Position> mapper = world.getComponents(classType);
            Composition.Builder positioned = Composition.all(classType);

            int entity1 = world.createEntity(new Position(10, 100));
            int entity2 = world.createEntity(new Position(20, 200));

            /**
             * When creating a composition, additional component types can be passed after the builder.
             * The methods themselves are generated and as of writing support up to 8 types.
             * 
             * <p>
             * <b>Note:</b> When working with larger numbers of components, it might be benefical to have a
             * look at ComponentSets. These are described in later examples.
             */
            CompositionData1<Position> composition = world.createComposition(positioned, classType);

            /**
             * Since CompositionDataN extends Composition, it can be used in the same way, which allows
             * passing it to function that may only work on Composition.
             */
            composition.process(entityId -> {
                // composition ensures that only entities with positions are passed, no null check required
                var pos = mapper.get(entityId);

                System.out.println("Process - Entity %d: (%2d, %2d)".formatted(entityId, pos.x, pos.y));
            });

            /**
             * When working with CompositionDataN, an additional overload of process, inserted and removed are
             * provided that match the type parameter R (read operations) of the passed component types.
             * 
             * <p>
             * <b>Note:</b> These components don't have to match the predicate (Composition.Builder) and can
             * include additional, optional components. If not ensured by Compositon.all(...), any other component
             * might be null though.
             */
            composition.process((entityId, pos) -> System.out.println("Lambda - Entity %d: (%2d, %2d)".formatted(entityId, pos.x, pos.y)));

            /**
             * Method references are a great way to simplify using compositions. 
             */
            composition.process(ClassTypeExample::processPositioned);
        }

        private static void processPositioned(int entityId, Position pos) {
            System.out.println("Method reference - Entity %d: (%2d, %2d)".formatted(entityId, pos.x, pos.y));
        }

    }

    /**
     * RegularComponent relations use ComponentRelationType. A component relation consists
     * of a relationship component and a target component.
     * 
     * <p>
     * The relationship component describes the type of relation the owning entity has with
     * the target component. An example of a relationship is (Birthplace, Position), where
     * Birthplace is an enum and Position a regular {@link ClassType} component.
     * 
     * <p>
     * The position in the example is separate from directly using Position through a {@link ClassType},
     * which allows for assigned multiple positions with different roles to entities.
     * 
     * <p>
     * RegularComponent relations themselves are separated into two types: non-exclusive and exclusive. <br/>
     * Non-exclusive component relations can be added to an entity multiple times as long as the targets
     * are not equal to an existing target. <br/>
     * Exclusive component relations can be added only once to an entity, replacing an existing relation
     * if another is added again.
     */
    static class ComponentRelationExample {

        static void run() {
            System.out.println("-- RegularComponent relation examples");

            nonExclusiveComponentType();
            removeEntities();

            exclusiveComponentType();
            removeEntities();

            componentMapper();
            removeEntities();

            exclusiveComponentMapper();
            removeEntities();

            compositionData();
            removeEntities();
        }

        static void nonExclusiveComponentType() {
            System.out.println("---- Non-exclusive component type");

            // For non-exclusive component relations, ComponentRelationType is used
            ComponentRelationType<Location, Position> relationType = ComponentType.relation(Location.class, Position.class);
            System.out.println(relationType);

            /*
             * When used as a component type, the type parameter T (write) will be assigned to a single instance,
             * whereas the type parameter R (read) will be assigned to an iterable type of component relations.
             */
            ComponentType<ComponentRelation<Location, Position>, ComponentRelations<Location, Position>> componentType = relationType;

            /*
             * When creating a new entity, the library will analyze its components and detect the correct
             * component type.
             */
            world.createEntity(
                    Relation.create(Location.Start, new Position(0, 0)),
                    Relation.create(Location.End, new Position(100, 100)));
        }

        static void exclusiveComponentType() {
            System.out.println("---- Exclusive component type");

            // For exclusive component relations, ExclusiveComponentRelationType is used
            ExclusiveComponentRelationType<Birthplace, Position> relationType = ComponentType.exclusiveRelation(Birthplace.class, Position.class);
            System.out.println(relationType);

            /*
             * When used as a component type, the both type parameters T (write) and R (read) will be assigned to a single instance.
             * This will allow accessing exclusive relations directly when reading components using mappers or compositions.
             */
            ComponentType<ComponentRelation<Birthplace, Position>, ComponentRelation<Birthplace, Position>> componentType = relationType;

            /*
             * When creating a new entity, the library will analyze its components and detect the correct
             * component type.
             */
            world.createEntity(Relation.create(Birthplace.Birthplace, new Position(42, 9001)));

            /**
             * Relationship components are not limited to enums, they can also carry data themselves.
             */
            world.createEntity(Relation.create(new Birth(2000), new Position(9001, 42)));
        }

        static void componentMapper() {
            System.out.println("---- Non-exclusive component mappers");

            ComponentRelationType<Location, Position> relationType = ComponentType.relation(Location.class, Position.class);
            ComponentType<ComponentRelation<Location, Position>, ComponentRelations<Location, Position>> componentType = relationType;

            int entityId = world.createEntity(
                    Relation.create(Location.Start, new Position(0, 0)),
                    Relation.create(Location.End, new Position(100, 100)));

            /*
             * When retrieving the mapper for a ComponentRelationType, a ComponentRelationMapper<T, R> will be returned.
             */
            ComponentRelationMapper<Location, Position> mapper = world.getComponents(relationType);
            {
                ComponentRelations<Location, Position> relations = mapper.get(entityId);
                for (ComponentRelation<Location, Position> relation : relations) {
                    Location location = relation.relationship();
                    Position position = relation.target();

                    System.out.println("%s: (%2d, %2d)".formatted(location, position.x, position.y));
                }
            }

            /*
             * The base type for component mappers is Components, which also carries matching type
             * arguments T and R.
             */
            Components<ComponentRelation<Location, Position>, ComponentRelations<Location, Position>> components = mapper;
            {
                ComponentRelations<Location, Position> relations = mapper.get(entityId);
                for (ComponentRelation<Location, Position> relation : relations) {
                    Location location = relation.relationship();
                    Position position = relation.target();

                    System.out.println("%s: (%2d, %2d)".formatted(location, position.x, position.y));
                }
            }

            /*
             * When only having access to a matching ComponentType of a relation type, 
             * the returned mapper will match the signature and return the same instance.
             */
            components = world.getComponents(componentType);

            /**
             * There is also a shortcut for accessing component relations by passing the
             * Class types of the relationship and target component.
             */
            components = world.getComponentRelations(Location.class, Position.class);
        }

        static void exclusiveComponentMapper() {
            System.out.println("---- Exclusive component mappers");

            ExclusiveComponentRelationType<Birthplace, Position> relationType = ComponentType.exclusiveRelation(Birthplace.class, Position.class);
            ComponentType<ComponentRelation<Birthplace, Position>, ComponentRelation<Birthplace, Position>> componentType = relationType;

            int entityId = world.createEntity(
                    Relation.create(Birthplace.Birthplace, new Position(42, 9001)),
                    Relation.create(new Birth(2000), new Position(9001, 42)));

            /*
             * When retrieving the mapper for a ExclusiveComponentRelationType, a ExclusiveComponentRelationMapper<T, R> will be returned.
             */
            ExclusiveComponentRelationMapper<Birthplace, Position> mapper = world.getComponents(relationType);
            {
                ComponentRelation<Birthplace, Position> relation = mapper.get(entityId);

                Birthplace birthplace = relation.relationship();
                Position position = relation.target();

                System.out.println("%s: (%2d, %2d)".formatted(birthplace, position.x, position.y));
            }

            /*
             * The base type for component mappers is Components, which also carries matching type
             * arguments T and R.
             */
            Components<ComponentRelation<Birthplace, Position>, ComponentRelation<Birthplace, Position>> components = mapper;
            {
                ComponentRelation<Birthplace, Position> relation = mapper.get(entityId);

                Birthplace birthplace = relation.relationship();
                Position position = relation.target();

                System.out.println("%s: (%2d, %2d)".formatted(birthplace, position.x, position.y));
            }

            /*
             * When only having access to a matching ComponentType of a relation type, 
             * the returned mapper will match the signature and return the same instance.
             */
            components = world.getComponents(relationType);

            /*
             * There is also a shortcut for accessing component relations by passing the
             * Class types of the relationship and target component.
             */
            ExclusiveComponentRelationMapper<Birth, Position> birthMapper = world.getExclusiveComponentRelations(Birth.class, Position.class);
            {
                ComponentRelation<Birth, Position> relation = birthMapper.get(entityId);

                Birth birth = relation.relationship();
                Position position = relation.target();

                System.out.println("Born year %d: (%2d, %2d)".formatted(birth.year, position.x, position.y));
            }
        }

        static void compositionData() {
            System.out.println("---- CompositionData");

            ComponentRelationType<Location, Position> locationType = ComponentType.relation(Location.class, Position.class);
            ExclusiveComponentRelationType<Birth, Position> birthType = ComponentType.exclusiveRelation(Birth.class, Position.class);

            int entity1 = world.createEntity(Relation.create(Location.Start, new Position(0, 0)), Relation.create(Location.End, new Position(100, 100)));
            int entity2 = world.createEntity(Relation.create(Location.Start, new Position(200, 200)));
            int entity3 = world.createEntity(Relation.create(Location.End, new Position(300, 300)));

            int entity4 = world.createEntity(Relation.create(new Birth(2000), new Position(9001, 42)));
            int entity5 = world.createEntity(Relation.create(new Birth(2005), new Position(42, 9001)));

            int entity6 = world.createEntity(Relation.create(Location.Start, new Position(1000, 1000)), Relation.create(new Birth(1782), new Position(50_7, 8_41)));

            /*
             * When using a relation types with Composition.all(...), it will match all entities that 
             * have that type of relation.
             */
            Composition.Builder locationBuilder = Composition.all(locationType);
            Composition.Builder birthBuilder = Composition.all(birthType);

            /*
             * When creating a composition and passing the relation type along, it is possible to
             * fetch the relations while processing entities.
             * 
             * <p>
             * Note how non-exclusive and exclusive relations have different type arguments. These map to
             * the ComponentType R, which is used for read operations like compositions.
             */
            CompositionData1<ComponentRelations<Location, Position>> locationComposition = world.createComposition(locationBuilder, locationType);
            CompositionData1<ComponentRelation<Birth, Position>> birthComposition = world.createComposition(birthBuilder, birthType);

            /*
             * Processing a composition with a non-exclusive relation will provide an iterable containg all relations. 
             */
            locationComposition.process((entityId, relations) -> {
                for (var relation : relations) {
                    Location location = relation.relationship();
                    Position position = relation.target();

                    System.out.println("Location - Entity %d, %s: (%2d, %2d)".formatted(entityId, location, position.x, position.y));
                }
            });

            /*
             * Processing a composition with an exclusive relation will provide the relation directly.
             */
            birthComposition.process((entityId, relation) -> {
                Birth birth = relation.relationship();
                Position position = relation.target();

                System.out.println("Birth - Entity %d born year %d: (%2d, %2d)".formatted(entityId, birth.year, position.x, position.y));
            });

            /*
             * Composition.Builder supports more than just matching entities that have all of a set of components.
             * Using Composition.one(...) we can match all entities that have atleast one of the passed component types.
             * 
             * EXCURSION:
             * 
             * These predicates can also be combined and even nested, providing a powerful query API:
             * 
             * Composition.one(Sprite.class, Shape.class).none(BoundingBox.class)
             * 
             * Such a composition could be used by a system that is responsible for calculating the bounding box
             * of entities based on either their 2d sprite (width, height) or a 2d shape (triangle, rectangle, polygon, ...).
             * By specifying "none(BoundingBox.class)" it will only match entities lacking a bounding box.
             *  
             * Such a system would use composition.inserted((entityId, sprite, shape) -> ...) instead of process, as every
             * entity would have to be processed only once.
             */
            Composition.Builder locationOrBirth = Composition.one(locationType, birthType);

            // this is where using var or component sets can become benefical
            CompositionData2<ComponentRelations<Location, Position>, ComponentRelation<Birth, Position>> composition = world.createComposition(locationOrBirth, locationType, birthType);

            /**
             * When processing a composition that only specifies "one", any component can be null and must be checked.
             * Only component types specified in "all" will never be null.
             */
            composition.process((entityId, locationRelations, birthRelation) -> {
                if (locationRelations != null) {
                    for (var relation : locationRelations) {
                        Location location = relation.relationship();
                        Position position = relation.target();

                        System.out.println("Location or birth - Entity %d, %s: (%2d, %2d)".formatted(entityId, location, position.x, position.y));
                    }
                }

                if (birthRelation != null) {
                    Birth birth = birthRelation.relationship();
                    Position position = birthRelation.target();

                    System.out.println("Location or birth - Entity %d born year %d: (%2d, %2d)".formatted(entityId, birth.year, position.x, position.y));
                }
            });
        }

    }

    /**
     * Entity relations use EntityRelationType. An entity relation consists
     * of a relationship component and a target entity. As such it only carries
     * type information of the relationship component.
     * 
     * <p>
     * The relationship component describes the type of relation the owning entity has with
     * the target entity. An example of a relationship is (Parent, int), where Parent is an enum.
     * 
     * <p>
     * Entity relations themselves are separated into two types: non-exclusive and exclusive. <br/>
     * Non-exclusive entity relations can be added to an entity multiple times as long as the targets
     * are not the same as for another relation. <br/>
     * Exclusive entity relations can be added only once to an entity, replacing an existing relation
     * if another is added again.
     */
    static class EntityRelationExample {

        static void run() {
            System.out.println("-- Entity relation examples");

            nonExclusiveComponentType();
            removeEntities();

            exclusiveComponentType();
            removeEntities();

            componentMapper();
            removeEntities();

            exclusiveComponentMapper();
            removeEntities();

            compositionData();
            removeEntities();
        }

        private static void nonExclusiveComponentType() {
            System.out.println("---- Non-exclusive entity type");

            // For non-exclusive entity relations, EntityRelationType is used
            EntityRelationType<Parent> relationType = ComponentType.relation(Parent.class);
            System.out.println(relationType);

            /*
             * When used as a component type, the type parameter T (write) will be assigned to a single instance,
             * whereas the type parameter R (read) will be assigned to an iterable type of entity relations.
             */
            ComponentType<EntityRelation<Parent>, EntityRelations<Parent>> componentType = relationType;

            /*
             * When creating a new entity, the library will analyze its components and detect the correct
             * component type.
             */
            var motherId = world.createEntity();
            var fatherId = world.createEntity();

            world.createEntity(
                    Relation.create(Parent.Mother, motherId),
                    Relation.create(Parent.Father, fatherId));
        }

        private static void exclusiveComponentType() {
            System.out.println("---- Exclusive entity type");

            // For exclusive entity relations, ExclusiveEntityRelationType is used
            ExclusiveEntityRelationType<FavoritePlushy> relationType = ComponentType.exclusiveRelation(FavoritePlushy.class);
            System.out.println(relationType);

            /*
             * When used as a component type, the both type parameters T (write) and R (read) will be assigned to a single instance.
             * This will allow accessing exclusive relations directly when reading components using mappers or compositions.
             */
            ComponentType<EntityRelation<FavoritePlushy>, EntityRelation<FavoritePlushy>> componentType = relationType;

            /*
             * When creating a new entity, the library will analyze its components and detect the correct
             * component type.
             */
            var bearId = world.createEntity();

            world.createEntity(Relation.create(FavoritePlushy.FavoritePlushy, bearId));

            /**
             * Relationship components are not limited to enums, they can also carry data themselves.
             */
            var dragonballId = world.createEntity();

            world.createEntity(Relation.create(new FavoriteMedia("It's over 9000!"), dragonballId));
        }

        static void componentMapper() {
            System.out.println("---- Non-exclusive component mappers");

            EntityRelationType<Parent> relationType = ComponentType.relation(Parent.class);
            ComponentType<EntityRelation<Parent>, EntityRelations<Parent>> componentType = relationType;

            var motherId = world.createEntity();
            var fatherId = world.createEntity();

            int entityId = world.createEntity(
                    Relation.create(Parent.Mother, motherId),
                    Relation.create(Parent.Father, fatherId));

            /*
             * When retrieving the mapper for a EntityRelationType, a EntityRelationMapper<T, R> will be returned.
             */
            EntityRelationMapper<Parent> mapper = world.getComponents(relationType);
            {
                EntityRelations<Parent> relations = mapper.get(entityId);
                for (EntityRelation<Parent> relation : relations) {
                    Parent parent = relation.relationship();
                    int parentId = relation.target();

                    System.out.println("%s: %d".formatted(parent, parentId));
                }
            }

            /*
             * The base type for component mappers is Components, which also carries matching type
             * arguments T and R.
             */
            Components<EntityRelation<Parent>, EntityRelations<Parent>> components = mapper;
            {
                EntityRelations<Parent> relations = mapper.get(entityId);
                for (EntityRelation<Parent> relation : relations) {
                    Parent parent = relation.relationship();
                    int parentId = relation.target();

                    System.out.println("%s: %d".formatted(parent, parentId));
                }
            }

            /*
             * When only having access to a matching ComponentType of a relation type, 
             * the returned mapper will match the signature and return the same instance.
             */
            components = world.getComponents(componentType);

            /**
             * There is also a shortcut for accessing component relations by passing the
             * Class types of the relationship and target component.
             */
            components = world.getEntityRelations(Parent.class);
        }

        static void exclusiveComponentMapper() {
            System.out.println("---- Exclusive component mappers");

            ExclusiveEntityRelationType<FavoritePlushy> relationType = ComponentType.exclusiveRelation(FavoritePlushy.class);
            ComponentType<EntityRelation<FavoritePlushy>, EntityRelation<FavoritePlushy>> componentType = relationType;

            var bearId = world.createEntity();
            var dragonballId = world.createEntity();

            int entityId = world.createEntity(
                    Relation.create(FavoritePlushy.FavoritePlushy, bearId),
                    Relation.create(new FavoriteMedia("It's over 9000!"), dragonballId));

            /*
             * When retrieving the mapper for a ExclusiveComponentRelationType, a ExclusiveComponentRelationMapper<T, R> will be returned.
             */
            ExclusiveEntityRelationMapper<FavoritePlushy> mapper = world.getComponents(relationType);
            {
                EntityRelation<FavoritePlushy> relation = mapper.get(entityId);

                FavoritePlushy plushy = relation.relationship();
                int plushyId = relation.target();

                System.out.println("%s: %d".formatted(plushy, plushyId));
            }

            /*
             * The base type for component mappers is Components, which also carries matching type
             * arguments T and R.
             */
            Components<EntityRelation<FavoritePlushy>, EntityRelation<FavoritePlushy>> components = mapper;
            {
                EntityRelation<FavoritePlushy> relation = mapper.get(entityId);

                FavoritePlushy plushy = relation.relationship();
                int plushyId = relation.target();

                System.out.println("%s: %d".formatted(plushy, plushyId));
            }

            /*
             * When only having access to a matching ComponentType of a relation type, 
             * the returned mapper will match the signature and return the same instance.
             */
            components = world.getComponents(relationType);

            /**
             * There is also a shortcut for accessing component relations by passing the
             * Class types of the relationship and target component.
             */
            ExclusiveEntityRelationMapper<FavoriteMedia> favoriteMediaMapper = world.getExclusiveEntityRelations(FavoriteMedia.class);
            {
                EntityRelation<FavoriteMedia> relation = favoriteMediaMapper.get(entityId);

                FavoriteMedia media = relation.relationship();
                int mediaId = relation.target();

                System.out.println("Favorite media %d: %s".formatted(mediaId, media.quote));
            }
        }

        static void compositionData() {
            System.out.println("---- CompositionData");

            EntityRelationType<Parent> parentType = ComponentType.relation(Parent.class);
            ExclusiveEntityRelationType<FavoriteMedia> favoriteMediaType = ComponentType.exclusiveRelation(FavoriteMedia.class);

            var mother1 = world.createEntity();
            var mother2 = world.createEntity();
            var father1 = world.createEntity();
            var father2 = world.createEntity();

            var dragonballId = world.createEntity();
            var oldbody = world.createEntity();

            int entity1 = world.createEntity(Relation.create(Parent.Mother, mother1), Relation.create(Parent.Father, father1));
            int entity2 = world.createEntity(Relation.create(Parent.Mother, mother2));

            var entity3 = world.createEntity(Relation.create(new FavoriteMedia("It's over 9000!"), dragonballId));

            var entity4 = world.createEntity(
                    Relation.create(Parent.Mother, mother1), Relation.create(Parent.Father, father2),
                    Relation.create(new FavoriteMedia("Laugh and the world laughs with you. Weep and you weep alone."), oldbody));

            /**
             * Create a composition matching all entities having a parent or a favorite media
             */
            Composition.Builder parentOrMedia = Composition.one(parentType, favoriteMediaType);
            CompositionData2<EntityRelations<Parent>, EntityRelation<FavoriteMedia>> composition = world.createComposition(parentOrMedia, parentType, favoriteMediaType);

            /**
             * Process all entities with parents or favorite media
             */
            composition.process((entityId, parents, favoriteMedia) -> {
                if (parents != null) {
                    for (var parent : parents) {
                        Parent type = parent.relationship();
                        int parentId = parent.target();

                        System.out.println("Entity %d's %s is entity %d".formatted(entityId, type, parentId));
                    }
                }

                if (favoriteMedia != null) {
                    FavoriteMedia media = favoriteMedia.relationship();
                    int mediaId = favoriteMedia.target();

                    System.out.println("Entity %d's favorite media is %d: %s".formatted(entityId, mediaId, media.quote));
                }
            });
        }

    }

}
