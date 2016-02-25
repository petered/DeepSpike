package nl.uva.deepstream.event_modules;

import nl.uva.deepstream.events.SpikeEvent;
import nl.uva.deepstream.interfaces.Quantizer;

/**
 * Created by peter on 2/24/16.
 */
public class ScaledRectRoundingHerder extends BaseQueueHerder implements Quantizer {

    final float scale;
    final float scale_over_2;

    public ScaledRectRoundingHerder(int n_units, float scale) {
        super(n_units);
        this.scale=scale;
        this.scale_over_2 = scale/2;

    }

    @Override
    void create_spikes_from_unit(int unit_ix) {
//        System.out.println("phi: "+phi.getFloat(unit_ix)+", thresh: "+this.scale_over_2);
        while(phi.getFloat(unit_ix) > this.scale_over_2){
            queue.add(new DispatchEvent(new SpikeEvent(unit_ix), phi.getFloat(unit_ix)));
            phi.addiScalar(unit_ix, -this.scale);
        }
    }
}
