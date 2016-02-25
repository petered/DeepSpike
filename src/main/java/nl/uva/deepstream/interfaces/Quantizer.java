package nl.uva.deepstream.interfaces;

import nl.uva.deepstream.events.SignedSpikeEvent;
import nl.uva.deepstream.events.VectorEvent;

/**
 * Created by peter on 2/24/16.
 */
public interface Quantizer extends EventModule<SignedSpikeEvent>{

    void feed_event(VectorEvent ev);

}
