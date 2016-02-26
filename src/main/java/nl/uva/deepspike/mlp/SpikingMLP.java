package nl.uva.deepspike.mlp;

import nl.uva.arrays.*;
import nl.uva.deepspike.event_modules.*;
import nl.uva.deepspike.events.*;
import nl.uva.deepspike.interfaces.*;
import nl.uva.deepspike.routers.*;

import java.util.*;
import java.util.stream.Collectors;

/*
Event-Based Multi Layer Perceptron.


*/


public class SpikingMLP{

    int n_steps;
    boolean fractional_updates;
    int n_layers;
    boolean return_counts;
    boolean smooth_grads;
    boolean hold_error;
    AccessibleRouter training_router;
    AccessibleRouter test_router;
    IdentityModule<VectorEvent> input_node;
    VectorSumModule input_summer;
    VectorSumModule output_summer;
    Quantizer input_herder;
    Quantizer neg_target_quantizer;
    QuantizingLayer[] layers;
    CountModule error_counter;

    public SpikingMLP(float[][][] ws, int n_steps, float eta, boolean depth_first,
                      boolean fractional_updates, boolean queue_implementation, boolean return_counts,
                      boolean smooth_grads, String forward_discretize, String backward_discretize,
                      String test_discretize, int seed, float regularization, boolean hold_error,
                      String dtype) {

        String input_target_discretize;
        float learning_rate;
        if (Arrays.asList("INT", "SHORT").contains(dtype)){
            int total_scale_factor = (int) (1/eta * n_steps * n_steps);
            learning_rate = 1;
            VectorFactory.set_scale(total_scale_factor);
            MatrixFactory.set_scale(total_scale_factor);
            forward_discretize = "scaled-rect-herding";
            test_discretize = "scaled-rect-herding";
            backward_discretize = "scaled-noreset-herding";
            input_target_discretize = "scaled-herding";
        }
        else{
            input_target_discretize = "herding";
            learning_rate = eta / ((float) n_steps * n_steps);
        }

        VectorFactory.set_dtype(dtype);
        MatrixFactory.set_dtype(dtype);
        List<IMatrix> weights = Arrays.stream(ws).map(MatrixFactory::create).collect(Collectors.toList());

        this.hold_error = hold_error;
        this.n_steps = n_steps;
        n_layers = weights.size();
        this.fractional_updates = fractional_updates;
        this.return_counts = return_counts;
        this.smooth_grads = smooth_grads;

        // Initialize stuff
        Random rng = new Random(seed);
        layers = new QuantizingLayer[n_layers];
        for (int i=0; i<n_layers; i++)
            layers[i] = new QuantizingLayer(weights.get(i), learning_rate, fractional_updates,
                    forward_discretize, backward_discretize, test_discretize, rng, i==0, regularization);
        int input_size = weights.get(0).rows();
        int[] layer_sizes = weights.stream().mapToInt(w->w.columns()).toArray();

        // Fwd pass components
        input_node = new IdentityModule();
        input_summer = new VectorSumModule(input_size);
        output_summer = new VectorSumModule(layer_sizes[layers.length-1]);
        input_herder = QuantizerFactory.make_quantizer(input_target_discretize, input_size, rng);
        neg_target_quantizer = QuantizerFactory.make_quantizer(input_target_discretize, layer_sizes[n_layers-1], rng);
        if (hold_error)
            error_counter = new CountModule(layer_sizes[layer_sizes.length-1]);
        training_router = create_training_router(depth_first);
        test_router = create_test_router(depth_first);
    }

    public void train_one(IVector input_vec, IVector target_vec){

        SwitchBoard.global_switchboard.set_handler(training_router);
        training_router.reset();
        for (int t = 0; t < n_steps; t++) {
            training_router.handle_event(new VectorEvent(input_vec), input_node::feed_event);
            training_router.handle_event(new VectorEvent(target_vec.neg()), neg_target_quantizer::feed_event);
            if (hold_error)
                error_counter.launch_events();
        }
        if (!fractional_updates)
            for (QuantizingLayer lay: layers)
                ((OuterProductModule)(lay.weight_updater)).produce_event();  // We need to artificially for now.
    }

