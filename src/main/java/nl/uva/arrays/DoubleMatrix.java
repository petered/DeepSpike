package nl.uva.arrays;

import java.util.Arrays;

/**
 * Created by peter on 2/20/16.
 */
public class DoubleMatrix extends BaseArray<IMatrix> implements IMatrix {

    final double[][] data;

    @Override
    public IMatrix dup() {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    public DoubleMatrix(double[][] data){
        super(DType.DOUBLE, new int[]{data.length, data[0].length});
        this.data=data;
    }

    @Override
    public IVector getRow(int index) {
        return new DoubleVector(data[index]);
    }

    @Override
    public IVector getColumn(int index) {
        double[] col = new double[this.shape()[0]];
        for (int i=0; i<this.shape()[0]; i++)
            col[i] = data[i][index];
        return new DoubleVector(col);
    }

    @Override
    public DoubleMatrix addi(IMatrix that) {
        assert Arrays.equals(this.shape(), that.shape());
        if (that.dtype() == DType.DOUBLE){
            DoubleMatrix that_double = (DoubleMatrix) that;
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
        if (that.dtype() == DType.DOUBLE){
            DoubleMatrix that_double = (DoubleMatrix) that;
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
        if (vec.dtype() == DType.DOUBLE){
            DoubleVector vec_double = (DoubleVector) vec;
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
    public DoubleMatrix neg() {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public DoubleMatrix mul(IMatrix arr) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public DoubleMatrix mul(double val) {
        double[][] new_data = new double[this.shape()[0]][this.shape()[1]];
        for (int i=0; i<this.shape()[0]; i++)
            for (int j=0; j<this.shape()[1]; j++)
                new_data[i][j] = this.data[i][j] * val;
        return new DoubleMatrix(new_data);
    }

    @Override
    public DoubleMatrix mul(int val) {
        return mul((double) val);
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
            case DOUBLE:
                System.arraycopy(((DoubleVector) vec).data, 0, this.data, 0, this.data.length);
                return this;
            default:
                throw new UnsupportedOperationException("Method not yet implemented.");
        }
    }

    @Override
    public double[][] asDouble() { return PrimativeArrayConverters.double2double(data);}

    @Override
    public float[][] asFloat() { return PrimativeArrayConverters.double2float(data);}

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
