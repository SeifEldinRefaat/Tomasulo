public class Instruction {
    String op;
    String V1, V2;
    String result;

    int issued = -1, stExec = -1, endExec = -1, wb = -1;

    public Instruction() {

    }
}
