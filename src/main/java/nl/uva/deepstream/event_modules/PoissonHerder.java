package nl.uva.deepstream.event_modules;

import nl.uva.arrays.IVector;
import nl.uva.arrays.VectorFactory;
import nl.uva.deepstream.events.SignedSpikeEvent;

import java.util.Random;

/**
 * Created by peter on 2/24/16.
 */
public class PoissonHerder extends BaseQueueHerder{

    Random rng;
    IVector thresholds;
    final boolean zero_mode;

    public PoissonHerder(int n_units, boolean zero_mode, Random rng) {
        super(n_units);
        this.rng = rng;
        this.zero_mode = zero_mode;
        thresholds = VectorFactory.create(n_units);

        reset();
    }

    @Override
    public void create_spikes_from_unit(int unit_ix) {
        // Note that this method can create opposite spikes from a single unit,
        // in a single call, which is kind of strange.  But it seems to only
        // about 15% extra events.
        float val = phi.getFloat(unit_ix);
        while (true){
            SignedSpikeEvent ev;
            float thresh = thresholds.getFloat(unit_ix);

            if (val > thresh)
                ev = new SignedSpikeEvent(unit_ix, false);
            else if (val <= -thresh)
                ev = new SignedSpikeEvent(unit_ix, true);
            else
                break;
            queue.add(new DispatchEvent(ev, Math.abs(val)));
            if (zero_mode)
                val = 0;
            else
                val -= ev.get_sign();

            thresholds.putScalar(unit_ix, next_random_threshold());
        }
        phi.putScalar(unit_ix, val);
    }

    public float next_random_threshold(){
        return -(float)Math.log(1-rng.nextDouble());  // Divide by lambda (but that's just 1)
    }

    @Override
    public void reset(){
        super.reset();

        for (int i=0; i<thresholds.length(); i++)
            thresholds.putScalar(i, next_random_threshold());
    }
}
