package nl.uva.deepstream.arr;

//import java.util.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

/*
Event-Based Multi Layer Perceptron.


*/


interface Resettable{

    void reset();
}


interface EventModule<EventType> extends Resettable {

    void send_event(EventType event);

    void set_handler(EventHandler handler);


}

@FunctionalInterface
interface EventHandler{

    <EventType extends BaseEvent> void handle_event(EventType ev, EventModule<EventType> src_module);

}

interface Router extends EventHandler, Resettable {

    <EventType extends BaseEvent> void add_binding(EventModule<EventType> src_module, Consumer<EventType> method_reference);

    <EventType extends BaseEvent> void handle_event(EventType ev, Consumer<EventType> consumer);

}


class Nd4jExt{

    static INDArray addiScalar(INDArray arr, int index, float scalar){
        return arr.putScalar(index, arr.getFloat(index) + scalar);
    }

    static INDArray addiScalar(INDArray arr, int index, double scalar){
        return arr.putScalar(index, arr.getDouble(index) + scalar);
    }

    static INDArray subiScalar(INDArray arr, int index, float scalar){
        return arr.putScalar(index, arr.getFloat(index) - scalar);
    }

    static INDArray subiScalar(INDArray arr, int index, double scalar){
        return arr.putScalar(index, arr.getDouble(index) - scalar);
    }

    static double[][] to2dDouble(INDArray arr){
        assert arr.shape().length == 2;
        double[][] out = new double[arr.shape()[0]][arr.shape()[1]];
        for (int i=0; i<arr.shape()[0]; i++)
            for (int j=0; j<arr.shape()[1]; j++)
                out[i][j] = arr.getDouble(i, j);
        return out;
    }
}


abstract class AccessibleRouter implements Router {
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


abstract class BaseMLP{

    public double[][] predict(double[][] input_vecs){
        if (input_vecs.length==0)
            return new double[0][0];
        double[] first_out = predict_one(input_vecs[0]);
        double[][] output = new double[input_vecs.length][first_out.length];
        System.arraycopy(first_out, 0, output[0], 0, first_out.length);
        for (int i=1; i<input_vecs.length; i++)
            System.arraycopy(predict_one(input_vecs[i]), 0, output[i], 0, output[i].length);
        return output;
    }

    public abstract double[] predict_one(double [] vec);

    public void train(double[][] input_vecs, double[][] target_vecs){
        for (int i=0; i<input_vecs.length; i++)
            train_one(input_vecs[i], target_vecs[i]);
    }

    public abstract void train_one(double[] input_vec, double[] target_vec);

}


public class ArrSpikingMLP extends BaseMLP {

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

    public ArrSpikingMLP(double[][][] ws, int n_steps, double eta, boolean depth_first,
                      boolean fractional_updates, boolean queue_implementation, boolean return_counts,
                      boolean smooth_grads, String forward_discretize, String backward_discretize,
                      String test_discretize, int seed,
                      double regularization, boolean hold_error) {

        List<INDArray> weights = Arrays.stream(ws).map(Nd4j::create).collect(Collectors.toList());

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
            layers[i] = new QuantizingLayer(weights.get(i), eta, fractional_updates, n_steps,
                    forward_discretize, backward_discretize, test_discretize, rng, i==0, regularization);
        int input_size = weights.get(0).rows();
        int[] layer_sizes = weights.stream().mapToInt(w->w.columns()).toArray();

        // Fwd pass components
        input_node = new IdentityModule();
        input_summer = new VectorSumModule(input_size);
        output_summer = new VectorSumModule(layer_sizes[layers.length-1]);
        input_herder = new VarQueueHerder(input_size);
        neg_target_quantizer = new VarQueueHerder(layer_sizes[n_layers-1]);
        if (hold_error)
            error_counter = new CountModule(layer_sizes[layer_sizes.length-1]);
        training_router = create_training_router(depth_first);
        test_router = create_test_router(depth_first);
    }

