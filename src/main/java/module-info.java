module io.maestro.job.engine {
    // JSON
    requires transitive com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.joda;
    requires com.fasterxml.jackson.datatype.jsr310;

    // Logs
    requires org.slf4j;

    requires org.apache.commons.lang3;

    exports io.maestro3.job.engine;
    exports io.maestro3.job.engine.exception;
    exports io.maestro3.job.engine.model;
    exports io.maestro3.job.engine.tracker;
}