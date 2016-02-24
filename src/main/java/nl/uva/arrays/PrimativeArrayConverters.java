package nl.uva.arrays;

/**
 * Created by peter on 2/22/16.
 */
public class PrimativeArrayConverters {

    static double[] short2double(short[] vec){
        double[] newdata = new double[vec.length];
        for (int i=0; i<vec.length; i++)
            newdata[i] = vec[i];
        return newdata;
    }

    static float[] short2float(short[] vec){
        float[] newdata = new float[vec.length];
        for (int i=0; i<vec.length; i++)
            newdata[i] = vec[i];
        return newdata;
    }

    static double[] int2double(int[] vec){
        double[] newdata = new double[vec.length];
        for (int i=0; i<vec.length; i++)
            newdata[i] = vec[i];
        return newdata;
    }

    static float[] int2float(int[] vec){
        float[] newdata = new float[vec.length];
        for (int i=0; i<vec.length; i++)
            newdata[i] = vec[i];
        return newdata;
    }

    static double[] float2double(float[] vec){
        double[] newdata = new double[vec.length];
        for (int i=0; i<vec.length; i++)
            newdata[i] = vec[i];
        return newdata;
    }

    static float[] double2float(double[] vec){
        float[] newdata = new float[vec.length];
        for (int i=0; i<vec.length; i++)
            newdata[i] = (float) vec[i];
        return newdata;
    }

    static int[] double2int(double[] vec){
        int[] newdata = new int[vec.length];
        for (int i=0; i<vec.length; i++)
            newdata[i] = (int) vec[i];
        return newdata;
    }

    static int[] double2int(double[] vec, double scale){
        int[] newdata = new int[vec.length];
        for (int i=0; i<vec.length; i++)
            newdata[i] = (int) (vec[i]*scale);
        return newdata;
    }

    static int[] float2int(float[] vec){
        int[] newdata = new int[vec.length];
        for (int i=0; i<vec.length; i++)
            newdata[i] = (int) vec[i];
        return newdata;
    }

    static int[] float2int(float[] vec, float scale){
        int[] newdata = new int[vec.length];
        for (int i=0; i<vec.length; i++)
            newdata[i] = (int) (vec[i] * scale);
        return newdata;
    }

    static double[][] float2double(float[][] mat){
        double[][] newdata = new double[mat.length][];
        for (int i=0; i<mat.length; i++)
            newdata[i] = float2double(mat[i]);
        return newdata;
    }

    static int[][] float2int(float[][] mat){
        int[][] newdata = new int[mat.length][];
        for (int i=0; i<mat.length; i++)
            newdata[i] = float2int(mat[i]);
        return newdata;
    }

    static int[][] float2int(float[][] mat, float scale){
        int[][] newdata = new int[mat.length][];
        for (int i=0; i<mat.length; i++)
            newdata[i] = float2int(mat[i], scale);
        return newdata;
    }

    static float[][] double2float(double[][] mat){
        float[][] newdata = new float[mat.length][];
        for (int i=0; i<mat.length; i++)
            newdata[i] = double2float(mat[i]);
        return newdata;
    }

    static double[][] short2double(short[][] mat){
        double[][] newdata = new double[mat.length][];
        for (int i=0; i<mat.length; i++)
            newdata[i] = short2double(mat[i]);
        return newdata;
    }

    static float[][] short2float(short[][] mat){
        float[][] newdata = new float[mat.length][];
        for (int i=0; i<mat.length; i++)
            newdata[i] = short2float(mat[i]);
        return newdata;
    }

    static double[][] int2double(int[][] mat){
        double[][] newdata = new double[mat.length][];
        for (int i=0; i<mat.length; i++)
            newdata[i] = int2double(mat[i]);
        return newdata;
    }

    static float[][] int2float(int[][] mat){
        float[][] newdata = new float[mat.length][];
        for (int i=0; i<mat.length; i++)
            newdata[i] = int2float(mat[i]);
        return newdata;
    }


    // Just for this ridiculous code-generation thing we have.
    static int[] int2int(int[] vec){return vec;};
    static short[] short2short(short[] vec){return vec;};
    static float[] float2float(float[] vec){return vec;};
    static double[] double2double(double[] vec){return vec;};

    static int[][] int2int(int[][] vec){return vec;};
    static short[][] short2short(short[][] vec){return vec;};
    static float[][] float2float(float[][] vec){return vec;};
    static double[][] double2double(double[][] vec){return vec;};


}
