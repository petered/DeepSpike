package nl.uva.deepspike.interfaces;

/**
 * Created by peter on 2/24/16.
 */
public interface EventModule<EventType> extends Resettable {

    void send_event(EventType event);

    void set_handler(EventHandler handler);

}
