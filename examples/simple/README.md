## Examples

This module contains a bunch of examples explaining the functionality of the library.

These are in a sequential order and later examples build on top of the previous examples. \
They all contain a single class with a main method that can be executed. 
They are intended to be read from top to bottom unless otherwise stated.

### Annotation processing

> [!IMPORTANT]
> Some of the examples make use of annotation processing. When opening this project in an IDE, make sure
> it is correctly set up to use annotation processors. 

If it does not compile and complain about missing types, check with the documentation of your IDE
and make sure the dependency `de.schosin.ecs.buildtools:ecs-build-tools-codegen-apt` 
is used a an annotation processor.

### Additional features

> [!NOTE]
> These examples are not complete. There are a lot more features that don't have
> any examples yet. 
>
> To get a glimpse of what more this library offers, you can take a look at the
> [benchmarks](/benchmark) or [integration-tests](/integration-tests).
>
> While most tests are not easy to read, they also show additional features.
> Take a look at the simpler tests in the [archetype](/plugins/archetype) and
> [transmuter](/plugins/transmuter) plugins for example and focus on where `world`
> is used. \
> Same with the tests for the various component mappers in [core](/core).
> Just look for tests ending with [`MapperImplTest`](/core/src/test/java/de/schosin/ecs/engine/components/mappers).
> They will have examples for all the component types not yet covered by this module.
