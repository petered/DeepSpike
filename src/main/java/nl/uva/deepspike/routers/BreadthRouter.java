package nl.uva.deepspike.routers;

import nl.uva.deepspike.interfaces.EventModule;

import java.util.*;
import java.util.function.Consumer;

/**
 * Created by peter on 2/24/16.
 */
public class BreadthRouter extends AccessibleRouter{
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

    Map<EventModule, BreadthRouter.ModuleLinks> src_to_links;
    private Deque<BreadthRouter.ModuleLinks> src_deque; // If this is a stack instead is this then just a DepthRouter?
    boolean work_in_progress;

    public BreadthRouter(){
//        System.out.println("Starting Breadth-router");
        src_to_links = new HashMap();
        work_in_progress = false;
    }

    @Override
    public void print_connectivity() {
        System.out.println("== Router "+this+" Connectivity ==");
        for (BreadthRouter.ModuleLinks ml: src_to_links.values()){
            System.out.println("Module "+ml.src+" links to " + ml.consumers.size());
        }
        System.out.println("===================================");
    }

    class ModuleLinks<EventType>{
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
    public <EventType> void add_binding(EventModule<EventType> src_module, Consumer<EventType> method_reference) {
        assert src_module != null: "You tried to add a null pointer, fool.";
        if (!src_to_links.containsKey(src_module))
            src_to_links.put(src_module, new BreadthRouter.ModuleLinks(src_module));
        if (method_reference!=null)  // Note that we allow null method references for the case when we want to add modules without destinations.
            src_to_links.get(src_module).add_binding(method_reference);
    }

    @Override
    public <EventType> void handle_event(EventType ev, Consumer<EventType> consumer) {
        work_in_progress = true;
        consumer.accept(ev);
        process_head_queue();
        work_in_progress = false;
    }

    private <EventType> void process_head_queue(){
        BreadthRouter.ModuleLinks<EventType> links = src_deque.pollLast();
        if (links == null)
            return;
        links.consumers.stream().forEach(consumer -> links.queue.stream().forEach(consumer::accept));  // Haha!  Totally understantable!  But seriously.  It means send each event in the queue to each consumer
        links.queue.clear();
        process_head_queue();  // Could also avoid this with a while loop in feed_event, but who doesn't like tail recursion!
    }

    @Override
    public <EventType> void handle_event(EventType ev, EventModule<EventType> src_module) {
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
        BreadthRouter.ModuleLinks<EventType> src_links = src_deque.peekLast();
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
