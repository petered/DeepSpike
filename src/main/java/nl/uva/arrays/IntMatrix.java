package nl.uva.arrays;

import java.util.Arrays;

/**
 * Created by peter on 2/20/16.
 */
public class IntMatrix extends BaseArray<IMatrix> implements IMatrix {

    final int[][] data;

    @Override
    public IMatrix dup() {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    public IntMatrix(int[][] data){
        super(DType.INT, new int[]{data.length, data[0].length});
        this.data=data;
    }

    @Override
    public IVector getRow(int index) {
        return new IntVector(data[index]);
    }

    @Override
    public IVector getColumn(int index) {
        int[] col = new int[this.shape()[0]];
        for (int i=0; i<this.shape()[0]; i++)
            col[i] = data[i][index];
        return new IntVector(col);
    }

    @Override
    public IntMatrix addi(IMatrix that) {
        assert Arrays.equals(this.shape(), that.shape());
        if (that.dtype() == DType.INT){
            IntMatrix that_double = (IntMatrix) that;
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
        if (that.dtype() == DType.INT){
            IntMatrix that_double = (IntMatrix) that;
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
        if (vec.dtype() == DType.INT){
            IntVector vec_double = (IntVector) vec;
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
    public IntMatrix neg() {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public IntMatrix mul(IMatrix arr) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public IntMatrix mul(double val) {
        int[][] new_data = new int[this.shape()[0]][this.shape()[1]];
        for (int i=0; i<this.shape()[0]; i++)
            for (int j=0; j<this.shape()[1]; j++)
                new_data[i][j] = this.data[i][j] * (int)val;
        return new IntMatrix(new_data);
    }

    @Override
    public IntMatrix mul(int val) {
        return mul((int) val);
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
            case INT:
                System.arraycopy(((IntVector) vec).data, 0, this.data, 0, this.data.length);
                return this;
            default:
                throw new UnsupportedOperationException("Method not yet implemented.");
        }
    }

    @Override
    public double[][] asDouble() { return PrimativeArrayConverters.int2double(data);}

    @Override
    public float[][] asFloat() { return PrimativeArrayConverters.int2float(data);}

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