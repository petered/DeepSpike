package nl.uva.deepstream.event_modules;

import nl.uva.arrays.IVector;
import nl.uva.arrays.VectorFactory;
import nl.uva.deepstream.events.VectorEvent;

/**
 * Created by peter on 2/24/16.
 */
public class EventGTFilterModule<EventType extends BaseSpikeEvent> extends BaseEventModule<EventType>{

    int n_srcs;
    IVector filters;

    public EventGTFilterModule(int n_srcs){
        this.n_srcs = n_srcs;
        reset();  // Filters now start at true;
    }

    public void feed_filter_event(VectorEvent ev){
        filters = ev.vec;
    }

    public void feed_event(EventType ev){
        if (!filters.elementLessThan(ev.src, 0))
            send_event(ev);
    }

    @Override
    public void reset(){
        filters= VectorFactory.create(n_srcs);
    }
}
