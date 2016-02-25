package nl.uva.arrays;

import java.util.Arrays;

/**
 * Created by peter on 2/20/16.
 */
public class FloatMatrix extends BaseArray<IMatrix> implements IMatrix {

    final float[][] data;

    @Override
    public IMatrix dup() {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    public FloatMatrix(float[][] data){
        super(DType.FLOAT, new int[]{data.length, data[0].length});
        this.data=data;
    }

    @Override
    public IVector getRow(int index) {
        return new FloatVector(data[index]);
    }

    @Override
    public IVector getColumn(int index) {
        float[] col = new float[this.shape()[0]];
        for (int i=0; i<this.shape()[0]; i++)
            col[i] = data[i][index];
        return new FloatVector(col);
    }

    @Override
    public FloatMatrix addi(IMatrix that) {
        assert Arrays.equals(this.shape(), that.shape());
        if (that.dtype() == DType.FLOAT){
            FloatMatrix that_float = (FloatMatrix) that;
            for (int i=0; i<this.shape()[0]; i++)
                for (int j=0; j<this.shape()[1]; j++)
                    this.data[i][j] += that_float.data[i][j];
        }
        else
            throw new UnsupportedOperationException("Method not yet implemented.");
        return this;
    }

    @Override
    public IMatrix subi(IMatrix that) {
        assert Arrays.equals(this.shape(), that.shape());
        if (that.dtype() == DType.FLOAT){
            FloatMatrix that_float = (FloatMatrix) that;
            for (int i=0; i<this.shape()[0]; i++)
                for (int j=0; j<this.shape()[1]; j++)
                    this.data[i][j] -= that_float.data[i][j];
        }
        else
            throw new UnsupportedOperationException("Method not yet implemented.");
        return this;
    }

    @Override
    public IMatrix subiColumn(int index, IVector vec) {
        assert this.shape()[0] == vec.length();
        if (vec.dtype() == DType.FLOAT){
            FloatVector vec_float = (FloatVector) vec;
            for (int i=0; i<this.shape()[0]; i++)
                data[i][index] -= vec_float.data[i];
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
    public FloatMatrix neg() {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public FloatMatrix mul(IMatrix arr) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public FloatMatrix mul(double val) {
        float float_val = (float) val;
        float[][] new_data = new float[this.shape()[0]][this.shape()[1]];
        for (int i=0; i<this.shape()[0]; i++)
            for (int j=0; j<this.shape()[1]; j++)
                new_data[i][j] = this.data[i][j] * float_val;
        return new FloatMatrix(new_data);
    }

    @Override
    public FloatMatrix mul(int val) {
        return mul((float) val);
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
            case FLOAT:
                System.arraycopy(((FloatVector) vec).data, 0, this.data, 0, this.data.length);
                return this;
            default:
                throw new UnsupportedOperationException("Method not yet implemented.");
        }
    }

    @Override
    public double[][] asDouble() {
        return PrimativeArrayConverters.float2double(data);
    }

    @Override
    public float[][] asFloat() {
        return PrimativeArrayConverters.float2float(data);
    }

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
