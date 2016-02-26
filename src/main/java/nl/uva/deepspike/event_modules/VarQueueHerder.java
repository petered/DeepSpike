package nl.uva.deepspike.event_modules;

import nl.uva.deepspike.events.SignedSpikeEvent;

/**
 * Created by peter on 2/24/16.
 */
public class VarQueueHerder extends BaseQueueHerder{

    public VarQueueHerder(int n_units) {
        super(n_units);
    }

    @Override
    public void create_spikes_from_unit(int unit_ix) {
        float val = phi.getFloat(unit_ix);
        while(val > 0.5){  // Positive spikes, if any
            queue.add(new DispatchEvent(new SignedSpikeEvent(unit_ix, false), val));
            val -= 1;
        }
        while(val < -0.5){ // Negative spikes, if any
            queue.add(new DispatchEvent(new SignedSpikeEvent(unit_ix, true), -val));
            val += 1;
        }
        phi.putScalar(unit_ix, val);
    }
}