    @Override
    public double[] predict_one(double[] input_vec) {

        SwitchBoard.global_switchboard.set_handler(test_router);
        test_router.reset();
        GlobalClock.reset();

        INDArray input_arr = Nd4j.create(input_vec);

        for (int t = 0; t < n_steps; t++) {
            GlobalClock.update_time(t);
            test_router.handle_event(new VectorEvent(input_arr), input_node::feed_event);
        }

        INDArray out_sum = output_summer.get_sum();
        INDArray output = out_sum.div((double) n_steps);
        return output.data().asDouble();
    }

    @Override
    public void train_one(double[] input_vec, double[] target_vec){

        INDArray input_arr = Nd4j.create(input_vec);
        INDArray neg_target_arr = Nd4j.create(target_vec).neg();
//        double[] neg_target_vec = new double[target_vec.length];
//        for (int i=0; i<target_vec.length; i++)
//            neg_target_vec[i] = -target_vec[i];

//        System.out.println("Targ Shape " + neg_target_arr.shape()[0] + " " + neg_target_arr.shape()[1]);

        SwitchBoard.global_switchboard.set_handler(training_router);
        training_router.reset();
        GlobalClock.reset();
        for (int t = 0; t < n_steps; t++) {
            GlobalClock.update_time(t);
            training_router.handle_event(new VectorEvent(input_arr), input_node::feed_event);
            training_router.handle_event(new VectorEvent(neg_target_arr), neg_target_quantizer::feed_event);
            if (hold_error)
                error_counter.launch_events();
        }

        if (!fractional_updates)
            for (QuantizingLayer lay: layers)
                ((OuterProductModule)(lay.weight_updater)).produce_event();  // We need to artificially for now.
    }

