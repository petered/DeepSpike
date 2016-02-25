package nl.uva.deepstream.event_modules;

import nl.uva.arrays.IVector;
import nl.uva.arrays.VectorFactory;
import nl.uva.deepstream.events.VectorEvent;

/**
 * Created by peter on 2/24/16.
 */
public abstract class BaseHerder<EventType> extends BaseEventModule<EventType>{

    IVector phi;

    BaseHerder(int n_units) {

        this.phi = VectorFactory.create(n_units);
//        System.out.println("Created phi of type " + this.phi.dtype().toString());
    }

    public abstract void feed_event(VectorEvent ev);

    @Override
    public void reset() {
        this.phi.assign(0);
    }
}
