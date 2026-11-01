import java.util.*;
import java.io.*;


public class InstructionUnit {
    // F = FP register, R = integer register, I = immediate, A = address, L = label
    private static final Map<String, String> FORMATS = Map.ofEntries(
            Map.entry("ADD.D", "FFF"), Map.entry("SUB.D", "FFF"),
            Map.entry("MUL.D", "FFF"), Map.entry("DIV.D", "FFF"),
            Map.entry("ADD", "RRR"), Map.entry("SUB", "RRR"),
            Map.entry("DADD", "RRR"), Map.entry("DSUB", "RRR"),
            Map.entry("ADDI", "RRI"), Map.entry("SUBI", "RRI"),
            Map.entry("L.D", "FA"), Map.entry("S.D", "FA"),
            Map.entry("BNEZ", "RL"));
    private static final Map<Character, String> OPERANDS = Map.of(
            'F', "F([0-9]|[12][0-9]|3[01])",
            'R', "R([0-9]|[12][0-9]|3[01])",
            'I', "-?\\d{1,9}",
            'A', "\\d{1,9}",
            'L', "[^\\s:]+");

    ArrayList<Instruction> instructionList;
    Queue<Instruction> instructionQueue;

    public InstructionUnit(String fileName) throws IOException {
        instructionQueue = new LinkedList<>();
        instructionList = new ArrayList<>();
        loadInstructions(fileName);
    }

    public void loadInstructions(String fileName) throws IOException {
        Map<String, Integer> labels = new HashMap<>();
        List<String[]> programs = new ArrayList<>();
        List<Integer> lineNumbers = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String raw;
            int lineNumber = 0;
            while ((raw = reader.readLine()) != null) {
                ++lineNumber;
                String line = raw.split("[#;]", 2)[0].trim().toUpperCase();
                int colon = line.indexOf(':');
                if (colon >= 0) {
                    String label = line.substring(0, colon).trim();
                    if (labels.put(label, programs.size()) != null) {
                        throw new IllegalArgumentException("line " + lineNumber + ": duplicate label " + label);
                    }
                    line = line.substring(colon + 1).trim();
                }
                if (line.isEmpty()) continue;
                String[] tokens = line.split("[\\s,]+");
                validate(tokens, lineNumber);
                programs.add(tokens);
                lineNumbers.add(lineNumber);
            }
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("cannot open program file " + fileName);
        }
        if (programs.isEmpty()) {
            throw new IllegalArgumentException(fileName + " contains no instructions");
        }

        for (int i = 0; i < programs.size(); i++) {
            String[] s = programs.get(i);
            Instruction inst = new Instruction();
            inst.op = s[0];
            inst.V2 = "-";
            if (inst.op.equals("BNEZ")) {
                Integer target = labels.get(s[2]);
                if (target == null) {
                    throw new IllegalArgumentException("line " + lineNumbers.get(i) + ": undefined label " + s[2]);
                }
                inst.V1 = s[1];
                inst.result = target.toString();
            } else {
                inst.result = s[1];
                inst.V1 = s[2];
                if (s.length == 4) inst.V2 = s[3];
            }
            instructionQueue.add(inst);
            instructionList.add(inst);
        }
    }

    private static void validate(String[] tokens, int lineNumber) {
        String format = FORMATS.get(tokens[0]);
        if (format == null) {
            throw new IllegalArgumentException("line " + lineNumber + ": unknown instruction " + tokens[0]);
        }
        boolean valid = tokens.length == format.length() + 1;
        for (int i = 0; valid && i < format.length(); i++) {
            valid = tokens[i + 1].matches(OPERANDS.get(format.charAt(i)));
        }
        if (!valid) {
            throw new IllegalArgumentException("line " + lineNumber + ": invalid operands for "
                    + tokens[0] + " in \"" + String.join(" ", tokens) + "\"");
        }
    }

    public int maxAddress() {
        int max = 0;
        for (Instruction inst : instructionList) {
            if (inst.op.equals("L.D") || inst.op.equals("S.D")) {
                max = Math.max(max, Integer.parseInt(inst.V1));
            }
        }
        return max;
    }

    public void printInstructions() {
        System.out.print("Instruction Queue: ");
        if(instructionQueue.isEmpty()){
            System.out.println("Empty");
        }else{
            System.out.println();
            System.out.printf("%-10s%-10s%-10s%-10s\n", "Op", "Result", "V1", "V2");
            for (Instruction inst : instructionQueue) {
                System.out.printf("%-10s%-10s%-10s%-10s\n",
                        inst.op, inst.result, inst.V1, inst.V2);
            }
        }
        System.out.println();
    }
}
