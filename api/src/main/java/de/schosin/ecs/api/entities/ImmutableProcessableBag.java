package de.schosin.ecs.api.entities;

import de.schosin.ecs.api.data.DataProcessor;
import de.schosin.ecs.api.data.Processable;

public interface ImmutableProcessableBag<P extends DataProcessor<?>> extends ImmutableEntityBag, Processable<P> {
}
