package nl.uva.arrays;

import java.util.Arrays;

/**
 * Created by peter on 2/20/16.
 */
abstract class BaseArray<T extends IArray<T>> implements IArray<T>{

    final DType datatype;
    final int[] datashape;

    BaseArray(DType dtype, int[] shape){
        this.datatype=dtype;
        this.datashape=shape;
    }

    public BaseArray.DType dtype(){
        return datatype;
    }

    public int[] shape(){
        return this.datashape;
    }

}
