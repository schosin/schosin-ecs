#!/bin/bash
mvn install -pl codegen && rm -rf api/target/classes/de/schosin/ecs/api/Pooled.class && rm -rf engine/target/classes && mvn compile -pl api,engine -e