    private AccessibleRouter create_training_router(boolean depth_first){
        /*
        IS --+-> IG --> W[0] --> S[0] ---> G[0] ---> W[1] ---> S[1] --> G[1]
             |                   T[0]                          T[1]     |
             |                   F[0]<--BG[1]<--BS[1]<--WT[1]<-F[1]<----+--N<--
             |                    |                              |
             '----> OP[0] <-------'           S[0] --> OP[1] <---'
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
        IL ---> W[0] ----> L[0] ----> W[1] ----> L[1] ----> SC
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
    VectorThresholdModule thresh;
    ReverseWeightModule backward_weights;
    PassThroughQuantizer backward_herder;
    EventFilterModule<SignedSpikeEvent> back_filter;
    WeightGradientModule weight_updater;
    boolean first_layer;
    boolean fractional_updates;
    SpikeWeighterModule penalty;


    public QuantizingLayer(INDArray w, double eta, boolean fractional_updates,
                           int n_steps, String forward_discretize, String back_discretize, String test_discretize,
                           Random rng, boolean first_layer, double regularization){

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
        thresh = new VectorThresholdModule();
        if (!first_layer){  // No need for this if we're in the first layer.
            backward_weights = new ReverseWeightModule(w);
            backward_herder = QuantizerFactory.make_quantizer(back_discretize, n_in, rng);
        }
        back_filter = new EventFilterModule(n_out);
        weight_updater = fractional_updates?
                new IncrementalOuterProductModule(n_in, eta, n_steps, !first_layer) :
                new OuterProductModule(n_in, n_out, eta, n_steps, !first_layer);
    }

    public void bind_internals_for_training(Router r){
        /* Note that this does NOT bind anything to the input of the weight_updater */
        r.add_binding(forward_weights, pre_quant_summer::feed_event);
        r.add_binding(pre_quant_summer, thresh::feed_event);  // Important that this comes before next line
        r.add_binding(thresh, back_filter::feed_filter_event);
        r.add_binding(back_filter, weight_updater::feed_error_event);
        r.add_binding(forward_weights, forward_herder::feed_event);
        if (penalty != null){
            System.out.println("REGULARIZATIONS ADDAF");
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


abstract class StatelessBaseEventModule<EventType extends BaseEvent> extends BaseEventModule<EventType> {
    /* Started this because of bugs caused by forgetting to reset.  Now you have to explicitly not reset */

    @Override
    public void reset(){
    }

}


class IdentityModule<EventType extends BaseEvent> extends StatelessBaseEventModule<EventType> {
    // Why?  Useful as an input node, where you may want to distribute an input signal
    // to multiple nodes.  Distributing the input to the appropriate nodes in the
    // graph is then the concern of the graph-builder, rather than the input-feeder,

    public void feed_event(EventType ev){
        send_event(ev);
    }

}


class WeightModule extends StatelessBaseEventModule<VectorEvent> {

    INDArray w;
    int n_in;
    int n_out;

    public WeightModule(INDArray w) {
        this.w = w;
        this.n_in = w.shape()[0];
        this.n_out = w.shape()[1];
    }

    public void feed_event(SpikeEvent spike) {

//        System.out.println(w.getRow(spike.src).shape());

        send_event(new VectorEvent(w.getRow(spike.src), true)); // CAREFUL!!! For now we say, safe to copy because we know this won't be modified before it's used.  May be a differnet story in the future.
//        send_event(new VectorEvent(w[spike.src], true));  // CAREFUL!!! For now we say, safe to copy because we know this won't be modified before it's used.  May be a differnet story in the future.
    }

    public void feed_event(SignedSpikeEvent spike) {
        INDArray weight = spike.is_negative? w.getRow(spike.src).neg() : w.getRow(spike.src);
//        double[] weight = spike.is_negative? VectorNegationModule.negate(w[spike.src]):w[spike.src];
        send_event(new VectorEvent(weight));
    }

    public void feed_update(MatrixEvent ev){
        // Gets a gradient estimate, already multiplied by eta.

        w.subi(ev.matrix);
//        for (int i=0; i<n_in; i++){
//            //assert n_out == ev.matrix[i].length;
//
//            w.putColumn()
//
//            for (int j=0; j<n_out; j++){
//                w.[i][j] -= ev.matrix[i][j];
//            }
//        }
    }

    public void feed_update(ColumnChangeEvent ev){

        w.getColumn(ev.col_ix).subi(ev.vec.transpose());
//        w.subiColumnVector(ev.vec.transpose());
//        for (int i=0; i<n_in; i++)
//            w[i][ev.col_ix] -= ev.vec[i];
    }
}

class ReverseWeightModule extends StatelessBaseEventModule<VectorEvent> {

    int n_left;
    int n_right;
    INDArray w;

    ReverseWeightModule(INDArray w){
        this.w = w;
        n_left = w.shape()[0];
        n_right = w.shape()[1];
    }

    public void feed_event(SignedSpikeEvent spike) {

        INDArray weight = spike.is_negative? w.getColumn(spike.src).neg() : w.getColumn(spike.src);
//        double[] weight = new double[n_left];
//        if (spike.is_negative)
//            for (int i=0; i<n_left; i++)
//                weight[i] = -w[i][spike.src];
//        else
//            for (int i=0; i<n_left; i++)
//                weight[i] = w[i][spike.src];
        send_event(new VectorEvent(weight.transpose(), true));
    }
}


interface Quantizer extends EventModule<SignedSpikeEvent> {

    void feed_event(VectorEvent ev);

}

interface PassThroughQuantizer extends Quantizer {

    void feed_spike(SignedSpikeEvent spike);  // Shortcut method

}


class QuantizerFactory{

    public static PassThroughQuantizer make_quantizer(String implementation, int n_units, Random rng){
        switch (implementation) {
            case "herding":
                return new VarQueueHerder(n_units);
            case "noreset-herding":
                return new NonResettingVarQueueHerder(n_units);
            case "noreset-rect-herding":
                return new NonResettingRectVarQueueHerder(n_units);
            case "poisson":
                return new PoissonHerder(n_units, false, rng);
            case "poisson-zero":
                return new PoissonHerder(n_units, true, rng);
            case "rect-herding":
                return new RectVarQueueHerder(n_units);
            case "scaled-4":
                return new ScaledRoundingHerder(n_units, 4);
            case "rand-shift-herding":
                return new RandomShiftedThresholdHerder(n_units, rng);
            default:
                throw new UnsupportedOperationException("No implementation "+implementation);
        }
    }
}


abstract class BaseHerder<EventType extends BaseEvent> extends BaseEventModule<EventType> {

//    double[] phi;
    INDArray phi;

    BaseHerder(int n_units) {
        this.phi = Nd4j.create(n_units);
//        this.phi = new double[n_units];
    }

    public abstract void feed_event(VectorEvent ev);

    @Override
    public void reset() {
        this.phi.assign(0);
//        for (int i = 0; i < phi.length; i++)
//            this.phi[i] = 0;
    }
}


class DispatchEvent<EventType> implements Comparable<DispatchEvent>{
    /* For internal use by herding modules.  Indentifies both the event and the
    order in which it should be distpatched.
    */
    final public double order;
    final EventType ev;

    DispatchEvent(EventType ev, double order){
        this.order = order;
        this.ev = ev;
    }

    @Override
    public int compareTo(DispatchEvent o) {
        return Double.compare(o.order, order);  // Opposite of the phi... Head of a priority queue is the LOWEST element
    }
}


abstract class BaseQueueHerder extends BaseHerder<SignedSpikeEvent> implements PassThroughQuantizer {

    PriorityQueue<DispatchEvent<SignedSpikeEvent>> queue;

    public BaseQueueHerder(int n_units) {
        super(n_units);
        this.queue = new PriorityQueue();
    }

    @Override
    public void feed_event(VectorEvent ev){
        assert ev.vec.length() == phi.length();
        phi.addi(ev.vec);
//        for (int i=0; i<phi.length; i++)
//            phi[i] += ev.vec[i];
        herd_away();
    }

    void herd_away(){
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

        Nd4jExt.subiScalar(phi, spike.src, spike.get_sign());
//        phi[spike.src] -= spike.get_sign();
        create_spikes_from_unit(spike.src);
        flush_queue();
    }

}

class RectVarQueueHerder extends BaseQueueHerder implements Quantizer {

    public RectVarQueueHerder(int n_units) {
        super(n_units);
    }

    @Override
    public void create_spikes_from_unit(int unit_ix) {
        while(phi.getFloat(unit_ix) > 0.5){
            queue.add(new DispatchEvent(new SpikeEvent(unit_ix), phi.getFloat(unit_ix)));
            Nd4jExt.subiScalar(phi, unit_ix, 1);
//            phi[unit_ix] -= 1;

        }
    }

}

class VarQueueHerder extends BaseQueueHerder {

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
//        while(phi[unit_ix] > 0.5){  // Positive spikes, if any
//            queue.add(new DispatchEvent(new SignedSpikeEvent(unit_ix, false), phi[unit_ix]));
//            phi[unit_ix] -= 1;
//        }
//        while(phi[unit_ix] < -0.5){ // Negative spikes, if any
//            queue.add(new DispatchEvent(new SignedSpikeEvent(unit_ix, true), -phi[unit_ix]));
//            phi[unit_ix] += 1;
//        }
    }
}

class NonResettingVarQueueHerder extends VarQueueHerder {
    /* A version of the varqueueherder where phi never resets */

    public NonResettingVarQueueHerder(int n_units) {
        super(n_units);
    }

    @Override
    public void reset(){
    }

}


class NonResettingRectVarQueueHerder extends RectVarQueueHerder {
    /* A version of the varqueueherder where phi never resets */

    public NonResettingRectVarQueueHerder(int n_units) {
        super(n_units);
    }

    @Override
    public void reset(){
    }
}

class RandomShiftedThresholdHerder extends VarQueueHerder {

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
//            phi[i] = rng.nextDouble()-.5;
    }
}


class ScaledRoundingHerder extends BaseQueueHerder {

