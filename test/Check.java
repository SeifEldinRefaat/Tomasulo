import java.io.*;
import java.nio.file.*;

// Regression check. Build and run from the project root:
//   javac -d out/check src/*.java test/Check.java
//   java -cp out/check Check
public class Check {
    static int failures = 0;
    static final PrintStream console = System.out;

    // Latencies MUL DIV ADD SUB L.D S.D, then sizes add mul load store cache, then no stepping.
    static final String DEFAULTS = "6\n6\n4\n4\n1\n1\n3\n2\n2\n2\n16\nn\n";

    static void run(String program, String input) throws IOException {
        Path file = Files.createTempFile("program", ".txt");
        Files.writeString(file, program);
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        try {
            Tomasulo.initialize(file.toString());
            Tomasulo.registerFile.generalPurpose[1] = 2;
            Tomasulo.Tomasulo();
        } finally {
            System.setOut(console);
            Files.delete(file);
        }
    }

    static void expect(String name, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            console.println("FAIL " + name + ": expected " + expected + ", got " + actual);
            failures++;
        }
    }

    static void expectRejected(String name, String program) throws IOException {
        try {
            run(program, DEFAULTS);
            expect(name, "rejected", "accepted");
        } catch (IllegalArgumentException expected) {
            // rejected with a message, as intended
        }
    }

    static Instruction row(int i) {
        return Tomasulo.ans.get(i);
    }

    public static void main(String[] args) throws IOException {
        // Sample loop: runs twice, and ADD.D beats BNEZ for the bus in cycle 7 because S.D waits on it.
        run("LOOP:\nL.D F1 1\nADD.D F3 F1 F1\nS.D F3 3\nSUBI R1 R1 1\nBNEZ R1 LOOP\nADDI R1 R1 5\n", DEFAULTS);
        expect("sample rows", 11, Tomasulo.ans.size());
        expect("sample R1", 5L, Tomasulo.registerFile.generalPurpose[1]);
        expect("arbitration winner", 7, row(1).wb);
        expect("arbitration loser", 8, row(4).wb);

        // One add station: the stalled ADD.D gets one row, and MUL.D waits for its real result.
        run("ADD.D F1 F2 F3\nADD.D F4 F5 F6\nMUL.D F7 F4 F4\n", "6\n6\n4\n4\n1\n1\n1\n2\n2\n2\n16\nn\n");
        expect("no stall rows", 3, Tomasulo.ans.size());
        expect("second ADD.D write-back", 11, row(1).wb);
        expect("MUL.D waits for bus, not stale tag", 12, row(2).stExec);

        // Each type uses its own latency.
        run("MUL.D F1 F2 F3\nDIV.D F4 F5 F6\nADD.D F7 F8 F9\nSUB.D F10 F11 F12\nL.D F13 0\nS.D F14 1\n",
                "2\n3\n4\n5\n6\n7\n3\n2\n2\n2\n16\nn\n");
        int[] latencies = {2, 3, 4, 5, 6, 7};
        for (int i = 0; i < latencies.length; i++) {
            expect("latency of " + row(i).op, latencies[i], row(i).endExec - row(i).stExec + 1);
        }

        // Only one memory operation completes per cycle, and loads go first.
        run("L.D F1 0\nS.D F2 1\n", "6\n6\n4\n4\n3\n2\n3\n2\n2\n2\n16\nn\n");
        expect("load completes on time", 3, row(0).endExec);
        expect("store waits for the memory port", 4, row(1).endExec);

        // Overlapping loads that finish in different cycles don't slow each other down.
        run("L.D F1 0\nL.D F2 1\n", "6\n6\n4\n4\n3\n1\n3\n2\n2\n2\n16\nn\n");
        expect("first load end", 3, row(0).endExec);
        expect("second load end", 4, row(1).endExec);

        // Branch target past register 31 must not index the register file.
        StringBuilder longProgram = new StringBuilder();
        for (int i = 0; i < 40; i++) longProgram.append("ADDI R2 R2 1\n");
        run(longProgram + "LOOP: SUBI R1 R1 1\nBNEZ R1 LOOP\n", DEFAULTS);
        expect("long program R1", 0L, Tomasulo.registerFile.generalPurpose[1]);
        expect("long program R2", 40L, Tomasulo.registerFile.generalPurpose[2]);

        // Parser accepts the brief's syntax, comments, lower case, and a label sharing a line.
        run("# comment\n\nl.d F2,5 ; trailing\nstart: add.d f1, f2, f2\n", DEFAULTS);
        expect("flexible syntax rows", 2, Tomasulo.ans.size());

        // An address beyond the requested cache size asks again rather than crashing later.
        run("L.D F1 100\n", "6\n6\n4\n4\n1\n1\n3\n2\n2\n2\n16\n101\nn\n");
        expect("cache grows to fit", 101, Tomasulo.cache.size);

        // Invalid numeric input asks again, and blank lines take defaults.
        run("ADD.D F1 F2 F3\n", "abc\n0\n\n");
        expect("default latency used", 4, row(0).endExec - row(0).stExec + 1);

        // A branch that never exits stops at the cycle cap.
        run("LOOP:\nBNEZ R1 LOOP\n", DEFAULTS);
        expect("cycle cap", Tomasulo.MAX_CYCLES, Tomasulo.currentCycle);

        expectRejected("unknown opcode", "FOO F1 F2 F3\n");
        expectRejected("undefined label", "BNEZ R1 NOWHERE\n");
        expectRejected("register out of range", "ADD.D F1 F2 F32\n");
        expectRejected("wrong register type", "ADD.D R1 F2 F3\n");
        expectRejected("missing operand", "L.D F1\n");
        expectRejected("duplicate label", "A:\nADDI R2 R2 1\nA:\nADDI R2 R2 1\n");
        expectRejected("empty program", "\n");

        console.println(failures == 0 ? "All checks passed." : failures + " check(s) failed.");
        System.exit(failures == 0 ? 0 : 1);
    }
}
