package nl.uva.deepspike.eavesdroppers;

import nl.uva.deepspike.event_modules.BaseEavesdropper;
import nl.uva.deepspike.interfaces.EventModule;

/**
 * Created by peter on 2/24/16.
 */
public class SpikeCountingEavesdropper extends BaseEavesdropper {

    int count=0;

    public SpikeCountingEavesdropper() {
        super();
    }

    @Override
    public <EventType> void eat_event(EventType ev, EventModule<EventType> src_module) {
        count += 1;
    }

    @Override
    public void reset(){
        count = 0;
    }

    public int get_count(){
        return count;

    }
}
