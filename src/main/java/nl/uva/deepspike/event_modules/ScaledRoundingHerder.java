package nl.uva.deepspike.event_modules;

import nl.uva.deepspike.events.SignedSpikeEvent;

/**
 * Created by peter on 2/24/16.
 */
public class ScaledRoundingHerder extends BaseQueueHerder{

    final float scale;
    final float scale_over_2;

    public ScaledRoundingHerder(int n_units, float scale) {
        super(n_units);
        this.scale = scale;
        this.scale_over_2 = scale/2;
//        System.out.println("Scale "+scale);
//        System.out.println("scale_over_2 "+scale_over_2);
    }

    @Override
    void create_spikes_from_unit(int unit_ix) {
        float val = phi.getFloat(unit_ix);
//        System.out.println("phi: "+phi.getFloat(unit_ix)+", thresh: "+this.scale_over_2);
        while(true){
//            System.out.println("val: "+val);
            SignedSpikeEvent ev;
            if (val > scale_over_2)
                ev = new SignedSpikeEvent(unit_ix, false);
            else if (val <= -scale_over_2)
                ev = new SignedSpikeEvent(unit_ix, true);
            else
                break;
            queue.add(new DispatchEvent(ev, Math.abs(val)));
            val -= scale * ev.get_sign();

        }
//        System.out.println("Out");
        phi.putScalar(unit_ix, val);
    }
}
