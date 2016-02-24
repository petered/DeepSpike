package nl.uva.deepstream.event_modules;

/**
 * Created by peter on 2/24/16.
 */
public class ScaledNoResetRoundingHerder extends ScaledRoundingHerder {

    public ScaledNoResetRoundingHerder(int n_units, float scale){
        super(n_units, scale);
    }

    @Override
    public void reset(){
    }
}
