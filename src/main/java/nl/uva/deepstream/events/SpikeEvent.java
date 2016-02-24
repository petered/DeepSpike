package nl.uva.deepstream.events;

/**
 * Created by peter on 2/24/16.
 */
public class SpikeEvent extends SignedSpikeEvent {
    /* This exists purely to avoid accidently treating SignedSpikeEvents as SpikeEvents */
    public SpikeEvent (int src){
        super(src, false);
    }
}
