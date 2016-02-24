package nl.uva.arrays;

import com.sun.org.apache.xpath.internal.operations.Bool;

import java.util.Arrays;

/**
 * Created by peter on 2/20/16.
 */

public class BooleanVector extends BaseVector{

    boolean[] data;

    public BooleanVector(int n_elements){
        this(new boolean[n_elements]);
    }

    public BooleanVector(boolean[] data){
        super(DType.BOOLEAN, data.length);
        this.data = data;
    }

    public boolean getBoolean(int index){
        return data[index];
    }

    public BooleanVector assign(boolean val){
        Arrays.fill(data, val);
        return this;
    }

    @Override
    public BooleanVector dup() {
        return new BooleanVector(Arrays.copyOf(data, data.length));
    }

    @Override
    public BooleanVector assign(IVector that){
        if (that.dtype()==DType.BOOLEAN) {
            assert this.length() == that.length();
            BooleanVector bool_that = (BooleanVector) that;
            System.arraycopy(bool_that.data, 0, this.data, 0, this.length());
            return this;
        }
        else
            throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public IVector addiScalar(int index, int value) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public IVector addiScalar(int index, double value) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public IVector addiScalar(int index, float value) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public IMatrix outer(IVector vec) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public BooleanVector gt(double value) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public boolean elementGreaterThan(int index, double threshold) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public boolean elementGreaterThan(int index, float threshold) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public boolean elementGreaterThan(int index, int threshold) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public boolean elementLessThan(int index, double threshold) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public boolean elementLessThan(int index, float threshold) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public boolean elementLessThan(int index, int threshold) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public int length() {
        return data.length;
    }

    @Override
    public IVector neg() {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public BooleanVector mul(IVector arr) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public IVector mul(double val) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public IVector mul(int val) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public int argMax() {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public double[] asDouble() {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public float[] asFloat() {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public float getFloat(int index) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public IVector addi(IVector arr) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public IVector subi(IVector arr) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public IVector abs() {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public IVector div(double val) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public IVector assign(double val) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public IVector assign(float val) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public IVector assign(int val) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public IVector putScalar(int index, double val) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public IVector putScalar(int index, float val) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }

    @Override
    public IVector putScalar(int index, int val) {
        throw new UnsupportedOperationException("Method not yet implemented.");    }
}
