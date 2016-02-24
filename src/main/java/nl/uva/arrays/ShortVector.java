package nl.uva.arrays;

import java.util.Arrays;

public class ShortVector extends BaseVector{

    final public short[] data;

    public ShortVector(int length){
        this(new short[length]);
    }

    public ShortVector(short[] data){
        super(DType.SHORT, data.length);
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
        if (arr.dtype() == DType.SHORT){
            ShortVector dvec = (ShortVector) arr;
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
        if (arr.dtype() == DType.SHORT){
            ShortVector dvec = (ShortVector) arr;
            for (int i=0; i<arr.length(); i++)
                data[i] -= dvec.data[i];
        }
        else
            throw new UnsupportedOperationException("Subtracting a "+arr.dtype() + " from a " + this.dtype() + " is not yet supported.");
        return this;
    }

    @Override
    public ShortVector neg(){
        short[] negarr = new short[this.length()];
        for (int i=0; i<this.length(); i++)
            negarr[i] = (short)(-this.data[i]);
        return new ShortVector(negarr);
    }

    @Override
    public ShortVector mul(IVector arr){
        assert (this.length() == arr.length());
        short[] mularr = new short[this.length()];
        switch (arr.dtype()) {
            case SHORT:
                ShortVector dvec = (ShortVector) arr;
                for (int i = 0; i < arr.length(); i++)
                    mularr[i] = (short)(this.data[i] * dvec.data[i]);
                return new ShortVector(mularr);
            case BOOLEAN:
                BooleanVector bvec = (BooleanVector) arr;
                for (int i = 0; i < arr.length(); i++)
                    mularr[i] = bvec.data[i] ? this.data[i] : 0;
                return new ShortVector(mularr);
            default:
                throw new UnsupportedOperationException("Multiplying a " + arr.dtype() + " by a " + this.dtype() + " is not yet supported.");
        }
    }

    @Override
    public ShortVector mul(double val){
        if (val==1)
            return this;  // .dup()?
        else if (val==-1)
            return this.neg();
        else {
            short[] mularr = new short[this.length()];
            for (int i = 0; i < this.length(); i++)
                mularr[i] = (short)(this.data[i] * val);
            return new ShortVector(mularr);
        }
    }

    @Override
    public ShortVector mul(int val) {
        return mul(val);
    }

    @Override
    public IVector div(double divisor) {
        short[] divarr = new short[this.length()];
        for (int i=0; i<this.length(); i++)
            divarr[i] = (short) (this.data[i] / divisor);
        return new ShortVector(divarr);
    }

    @Override
    public IMatrix outer(IVector that) {
        if (this.dtype() == that.dtype()) {
            ShortVector that_doublevec = (ShortVector) that;
            short[][] mat = new short[this.length()][that.length()];
            for (int i = 0; i < this.length(); i++)
                for (int j = 0; j < that.length(); j++)
                    mat[i][j] = (short) (this.data[i] * that_doublevec.data[j]);
            return new ShortMatrix(mat);
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
    public ShortVector dup() {
        return new ShortVector(Arrays.copyOf(this.data, this.length()));
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
    public ShortVector assign(double val) { Arrays.fill(data, (short)val); return this;}

    @Override
    public ShortVector assign(float val) {return assign((short) val);}

    @Override
    public ShortVector assign(int val) {return assign((short) val);}

    @Override
    public ShortVector assign(IVector that) {
        assert this.length()==that.length();
        if (that.dtype() == DType.SHORT)
            System.arraycopy(((ShortVector)that).data, 0, this.data, 0, this.length());
        else
            throw new UnsupportedOperationException();
        return this;
    }

    @Override
    public ShortVector abs() {
        short[] newdata = new short[this.length()];
        for (int i=0; i<this.length(); i++)
            newdata[i] = data[i]>0 ? data[i] : (short)-data[i];
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
    public double[] asDouble() {return PrimativeArrayConverters.short2double(data);}

    @Override
    public float[] asFloat() {return PrimativeArrayConverters.short2float(data);}

    @Override
    public IVector putScalar(int index, double val) {data[index] = (short) val; return this;}

    @Override
    public IVector putScalar(int index, float val) {data[index] = (short) val; return this;}

    @Override
    public IVector putScalar(int index, int val) {data[index] = (short) val; return this;}

    @Override
    public String toString(){
        String start_string = super.toString();
        return start_string + ": " + Arrays.toString(data);
    }

}