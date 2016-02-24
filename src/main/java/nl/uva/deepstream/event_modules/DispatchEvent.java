package nl.uva.deepstream.event_modules;

/**
 * Created by peter on 2/24/16.
 */
public class DispatchEvent<EventType> implements Comparable<DispatchEvent>{
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
