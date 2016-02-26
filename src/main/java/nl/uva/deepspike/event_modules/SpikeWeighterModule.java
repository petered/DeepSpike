package nl.uva.deepspike.event_modules;

import nl.uva.deepspike.events.SignedSpikeEvent;

/**
 * Created by peter on 2/24/16.
 */
public class SpikeWeighterModule extends StatelessBaseEventModule<WeightedSpikeEvent>{

    public float weight;

    public SpikeWeighterModule(float weight){
        this.weight = weight;
    }

    public void feed_event(SignedSpikeEvent ev){
        send_event(new WeightedSpikeEvent(ev.src, ev.is_negative ? -weight : weight));
    }
}
