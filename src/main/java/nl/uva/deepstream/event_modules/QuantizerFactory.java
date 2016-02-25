package nl.uva.deepstream.event_modules;

import nl.uva.arrays.VectorFactory;

import java.util.Random;

/**
 * Created by peter on 2/24/16.
 */
public class QuantizerFactory{

    public enum QuantizerTypes {HERDING, NORESET_HERDING, NORESET_RECT_HERDING, POISSON, POISSON_ZERO,
        RECT_HERDING, RANDRESET_HERDING, SCALED_HERDING, SCALED_RECT_HERDING, SCALED_NORESET_HERDING}


    public static PassThroughQuantizer make_quantizer(String implementation, int n_units, Random rng){

        return make_quantizer(QuantizerFactory.QuantizerTypes.valueOf(implementation.toUpperCase().replace("-", "_")), n_units, rng);
    }

    public static PassThroughQuantizer make_quantizer(QuantizerFactory.QuantizerTypes implementation, int n_units, Random rng){
        switch (implementation) {
            case HERDING:
                return new VarQueueHerder(n_units);
            case NORESET_HERDING:
                return new NonResettingVarQueueHerder(n_units);
            case NORESET_RECT_HERDING:
                return new NonResettingRectVarQueueHerder(n_units);
            case POISSON:
                return new PoissonHerder(n_units, false, rng);
            case POISSON_ZERO:
                return new PoissonHerder(n_units, true, rng);
            case RECT_HERDING:
                return new RectVarQueueHerder(n_units);
//            case SCALED_HERDING:
//                return new ScaledRoundingHerder(n_units, 4);
            case RANDRESET_HERDING:
                return new RandomShiftedThresholdHerder(n_units, rng);
            case SCALED_HERDING:
                return new ScaledRoundingHerder(n_units, VectorFactory.get_scale());
            case SCALED_RECT_HERDING:
                return new ScaledRectRoundingHerder(n_units, VectorFactory.get_scale());
            case SCALED_NORESET_HERDING:
                return new ScaledNoResetRoundingHerder(n_units, VectorFactory.get_scale());
            default:
                throw new UnsupportedOperationException("No implementation "+implementation);
        }
    }
}
