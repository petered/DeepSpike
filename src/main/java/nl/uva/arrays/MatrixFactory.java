package nl.uva.arrays;

import sun.plugin2.message.Conversation;

/**
 * Created by peter on 2/22/16.
 */
public class MatrixFactory {

    static IArray.DType dtype = IArray.DType.DOUBLE;

    static float scale = 1;

    public static void set_dtype(IArray.DType new_dtype){
        dtype=new_dtype;
    }

    public static void set_dtype(String new_dtype){
        dtype= IArray.DType.valueOf(new_dtype);
    }

    public static void set_scale(float val){
        scale = val;
    }

    public static float get_scale(){
        return scale;
    }
//
//    public static IVector create(int length){
//
//        switch (dtype) {
//            case DOUBLE:
//                return new DoubleVector(length);
//            default:
//                throw new UnsupportedOperationException("No method for creatoing "+dtype+" arrays yet.");
//        }
//    }


    public static IMatrix create(float[][] data){

        switch (dtype) {
            case DOUBLE:
                return new DoubleMatrix(PrimativeArrayConverters.float2double(data));
            case FLOAT:
                return new FloatMatrix(data);
            case INT:
                return new IntMatrix(PrimativeArrayConverters.float2int(data, scale));
            default:
                throw new UnsupportedOperationException("No method for creatoing "+dtype+" arrays yet.");
        }
    }

//    public static IMatrix create(double[][] data){
//
//        switch (dtype) {
//            case DOUBLE:
//                return new DoubleMatrix(data);
//            case FLOAT:
//                return new FloatMatrix(PrimativeArrayConverters.double2float(data));
//            case
//            default:
//                throw new UnsupportedOperationException("No method for creatoing "+dtype+" arrays yet.");
//        }
//    }

}
