package nl.uva.deepspike.events;

import nl.uva.deepspike.event_modules.BaseSpikeEvent;

/**
 * Created by peter on 2/24/16.
 */
public class SignedSpikeEvent extends BaseSpikeEvent {

    public final boolean is_negative;

    public SignedSpikeEvent(int src, boolean is_negative) {
        super(src);
        this.is_negative = is_negative;
//        System.out.println("Spike Produced: addr: " + src);
    }

    public int get_sign(){
        return is_negative ? -1 : 1;
    }
}