    public IVector predict_one(IVector input_vec){

        SwitchBoard.global_switchboard.set_handler(test_router);
        test_router.reset();
        for (int t = 0; t < n_steps; t++) {
            test_router.handle_event(new VectorEvent(input_vec), input_node::feed_event);
        }
        IVector out_sum = output_summer.get_sum();

        IVector output = out_sum.div((float) n_steps);
        return output;
    }

    public void train_one(float[] input_vec, float[] target_vec){
        train_one(VectorFactory.create(input_vec), VectorFactory.create(target_vec));
    }

    public float[] predict_one(float[] input_vec) {
        return predict_one(VectorFactory.create(input_vec)).asFloat();
    }

    public float[][] predict(float[][] input_vecs){
        if (input_vecs.length==0)
            return new float[0][0];
        float[] first_out = predict_one(input_vecs[0]);
        float[][] output = new float[input_vecs.length][first_out.length];
        System.arraycopy(first_out, 0, output[0], 0, first_out.length);
        for (int i=1; i<input_vecs.length; i++)
            System.arraycopy(predict_one(input_vecs[i]), 0, output[i], 0, output[i].length);
        return output;
    }

    public void train(float[][] input_vecs, float[][] target_vecs){
        for (int i=0; i<input_vecs.length; i++)
            train_one(input_vecs[i], target_vecs[i]);
    }

    private AccessibleRouter create_training_router(boolean depth_first){
        /*
        in --+-> IH --> W[0] --+-----> H[0] ------> W[1] ----+----> H[1]
             |                S[0]                          S[1]     |
             IS                F[0] <-- BH[1] <-- WT[1] <-- F[1] <-- N <-- target
             |                 |                             |
             '----> OP[0] <----'           S[0] --> OP[1] <--'
        */

        AccessibleRouter r = depth_first?new DepthRouter():new BreadthRouter();
        r.add_binding(input_node, input_herder::feed_event);
        r.add_binding(input_herder, layers[0].forward_weights::feed_event);
        if (smooth_grads){
            r.add_binding(input_node, input_summer::feed_event);
            r.add_binding(input_summer, layers[0].weight_updater::feed_input_sum_event);
        }
        else
            r.add_binding(input_herder, layers[0].weight_updater::feed_input_event);
        for (int i=0; i<n_layers; i++){
            if (i>0){
                r.add_binding(layers[i-1].forward_herder, layers[i].forward_weights::feed_event);
                r.add_binding(layers[i].backward_herder, layers[i-1].back_filter::feed_event);
                if (smooth_grads)
                    r.add_binding(layers[i-1].pre_quant_summer, layers[i].weight_updater::feed_input_sum_event);
                else
                    r.add_binding(layers[i-1].forward_herder, layers[i].weight_updater::feed_input_event);
            }
            layers[i].bind_internals_for_training(r);
        }
        if (hold_error){
            r.add_binding(layers[n_layers-1].forward_herder, error_counter::feed_event);
            r.add_binding(error_counter, layers[n_layers-1].back_filter::feed_event);
            r.add_binding(neg_target_quantizer, error_counter::feed_event);
        }
        else{
            r.add_binding(layers[n_layers-1].forward_herder, layers[n_layers-1].back_filter::feed_event);
            r.add_binding(neg_target_quantizer, layers[n_layers-1].back_filter::feed_event);
        }
        return r;
    }

    private AccessibleRouter create_test_router(boolean depth_first){
        /*
        IL ---> W[0] ----> H[0] ----> W[1] ----> H[1] ----> OS
        */
        AccessibleRouter r = depth_first?new DepthRouter():new BreadthRouter();
        r.add_binding(input_node, input_herder::feed_event);
        r.add_binding(input_herder, layers[0].forward_weights::feed_event);
        for (int i=0; i<n_layers; i++){
            layers[i].bind_internals_for_prediction(r);
            if (i>0)
                r.add_binding(layers[i-1].test_herder, layers[i].forward_weights::feed_event);
        }
        if (return_counts)
            r.add_binding(layers[n_layers-1].test_herder, output_summer::feed_event);
        else
            r.add_binding(layers[n_layers-1].forward_weights, output_summer::feed_event);  // Attach it to the router so that it is reset when the router is.
        r.add_binding(output_summer, null, "Output Summer");
        return r;
    }
}


