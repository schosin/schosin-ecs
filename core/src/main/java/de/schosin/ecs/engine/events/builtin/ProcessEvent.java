package de.schosin.ecs.engine.events.builtin;

public sealed interface ProcessEvent extends Event {

    Process PROCESS = Process.PROCESS;
    ProcessStep PROCESS_STEP = ProcessStep.PROCESS_STEP;

    enum Process implements ProcessEvent {
        PROCESS;
    }

    enum ProcessStep implements ProcessEvent {
        PROCESS_STEP;
    }

}
