package nl.uva.deepspike.event_modules;

import nl.uva.deepspike.events.SignedSpikeEvent;
import nl.uva.deepspike.events.VectorEvent;
import java.util.PriorityQueue;

/**
 * Created by peter on 2/24/16.
 */
public abstract class BaseQueueHerder extends BaseHerder<SignedSpikeEvent> implements PassThroughQuantizer {

    PriorityQueue<DispatchEvent<SignedSpikeEvent>> queue;

    public BaseQueueHerder(int n_units) {
        super(n_units);
        this.queue = new PriorityQueue();
//        System.out.println(this.getClass()+" launched");
    }

    @Override
    public void feed_event(VectorEvent ev){
        assert ev.vec.length() == phi.length(): "Tried to add vector of length "+ev.vec.length() + " to phi, which has length " + phi.length();

        phi.addi(ev.vec);
        herd_away();
    }

    void herd_away(){
//        System.out.println(this.getClass()+" herding away: len: "+phi.length());
//
        for (int i=0; i<phi.length(); i++)
            create_spikes_from_unit(i);
        flush_queue();
    }

    abstract void create_spikes_from_unit(int unit_ix);

    protected void flush_queue(){
        while (!queue.isEmpty()){
            send_event(queue.poll().ev);
        }
    }

    @Override
    public void feed_spike(SignedSpikeEvent spike){

        phi.addiScalar(spike.src, -spike.get_sign());
        create_spikes_from_unit(spike.src);
        flush_queue();
    }
}