    final double sensitivity;

    public ScaledRoundingHerder(int n_units, double scale) {
        super(n_units);
        this.sensitivity=1d/scale;
    }

    @Override
    void create_spikes_from_unit(int unit_ix) {
        float val = phi.getFloat(unit_ix);
        while(true){
            SignedSpikeEvent ev;
            if (val > sensitivity/2)
                ev = new SignedSpikeEvent(unit_ix, false);
            else if (val <= -sensitivity/2)
                ev = new SignedSpikeEvent(unit_ix, true);
            else
                break;
            int sign = ev.is_negative?-1:1;
            queue.add(new DispatchEvent(ev, Math.abs(val)));
            val -= sign*sensitivity;
        }
        phi.putScalar(unit_ix, val);
    }
}


class PoissonHerder extends BaseQueueHerder {

    Random rng;
//    double[] thresholds;
    INDArray thresholds;
    final boolean zero_mode;

    public PoissonHerder(int n_units, boolean zero_mode, Random rng) {
        super(n_units);
        this.rng = rng;
        this.zero_mode = zero_mode;
        thresholds = Nd4j.create(n_units);

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

    public double next_random_threshold(){
        return -Math.log(1-rng.nextDouble());  // Divide by lambda (but that's just 1)
    }

    @Override
    public void reset(){
        super.reset();

        for (int i=0; i<thresholds.length(); i++)
            thresholds.putScalar(i, next_random_threshold());
//            thresholds[i] = next_random_threshold();
    }
}


abstract class BaseArgmaxHerder<EventType extends BaseEvent> extends BaseHerder<EventType> {

