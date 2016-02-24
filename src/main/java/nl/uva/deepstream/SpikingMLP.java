package nl.uva.deepstream;

import nl.uva.arrays.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/*
Event-Based Multi Layer Perceptron.


*/



interface Resettable{

    void reset();
}


interface EventModule<EventType> extends Resettable{

    void send_event(EventType event);

    void set_handler(EventHandler handler);

}

@FunctionalInterface
interface EventHandler{

    <EventType extends BaseEvent> void handle_event(EventType ev, EventModule<EventType> src_module);

}

interface Router extends EventHandler, Resettable{

    <EventType extends BaseEvent> void add_binding(EventModule<EventType> src_module, Consumer<EventType> method_reference);

    <EventType extends BaseEvent> void handle_event(EventType ev, Consumer<EventType> consumer);

}

abstract class AccessibleRouter implements Router{
    /*This is a handy base-implementation of Router, which allows you to name modules that you add*/

    HashMap<String, EventModule> name_src_map;

    public AccessibleRouter(){
        name_src_map = new HashMap();
    }

    public <EventType extends BaseEvent> void add_binding(EventModule<EventType> src_module, Consumer<EventType> method_reference, String src_name){
        assert (!name_src_map.containsKey(src_name)) || src_module!=name_src_map.get(src_name): "You already added a different module named '"+src_name+"'";
        name_src_map.put(src_name, src_module);
        add_binding(src_module, method_reference);
    }

    public EventModule get_module(String src_name){
        /* Get a module by name, or return null if it does not exist */
        if (!name_src_map.containsKey(src_name))
            throw new RuntimeException("No module named "+src_name+" exists.");
        else
            return name_src_map.get(src_name);
    }

