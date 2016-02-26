package nl.uva.deepspike.event_modules;

import nl.uva.arrays.IVector;

/**
 * Created by peter on 2/24/16.
 */
public class ColumnChangeEvent{
    /*An event which sends a change in value to a row at a given index.*/

    public int col_ix;
    public IVector vec;

    ColumnChangeEvent(int row_ix, IVector vec){
        this(row_ix, vec, false);
    }

    ColumnChangeEvent(int row_ix, IVector vec, boolean safe){
        this.col_ix = row_ix;
        this.vec = safe ? vec.dup() : vec;
    }

}
