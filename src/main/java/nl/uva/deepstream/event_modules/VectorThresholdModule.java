package nl.uva.deepstream.event_modules;

import nl.uva.arrays.BooleanVector;
import nl.uva.deepstream.events.VectorEvent;

/**
 * Created by peter on 2/24/16.
 */
public class VectorThresholdModule extends StatelessBaseEventModule<BooleanVectorEvent>{

    public void feed_event(VectorEvent ev){

        BooleanVector exceeders = ev.vec.gt(0);
        send_event(new BooleanVectorEvent(exceeders, true));
    }
}