    public BaseArgmaxHerder(int n_units) {
        super(n_units);
    }

    void add_vec(INDArray vec){
        assert phi.length() == vec.length(): "phi length: "+phi.length()+" must match input length: "+vec.length();
        phi.addi(vec);
//        for (int i=0; i<vec.length; i++){
//            phi[i]+=vec[i];
//        }
    }

    public static int argmax(double[] vec){
        int winner = 0;
        for (int i = 1; i < vec.length; i++) {
            if (vec[i] > vec[winner]) {
                winner = i;
            }
        }
        return winner;
    }

    @Override
    public void feed_event(VectorEvent vec_event) {
        add_vec(vec_event.vec);
        herd_away();
    }

    abstract void herd_away();

}


class VarArgmaxHerder extends BaseArgmaxHerder<SignedSpikeEvent> implements Quantizer {

    public VarArgmaxHerder(int n_units) {
        super(n_units);
    }

    @Override
    void herd_away(){
        while (true) {
            int winner = Nd4j.argMax(phi, 0).getInt(0);

            float val = phi.getFloat(winner);
//            int winner = argmax(phi);
//            int winner = phi.argmax
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

class BiVarArgmaxHerder extends BaseArgmaxHerder<SignedSpikeEvent> implements PassThroughQuantizer {

    public BiVarArgmaxHerder(int n_units) {
        super(n_units);
    }

    @Override
    void herd_away(){
        // Note: inefficiency here... better to just look once for threshold crossers.
        while (true) {
            INDArray absphi = Transforms.abs(phi);
            int winner = Nd4j.argMax(absphi, 0).getInt(0);
            float val = phi.getFloat(winner);
            if (Math.abs(val) > 0.5) {
                boolean is_negative = val < 0;
                val -= is_negative ? -1 : 1;
                send_event(new SignedSpikeEvent(winner, is_negative));
            } else {
                break;
            }
            phi.putScalar(winner, val);
//            double[] absphi = absvec(phi);
//            int winner = argmax(absphi);
//            if (absphi[winner] > 0.5) {
//                boolean is_negative = phi[winner] < 0;
//                phi[winner] -= is_negative ? -1 : 1;
//                send_event(new SignedSpikeEvent(winner, is_negative));
//            } else {
//                break;
//            }
        }
    }

//    public static double[] absvec(double[] vec){
//        double[] newvec = new double[vec.length];
//        for(int i=0; i<vec.length; i++)
//            newvec[i] = Math.abs(vec[i]);
//        return newvec;
//    }

    @Override
    public void feed_spike(SignedSpikeEvent spike){
        send_event(spike);
    }
}


class CountModule extends BaseEventModule<SignedSpikeEvent> {

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
        counts[e.src]+=e.is_negative?-1:1;
    }

    public int[] get_counts(){
        return counts;
    }

    public double[] get_historical_mean(){
        double doubleal = (double) GlobalClock.time+1;
        double[] dist = new double[n_srcs];
        for (int i=0; i<n_srcs; i++)
            dist[i] = counts[i]/doubleal;
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


interface WeightGradientModule<EventType extends BaseEvent> extends EventModule<EventType> {

    void feed_error_event(SignedSpikeEvent event);

    void feed_error_event(WeightedSpikeEvent event);

    void feed_input_sum_event(VectorEvent event);

    void feed_input_event(SpikeEvent event);

    void feed_input_event(SignedSpikeEvent event);

}


class OuterProductModule extends BaseEventModule<MatrixEvent> implements WeightGradientModule<MatrixEvent> {
    /* Accumulates input/output histograms, returns outer-product on command. */

    INDArray input_vec;
    INDArray output_counts;
//    double[] input_vec;
//    double[] output_counts;
    final boolean rectify_input;
    double eta;
    final int n_in;
    final int n_out;
    final int n_steps;

    OuterProductModule(int n_in, int n_out, double eta, int n_steps, boolean rectify_input){

//        input_vec = new double[n_in];
//        output_counts = new double[n_out];
        input_vec = Nd4j.create(n_in);
        output_counts = Nd4j.create(n_out);
        this.eta = eta;
        this.n_in = n_in;
        this.n_out = n_out;
        this.n_steps = n_steps;
        this.rectify_input = rectify_input;
    }

    public void feed_error_event(SignedSpikeEvent event){
        Nd4jExt.addiScalar(output_counts, event.src, event.get_sign());
//        output_counts[event.src]+=event.is_negative?-1:1;
    }

    public void feed_input_sum_event(VectorEvent event){
        assert input_vec.length() == event.vec.length();
        input_vec.assign(event.vec);
//        System.arraycopy(event.vec, 0, input_vec, 0, input_vec.length);
    }

    public void feed_input_event(SpikeEvent event){
        Nd4jExt.addiScalar(input_vec, event.src, 1);
//        input_vec[event.src]+=1;
    }

    public void feed_input_event(SignedSpikeEvent event){
        Nd4jExt.addiScalar(input_vec, event.src, event.get_sign());
//        input_vec[event.src]+=event.is_negative?-1:1;
    }

    public void produce_event(){

        double multiplier = eta / (n_steps*n_steps);
        INDArray update = (input_vec.mul(input_vec.gt(0))).reshape(n_in, 1).mmul((output_counts).mul(multiplier).reshape(1, n_out));

//        double[][] outer_prod = new double[n_in][n_out];
//        for (int i=0; i<n_in; i++)
//            for (int j=0; j<n_out; j++){
//                double input = rectify_input ? (input_vec[i]>0?input_vec[i]:0): input_vec[i];
//                outer_prod[i][j] = input * output_counts[j] * multiplier;
//            }
        send_event(new MatrixEvent(update));
    }

    @Override
    public void reset(){
        input_vec.assign(0);
        output_counts.assign(0);

//        Arrays.fill(input_vec, 0);
//        Arrays.fill(output_counts, 0);
    }

    @Override
    public void feed_error_event(WeightedSpikeEvent event) {

        Nd4jExt.addiScalar(output_counts, event.src, event.weight);
//        output_counts[event.src]+=event.weight;



    }
}


class IncrementalOuterProductModule extends BaseEventModule<ColumnChangeEvent> implements WeightGradientModule<ColumnChangeEvent> {
    /* Just sends a delta-column to the weight matrix for every error event that comes back */

    INDArray input_vec;
    double eta;
    int n_steps;
    final boolean rectify_input;

    IncrementalOuterProductModule(int n_in, double eta, int n_steps, boolean rectify_input){
        input_vec = Nd4j.create(n_in);
        this.eta = eta;
        this.n_steps = n_steps;
        this.rectify_input = rectify_input;
    }

    private void handle_weighted_error_event(int src, double weight){

        double multiplier = weight * eta / (n_steps*n_steps);
        INDArray delta_vec = input_vec.mul(multiplier).mul(input_vec.gt(0));

//        double[] delta_vec = new double[input_vec.length];
//        for (int i=0; i<input_vec.length; i++){
//            double input = rectify_input ? (input_vec[i]>0?input_vec[i]:0): input_vec[i];
//            delta_vec[i] = input*multiplier;
//        }
        send_event(new ColumnChangeEvent(src, delta_vec, true));
    }

    @Override
    public void feed_error_event(SignedSpikeEvent event){
        handle_weighted_error_event(event.src, event.is_negative?-1:1);
    }

    @Override
    public void feed_input_sum_event(VectorEvent event){
        input_vec.assign(event.vec);
//        System.arraycopy(event.vec, 0, input_vec, 0, input_vec.length());
    }

    @Override
    public void feed_input_event(SpikeEvent event){
        Nd4jExt.addiScalar(input_vec, event.src, 1);
//        input_vec.putScalar(event.src, input_vec.getFloat(event.src) + 1);
//        input_vec[event.src]+=1;
    }

    @Override
    public void feed_input_event(SignedSpikeEvent event){
        Nd4jExt.addiScalar(input_vec, event.src, event.get_sign());
//        input_vec[event.src]+=event.is_negative?-1:1;
    }

    @Override
    public void reset(){
        input_vec.assign(0);
//        Arrays.fill(input_vec, 0.);
    }

    @Override
    public void feed_error_event(WeightedSpikeEvent event) {
        handle_weighted_error_event(event.src, event.weight);
    }



}


class EventFilterModule<EventType extends BaseSpikeEvent> extends BaseEventModule<EventType> {

    int n_srcs;
//    boolean[] filters;
    INDArray filters;

    EventFilterModule(int n_srcs){
        this.n_srcs = n_srcs;
        this.filters = Nd4j.create(n_srcs);
//        this.filters = new boolean[n_srcs];  // Defaults to false!
        reset();  // Filters now start at true;
    }

    public void feed_filter_event(BooleanVectorEvent ev){

        filters.assign(ev.vec);
//        System.arraycopy(ev.vec, 0, filters, 0, filters.length);
    }

    public void feed_event(EventType ev){

        if (filters.getInt(ev.src) != 0)
            send_event(ev);


//        if (filters[ev.src])
//            send_event(ev);  // Note - it just forwards the instance and doesn't copy.  Which I guess is ok, as long as events are immutable.
    }

    @Override
    public void reset(){
        filters.assign(1);
//        Arrays.fill(filters, true);
    }
}


class VectorSumModule extends BaseEventModule<VectorEvent> {

    final public INDArray sum;
    boolean send_events;

    VectorSumModule(int n_units){
        this(n_units, true);
    }

    VectorSumModule(int n_units, boolean send_events){
        this.sum = Nd4j.create(n_units);
        this.send_events = send_events;
        reset();
    }

    public INDArray get_sum(){
        return sum;
    }

    public void feed_event(VectorEvent ev){
        sum.addi(ev.vec);
//        for (int i=0; i<ev.vec.length; i++){
//            sum[i] += ev.vec[i];
//        }
        if (send_events)
            send_event(new VectorEvent(sum));
        // NOTE: Kinda dangerous because it exports the state vector.
        // Could copy it, but that can be wasteful.  Is there some way to lock it?
    }

    public void feed_event(SpikeEvent ev){
        sum.putScalar(ev.src, sum.getFloat(ev.src) + 1);
//        this.sum[ev.src]+=1;
    }

    public void feed_event(SignedSpikeEvent ev){
        sum.putScalar(ev.src, sum.getFloat(ev.src) + (ev.is_negative?-1:1));
//        this.sum[ev.src]+=ev.is_negative?-1:1;
    }

    @Override
    public void reset(){
        sum.assign(0);
//        for (int i=0; i<sum.length; i++)
//            sum[i] = 0;
    }
}


class SpikeWeighterModule extends StatelessBaseEventModule<WeightedSpikeEvent> {

    public double weight;

    public SpikeWeighterModule(double weight){
        this.weight = weight;
    }

    public void feed_event(SignedSpikeEvent ev){
        send_event(new WeightedSpikeEvent(ev.src, ev.is_negative?-weight:weight));
    }
}


class VectorThresholdModule extends StatelessBaseEventModule<BooleanVectorEvent> {

    public void feed_event(VectorEvent ev){

        INDArray exceeders = ev.vec.gt(0);
//        boolean[] exceeders = new boolean[ev.vec.length];
//        for (int i=0; i<ev.vec.length; i++)
//            exceeders[i] = ev.vec[i] >= 0;
//        boolean[] exceeders = new boolean[ev.vec.length];
//        for (int i=0; i<ev.vec.length; i++)
//            exceeders[i] = ev.vec[i] >= 0;
        send_event(new BooleanVectorEvent(exceeders, true));
    }
}

class VectorNegationModule extends StatelessBaseEventModule<VectorEvent> {

    public void feed_event(VectorEvent ev){
        send_event(new VectorEvent(ev.vec.neg()));
//        send_event(new VectorEvent(negate(ev.vec)));
    }

    public static double[] negate(double[] vec){
        double[] v_neg = new double[vec.length];
        for (int i = 0; i < vec.length; i++)
            v_neg[i] = -vec[i];
        return v_neg;
    }
}


class BaseEvent{

    public final int time;

    BaseEvent() {
        this.time = GlobalClock.time;
    }
}


class BaseSpikeEvent extends BaseEvent {

    public final int src;

    public BaseSpikeEvent(int src) {
        this.src = src;
    }
}


class SpikeEvent extends SignedSpikeEvent {
    /* This exists purely to avoid accidently treating SignedSpikeEvents as SpikeEvents */
    public SpikeEvent (int src){
        super(src, false);
    }
}


class SignedSpikeEvent extends BaseSpikeEvent {

    public final boolean is_negative;

    public SignedSpikeEvent(int src, boolean is_negative) {
        super(src);
        this.is_negative = is_negative;
    }

    public int get_sign(){
        return is_negative ? -1 : 1;
    }
}

class WeightedSpikeEvent extends BaseSpikeEvent {

    final double weight;

    public WeightedSpikeEvent(int src, double weight) {
        super(src);
        this.weight = weight;
    }
}

class VectorEvent extends BaseEvent {

    final public INDArray vec;
//    final public double[] vec;

    public VectorEvent(INDArray vec) {
        this(vec, false);
    }

    public VectorEvent(INDArray vec, boolean safe){
        // Set safe to true if you're certain that the array won't be modified later and you want to save time/memory.
        this.vec = safe ? vec.dup() : vec;
//        this.vec = safe ? vec : Arrays.copyOf(vec, vec.length);

    }

}

class BooleanVectorEvent extends BaseEvent {

    final public INDArray vec;

    public BooleanVectorEvent(INDArray vec) {
        this(vec, false);
    }

    public BooleanVectorEvent(INDArray vec, boolean safe){
        // TODO: Maybe remove this event type, or somehow assert boolean data without checking every element.
        this.vec = safe ? vec.dup() : vec;
    }
}


class MatrixEvent extends BaseEvent {

    final public INDArray matrix;

    public MatrixEvent(INDArray matrix){
        assert matrix.shape().length == 2;
        this.matrix = matrix;
    }
}


class ColumnChangeEvent extends BaseEvent {
    /*An event which sends a change in value to a row at a given index.*/

    public int col_ix;
    public INDArray vec;

    ColumnChangeEvent(int row_ix, INDArray vec){
        this(row_ix, vec, false);
    }

    ColumnChangeEvent(int row_ix, INDArray vec, boolean safe){
        this.col_ix = row_ix;
        this.vec = safe ? vec.dup() : vec;
    }

}


class SwitchBoard implements EventHandler {

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


class DepthRouter extends AccessibleRouter {
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
        System.out.println("Starting depth-router");
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
//        System.out.println("Event from module "+src_module);
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


class BreadthRouter extends AccessibleRouter {
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
        System.out.println("Starting Breadth-router");
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
//        System.out.println("Module: " + links.src);
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


abstract class BaseEavesdropper implements EventHandler {

    EventHandler current_handler;
    boolean enabled;
    Consumer consumer;

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


class Eavesdropper extends BaseEavesdropper {
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


class SpikeCountingEavesdropper extends BaseEavesdropper {

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
