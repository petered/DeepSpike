package nl.uva.arrays;

import java.util.Arrays;

public class FloatVector extends BaseVector{

    final public float[] data;

    public FloatVector(int length){
        this(new float[length]);
    }

    public FloatVector(float[] data){
        super(DType.FLOAT, data.length);
        this.data = data;
    }

    @Override
    public int length(){
        return data.length;
    }

    @Override
    public IVector addiScalar(int index, int value){data[index]+=value; return this;}

    @Override
    public IVector addiScalar(int index, double value){data[index]+=value; return this;}

    @Override
    public IVector addiScalar(int index, float value){data[index]+=value; return this;}

    @Override
    public IVector addi(IVector arr){
        assert (this.length() == arr.length());
        if (arr.dtype() == DType.FLOAT){
            FloatVector dvec = (FloatVector) arr;
            for (int i=0; i<arr.length(); i++)
                data[i] += dvec.data[i];
        }
        else
            throw new UnsupportedOperationException("Adding a "+this.dtype() + " to a " + arr.dtype() + " is not yet supported.");
        return this;
    }

    @Override
    public IVector subi(IVector arr){
        assert (this.length() == arr.length());
        if (arr.dtype() == DType.DOUBLE){
            FloatVector dvec = (FloatVector) arr;
            for (int i=0; i<arr.length(); i++)
                data[i] -= dvec.data[i];
        }
        else
            throw new UnsupportedOperationException("Adding a "+this.dtype() + " to a " + arr.dtype() + " is not yet supported.");
        return this;
    }

    @Override
    public FloatVector neg(){
        float[] negarr = new float[this.length()];
        for (int i=0; i<this.length(); i++)
            negarr[i] = - this.data[i];
        return new FloatVector(negarr);
    }

    @Override
    public FloatVector mul(IVector arr){
        assert (this.length() == arr.length());
        float[] mularr = new float[this.length()];
        switch (arr.dtype()) {
            case FLOAT:
                FloatVector dvec = (FloatVector) arr;
                for (int i = 0; i < arr.length(); i++)
                    mularr[i] = this.data[i] * dvec.data[i];
                return new FloatVector(mularr);
            case BOOLEAN:
                BooleanVector bvec = (BooleanVector) arr;
                for (int i = 0; i < arr.length(); i++)
                    mularr[i] = bvec.data[i] ? this.data[i] : 0;
                return new FloatVector(mularr);
            default:
                throw new UnsupportedOperationException("Adding a " + this.dtype() + " to a " + arr.dtype() + " is not yet supported.");
        }
    }

    @Override
    public FloatVector mul(double val){
        float float_val = (float) val;
        float[] mularr = new float[this.length()];
        for (int i=0; i<this.length(); i++)
            mularr[i] = this.data[i] * float_val;
        return new FloatVector(mularr);
    }

    @Override
    public FloatVector mul(int val) {
        return mul(val);
    }

    @Override
    public IVector div(double divisor) {
        float float_div = (float) divisor;
        float[] divarr = new float[this.length()];
        for (int i=0; i<this.length(); i++)
            divarr[i] = this.data[i] / float_div;
        return new FloatVector(divarr);
    }

    @Override
    public IMatrix outer(IVector that) {
        if (this.dtype() == that.dtype()) {
            FloatVector that_doublevec = (FloatVector) that;
            float[][] mat = new float[this.length()][that.length()];
            for (int i = 0; i < this.length(); i++)
                for (int j = 0; j < that.length(); j++)
                    mat[i][j] = this.data[i] * that_doublevec.data[j];
            return new FloatMatrix(mat);
        }
        else
            throw new UnsupportedOperationException("Adding a "+this.dtype() + " to a " + that.dtype() + " is not yet supported.");
    }

    @Override
    public BooleanVector gt(double value) {
        boolean[] vec = new boolean[this.length()];
        for (int i=0; i<this.length(); i++)
            vec[i] = this.data[i] > value;
        return new BooleanVector(vec);
    }

    @Override
    public FloatVector dup() {
        return new FloatVector(Arrays.copyOf(this.data, this.length()));
    }

    @Override
    public boolean elementGreaterThan(int index, double threshold) {return data[index] > threshold;}

    @Override
    public boolean elementGreaterThan(int index, float threshold) {return data[index] > threshold;}

    @Override
    public boolean elementGreaterThan(int index, int threshold) {return data[index] > threshold;}

    @Override
    public boolean elementLessThan(int index, double threshold) {return data[index] < threshold;}

    @Override
    public boolean elementLessThan(int index, float threshold) {return data[index] < threshold;}

    @Override
    public boolean elementLessThan(int index, int threshold) {return data[index] < threshold;}

    @Override
    public FloatVector assign(double val) {return assign((float) val);}

    @Override
    public FloatVector assign(float val) {Arrays.fill(data, val); return this;}

    @Override
    public FloatVector assign(int val) {return assign((double) val);}

    @Override
    public FloatVector assign(IVector that) {
        assert this.length()==that.length();
        if (that.dtype() == DType.FLOAT)
            System.arraycopy(((FloatVector)that).data, 0, this.data, 0, this.length());
        else
            throw new UnsupportedOperationException();
        return this;
    }

    @Override
    public FloatVector abs() {
        float[] newdata = new float[this.length()];
        for (int i=0; i<this.length(); i++)
            newdata[i] = Math.abs(data[i]);
        return this;
    }

    @Override
    public int argMax(){
        int ix = 0;
        double val = data[0];
        for (int i = 1; i<this.length(); i++)
            if (data[i]>val){
                val = data[i];
                ix = i;
            }
        return ix;
    }

//    @Override
//    public Dou assign(IArray that) {  // TODO: Get rid of this, don't know why I can't.
//        throw new UnsupportedOperationException("Not yet supported.");
//    }


    @Override
    public float getFloat(int index){
        return data[index];
    }

    @Override
    public double[] asDouble() {
        return PrimativeArrayConverters.float2double(data);
    }

    @Override
    public float[] asFloat() {
        return data;
    }

    @Override
    public IVector putScalar(int index, double val) {data[index] = (float)val; return this;}

    @Override
    public IVector putScalar(int index, float val) {data[index] = val; return this;}

    @Override
    public IVector putScalar(int index, int val) {data[index] = val; return this;}

}