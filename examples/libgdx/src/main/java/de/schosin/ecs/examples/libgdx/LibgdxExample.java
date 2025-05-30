package de.schosin.ecs.examples.libgdx;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class LibgdxExample {

    public static Lwjgl3Application create(ApplicationListener application) {
        var config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1280, 720);
        config.setResizable(false);
        config.setForegroundFPS(60);
        config.setIdleFPS(30);

        return new Lwjgl3Application(application, config);
    }

}
