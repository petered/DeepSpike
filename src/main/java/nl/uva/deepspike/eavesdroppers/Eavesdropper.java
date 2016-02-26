package nl.uva.deepspike.eavesdroppers;

import nl.uva.deepspike.event_modules.BaseEavesdropper;
import nl.uva.deepspike.events.VectorEvent;
import nl.uva.deepspike.interfaces.EventModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 2/24/16.
 */
public class Eavesdropper extends BaseEavesdropper {
    /* An event handler that you can sneak in between an EventModule and it's current handler.
    It will listen to events and pass them on.

    If you're going to make dirty hacks, might as well put them all in one class.
    This is that class.
    */

    List record;

    public Eavesdropper(){
        record = new ArrayList();
    }

    public List get_record(){
        return record;
    }

    @Override
    public void reset(){
        this.record.clear();
    }

    @Override
    public <EventType> void eat_event(EventType ev, EventModule<EventType> src_module) {
        if (ev instanceof VectorEvent) // HACK!!!  It's needed to make sure recorded events don't change when the weights they're referencing do.
            record.add(new VectorEvent(((VectorEvent)ev).vec, false));
        else
            record.add(ev);
    }
}
