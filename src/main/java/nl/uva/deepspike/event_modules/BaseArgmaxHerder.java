package nl.uva.deepspike.event_modules;

import nl.uva.arrays.IVector;
import nl.uva.deepspike.events.VectorEvent;

/**
 * Created by peter on 2/24/16.
 */
public abstract class BaseArgmaxHerder<EventType> extends BaseHerder<EventType>{

    public BaseArgmaxHerder(int n_units) {
        super(n_units);
    }

    void add_vec(IVector vec){
        assert phi.length() == vec.length(): "phi length: "+phi.length()+" must match input length: "+vec.length();
        phi.addi(vec);
    }

    @Override
    public void feed_event(VectorEvent vec_event) {
        add_vec(vec_event.vec);
        herd_away();
    }

    abstract void herd_away();

}
