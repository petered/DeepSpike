package nl.uva.arrays;

import java.util.Arrays;

/**
 * Created by peter on 2/20/16.
 */
public class ShortMatrix extends BaseArray<IMatrix> implements IMatrix {

    final short[][] data;

    @Override
    public IMatrix dup() {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    public ShortMatrix(short[][] data){
        super(DType.SHORT, new int[]{data.length, data[0].length});
        this.data=data;
    }

    @Override
    public IVector getRow(int index) {
        return new ShortVector(data[index]);
    }

    @Override
    public IVector getColumn(int index) {
        short[] col = new short[this.shape()[0]];
        for (int i=0; i<this.shape()[0]; i++)
            col[i] = data[i][index];
        return new ShortVector(col);
    }

    @Override
    public ShortMatrix addi(IMatrix that) {
        assert Arrays.equals(this.shape(), that.shape());
        if (that.dtype() == DType.SHORT){
            ShortMatrix that_double = (ShortMatrix) that;
            for (int i=0; i<this.shape()[0]; i++)
                for (int j=0; j<this.shape()[1]; j++)
                    this.data[i][j] += that_double.data[i][j];
        }
        else
            throw new UnsupportedOperationException("Method not yet implemented.");
        return this;
    }

    @Override
    public IMatrix subi(IMatrix that) {
        assert Arrays.equals(this.shape(), that.shape());
        if (that.dtype() == DType.SHORT){
            ShortMatrix that_double = (ShortMatrix) that;
            for (int i=0; i<this.shape()[0]; i++)
                for (int j=0; j<this.shape()[1]; j++)
                    this.data[i][j] -= that_double.data[i][j];
        }
        else
            throw new UnsupportedOperationException("Method not yet implemented.");
        return this;
    }

    @Override
    public IMatrix subiColumn(int index, IVector vec) {
        assert this.shape()[0] == vec.length();
        if (vec.dtype() == DType.SHORT){
            ShortVector vec_double = (ShortVector) vec;
            for (int i=0; i<this.shape()[0]; i++)
                data[i][index] -= vec_double.data[i];
        }
        else
            throw new UnsupportedOperationException("Method not yet implemented.");

        return this;
    }

    @Override
    public int length() {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public ShortMatrix neg() {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public ShortMatrix mul(IMatrix arr) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public ShortMatrix mul(double val) {
        short[][] new_data = new short[this.shape()[0]][this.shape()[1]];
        for (int i=0; i<this.shape()[0]; i++)
            for (int j=0; j<this.shape()[1]; j++)
                new_data[i][j] = (short)(this.data[i][j] * val);
        return new ShortMatrix(new_data);
    }

    @Override
    public ShortMatrix mul(int val) {
        return mul((short) val);
    }

    @Override
    public int rows() {
        return this.shape()[0];
    }

    @Override
    public int columns() {
        return this.shape()[1];
    }

    @Override
    public IMatrix putiRow(int index, IVector vec) {
        switch (vec.dtype()){
            case SHORT:
                System.arraycopy(((ShortVector) vec).data, 0, this.data, 0, this.data.length);
                return this;
            default:
                throw new UnsupportedOperationException("Method not yet implemented.");
        }
    }

    @Override
    public double[][] asDouble() { return PrimativeArrayConverters.short2double(data);}

    @Override
    public float[][] asFloat() { return PrimativeArrayConverters.short2float(data);}

    @Override
    public IMatrix abs() {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public IMatrix assign(IMatrix arr) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public IMatrix div(double val) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public IMatrix assign(double val) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public IMatrix assign(float val) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public IMatrix assign(int val) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }
}