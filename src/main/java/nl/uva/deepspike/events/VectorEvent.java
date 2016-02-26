package nl.uva.deepspike.events;

import nl.uva.arrays.IVector;

/**
 * Created by peter on 2/24/16.
 */
public class VectorEvent{

    final public IVector vec;

    public VectorEvent(IVector vec) {
        this(vec, false);
    }

    public VectorEvent(IVector vec, boolean safe){
        // Set safe to true if you're certain that the array won't be modified later and you want to save time/memory.
        this.vec = safe ? vec.dup() : vec;
    }

}
