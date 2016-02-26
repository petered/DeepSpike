package nl.uva.deepspike.events;

import nl.uva.arrays.IMatrix;

/**
 * Created by peter on 2/24/16.
 */
public class MatrixEvent{

    final public IMatrix matrix;

    public MatrixEvent(IMatrix matrix){
        this.matrix = matrix;
    }
}
