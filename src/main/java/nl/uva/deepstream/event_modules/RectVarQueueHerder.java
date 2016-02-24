package nl.uva.deepstream.event_modules;

import nl.uva.deepstream.events.SpikeEvent;
import nl.uva.deepstream.interfaces.Quantizer;

/**
 * Created by peter on 2/24/16.
 */
public class RectVarQueueHerder extends BaseQueueHerder implements Quantizer {

    public RectVarQueueHerder(int n_units) {
        super(n_units);
    }

    @Override
    void create_spikes_from_unit(int unit_ix) {
        while(phi.getFloat(unit_ix) > 0.5){
            queue.add(new DispatchEvent(new SpikeEvent(unit_ix), phi.getFloat(unit_ix)));
            phi.addiScalar(unit_ix, -1);
        }
    }

}
