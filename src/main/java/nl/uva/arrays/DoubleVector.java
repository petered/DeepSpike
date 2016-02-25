package nl.uva.arrays;

import java.util.Arrays;

public class DoubleVector extends BaseVector{

    final public double[] data;

    public DoubleVector(int length){
        this(new double[length]);
    }

    public DoubleVector(double[] data){
        super(DType.DOUBLE, data.length);
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
        if (arr.dtype() == DType.DOUBLE){
            DoubleVector dvec = (DoubleVector) arr;
            for (int i=0; i<arr.length(); i++)
                data[i] += dvec.data[i];
        }
        else
            throw new UnsupportedOperationException("Adding a "+arr.dtype() + " to a " + this.dtype() + " is not yet supported.");
        return this;
    }

    @Override
    public IVector subi(IVector arr){
        assert (this.length() == arr.length());
        if (arr.dtype() == DType.DOUBLE){
            DoubleVector dvec = (DoubleVector) arr;
            for (int i=0; i<arr.length(); i++)
                data[i] -= dvec.data[i];
        }
        else
            throw new UnsupportedOperationException("Subtracting a "+arr.dtype() + " from a " + this.dtype() + " is not yet supported.");
        return this;
    }

    @Override
    public DoubleVector neg(){
        double[] negarr = new double[this.length()];
        for (int i=0; i<this.length(); i++)
            negarr[i] = -this.data[i];
        return new DoubleVector(negarr);
    }

    @Override
    public DoubleVector mul(IVector arr){
        assert (this.length() == arr.length());
        double[] mularr = new double[this.length()];
        switch (arr.dtype()) {
            case DOUBLE:
                DoubleVector dvec = (DoubleVector) arr;
                for (int i = 0; i < arr.length(); i++)
                    mularr[i] = this.data[i] * dvec.data[i];
                return new DoubleVector(mularr);
            case BOOLEAN:
                BooleanVector bvec = (BooleanVector) arr;
                for (int i = 0; i < arr.length(); i++)
                    mularr[i] = bvec.data[i] ? this.data[i] : 0;
                return new DoubleVector(mularr);
            default:
                throw new UnsupportedOperationException("Multiplying a " + arr.dtype() + " by a " + this.dtype() + " is not yet supported.");
        }
    }

    @Override
    public DoubleVector mul(double val){
        if (val==1)
            return this;  // .dup()?
        else if (val==-1)
            return this.neg();
        else {
            double[] mularr = new double[this.length()];
            for (int i = 0; i < this.length(); i++)
                mularr[i] = this.data[i] * val;
            return new DoubleVector(mularr);
        }
    }

    @Override
    public DoubleVector mul(int val) {
        return mul(val);
    }

    @Override
    public IVector div(double divisor) {
        double[] divarr = new double[this.length()];
        for (int i=0; i<this.length(); i++)
            divarr[i] = this.data[i] / divisor;
        return new DoubleVector(divarr);
    }

    @Override
    public IMatrix outer(IVector that) {
        if (this.dtype() == that.dtype()) {
            DoubleVector that_doublevec = (DoubleVector) that;
            double[][] mat = new double[this.length()][that.length()];
            for (int i = 0; i < this.length(); i++)
                for (int j = 0; j < that.length(); j++)
                    mat[i][j] = this.data[i] * that_doublevec.data[j];
            return new DoubleMatrix(mat);
        }
        else
            throw new UnsupportedOperationException("Taking the outer product of a "+that.dtype() + " with a " + this.dtype() + " is not yet supported.");
    }

    @Override
    public BooleanVector gt(double value) {
        boolean[] vec = new boolean[this.length()];
        for (int i=0; i<this.length(); i++)
            vec[i] = this.data[i] > value;
        return new BooleanVector(vec);
    }

    @Override
    public DoubleVector dup() {
        return new DoubleVector(Arrays.copyOf(this.data, this.length()));
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
    public DoubleVector assign(double val) { Arrays.fill(data, val); return this;}

    @Override
    public DoubleVector assign(float val) {return assign((double) val);}

    @Override
    public DoubleVector assign(int val) {return assign((double) val);}

    @Override
    public DoubleVector assign(IVector that) {
        assert this.length()==that.length();
        if (that.dtype() == DType.DOUBLE)
            System.arraycopy(((DoubleVector)that).data, 0, this.data, 0, this.length());
        else
            throw new UnsupportedOperationException();
        return this;
    }

    @Override
    public DoubleVector abs() {
        double[] newdata = new double[this.length()];
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
        return (float)data[index];
    }

    @Override
    public double[] asDouble() {return PrimativeArrayConverters.double2double(data);}

    @Override
    public float[] asFloat() {return PrimativeArrayConverters.double2float(data);}

    @Override
    public IVector putScalar(int index, double val) {data[index] = val; return this;}

    @Override
    public IVector putScalar(int index, float val) {data[index] = val; return this;}

    @Override
    public IVector putScalar(int index, int val) {data[index] = val; return this;}

    @Override
    public String toString(){
        String start_string = super.toString();
        return start_string + ": " + Arrays.toString(data);
    }

}