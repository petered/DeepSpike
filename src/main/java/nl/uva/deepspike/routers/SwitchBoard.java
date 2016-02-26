package nl.uva.deepspike.routers;

import nl.uva.deepspike.interfaces.EventHandler;
import nl.uva.deepspike.interfaces.EventModule;

/**
 * Created by peter on 2/24/16.
 */
public class SwitchBoard implements EventHandler {

    public static final SwitchBoard global_switchboard = new SwitchBoard();
    EventHandler current_handler;

    public void set_handler(EventHandler handler){
        current_handler = handler;
    }

    @Override
    public <EventType> void handle_event(EventType ev, EventModule<EventType> src_module) {
        current_handler.handle_event(ev, src_module);
    }
}
