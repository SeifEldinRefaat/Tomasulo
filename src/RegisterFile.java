import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class RegisterFile {
    long[] generalPurpose;
    String[] generalPurposeQi;
    double[] floatingPoint;
    String[] floatingPointQi;

    public RegisterFile() {
        generalPurpose = new long[32];
        generalPurposeQi = new String[32];
        floatingPoint = new double[32];
        floatingPointQi = new String[32];

        for (int i = 0; i < 32; ++i) {
            floatingPointQi[i] = "0";
            generalPurposeQi[i] = "0";
        }

    }

    //    public void print(){
//        System.out.println("Floating Point Registers: ");
//        for(int i = 0; i < floatingPoint.length; ++i){
//            System.out.print(floatingPoint[i] + " ");
//        }
//        System.out.println();
//        System.out.println();
//
//        System.out.println("Floating Point Registers Qi: ");
//        for(int i = 0; i < floatingPointQi.length; ++i){
//            System.out.print(floatingPointQi[i] + " ");
//        }
//        System.out.println();
//        System.out.println();
//
//        System.out.println("General Purpose Registers: ");
//        for(int i = 0; i < generalPurpose.length; ++i){
//            System.out.print(generalPurpose[i] + " ");
//        }
//        System.out.println();
//        System.out.println();
//
//        System.out.println("General Purpose Registers Qi: ");
//        for(int i = 0; i < generalPurposeQi.length; ++i){
//            System.out.print(generalPurposeQi[i] + " ");
//        }
//        System.out.println();
//        System.out.println();
//    }
    public void print() {
        System.out.println("Floating Point Registers: ");
        printRegistersWithNames(floatingPoint, "F", floatingPoint.length);

        System.out.println("Floating Point Registers Qi: ");
        printQiWithNames(floatingPointQi, "F", floatingPointQi.length);

        System.out.println("General Purpose Registers: ");
        printRegistersWithNames(generalPurpose, "R", generalPurpose.length);

        System.out.println("General Purpose Registers Qi: ");
        printQiWithNames(generalPurposeQi, "R", generalPurposeQi.length);
    }

    private void printRegistersWithNames(double[] registers, String prefix, int length) {
        for (int i = 0; i < length; ++i) {
            System.out.printf("%-6s", prefix+i);
        }
        System.out.println();

        for (double value : registers) {
            System.out.printf("%-6.2f", value);
        }
        System.out.println("\n");
    }

    private void printRegistersWithNames(long[] registers, String prefix, int length) {
        for (int i = 0; i < length; ++i) {
            System.out.printf("%-6s", prefix+i);
        }
        System.out.println();

        for (long value : registers) {
            System.out.printf("%-6d", value);
        }
        System.out.println("\n");
    }

    private void printQiWithNames(String[] qis, String prefix, int length) {
        for (int i = 0; i < length; ++i) {
            System.out.printf("%-6s", prefix+i);
        }
        System.out.println();

        for (String qi : qis) {
            System.out.printf("%-6s", qi);
        }
        System.out.println("\n");
    }
}
