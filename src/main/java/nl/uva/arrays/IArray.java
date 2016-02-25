package nl.uva.arrays;

/**
 * Created by peter on 2/19/16.
 */

interface IArray <T extends IArray<T>>{

    enum DType {DOUBLE, FLOAT, INT, SHORT, BOOLEAN};

    T addi(T arr);

    T subi(T arr);

    DType dtype();

    int length();

    T neg();

    T mul(double val);

    T mul(int val);

    T div(double val);

    T mul(T arr);

    T abs();

    T dup();

    int[] shape();

    T assign(double val);

    T assign(float val);

    T assign(int val);

    T assign(T arr);

}
