package nl.uva.deepstream.event_modules;

import nl.uva.deepstream.events.SignedSpikeEvent;
import nl.uva.deepstream.events.SpikeEvent;
import nl.uva.deepstream.events.VectorEvent;
import nl.uva.deepstream.interfaces.EventModule;

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
