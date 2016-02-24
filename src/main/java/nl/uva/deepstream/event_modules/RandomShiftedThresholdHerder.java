package nl.uva.deepstream.event_modules;

import java.util.Random;

/**
 * Created by peter on 2/24/16.
 */
public class RandomShiftedThresholdHerder extends VarQueueHerder{

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
    }
}
