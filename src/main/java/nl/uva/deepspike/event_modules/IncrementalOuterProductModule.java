package nl.uva.deepspike.event_modules;

import nl.uva.arrays.IVector;
import nl.uva.arrays.VectorFactory;
import nl.uva.deepspike.events.SignedSpikeEvent;
import nl.uva.deepspike.events.SpikeEvent;
import nl.uva.deepspike.events.VectorEvent;

/**
 * Created by peter on 2/24/16.
 */
public class IncrementalOuterProductModule extends BaseEventModule<ColumnChangeEvent> implements WeightGradientModule<ColumnChangeEvent>{
    /* Just sends a delta-column to the weight matrix for every error event that comes back */

    IVector input_vec;
//    float eta;
//    int n_steps;
    float learning_rate;
    final boolean rectify_input;

    public IncrementalOuterProductModule(int n_in, float learning_rate, boolean rectify_input){
        input_vec = VectorFactory.create(n_in);
//        this.eta = eta;
//        this.n_steps = n_steps;
        this.learning_rate = learning_rate;
        this.rectify_input = rectify_input;
    }

    private void handle_weighted_error_event(int src, float weight){

        float multiplier = weight * learning_rate;


//        System.out.println("Multiplier: "+multiplier);

//        System.out.println("unit mult: " + (multiplier==1));

        IVector delta_vec = input_vec.mul(multiplier).mul(input_vec.gt(0));
        send_event(new ColumnChangeEvent(src, delta_vec, true));
    }

    @Override
    public void feed_error_event(SignedSpikeEvent event){
        handle_weighted_error_event(event.src, event.get_sign());
    }

    @Override
    public void feed_input_sum_event(VectorEvent event){
        input_vec.assign(event.vec);
    }

    @Override
    public void feed_input_event(SpikeEvent event){
        input_vec.addiScalar(event.src, 1);
    }

    @Override
    public void feed_input_event(SignedSpikeEvent event){
        input_vec.addiScalar(event.src, event.get_sign());
    }

    @Override
    public void reset(){
        input_vec.assign(0);
    }

    @Override
    public void feed_error_event(WeightedSpikeEvent event) {
        handle_weighted_error_event(event.src, event.weight);
    }



}
