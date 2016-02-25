package nl.uva.arrays;

/**
 * Created by peter on 2/20/16.
 */
public interface IVector extends IArray<IVector>{

    IVector addiScalar(int index, int value);

    IVector addiScalar(int index, double value);

    IVector addiScalar(int index, float value);

//    T mul(IVector vec);
//
//    T mul(double val);
//
//    T mul(int val);

    IMatrix outer(IVector vec);

    BooleanVector gt(double value);

//    T dup();
//
//    T neg();  // WHYYYYY



    boolean elementGreaterThan(int index, double threshold);

    boolean elementGreaterThan(int index, float threshold);

    boolean elementGreaterThan(int index, int threshold);

    boolean elementLessThan(int index, double threshold);

    boolean elementLessThan(int index, float threshold);

    boolean elementLessThan(int index, int threshold);

    int argMax();

    double[] asDouble();

    float[] asFloat();

//    T assign(Number val);

//    T assign(IVector vec);

//    T mul(T arr);
//
//    T assign(T arr); // Why?

    IVector putScalar(int index, double val);

    IVector putScalar(int index, float val);

    IVector putScalar(int index, int val);

    float getFloat(int index);

}
