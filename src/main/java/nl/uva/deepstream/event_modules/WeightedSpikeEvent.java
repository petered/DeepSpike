package nl.uva.deepstream.event_modules;

/**
 * Created by peter on 2/24/16.
 */
public class WeightedSpikeEvent extends BaseSpikeEvent{

    final float weight;

    public WeightedSpikeEvent(int src, float weight) {
        super(src);
        this.weight = weight;
    }
}
