package nl.uva.deepstream.event_modules;

/**
 * Created by peter on 2/24/16.
 */
public class NonResettingRectVarQueueHerder extends RectVarQueueHerder {
    /* A version of the varqueueherder where phi never resets */

    public NonResettingRectVarQueueHerder(int n_units) {
        super(n_units);
    }

    @Override
    public void reset(){
    }
}
