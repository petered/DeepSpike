package nl.uva.arrays;

/**
 * Created by peter on 2/22/16.
 */
public class VectorFactory {

    static IArray.DType dtype = IArray.DType.DOUBLE;
    static float scale = 1;

    public static void set_dtype(IArray.DType new_dtype){
        dtype=new_dtype;
    }

    public static void set_dtype(String new_dtype){
        dtype= IArray.DType.valueOf(new_dtype);
    }

    public IArray.DType get_dtype(){
        return dtype;
    }

    public static void set_scale(float val){
        scale = val;
    }

    public static float get_scale(){
        return scale;
    }


    public static IVector create(int length){

        switch (dtype) {
            case DOUBLE:
                return new DoubleVector(length);
            case FLOAT:
                return new FloatVector(length);
            case INT:
                return new IntVector(length);
            default:
                throw new UnsupportedOperationException("No method for creatoing "+dtype+" arrays yet.");
        }
    }

    public static IVector create(double[] data){

        switch (dtype) {
            case DOUBLE:
                return new DoubleVector(data);
            case FLOAT:
                return new FloatVector(PrimativeArrayConverters.double2float(data));
            case INT:
                return new IntVector(PrimativeArrayConverters.double2int(data, scale));
            default:
                throw new UnsupportedOperationException("No method for creatoing "+dtype+" arrays yet.");
        }
    }

    public static IVector create(float[] data){

        switch (dtype) {
            case DOUBLE:
                return new DoubleVector(PrimativeArrayConverters.float2double(data));
            case FLOAT:
                return new FloatVector(data);
            case INT:
                return new IntVector(PrimativeArrayConverters.float2int(data, scale));
            default:
                throw new UnsupportedOperationException("No method for creatoing "+dtype+" arrays yet.");
        }
    }
}
