package nl.uva.deepspike.event_modules;

import nl.uva.arrays.IMatrix;
import nl.uva.arrays.IVector;
import nl.uva.deepspike.events.SignedSpikeEvent;
import nl.uva.deepspike.events.VectorEvent;

/**
 * Created by peter on 2/24/16.
 */
public class ReverseWeightModule extends StatelessBaseEventModule<VectorEvent>{

    int n_left;
    int n_right;
    IMatrix w;

    public ReverseWeightModule(IMatrix w){
        this.w = w;
        n_left = w.shape()[0];
        n_right = w.shape()[1];
    }

    public void feed_event(SignedSpikeEvent spike) {

        IVector weight = spike.is_negative? w.getColumn(spike.src).neg() : w.getColumn(spike.src);
        send_event(new VectorEvent(weight, true));
    }
}
