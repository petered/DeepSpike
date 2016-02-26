package nl.uva.deepspike.event_modules;

import nl.uva.arrays.IVector;
import nl.uva.deepspike.events.SignedSpikeEvent;

/**
 * Created by peter on 2/24/16.
 */
public class BiVarArgmaxHerder extends BaseArgmaxHerder<SignedSpikeEvent> implements PassThroughQuantizer {

    public BiVarArgmaxHerder(int n_units) {
        super(n_units);
    }

    @Override
    void herd_away(){
        // Note: inefficiency here... better to just look once for threshold crossers.
        while (true) {
            IVector absphi = phi.abs();
            int winner = absphi.argMax();
            float val = phi.getFloat(winner);
            if (Math.abs(val) > 0.5) {
                boolean is_negative = val < 0;
                val -= is_negative ? -1 : 1;
                send_event(new SignedSpikeEvent(winner, is_negative));
            } else {
                break;
            }
            phi.putScalar(winner, val);
        }
    }

    @Override
    public void feed_spike(SignedSpikeEvent spike){
        send_event(spike);
    }
}
