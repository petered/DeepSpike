package nl.uva.arrays;

import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Java has a kind-of-major shortcoming: You can't use primatives as Generics.  You can use
 * boxed types (e.g. Float, Double, Boolean), but this adds one word to memory (64 bytes) for
 * each primative (8-64 bytes depending on type), which is not a great solution.
 *
 * This becomes important when we want to use different primative classes depending on
 * some argument.
 *
 * Created by peter on 2/22/16.
 */
public class GenerateArrayClasses {


    public static void main(String[] args) throws FileNotFoundException{

        String content = read_class("nl.uva.arrays.DoubleVector");
//        System.out.println(content);


//        writeFile("nl.uva.arrays.DoubleVector", "short");
        writeFile("nl.uva.arrays.DoubleVector", "int");
        writeFile("nl.uva.arrays.DoubleMatrix", "int");
        writeFile("nl.uva.arrays.DoubleMatrix", "short");
        writeFile("nl.uva.arrays.DoubleVector", "short");

    }



    public static String read_class(String class_path) throws FileNotFoundException{
        String code_path = classname2filename(class_path);
        String content = new Scanner(new File(code_path)).useDelimiter("\\Z").next();
        return content;
    }

    public static void writeFile(String class_path, String destType) throws FileNotFoundException{

        assert Arrays.asList("int", "float", "short", "boolean").contains(destType);

        String srcShape = class_path.endsWith("Vector") ? "vector" : "matrix";
        System.out.println(srcShape);
        String code = read_class(class_path);
        String new_code = toType(code, destType, srcShape);

        String classForm = destType.substring(0, 1).toUpperCase() + destType.substring(1);
        String dest_path = classname2filename(class_path.replace("Double", classForm));

        System.out.println(dest_path);
        System.out.println("====================");
//        System.out.println(new_code);


        try (PrintStream out = new PrintStream(new FileOutputStream(dest_path))) {
            out.print(new_code);
        }

    }

    public static String toType(String code, String destType, String srcShape){

        assert Arrays.asList("vector", "matrix").contains(srcShape);

        code = code.replace("double[", destType.toLowerCase()+"[");
        code = code.replace("(double)", "("+destType.toLowerCase()+")");
        code = code.replace(" Double",  " "+destType.substring(0, 1).toUpperCase() + destType.substring(1));
        code = code.replace("(Double", "("+destType.substring(0, 1).toUpperCase() + destType.substring(1));
        code = code.replace("DOUBLE", destType.toUpperCase());
        code = code.replace("double2", destType+"2");

        if (srcShape == "vector"){
            code = code.replace(destType+"[] asDouble", "double[] asDouble");
        }
        else if (srcShape == "matrix"){
            code = code.replace(destType+"[][] asDouble", "double[][] asDouble");
        }

        if (destType == "short" && srcShape=="vector"){
            code = code.replace("{data[index] = val", "{data[index] = (short) val");
            code = code.replace("Math.abs(data[i])", "data[i]>0 ? data[i] : (short)-data[i]");
            code = code.replace("this.data[i] * that_doublevec.data[j]", "(short) (this.data[i] * that_doublevec.data[j])");
            code = code.replace("negarr[i] = -this.data[i]", "negarr[i] = (short)(-this.data[i])");
            code = code.replace("mularr[i] = this.data[i] * val", "mularr[i] = (short)(this.data[i] * val)");
            code = code.replace("mularr[i] = this.data[i] * dvec.data[i]", "mularr[i] = (short)(this.data[i] * dvec.data[i])");
            code = code.replace("divarr[i] = this.data[i] / divisor", "divarr[i] = (short) (this.data[i] / divisor)");
            code = code.replace("Arrays.fill(data, val)", "Arrays.fill(data, (short)val)");
            code = code.replace("asDouble() {return data;}", "asDouble() {return PrimativeArrayConverters.short2double(data);}");
        }
        else if (destType == "short" && srcShape=="matrix"){
            code = code.replace("this.data[i][j] * val", "(short)(this.data[i][j] * val)");
        }

        else if (destType == "int" && srcShape=="vector"){

            code = code.replace("Arrays.fill(data, val)", "Arrays.fill(data, (int)val)");
            code = code.replace("double val) {data[index] = val", "double val) {data[index] = (int)val");
            code = code.replace("float val) {data[index] = val", "float val) {data[index] = (int)val");

            code = code.replace("mularr[i] = this.data[i] * val;", "mularr[i] = (int)(this.data[i] * val);");
            code = code.replace("divarr[i] = this.data[i] / divisor;", "divarr[i] = (int)(this.data[i] / divisor);");
            code = code.replace("assign(int val) {return assign((int) val);}", "assign(int val) {Arrays.fill(data, val); return this;}");


        }
        else if (destType == "int" && srcShape=="matrix"){
            code = code.replace("this.data[i][j] * val", "this.data[i][j] * (int)val");
        }



        return code;

    }

//    public static String findAndReplace(String text, String pattern, String replacement){
//
//        Pattern p = Pattern.compile(pattern);
//        Matcher m = p.matcher(text);
//
//        while(m.find()){
//
//            String token = m.group(0);
//            System.out.println(token);
//            String newValue = replacement;
//            text = text.replaceAll(token, newValue);
//        }
//
//        return text;
//    }

    public static String classname2filename(String class_path){
        return System.getProperty("user.dir")+"/src/main/java/"+class_path.replace(".", "/")+".java";
    }


}
