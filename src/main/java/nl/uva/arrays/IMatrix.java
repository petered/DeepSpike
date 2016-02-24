package nl.uva.arrays;

/**
 * Created by peter on 2/19/16.
 */




public interface IMatrix extends IArray<IMatrix>{

    IVector getRow(int index);

    IVector getColumn(int index);

//    IMatrix addi(IMatrix mat);

//    IMatrix subi(IMatrix mat);

    double[][] asDouble();

    float[][] asFloat();

    IMatrix subiColumn(int index, IVector vec);

//    IMatrix dup();

    IMatrix putiRow(int index, IVector vec);

    int rows();

    int columns();
}
