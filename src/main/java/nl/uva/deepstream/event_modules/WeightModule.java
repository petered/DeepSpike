package nl.uva.deepstream.event_modules;

import nl.uva.arrays.IMatrix;
import nl.uva.arrays.IVector;
import nl.uva.deepstream.events.MatrixEvent;
import nl.uva.deepstream.events.SignedSpikeEvent;
import nl.uva.deepstream.events.SpikeEvent;
import nl.uva.deepstream.events.VectorEvent;

/**
 * Created by peter on 2/24/16.
 */
public class WeightModule extends StatelessBaseEventModule<VectorEvent>{

    IMatrix w;
    int n_in;
    int n_out;

    public WeightModule(IMatrix w) {
        this.w = w;
        this.n_in = w.shape()[0];
        this.n_out = w.shape()[1];
    }

    public void feed_event(SpikeEvent spike) {

        send_event(new VectorEvent(w.getRow(spike.src), true)); // CAREFUL!!! For now we say, safe to copy because we know this won't be modified before it's used.  May be a differnet story in the future.
    }

    public void feed_event(SignedSpikeEvent spike) {
        IVector weight = spike.is_negative? w.getRow(spike.src).neg() : w.getRow(spike.src);
        send_event(new VectorEvent(weight));
    }

    public void feed_update(MatrixEvent ev){
        // Gets a gradient estimate, already multiplied by eta.
        w.subi(ev.matrix);
    }

    public void feed_update(ColumnChangeEvent ev){
        w.subiColumn(ev.col_ix, ev.vec);
    }
}
