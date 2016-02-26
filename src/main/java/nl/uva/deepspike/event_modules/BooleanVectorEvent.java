package nl.uva.deepspike.event_modules;

import nl.uva.arrays.BooleanVector;

/**
 * Created by peter on 2/24/16.
 */
public class BooleanVectorEvent{

    final public BooleanVector vec;

    public BooleanVectorEvent(BooleanVector vec) {
        this(vec, false);
    }

    public BooleanVectorEvent(BooleanVector vec, boolean safe){
        // TODO: Maybe remove this event type, or somehow assert boolean data without checking every element.
        this.vec = safe ? vec.dup() : vec;
    }
}
