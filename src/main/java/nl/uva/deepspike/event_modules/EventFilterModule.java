package nl.uva.deepspike.event_modules;

import nl.uva.arrays.BooleanVector;

/**
 * Created by peter on 2/24/16.
 */
public class EventFilterModule<EventType extends BaseSpikeEvent> extends BaseEventModule<EventType>{

    int n_srcs;
    BooleanVector filters;

    EventFilterModule(int n_srcs){
        this.n_srcs = n_srcs;
        this.filters = new BooleanVector(n_srcs);
        reset();  // Filters now start at true;
    }

    public void feed_filter_event(BooleanVectorEvent ev){
        filters.assign(ev.vec);
    }

    public void feed_event(EventType ev){

        if (filters.getBoolean(ev.src))
            send_event(ev);
    }

    @Override
    public void reset(){
        filters.assign(true);
    }
}
