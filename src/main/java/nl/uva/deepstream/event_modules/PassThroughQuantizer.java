package nl.uva.deepstream.event_modules;

import nl.uva.deepstream.events.SignedSpikeEvent;
import nl.uva.deepstream.interfaces.Quantizer;

/**
 * Created by peter on 2/24/16.
 */
public interface PassThroughQuantizer extends Quantizer {
    // TODO: Delete?

    void feed_spike(SignedSpikeEvent spike);  // Shortcut method

}
