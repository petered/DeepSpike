package nl.uva.deepspike.event_modules;

import nl.uva.arrays.IVector;
import nl.uva.arrays.VectorFactory;
import nl.uva.deepspike.events.SignedSpikeEvent;
import nl.uva.deepspike.events.SpikeEvent;
import nl.uva.deepspike.events.VectorEvent;

/**
 * Created by peter on 2/24/16.
 */
public class VectorSumModule extends BaseEventModule<VectorEvent>{

    final public IVector sum;
    boolean send_events;

    public VectorSumModule(int n_units){
        this(n_units, true);
    }

    VectorSumModule(int n_units, boolean send_events){

        this.sum = VectorFactory.create(n_units);
        this.send_events = send_events;
        reset();
    }

    public IVector get_sum(){
        return sum;
    }

    public void feed_event(VectorEvent ev){
        sum.addi(ev.vec);
        if (send_events)
            send_event(new VectorEvent(sum));
        // NOTE: Kinda dangerous because it exports the state vector.
        // Could copy it, but that can be wasteful.  Is there some way to lock it?
    }

    public void feed_event(SpikeEvent ev){
        sum.addiScalar(ev.src, +1);
    }

    public void feed_event(SignedSpikeEvent ev){
        sum.addiScalar(ev.src, ev.get_sign());
    }

    @Override
    public void reset(){
        sum.assign(0);
    }
}
