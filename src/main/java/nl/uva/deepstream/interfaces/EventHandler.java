package nl.uva.deepstream.interfaces;

/**
 * Created by peter on 2/24/16.
 */
@FunctionalInterface
public interface EventHandler{

    <EventType> void handle_event(EventType ev, EventModule<EventType> src_module);

}