    public abstract void print_connectivity();

}

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
        GlobalClock.reset();
        for (int t = 0; t < n_steps; t++) {
            GlobalClock.update_time(t);
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
        GlobalClock.reset();

        for (int t = 0; t < n_steps; t++) {
            GlobalClock.update_time(t);
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


class GlobalClock {

    public static int time = 0;

    public static void reset(){
        time = 0;
    }

    public static void update_time(int t) {
        assert t >= time;
        time = t;
    }
}


abstract class BaseEventModule<EventType extends BaseEvent> implements EventModule<EventType> {

    EventHandler event_handler;

    public BaseEventModule() {
        event_handler = SwitchBoard.global_switchboard;  // Bad, maybe, but whatchagonnadoaboudit?
    }

    @Override
    public void send_event(EventType event){
        event_handler.handle_event(event, this);
    }

    @Override
    public void set_handler(EventHandler handler){
        event_handler = handler;
    }

    public List<EventType> create_monitor(boolean keep_current){
        Eavesdropper eavesdropper = new Eavesdropper();

        this.set_handler(eavesdropper);
        return eavesdropper.get_record();
    }

    public List<EventType> create_monitor(){
        return create_monitor(true);
    }

    public <T extends BaseEavesdropper> T add_eavesdropper(T eavesdropper){
        eavesdropper.set_handler(event_handler);
        this.set_handler(eavesdropper);
        return eavesdropper;
    }

    public Eavesdropper add_eavesdropper(){
        return add_eavesdropper(new Eavesdropper());
    }
}


abstract class StatelessBaseEventModule<EventType extends BaseEvent> extends BaseEventModule<EventType>{
    /* Started this because of bugs caused by forgetting to reset.  Now you have to explicitly not reset */

    @Override
    public void reset(){
    }

}


class IdentityModule<EventType extends BaseEvent> extends StatelessBaseEventModule<EventType>{
    // Why?  Useful as an input node, where you may want to distribute an input signal
    // to multiple nodes.  Distributing the input to the appropriate nodes in the
    // graph is then the concern of the graph-builder, rather than the input-feeder,

    public void feed_event(EventType ev){
        send_event(ev);
    }

}


class WeightModule extends StatelessBaseEventModule<VectorEvent>{

    IMatrix w;
    int n_in;
    int n_out;

    public WeightModule(IMatrix w) {
        this.w = w;
        this.n_in = w.shape()[0];
        this.n_out = w.shape()[1];
    }

    public void feed_event(SpikeEvent spike) {

        send_event(new VectorEvent(w.getRow(spike.src), true)); // CAREFUL!!! For now we say, safe to copy because we know this won't be modified before it's used.  May be a differnet story in the future.
    }

    public void feed_event(SignedSpikeEvent spike) {
        IVector weight = spike.is_negative? w.getRow(spike.src).neg() : w.getRow(spike.src);
        send_event(new VectorEvent(weight));
    }

    public void feed_update(MatrixEvent ev){
        // Gets a gradient estimate, already multiplied by eta.
        w.subi(ev.matrix);
    }

    public void feed_update(ColumnChangeEvent ev){
        w.subiColumn(ev.col_ix, ev.vec);
    }
}

class ReverseWeightModule extends StatelessBaseEventModule<VectorEvent>{

    int n_left;
    int n_right;
    IMatrix w;

    ReverseWeightModule(IMatrix w){
        this.w = w;
        n_left = w.shape()[0];
        n_right = w.shape()[1];
    }

    public void feed_event(SignedSpikeEvent spike) {

        IVector weight = spike.is_negative? w.getColumn(spike.src).neg() : w.getColumn(spike.src);
        send_event(new VectorEvent(weight, true));
    }
}


interface Quantizer extends EventModule<SignedSpikeEvent>{

    void feed_event(VectorEvent ev);

}

interface PassThroughQuantizer extends Quantizer{
    // TODO: Delete?

    void feed_spike(SignedSpikeEvent spike);  // Shortcut method

}


class QuantizerFactory{

    public enum QuantizerTypes {HERDING, NORESET_HERDING, NORESET_RECT_HERDING, POISSON, POISSON_ZERO,
        RECT_HERDING, RANDRESET_HERDING, SCALED_HERDING, SCALED_RECT_HERDING, SCALED_NORESET_HERDING}


    public static PassThroughQuantizer make_quantizer(String implementation, int n_units, Random rng){

        return make_quantizer(QuantizerTypes.valueOf(implementation.toUpperCase().replace("-", "_")), n_units, rng);
    }

    public static PassThroughQuantizer make_quantizer(QuantizerTypes implementation, int n_units, Random rng){
        switch (implementation) {
            case HERDING:
                return new VarQueueHerder(n_units);
            case NORESET_HERDING:
                return new NonResettingVarQueueHerder(n_units);
            case NORESET_RECT_HERDING:
                return new NonResettingRectVarQueueHerder(n_units);
            case POISSON:
                return new PoissonHerder(n_units, false, rng);
            case POISSON_ZERO:
                return new PoissonHerder(n_units, true, rng);
            case RECT_HERDING:
                return new RectVarQueueHerder(n_units);
//            case SCALED_HERDING:
//                return new ScaledRoundingHerder(n_units, 4);
            case RANDRESET_HERDING:
                return new RandomShiftedThresholdHerder(n_units, rng);
            case SCALED_HERDING:
                return new ScaledRoundingHerder(n_units, VectorFactory.get_scale());
            case SCALED_RECT_HERDING:
                return new ScaledRectRoundingHerder(n_units, VectorFactory.get_scale());
            case SCALED_NORESET_HERDING:
                return new ScaledNoResetRoundingHerder(n_units, VectorFactory.get_scale());
            default:
                throw new UnsupportedOperationException("No implementation "+implementation);
        }
    }
}


abstract class BaseHerder<EventType extends BaseEvent> extends BaseEventModule<EventType>{

    IVector phi;

    BaseHerder(int n_units) {

        this.phi = VectorFactory.create(n_units);
//        System.out.println("Created phi of type " + this.phi.dtype().toString());
    }

    public abstract void feed_event(VectorEvent ev);

    @Override
    public void reset() {
        this.phi.assign(0);
    }
}


class DispatchEvent<EventType> implements Comparable<DispatchEvent>{
    /* For internal use by herding modules.  Indentifies both the event and the
    order in which it should be distpatched.
    */
    final public float order;
    final EventType ev;

    DispatchEvent(EventType ev, float order){
        this.order = order;
        this.ev = ev;
    }

    @Override
    public int compareTo(DispatchEvent o) {
        return Double.compare(o.order, order);  // Opposite of the phi... Head of a priority queue is the LOWEST element
    }
}


abstract class BaseQueueHerder extends BaseHerder<SignedSpikeEvent> implements PassThroughQuantizer{

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

class RectVarQueueHerder extends BaseQueueHerder implements Quantizer{

    public RectVarQueueHerder(int n_units) {
        super(n_units);
    }

    @Override
    void create_spikes_from_unit(int unit_ix) {
        while(phi.getFloat(unit_ix) > 0.5){
            queue.add(new DispatchEvent(new SpikeEvent(unit_ix), phi.getFloat(unit_ix)));
            phi.addiScalar(unit_ix, -1);
        }
    }

}



class VarQueueHerder extends BaseQueueHerder{

    public VarQueueHerder(int n_units) {
        super(n_units);
    }

    @Override
    public void create_spikes_from_unit(int unit_ix) {
        float val = phi.getFloat(unit_ix);
        while(val > 0.5){  // Positive spikes, if any
            queue.add(new DispatchEvent(new SignedSpikeEvent(unit_ix, false), val));
            val -= 1;
        }
        while(val < -0.5){ // Negative spikes, if any
            queue.add(new DispatchEvent(new SignedSpikeEvent(unit_ix, true), -val));
            val += 1;
        }
        phi.putScalar(unit_ix, val);
    }
}

class NonResettingVarQueueHerder extends VarQueueHerder{
    /* A version of the varqueueherder where phi never resets */

    public NonResettingVarQueueHerder(int n_units) {
        super(n_units);
    }

    @Override
    public void reset(){
    }

}


class NonResettingRectVarQueueHerder extends RectVarQueueHerder{
    /* A version of the varqueueherder where phi never resets */

    public NonResettingRectVarQueueHerder(int n_units) {
        super(n_units);
    }

    @Override
    public void reset(){
    }
}

class RandomShiftedThresholdHerder extends VarQueueHerder{

    Random rng;

    public RandomShiftedThresholdHerder(int n_units, Random rng) {

        super(n_units);
        this.rng = rng;
        reset();
    }

    @Override
    public void reset(){
        super.reset();
        for (int i = 0; i < phi.length(); i++)
            phi.putScalar(i, rng.nextDouble()-.5);
    }
}


class ScaledRoundingHerder extends BaseQueueHerder{

    final float scale;
    final float scale_over_2;

    public ScaledRoundingHerder(int n_units, float scale) {
        super(n_units);
        this.scale = scale;
        this.scale_over_2 = scale/2;
//        System.out.println("Scale "+scale);
//        System.out.println("scale_over_2 "+scale_over_2);
    }

    @Override
    void create_spikes_from_unit(int unit_ix) {
        float val = phi.getFloat(unit_ix);
//        System.out.println("phi: "+phi.getFloat(unit_ix)+", thresh: "+this.scale_over_2);
        while(true){
//            System.out.println("val: "+val);
            SignedSpikeEvent ev;
            if (val > scale_over_2)
                ev = new SignedSpikeEvent(unit_ix, false);
            else if (val <= -scale_over_2)
                ev = new SignedSpikeEvent(unit_ix, true);
            else
                break;
            queue.add(new DispatchEvent(ev, Math.abs(val)));
            val -= scale * ev.get_sign();

        }
//        System.out.println("Out");
        phi.putScalar(unit_ix, val);
    }
}


class ScaledNoResetRoundingHerder extends ScaledRoundingHerder{

    public ScaledNoResetRoundingHerder(int n_units, float scale){
        super(n_units, scale);
    }

    @Override
    public void reset(){
    }
}


class ScaledRectRoundingHerder extends BaseQueueHerder implements Quantizer{

    final float scale;
    final float scale_over_2;

    public ScaledRectRoundingHerder(int n_units, float scale) {
        super(n_units);
        this.scale=scale;
        this.scale_over_2 = scale/2;

    }

    @Override
    void create_spikes_from_unit(int unit_ix) {
//        System.out.println("phi: "+phi.getFloat(unit_ix)+", thresh: "+this.scale_over_2);
        while(phi.getFloat(unit_ix) > this.scale_over_2){
            queue.add(new DispatchEvent(new SpikeEvent(unit_ix), phi.getFloat(unit_ix)));
            phi.addiScalar(unit_ix, -this.scale);
        }
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




class PoissonHerder extends BaseQueueHerder{

    Random rng;
    IVector thresholds;
    final boolean zero_mode;

    public PoissonHerder(int n_units, boolean zero_mode, Random rng) {
        super(n_units);
        this.rng = rng;
        this.zero_mode = zero_mode;
        thresholds = VectorFactory.create(n_units);

        reset();
    }

    @Override
    public void create_spikes_from_unit(int unit_ix) {
        // Note that this method can create opposite spikes from a single unit,
        // in a single call, which is kind of strange.  But it seems to only
        // about 15% extra events.
        float val = phi.getFloat(unit_ix);
        while (true){
            SignedSpikeEvent ev;
            float thresh = thresholds.getFloat(unit_ix);

            if (val > thresh)
                ev = new SignedSpikeEvent(unit_ix, false);
            else if (val <= -thresh)
                ev = new SignedSpikeEvent(unit_ix, true);
            else
                break;
            queue.add(new DispatchEvent(ev, Math.abs(val)));
            if (zero_mode)
                val = 0;
            else
                val -= ev.get_sign();

            thresholds.putScalar(unit_ix, next_random_threshold());
        }
        phi.putScalar(unit_ix, val);
    }

    public float next_random_threshold(){
        return -(float)Math.log(1-rng.nextDouble());  // Divide by lambda (but that's just 1)
    }

    @Override
    public void reset(){
        super.reset();

        for (int i=0; i<thresholds.length(); i++)
            thresholds.putScalar(i, next_random_threshold());
    }
}


abstract class BaseArgmaxHerder<EventType extends BaseEvent> extends BaseHerder<EventType>{

    public BaseArgmaxHerder(int n_units) {
        super(n_units);
    }

    void add_vec(IVector vec){
        assert phi.length() == vec.length(): "phi length: "+phi.length()+" must match input length: "+vec.length();
        phi.addi(vec);
    }

    @Override
    public void feed_event(VectorEvent vec_event) {
        add_vec(vec_event.vec);
        herd_away();
    }

    abstract void herd_away();

}


class VarArgmaxHerder extends BaseArgmaxHerder<SignedSpikeEvent> implements Quantizer{

    public VarArgmaxHerder(int n_units) {
        super(n_units);
    }

    @Override
    void herd_away(){
        while (true) {
            int winner = phi.argMax();
            float val = phi.getFloat(winner);
            // Note: inefficiency here... better to just look once for threshold crossers.
            if (val > 0.5) {
                val -= 1;
                send_event(new SpikeEvent(winner));
            } else {
                break;
            }
            phi.putScalar(winner, val);
        }
    }
}

class BiVarArgmaxHerder extends BaseArgmaxHerder<SignedSpikeEvent> implements PassThroughQuantizer{

    public BiVarArgmaxHerder(int n_units) {
        super(n_units);
    }

    @Override
    void herd_away(){
        // Note: inefficiency here... better to just look once for threshold crossers.
        while (true) {
            IVector absphi = phi.abs();
            int winner = absphi.argMax();
            float val = phi.getFloat(winner);
            if (Math.abs(val) > 0.5) {
                boolean is_negative = val < 0;
                val -= is_negative ? -1 : 1;
                send_event(new SignedSpikeEvent(winner, is_negative));
            } else {
                break;
            }
            phi.putScalar(winner, val);
        }
    }

    @Override
    public void feed_spike(SignedSpikeEvent spike){
        send_event(spike);
    }
}


class CountModule extends BaseEventModule<SignedSpikeEvent>{

    int[] counts;
    int n_srcs;
    PriorityQueue<DispatchEvent<SignedSpikeEvent>> queue;

    CountModule(int n_srcs){
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

    public float[] get_historical_mean(){
        float floatal = (float) GlobalClock.time+1;
        float[] dist = new float[n_srcs];
        for (int i=0; i<n_srcs; i++)
            dist[i] = counts[i]/floatal;
        return dist;
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


interface WeightGradientModule<EventType extends BaseEvent> extends EventModule<EventType>{

    void feed_error_event(SignedSpikeEvent event);

    void feed_error_event(WeightedSpikeEvent event);

    void feed_input_sum_event(VectorEvent event);

    void feed_input_event(SpikeEvent event);

    void feed_input_event(SignedSpikeEvent event);

}


class OuterProductModule extends BaseEventModule<MatrixEvent> implements WeightGradientModule<MatrixEvent>{
    /* Accumulates input/output histograms, returns outer-product on command. */

    IVector input_vec;
    IVector output_counts;
    final boolean rectify_input;
//    float eta;
    final int n_in;
    final int n_out;
//    final int n_steps;
    final float learning_rate;

    OuterProductModule(int n_in, int n_out, float learning_rate, boolean rectify_input){

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


class IncrementalOuterProductModule extends BaseEventModule<ColumnChangeEvent> implements WeightGradientModule<ColumnChangeEvent>{
    /* Just sends a delta-column to the weight matrix for every error event that comes back */

    IVector input_vec;
//    float eta;
//    int n_steps;
    float learning_rate;
    final boolean rectify_input;

    IncrementalOuterProductModule(int n_in, float learning_rate, boolean rectify_input){
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


class EventFilterModule<EventType extends BaseSpikeEvent> extends BaseEventModule<EventType>{

    int n_srcs;
    BooleanVector filters;

    EventFilterModule(int n_srcs){
        this.n_srcs = n_srcs;
        this.filters = new BooleanVector(n_srcs);
        reset();  // Filters now start at true;
    }

    public void feed_filter_event(BooleanVectorEvent ev){
        filters.assign(ev.vec);
    }

    public void feed_event(EventType ev){

        if (filters.getBoolean(ev.src))
            send_event(ev);
    }

    @Override
    public void reset(){
        filters.assign(true);
    }
}



class EventGTFilterModule<EventType extends BaseSpikeEvent> extends BaseEventModule<EventType>{

    int n_srcs;
    IVector filters;

    EventGTFilterModule(int n_srcs){
        this.n_srcs = n_srcs;
        reset();  // Filters now start at true;
    }

    public void feed_filter_event(VectorEvent ev){
        filters = ev.vec;
    }

    public void feed_event(EventType ev){
        if (filters.elementGreaterThan(ev.src, 0))
            send_event(ev);
    }

    @Override
    public void reset(){
        filters=VectorFactory.create(n_srcs);
    }
}


class VectorSumModule extends BaseEventModule<VectorEvent>{

    final public IVector sum;
    boolean send_events;

    VectorSumModule(int n_units){
        this(n_units, true);
    }

    VectorSumModule(int n_units, boolean send_events){

        this.sum = VectorFactory.create(n_units);
        this.send_events = send_events;
        reset();
    }

    public IVector get_sum(){
        return sum;
    }

    public void feed_event(VectorEvent ev){
        sum.addi(ev.vec);
        if (send_events)
            send_event(new VectorEvent(sum));
        // NOTE: Kinda dangerous because it exports the state vector.
        // Could copy it, but that can be wasteful.  Is there some way to lock it?
    }

    public void feed_event(SpikeEvent ev){
        sum.addiScalar(ev.src, +1);
    }

    public void feed_event(SignedSpikeEvent ev){
        sum.addiScalar(ev.src, ev.get_sign());
    }

    @Override
    public void reset(){
        sum.assign(0);
    }
}


class SpikeWeighterModule extends StatelessBaseEventModule<WeightedSpikeEvent>{

    public float weight;

    public SpikeWeighterModule(float weight){
        this.weight = weight;
    }

    public void feed_event(SignedSpikeEvent ev){
        send_event(new WeightedSpikeEvent(ev.src, ev.is_negative ? -weight : weight));
    }
}


class VectorThresholdModule extends StatelessBaseEventModule<BooleanVectorEvent>{

    public void feed_event(VectorEvent ev){

        BooleanVector exceeders = ev.vec.gt(0);
        send_event(new BooleanVectorEvent(exceeders, true));
    }
}


class BaseEvent{

    public final int time;

    BaseEvent() {
        this.time = GlobalClock.time;
    }
}


class BaseSpikeEvent extends BaseEvent{

    public final int src;

    public BaseSpikeEvent(int src) {
        this.src = src;
    }
}


class SpikeEvent extends SignedSpikeEvent{
    /* This exists purely to avoid accidently treating SignedSpikeEvents as SpikeEvents */
    public SpikeEvent (int src){
        super(src, false);
    }
}


class SignedSpikeEvent extends BaseSpikeEvent{

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

class WeightedSpikeEvent extends BaseSpikeEvent{

    final float weight;

    public WeightedSpikeEvent(int src, float weight) {
        super(src);
        this.weight = weight;
    }
}

class VectorEvent extends BaseEvent{

    final public IVector vec;

    public VectorEvent(IVector vec) {
        this(vec, false);
    }

    public VectorEvent(IVector vec, boolean safe){
        // Set safe to true if you're certain that the array won't be modified later and you want to save time/memory.
        this.vec = safe ? vec.dup() : vec;
    }

}

class BooleanVectorEvent extends BaseEvent{

    final public BooleanVector vec;

    public BooleanVectorEvent(BooleanVector vec) {
        this(vec, false);
    }

    public BooleanVectorEvent(BooleanVector vec, boolean safe){
        // TODO: Maybe remove this event type, or somehow assert boolean data without checking every element.
        this.vec = safe ? vec.dup() : vec;
    }
}


class MatrixEvent extends BaseEvent{

    final public IMatrix matrix;

    public MatrixEvent(IMatrix matrix){
        this.matrix = matrix;
    }
}


class ColumnChangeEvent extends BaseEvent{
    /*An event which sends a change in value to a row at a given index.*/

    public int col_ix;
    public IVector vec;

    ColumnChangeEvent(int row_ix, IVector vec){
        this(row_ix, vec, false);
    }

    ColumnChangeEvent(int row_ix, IVector vec, boolean safe){
        this.col_ix = row_ix;
        this.vec = safe ? vec.dup() : vec;
    }

}


class SwitchBoard implements EventHandler{

    public static final SwitchBoard global_switchboard = new SwitchBoard();
    EventHandler current_handler;

    public void set_handler(EventHandler handler){
        current_handler = handler;
    }

    @Override
    public <EventType extends BaseEvent> void handle_event(EventType ev, EventModule<EventType> src_module) {
        current_handler.handle_event(ev, src_module);
    }
}


class DepthRouter extends AccessibleRouter{
    /*
    Processes events in Depth-First order.

    Say an event from module A (A1) causes two events from module B (B1, B2),
    which are passed on to modules C.  In Depth-First order, B1 is processed by
    C before B2 is produced by B.

    In Depth-First order, the sequence of events is:

    A.produce(A1)
        B.consume(A1)
            B.produce(B1)
                C.consume(B1)
            B.produce(B2)
                C.consume(B2)

    Indentation indicates stack-depth.  Contrast this to breadth-first order, below.
    */

    Map<EventModule, List<Consumer>> src_to_consumers;

    DepthRouter(){
//        System.out.println("Starting depth-router");
        src_to_consumers = new HashMap();
    }

    @Override
    public <EventType extends BaseEvent> void add_binding(EventModule<EventType> src_module, Consumer<EventType> method_reference) {
        assert src_module != null: "You tried to add a null pointer, fool.";
        if (!src_to_consumers.containsKey(src_module)){
            src_to_consumers.put(src_module, new ArrayList());
        }
        if (method_reference!=null)  // Note that we allow null method references for the case when we want to add modules without destinations.
            src_to_consumers.get(src_module).add(method_reference);
    }

    @Override
    public <EventType extends BaseEvent> void handle_event(EventType ev, Consumer<EventType> consumer) {
        consumer.accept(ev);
    }

    @Override
    public <EventType extends BaseEvent> void handle_event(EventType ev, EventModule<EventType> src_module) {
        src_to_consumers.get(src_module).stream().forEach(consumer -> consumer.accept(ev));
    }

    @Override
    public void reset(){
        src_to_consumers.keySet().stream().forEach(mod -> mod.reset());
    }

    @Override
    public void print_connectivity() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }



}


class BreadthRouter extends AccessibleRouter{
    /*
    Processes events in Breadth-First order.

    Say an event from module A (A1) causes two events from module B (B1, B2),
    which are passed on to modules C.  In Breadth-First order, all events from B
    are produced before the first event gets to act on C.

    In Breadth-first order, the sequence of events is:

    A.produce(A1)
    B.consume(A1)
        B.produce(B1)
        B.produce(B2)
            C.consume(B1)
            C.consume(B2)

    Indentation indicates stack-depth.  Contrast this to depth-first order, above.
    */

    Map<EventModule, ModuleLinks> src_to_links;
    private Deque<ModuleLinks> src_deque; // If this is a stack instead is this then just a DepthRouter?
    boolean work_in_progress;

    BreadthRouter(){
//        System.out.println("Starting Breadth-router");
        src_to_links = new HashMap();
        work_in_progress = false;
    }

    @Override
    public void print_connectivity() {
        System.out.println("== Router "+this+" Connectivity ==");
        for (ModuleLinks ml: src_to_links.values()){
            System.out.println("Module "+ml.src+" links to " + ml.consumers.size());
        }
        System.out.println("===================================");
    }

    class ModuleLinks<EventType extends BaseEvent>{
        // Contains the object linked to a module (an output queue and a list of consumer)
        List<EventType> queue;
        EventModule<EventType> src;
        List<Consumer<EventType>> consumers;

        public ModuleLinks(EventModule src){
            this.src = src;
            queue = new ArrayList();
            consumers = new ArrayList();
            src_deque = new ArrayDeque();
        }

        public void add_binding(Consumer<EventType> method_reference){
            consumers.add(0, method_reference);
        }
    }

    @Override
    public <EventType extends BaseEvent> void add_binding(EventModule<EventType> src_module, Consumer<EventType> method_reference) {
        assert src_module != null: "You tried to add a null pointer, fool.";
        if (!src_to_links.containsKey(src_module))
            src_to_links.put(src_module, new ModuleLinks(src_module));
        if (method_reference!=null)  // Note that we allow null method references for the case when we want to add modules without destinations.
            src_to_links.get(src_module).add_binding(method_reference);
    }

    @Override
    public <EventType extends BaseEvent> void handle_event(EventType ev, Consumer<EventType> consumer) {
        work_in_progress = true;
        consumer.accept(ev);
        process_head_queue();
        work_in_progress = false;
    }

    private <EventType extends BaseEvent> void process_head_queue(){
        ModuleLinks<EventType> links = src_deque.pollLast();
        if (links == null)
            return;
        links.consumers.stream().forEach(consumer -> links.queue.stream().forEach(consumer::accept));  // Haha!  Totally understantable!  But seriously.  It means send each event in the queue to each consumer
        links.queue.clear();
        process_head_queue();  // Could also avoid this with a while loop in feed_event, but who doesn't like tail recursion!
    }

    @Override
    public <EventType extends BaseEvent> void handle_event(EventType ev, EventModule<EventType> src_module) {
        /* Here's how we do it:

        We have a FIFO queue of src_modules to process.  When a src_module fires
        an event, the event is added to the (tail of the) queue, if that source
        module is not already sitting at the tail.
        */

        if (src_deque.isEmpty() || src_deque.peekLast().src != src_module)
            try{
                src_deque.addLast(src_to_links.get(src_module));
            }
            catch (NullPointerException err){
                throw new RuntimeException("An event fired from module "+src_module+" was sent to router "+this+" but the it was not in the list of modules handled by this router:\n  "+src_to_links.keySet());
            }
        ModuleLinks<EventType> src_links = src_deque.peekLast();
        src_links.queue.add(ev);
        if (!work_in_progress){  // This is the root event
            work_in_progress = true;
            process_head_queue();
            work_in_progress = false;
        }
    }

    @Override
    public void reset(){
        src_to_links.keySet().stream().forEach(mod -> mod.reset());
    }
}


abstract class BaseEavesdropper implements EventHandler{

    EventHandler current_handler;
    boolean enabled;

    public BaseEavesdropper(){
        this.enabled = true;
    }

    @Override
    public <EventType extends BaseEvent> void handle_event(EventType ev, EventModule<EventType> src_module) {

        if (enabled){
            eat_event(ev, src_module);
        }
        if (current_handler != null)
            current_handler.handle_event(ev, src_module);
    }

    public abstract <EventType extends BaseEvent> void eat_event(EventType ev, EventModule<EventType> src_module);

    public void set_enabled(boolean enabled){
        this.enabled = enabled;
    }

    public abstract void reset();

    public void set_handler(EventHandler handler){
        current_handler = handler;
    }
}


class Eavesdropper extends BaseEavesdropper{
    /* An event handler that you can sneak in between an EventModule and it's current handler.
    It will listen to events and pass them on.

    If you're going to make dirty hacks, might as well put them all in one class.
    This is that class.
    */

    List record;

    public Eavesdropper(){
        record = new ArrayList();
    }

    public List get_record(){
        return record;
    }

    @Override
    public void reset(){
        this.record.clear();
    }

    @Override
    public <EventType extends BaseEvent> void eat_event(EventType ev, EventModule<EventType> src_module) {
        if (ev instanceof VectorEvent) // HACK!!!  It's needed to make sure recorded events don't change when the weights they're referencing do.
            record.add(new VectorEvent(((VectorEvent)ev).vec, false));
        else
            record.add(ev);
    }
}


class SpikeCountingEavesdropper extends BaseEavesdropper{

    int count=0;

    public SpikeCountingEavesdropper() {
        super();
    }

    @Override
    public <EventType extends BaseEvent> void eat_event(EventType ev, EventModule<EventType> src_module) {
        count += 1;
    }

    @Override
    public void reset(){
        count = 0;
    }

    public int get_count(){
        return count;

    }
}
