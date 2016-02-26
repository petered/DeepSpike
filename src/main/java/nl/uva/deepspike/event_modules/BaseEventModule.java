package nl.uva.deepspike.event_modules;

import nl.uva.deepspike.eavesdroppers.Eavesdropper;
import nl.uva.deepspike.routers.SwitchBoard;
import nl.uva.deepspike.interfaces.EventHandler;
import nl.uva.deepspike.interfaces.EventModule;

import java.util.List;

/**
 * Created by peter on 2/24/16.
 */
abstract class BaseEventModule<EventType> implements EventModule<EventType> {

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
