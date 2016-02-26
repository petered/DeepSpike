package nl.uva.deepspike.event_modules;

import nl.uva.deepspike.events.SignedSpikeEvent;
import nl.uva.deepspike.events.SpikeEvent;
import nl.uva.deepspike.events.VectorEvent;
import nl.uva.deepspike.interfaces.EventModule;

/**
 * Created by peter on 2/24/16.
 */
public interface WeightGradientModule<EventType> extends EventModule<EventType> {

    void feed_error_event(SignedSpikeEvent event);

    void feed_error_event(WeightedSpikeEvent event);

    void feed_input_sum_event(VectorEvent event);

    void feed_input_event(SpikeEvent event);

    void feed_input_event(SignedSpikeEvent event);

}
