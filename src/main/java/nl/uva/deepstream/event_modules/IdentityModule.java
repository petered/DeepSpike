package nl.uva.deepstream.event_modules;

/**
 * Created by peter on 2/24/16.
 */
public class IdentityModule<EventType> extends StatelessBaseEventModule<EventType> {
    // Why?  Useful as an input node, where you may want to distribute an input signal
    // to multiple nodes.  Distributing the input to the appropriate nodes in the
    // graph is then the concern of the graph-builder, rather than the input-feeder,

    public void feed_event(EventType ev){
        send_event(ev);
    }

}
