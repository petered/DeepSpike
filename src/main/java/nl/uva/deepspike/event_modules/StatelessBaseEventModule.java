package nl.uva.deepspike.event_modules;

/**
 * Created by peter on 2/24/16.
 */
abstract class StatelessBaseEventModule<EventType> extends BaseEventModule<EventType> {
    /* Started this because of bugs caused by forgetting to reset.  Now you have to explicitly not reset */

    @Override
    public void reset(){
    }

}
