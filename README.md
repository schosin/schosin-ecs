## Unnamed ECS

This is a work-in-progress, not yet named ECS framework based on Java 21 providing an extensible API and a rich feature set.

It is inspired by other ECS frameworks such as [Artemis-odb](https://github.com/junkdog/artemis-odb)
and [Dominion](https://github.com/dominion-dev/dominion-ecs-java), as well as by ECS frameworks outside
of the java ecosystem like [Flecs](https://github.com/SanderMertens/flecs).

### Work-in-progress

> [!CAUTION]
> This framework is in active development. Expect breaking changes!

### Feature highlights

- Small core API for working with entities and components
- Extendable API using an interface based plugins
- Multiple types of components, including POJOs, relations and component sets
- Powerful query API for matching entities and fetching components
- Performance optimizations to reduce heap allocations and GC pressure

### Getting started

The framework is still work-in-progress and as such no artifacts are published yet. To start with the framework,
clone this repository and build the library before using it.

See the [examples module](examples/) on how the framework can be used.

#### Build the library

```bash
mvn install
```

#### Maven

```xml
<dependency>
    <groupId>de.schosin.ecs</groupId>
    <artifactId>ecs-worlds</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

#### Gradle

```groovy
  dependencies { compile "de.schosin.ecs:ecs-worlds:0.1.0-SNAPSHOT" }
```

### Resources

- [Examples](examples/)
