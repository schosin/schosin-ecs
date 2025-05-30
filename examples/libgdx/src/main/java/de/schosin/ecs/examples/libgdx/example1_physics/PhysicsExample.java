package de.schosin.ecs.examples.libgdx.example1_physics;

import java.util.Random;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;

import de.schosin.ecs.examples.libgdx.LibgdxExample;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.CompositionData2;
import de.schosin.ecs.plugins.composition.CompositionData3;
import de.schosin.ecs.worlds.DefaultWorld;

public class PhysicsExample extends ApplicationAdapter {
    static final Random rng = new Random();
    static final int SIZE = 20;
    static final int TARGET = 50;

    // Create default world instance
    final DefaultWorld world = DefaultWorld.create();

    // Create a composition to query entities
    final CompositionData3<Position, Velocity, Acceleration> physics = world.createComposition(Composition.all(Position.class, Velocity.class), Position.class, Velocity.class, Acceleration.class);
    final CompositionData2<Position, Display> render = world.createComposition(Composition.all(Position.class, Display.class), Position.class, Display.class);

    // ShapeRenderer for visualizing entities
    ShapeRenderer shapeRenderer;
    int centerX;
    int centerY;

    public static void main(String[] args) throws InterruptedException {
        LibgdxExample.create(new PhysicsExample());
    }

    @Override
    public void create() {
        this.shapeRenderer = new ShapeRenderer();
        this.centerX = Gdx.graphics.getWidth() / 2;
        this.centerY = Gdx.graphics.getHeight() / 2;

        // Create a few entities
        for (int i = 0; i < 10; i++) {
            createEntity();
        }
    }

    private int createEntity() {
        var position = new Position(centerX + rng.nextInt(-20, 20), centerY + rng.nextInt(-20, 20));

        var angle = rng.nextFloat(MathUtils.PI2);
        var velocity = new Velocity(50f * MathUtils.sin(angle), 50 * MathUtils.cos(angle));

        var display = new Display(new Color(rng.nextFloat(1), rng.nextFloat(1), rng.nextFloat(1), 1f));

        if (rng.nextInt(2) == 0) {
            return world.createEntity(position, velocity, display);
        } else {
            var acceleration = new Acceleration(rng.nextInt(-20, 20), rng.nextInt(-20, 20));

            return world.createEntity(position, velocity, display, acceleration);
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapeRenderer.begin(ShapeType.Filled);

        physics.process(this::physicsSystem);
        render.process(this::renderSystem);
        render.process(this::deleteOutOfBounds);
        spawnEntities();

        shapeRenderer.end();

        world.process();
    }

    void physicsSystem(int entityId, Position position, Velocity velocity, Acceleration acceleration) {
        var delta = Gdx.graphics.getDeltaTime();

        if (acceleration != null) {
            velocity.vx += delta * acceleration.ax;
            velocity.vy += delta * acceleration.ay;
        }

        position.x += delta * velocity.vx;
        position.y += delta * velocity.vy;
    }

    void renderSystem(int entityId, Position position, Display display) {
        shapeRenderer.setColor(display.color);
        shapeRenderer.circle(position.x, position.y, SIZE);
    }

    private void deleteOutOfBounds(int entityId, Position position, Display display) {
        // Remove entities that are out of bounds
        if (position.x < -SIZE || position.x > 2 * centerX + SIZE || position.y < -SIZE || position.y > 2 * centerY + SIZE) {
            world.deleteEntity(entityId);
        }
    }

    private void spawnEntities() {
        // Spawn missing entities
        var missing = TARGET - render.getCount();
        if (missing == 0) {
            return;
        }

        for (int i = 0; i < missing; i++) {
            createEntity();
        }
    }

}

class Display {

    Color color;

    public Display(Color color) {
        this.color = color;
    }

}

class Position {

    float x, y;

    public Position(float x, float y) {
        this.x = x;
        this.y = y;
    }

}

class Velocity {

    float vx, vy;

    public Velocity(float vx, float vy) {
        this.vx = vx;
        this.vy = vy;
    }

}

class Acceleration {

    float ax, ay;

    public Acceleration(float ax, float ay) {
        this.ax = ax;
        this.ay = ay;
    }

}
