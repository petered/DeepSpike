package nl.uva.deepstream.event_modules;

import nl.uva.arrays.IMatrix;
import nl.uva.arrays.IVector;
import nl.uva.arrays.VectorFactory;
import nl.uva.deepstream.events.MatrixEvent;
import nl.uva.deepstream.events.SignedSpikeEvent;
import nl.uva.deepstream.events.SpikeEvent;
import nl.uva.deepstream.events.VectorEvent;

/**
 * Created by peter on 2/24/16.
 */
public class OuterProductModule extends BaseEventModule<MatrixEvent> implements WeightGradientModule<MatrixEvent>{
    /* Accumulates input/output histograms, returns outer-product on command. */

    IVector input_vec;
    IVector output_counts;
    final boolean rectify_input;
//    float eta;
    final int n_in;
    final int n_out;
//    final int n_steps;
    final float learning_rate;

    public OuterProductModule(int n_in, int n_out, float learning_rate, boolean rectify_input){

        input_vec = VectorFactory.create(n_in);
        output_counts = VectorFactory.create(n_out);
//        this.eta = eta;
        this.n_in = n_in;
        this.n_out = n_out;
//        this.n_steps = n_steps;
        this.rectify_input = rectify_input;
        this.learning_rate = learning_rate;
    }

    public void feed_error_event(SignedSpikeEvent event){
        output_counts.addiScalar(event.src, event.get_sign());
    }

    public void feed_input_sum_event(VectorEvent event){
        assert input_vec.length() == event.vec.length();
        input_vec.assign(event.vec);
    }

    public void feed_input_event(SpikeEvent event){
        input_vec.addiScalar(event.src, +1);
    }

    public void feed_input_event(SignedSpikeEvent event){
        input_vec.addiScalar(event.src, event.get_sign());
    }

    public void produce_event(){

//        float multiplier = eta / (n_steps*n_steps);


        IMatrix update = input_vec.mul(input_vec.gt(0)).outer(output_counts.mul(learning_rate));
        send_event(new MatrixEvent(update));
    }

    @Override
    public void reset(){
        input_vec.assign(0);
        output_counts.assign(0);
    }

    @Override
    public void feed_error_event(WeightedSpikeEvent event) {
        output_counts.addiScalar(event.src, event.weight);
    }
}
