package nl.uva.deepspike.interfaces;

import nl.uva.deepspike.events.SignedSpikeEvent;
import nl.uva.deepspike.events.VectorEvent;

/**
 * Created by peter on 2/24/16.
 */
public interface Quantizer extends EventModule<SignedSpikeEvent>{

    void feed_event(VectorEvent ev);

}
