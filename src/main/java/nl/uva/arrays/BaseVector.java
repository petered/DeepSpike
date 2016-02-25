package nl.uva.arrays;

/**
 * Created by peter on 2/22/16.
 */
public abstract class BaseVector implements IVector {

    final DType datatype;
    final int datalength;

    BaseVector(DType dtype, int length){
        this.datatype=dtype;
        this.datalength=length;
    }

    public BaseArray.DType dtype(){
        return datatype;
    }

    public int[] shape(){
        return new int[]{datalength};
    }

    @Override
    public int length(){
        return datalength;
    }

}
