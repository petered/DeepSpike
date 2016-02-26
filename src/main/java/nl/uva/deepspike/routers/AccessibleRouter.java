package nl.uva.deepspike.routers;

import nl.uva.deepspike.interfaces.EventModule;
import nl.uva.deepspike.interfaces.Router;

import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Created by peter on 2/24/16.
 */
public abstract class AccessibleRouter implements Router {
    /*This is a handy base-implementation of Router, which allows you to name modules that you add*/

    HashMap<String, EventModule> name_src_map;

    public AccessibleRouter(){
        name_src_map = new HashMap();
    }

    public <EventType> void add_binding(EventModule<EventType> src_module, Consumer<EventType> method_reference, String src_name){
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
