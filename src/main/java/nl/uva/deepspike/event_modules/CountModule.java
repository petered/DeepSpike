package nl.uva.deepspike.event_modules;

import nl.uva.deepspike.events.SignedSpikeEvent;
import nl.uva.deepspike.events.SpikeEvent;

import java.util.PriorityQueue;

/**
 * Created by peter on 2/24/16.
 */
public class CountModule extends BaseEventModule<SignedSpikeEvent>{

    int[] counts;
    int n_srcs;
    PriorityQueue<DispatchEvent<SignedSpikeEvent>> queue;

    public CountModule(int n_srcs){
        this.counts = new int[n_srcs];
        this.n_srcs = n_srcs;
        queue = new PriorityQueue();
    }

    public void feed_event(SpikeEvent e){
        counts[e.src]+=1;
    }

    public void feed_event(SignedSpikeEvent e){
        counts[e.src]+= e.get_sign();
    }

    public int[] get_counts(){
        return counts;
    }

    @Override
    public void reset(){
        for (int i=0; i<n_srcs; i++)
            counts[i] = 0;
    }

    public void launch_events(){
        for (int i=0; i<n_srcs; i++){
            while (counts[i]!=0){
                SignedSpikeEvent spike = new SignedSpikeEvent(i, counts[i]<0);
                queue.add(new DispatchEvent(spike, Math.abs(counts[i])));
                counts[i] -= spike.get_sign();
            }
            while (!queue.isEmpty())
                send_event(queue.poll().ev);
        }
    }
}
