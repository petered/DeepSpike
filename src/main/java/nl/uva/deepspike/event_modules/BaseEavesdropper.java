package nl.uva.deepspike.event_modules;

import nl.uva.deepspike.interfaces.EventHandler;
import nl.uva.deepspike.interfaces.EventModule;

/**
 * Created by peter on 2/24/16.
 */
public abstract class BaseEavesdropper implements EventHandler {

    EventHandler current_handler;
    boolean enabled;

    public BaseEavesdropper(){
        this.enabled = true;
    }

    @Override
    public <EventType> void handle_event(EventType ev, EventModule<EventType> src_module) {

        if (enabled){
            eat_event(ev, src_module);
        }
        if (current_handler != null)
            current_handler.handle_event(ev, src_module);
    }

    public abstract <EventType> void eat_event(EventType ev, EventModule<EventType> src_module);

    public void set_enabled(boolean enabled){
        this.enabled = enabled;
    }

    public abstract void reset();

    public void set_handler(EventHandler handler){
        current_handler = handler;
    }
}
