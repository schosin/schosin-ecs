package de.schosin.ecs.buildtools.codegen.it;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.ComponentSetConfig;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.Relations.EntityRelations;

public class MovieSystem {

    private int entityId;

    private ComponentRelation<Favorite, Movie> favoriteMovie;
    private ComponentRelation<Rating, Movie> ratedMovie1;
    private ComponentRelation<Rating, Movie> ratedMovie2;

    private EntityRelation<Favorite> favoriteActor;
    private EntityRelation<Rating> ratedActor1;
    private EntityRelation<Rating> ratedActor2;

    public MovieSystem(World world) {
        var mapper = world.getComponents(MovieComponents.TYPE);

        this.favoriteMovie = Relation.create(Favorite.INSTANCE, new Movie("Oldboy"));
        this.ratedMovie1 = Relation.create(new Rating("Overrated"), new Movie("Pulp Fiction"));
        this.ratedMovie2 = Relation.create(new Rating("Underrated"), new Movie("Nightcrawler"));

        this.favoriteActor = Relation.create(Favorite.INSTANCE, world.createEntity());
        this.ratedActor1 = Relation.create(new Rating("2/10"), world.createEntity());
        this.ratedActor2 = Relation.create(new Rating("10/10"), world.createEntity());

        this.entityId = world.createEntity(
                favoriteMovie,
                ratedMovie1, ratedMovie2,
                favoriteActor,
                ratedActor1, ratedActor2);

        var components = mapper.get(entityId);
        if (components == null) {
            throw new IllegalStateException("Expected non-null component set");
        }

        MovieComponents.Processor processor = this::processEntities;
        processor.process(entityId, components);
    }

    @ComponentSetConfig("MovieComponents")
    private void processEntities(int entityId,
            ComponentRelation<Favorite, Movie> favoriteMovie,
            ComponentRelations<Rating, Movie> ratedMovies,
            EntityRelation<Favorite> favoriteActor,
            EntityRelations<Rating> ratedActors) {

        if (this.entityId != entityId) {
            throw new IllegalStateException("Expected entity %d, but got %d".formatted(this.entityId, entityId));
        }

        // identity check
        if (this.favoriteMovie != favoriteMovie) {
            throw new IllegalStateException("Expected favorite movie %s (%s), but got %s (%s)"
                    .formatted(this.favoriteMovie, System.identityHashCode(this.favoriteMovie), favoriteMovie, System.identityHashCode(favoriteMovie)));
        }

        // identity check
        var ratedMovie1 = ratedMovies.get(0);
        if (this.ratedMovie1 != ratedMovie1) {
            throw new IllegalStateException("Expected first rated movie %s (%s), but got %s (%s)"
                    .formatted(this.ratedMovie1, System.identityHashCode(this.ratedMovie1), ratedMovie1, System.identityHashCode(ratedMovie1)));
        }

        // identity check
        var ratedMovie2 = ratedMovies.get(1);
        if (this.ratedMovie2 != ratedMovie2) {
            throw new IllegalStateException("Expected second rated movie %s (%s), but got %s (%s)"
                    .formatted(this.ratedMovie2, System.identityHashCode(this.ratedMovie2), ratedMovie2, System.identityHashCode(ratedMovie2)));
        }

        // identity check
        if (this.favoriteActor != favoriteActor) {
            throw new IllegalStateException("Expected favorite actor %s (%s), but got %s (%s)"
                    .formatted(this.favoriteActor, System.identityHashCode(this.favoriteActor), favoriteActor, System.identityHashCode(favoriteActor)));
        }

        // identity check
        var ratedActor1 = ratedActors.get(0);
        if (this.ratedActor1 != ratedActor1) {
            throw new IllegalStateException("Expected first rated actor %s (%s), but got %s (%s)"
                    .formatted(this.ratedActor1, System.identityHashCode(this.ratedActor1), ratedActor1, System.identityHashCode(ratedActor1)));
        }

        // identity check
        var ratedActor2 = ratedActors.get(1);
        if (this.ratedActor2 != ratedActor2) {
            throw new IllegalStateException("Expected second rated actor %s (%s), but got %s (%s)"
                    .formatted(this.ratedActor2, System.identityHashCode(this.ratedActor2), ratedActor2, System.identityHashCode(ratedActor2)));
        }
    }

    public record Movie(String name) {
    }

    public record Rating(String rating) {
    }

    public enum Favorite implements Exclusive {
        INSTANCE
    }

}
