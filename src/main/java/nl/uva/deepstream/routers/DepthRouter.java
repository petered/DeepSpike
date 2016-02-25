package nl.uva.deepstream.routers;

import nl.uva.deepstream.interfaces.EventModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by peter on 2/24/16.
 */
public class DepthRouter extends AccessibleRouter{
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

    public DepthRouter(){
//        System.out.println("Starting depth-router");
        src_to_consumers = new HashMap();
    }

    @Override
    public <EventType> void add_binding(EventModule<EventType> src_module, Consumer<EventType> method_reference) {
        assert src_module != null: "You tried to add a null pointer, fool.";
        if (!src_to_consumers.containsKey(src_module)){
            src_to_consumers.put(src_module, new ArrayList());
        }
        if (method_reference!=null)  // Note that we allow null method references for the case when we want to add modules without destinations.
            src_to_consumers.get(src_module).add(method_reference);
    }

    @Override
    public <EventType> void handle_event(EventType ev, Consumer<EventType> consumer) {
        consumer.accept(ev);
    }

    @Override
    public <EventType> void handle_event(EventType ev, EventModule<EventType> src_module) {
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
