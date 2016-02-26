package nl.uva.deepspike.event_modules;

import nl.uva.deepspike.events.SignedSpikeEvent;
import nl.uva.deepspike.events.SpikeEvent;
import nl.uva.deepspike.interfaces.Quantizer;

/**
 * Created by peter on 2/24/16.
 */
public class VarArgmaxHerder extends BaseArgmaxHerder<SignedSpikeEvent> implements Quantizer {

    public VarArgmaxHerder(int n_units) {
        super(n_units);
    }

    @Override
    void herd_away(){
        while (true) {
            int winner = phi.argMax();
            float val = phi.getFloat(winner);
            // Note: inefficiency here... better to just look once for threshold crossers.
            if (val > 0.5) {
                val -= 1;
                send_event(new SpikeEvent(winner));
            } else {
                break;
            }
            phi.putScalar(winner, val);
        }
    }
}