class QuantizingLayer{

    final WeightModule forward_weights;
    VectorSumModule pre_quant_summer;
    PassThroughQuantizer forward_herder;
    PassThroughQuantizer test_herder;
//    VectorThresholdModule thresh;
    ReverseWeightModule backward_weights;
    PassThroughQuantizer backward_herder;
    EventGTFilterModule<SignedSpikeEvent> back_filter;
    WeightGradientModule weight_updater;
    boolean first_layer;
    boolean fractional_updates;
    SpikeWeighterModule penalty;


    public QuantizingLayer(IMatrix w, float learning_rate, boolean fractional_updates, String forward_discretize, String back_discretize, String test_discretize,
                           Random rng, boolean first_layer, float regularization){

        assert w.shape().length==2: "Can only handle 2D weight matrices now.";
        int n_in = w.shape()[0];
        int n_out = w.shape()[1];
        this.first_layer = first_layer;
        this.fractional_updates = fractional_updates;
        forward_weights = new WeightModule(w);
        pre_quant_summer = new VectorSumModule(n_out);
        forward_herder = QuantizerFactory.make_quantizer(forward_discretize, n_out, rng);
        test_herder = QuantizerFactory.make_quantizer(test_discretize, n_out, rng);
        if (regularization!=0)
            penalty = new SpikeWeighterModule(regularization);
//        thresh = new VectorThresholdModule();
        if (!first_layer){  // No need for this if we're in the first layer.
            backward_weights = new ReverseWeightModule(w);
            backward_herder = QuantizerFactory.make_quantizer(back_discretize, n_in, rng);
        }
        back_filter = new EventGTFilterModule(n_out);
        weight_updater = fractional_updates?
                new IncrementalOuterProductModule(n_in, learning_rate, !first_layer) :
                new OuterProductModule(n_in, n_out, learning_rate, !first_layer);
    }

    public void bind_internals_for_training(Router r){
        /* Note that this does NOT bind anything to the input of the weight_updater */
        r.add_binding(forward_weights, pre_quant_summer::feed_event);
        r.add_binding(pre_quant_summer, back_filter::feed_filter_event);  // Important that this comes before next line
//        r.add_binding(pre_quant_summer, thresh::feed_event);  // Important that this comes before next line
//        r.add_binding(thresh, back_filter::feed_filter_event);
        r.add_binding(back_filter, weight_updater::feed_error_event);
        r.add_binding(forward_weights, forward_herder::feed_event);
        if (penalty != null){
//            System.out.println("REGULARIZATIONS ADDAF");
            r.add_binding(forward_herder, penalty::feed_event);
            r.add_binding(penalty, weight_updater::feed_error_event);
        }
        if (!first_layer){
            r.add_binding(back_filter, backward_weights::feed_event);
            r.add_binding(backward_weights, backward_herder::feed_event);
        }
        if (fractional_updates)
            r.add_binding((IncrementalOuterProductModule)weight_updater, forward_weights::feed_update);
        else
            r.add_binding((OuterProductModule)weight_updater, forward_weights::feed_update);
    }

    public void bind_internals_for_prediction(Router r){
        r.add_binding(forward_weights, test_herder::feed_event);
        r.add_binding(test_herder, null);  // This prevents the error from the un-listened-to herder
    }
}


//class ScaledRectRoundingHerder extends ScaledRoundingHerder{
//
//    public ScaledNoResetRoundingHerder(int n_units, float scale){
//        super(n_units, scale);
//    }
//
//    @Override
//    public void reset(){
//    }
//}


