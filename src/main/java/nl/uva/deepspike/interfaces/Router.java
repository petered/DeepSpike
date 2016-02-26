package nl.uva.deepspike.interfaces;

import java.util.function.Consumer;

/**
 * Created by peter on 2/24/16.
 */
public interface Router extends EventHandler, Resettable{

    <EventType> void add_binding(EventModule<EventType> src_module, Consumer<EventType> method_reference);

    <EventType> void handle_event(EventType ev, Consumer<EventType> consumer);

}
