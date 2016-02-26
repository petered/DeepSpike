package nl.uva.deepspike.event_modules;

import nl.uva.deepspike.events.SignedSpikeEvent;
import nl.uva.deepspike.interfaces.Quantizer;

/**
 * Created by peter on 2/24/16.
 */
public interface PassThroughQuantizer extends Quantizer {
    // TODO: Delete?

    void feed_spike(SignedSpikeEvent spike);  // Shortcut method

}
