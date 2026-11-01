import java.util.*;
import java.io.IOException;

public class Tomasulo {
    static InstructionUnit instructionUnit;
    static ReservationStation add;
    static ReservationStation mul;
    static LoadBuffer load;
    static StoreBuffer store;
    static Cache cache;
    static RegisterFile registerFile;
    static int MulLatency = 6;
    static int DivLatency = 6;
    static int AddLatency = 4;
    static int SubLatency = 4;
    static int StoreLatency = 1;
    static int LoadLatency = 1;
    // ponytail: fixed cap so a non-terminating BNEZ loop can't print forever, make it a prompt if real programs hit it
    static final int MAX_CYCLES = 10000;
    static Scanner in;
    static boolean stepping = false;
    static String fetched = "0";
    static int fetchedIndex = 0;
    static int currentCycle = 0;
    static boolean branchFlag = false;
    static CommonDataBus commonDataBus = new CommonDataBus();

    static ArrayList<Instruction> ans = new ArrayList<>();

    public static void printInstructions() {
        System.out.printf("%-10s%-10s%-10s%-10s%-10s%-10s%-10s\n", "Op", "Result", "V1", "V2", "Issued", "Execution", "Write Back");
        for (Instruction inst : ans) {
            System.out.printf("%-10s%-10s%-10s%-10s%-10s%-10s%-10s\n",
                    inst.op, inst.result, inst.V1, inst.V2, inst.issued, inst.stExec+"..."+ inst.endExec, inst.wb);
        }
        System.out.println();
    }

    public static void printState() {
        System.out.println("=========================================================================");
        System.out.println("Cycle : " + currentCycle);
        System.out.println();
        instructionUnit.printInstructions();
        add.print();
        mul.print();
        load.print();
        store.print();
        cache.print();
        registerFile.print();
    }

    // Returns false if the user chose to quit.
    public static boolean pause() {
        if (!stepping) return true;
        System.out.print("Enter = next cycle, r = run to end, q = quit: ");
        if (!in.hasNextLine()) {
            stepping = false;
            return true;
        }
        String choice = in.nextLine().trim().toLowerCase();
        if (choice.equals("r")) stepping = false;
        return !choice.equals("q");
    }

    public static void Tomasulo() {
        while (true) {
            printState();
            if (check()) break;
            if (currentCycle >= MAX_CYCLES) {
                System.out.println("Stopped after " + MAX_CYCLES + " cycles. The program may contain an infinite loop.");
                break;
            }
            if (!pause()) break;
            issue();
            execute();
            writeBack();
            broadCast();
            ++currentCycle;
        }
        printInstructions();
    }

    public static void issue() {
        if (instructionUnit.instructionQueue.isEmpty() || branchFlag) {
            return;
        }
        Instruction i = instructionUnit.instructionQueue.peek();
        Instruction tmp = new Instruction();
        tmp.op = i.op;
        tmp.V1 = i.V1;
        tmp.V2 = i.V2;
        tmp.result = i.result;
        if (i.op.equals("L.D")) {
            for (int j = 0; j < load.size; ++j) {
                if (load.loadBuffer[j].busy.equals("0")) {
                    load.loadBuffer[j].busy = "1";
                    tmp.issued = currentCycle;
                    int resultRegisterNumber = Integer.parseInt(i.result.substring(1));
                    if (!registerFile.floatingPointQi[resultRegisterNumber].equals("0")) {
                        load.loadBuffer[j].qz = registerFile.floatingPointQi[resultRegisterNumber];
                    } else {
                        load.loadBuffer[j].qz = "0";
                    }
                    if (resultRegisterNumber != 0) {
                        registerFile.floatingPointQi[resultRegisterNumber] = load.loadBuffer[j].tag;
                    }
                    load.loadBuffer[j].result = resultRegisterNumber;
                    load.loadBuffer[j].address = Integer.parseInt(i.V1);
                    load.loadBuffer[j].cycles = LoadLatency;
                    load.loadBuffer[j].issuedAt = currentCycle;
                    instructionUnit.instructionQueue.poll();
                    fetched = "L";
                    fetchedIndex = j;
                    break;
                } else {
                    fetched = "0";
                }
            }
        }
        if (i.op.equals("S.D")) {
            for (int j = 0; j < store.size; ++j) {
                if (store.storeBuffer[j].busy.equals("0")) {
                    store.storeBuffer[j].busy = "1";
                    tmp.issued = currentCycle;
                    int resultRegisterNumber = Integer.parseInt(i.result.substring(1));
                    if (registerFile.floatingPointQi[resultRegisterNumber].equals("0")) {
                        store.storeBuffer[j].v = registerFile.floatingPoint[resultRegisterNumber];
                        store.storeBuffer[j].q = "0";
                    } else {
                        store.storeBuffer[j].q = registerFile.floatingPointQi[resultRegisterNumber];
                    }
                    store.storeBuffer[j].address = Integer.parseInt(i.V1);
                    store.storeBuffer[j].cycles = StoreLatency;
                    store.storeBuffer[j].issuedAt = currentCycle;
                    instructionUnit.instructionQueue.poll();
                    fetched = "S";
                    fetchedIndex = j;
                    break;
                } else {
                    fetched = "0";
                }
            }
        }
        if (i.op.equals("ADD.D") || i.op.equals("SUB.D")) {
            for (int j = 0; j < add.size; ++j) {
                if (add.sf[j].busy.equals("0")) {
                    add.sf[j].busy = "1";
                    tmp.issued = currentCycle;
                    int resultRegisterNumber = Integer.parseInt(i.result.substring(1));
                    int registerV1 = Integer.parseInt(i.V1.substring(1));
                    int registerV2 = Integer.parseInt(i.V2.substring(1));
//                    if(i.op.equals("ADD.D")){
//                        add.sf[j].op = "ADD";
//                    }else if(i.op.equals("SUB.D")){
//                        add.sf[j].op = "SUB";
//                    }
                    add.sf[j].op = i.op;
                    if (registerFile.floatingPointQi[registerV1].equals("0")) {
                        add.sf[j].vj = registerFile.floatingPoint[registerV1];
                        add.sf[j].qj = "0";
                    } else {
                        add.sf[j].qj = registerFile.floatingPointQi[registerV1];
                    }
                    if (registerFile.floatingPointQi[registerV2].equals("0")) {
                        add.sf[j].vk = registerFile.floatingPoint[registerV2];
                        add.sf[j].qk = "0";
                    } else {
                        add.sf[j].qk = registerFile.floatingPointQi[registerV2];
                    }
                    if (!registerFile.floatingPointQi[resultRegisterNumber].equals("0")) {
                        add.sf[j].qz = registerFile.floatingPointQi[resultRegisterNumber];
                    } else {
                        add.sf[j].qz = "0";
                    }
                    add.sf[j].result = resultRegisterNumber;
                    add.sf[j].cycles = i.op.contains("ADD") ? AddLatency : SubLatency;
                    if (resultRegisterNumber != 0) {
                        registerFile.floatingPointQi[resultRegisterNumber] = add.sf[j].tag;
                    }
                    add.sf[j].issuedAt = currentCycle;
                    instructionUnit.instructionQueue.poll();
                    fetched = "A";
                    fetchedIndex = j;
                    break;
                } else {
                    fetched = "0";
                }
            }
        }
        if (i.op.equals("DADD") || i.op.equals("DSUB") || i.op.equals("ADD") || i.op.equals("SUB")) {
            for (int j = 0; j < add.size; ++j) {
                if (add.sf[j].busy.equals("0")) {
                    add.sf[j].busy = "1";
                    tmp.issued = currentCycle;
                    int resultRegisterNumber = Integer.parseInt(i.result.substring(1));

                    int registerV1 = Integer.parseInt(i.V1.substring(1));
                    int registerV2 = Integer.parseInt(i.V2.substring(1));
//                    if(i.op.equals("DADD") || i.op.equals("ADD")){
//                        add.sf[j].op = "ADD";
//                    }else{
//                        add.sf[j].op = "SUB";
//                    }
                    add.sf[j].op = i.op;
                    if (registerFile.generalPurposeQi[registerV1].equals("0")) {
                        add.sf[j].vj = registerFile.generalPurpose[registerV1];
                        add.sf[j].qj = "0";
                    } else {
                        add.sf[j].qj = registerFile.generalPurposeQi[registerV1];
                    }
                    if (registerFile.generalPurposeQi[registerV2].equals("0")) {
                        add.sf[j].vk = registerFile.generalPurpose[registerV2];
                        add.sf[j].qk = "0";
                    } else {
                        add.sf[j].qk = registerFile.generalPurposeQi[registerV2];
                    }
                    if (!registerFile.generalPurposeQi[resultRegisterNumber].equals("0")) {
                        add.sf[j].qz = registerFile.generalPurposeQi[resultRegisterNumber];
                    } else {
                        add.sf[j].qz = "0";
                    }
                    add.sf[j].result = resultRegisterNumber;
                    add.sf[j].cycles = i.op.contains("ADD") ? AddLatency : SubLatency;
                    add.sf[j].issuedAt = currentCycle;
                    if (resultRegisterNumber != 0) {
                        registerFile.generalPurposeQi[resultRegisterNumber] = add.sf[j].tag;
                    }
                    instructionUnit.instructionQueue.poll();
                    fetched = "A";
                    fetchedIndex = j;
                    break;
                } else {
                    fetched = "0";
                }
            }
        }
        if (i.op.equals("ADDI") || i.op.equals("SUBI")) {
            for (int j = 0; j < add.size; ++j) {
                if (add.sf[j].busy.equals("0")) {
                    add.sf[j].busy = "1";
                    tmp.issued = currentCycle;
                    int resultRegisterNumber = Integer.parseInt(i.result.substring(1));
                    int registerV1 = Integer.parseInt(i.V1.substring(1));
                    int value = Integer.parseInt(i.V2);
//                    if(i.op.equals("ADDI")){
//                        add.sf[j].op = "ADD";
//                    }else{
//                        add.sf[j].op = "SUB";
//                    }
                    add.sf[j].op = i.op;
                    if (registerFile.generalPurposeQi[registerV1].equals("0")) {
                        add.sf[j].vj = registerFile.generalPurpose[registerV1];
                        add.sf[j].qj = "0";
                    } else {
                        add.sf[j].qj = registerFile.generalPurposeQi[registerV1];
                    }
                    if (!registerFile.generalPurposeQi[resultRegisterNumber].equals("0")) {
                        add.sf[j].qz = registerFile.generalPurposeQi[resultRegisterNumber];
                    } else {
                        add.sf[j].qz = "0";
                    }

                    add.sf[j].result = resultRegisterNumber;
                    add.sf[j].vk = value;
                    add.sf[j].cycles = 1;
                    add.sf[j].issuedAt = currentCycle;
                    if (resultRegisterNumber != 0) {
                        registerFile.generalPurposeQi[resultRegisterNumber] = add.sf[j].tag;
                    }
                    instructionUnit.instructionQueue.poll();
                    fetched = "A";
                    fetchedIndex = j;
                    break;
                } else {
                    fetched = "0";
                }
            }
        }

        if ((i.op.equals("MUL.D") || i.op.equals("DIV.D"))) {
            for (int j = 0; j < mul.size; ++j) {
                if (mul.sf[j].busy.equals("0")) {
                    mul.sf[j].busy = "1";
                    tmp.issued = currentCycle;
                    int resultRegisterNumber = Integer.parseInt(i.result.substring(1));
                    int registerV1 = Integer.parseInt(i.V1.substring(1));
                    int registerV2 = Integer.parseInt(i.V2.substring(1));
//                    if(i.op.equals("MUL.D")){
//                        mul.sf[j].op = "MUL";
//                    }else{
//                        mul.sf[j].op = "DIV";
//                    }
                    mul.sf[j].op = i.op;
//                    System.out.println("Register V1  :"+ registerV1);
                    if (registerFile.floatingPointQi[registerV1].equals("0")) {
                        mul.sf[j].vj = registerFile.floatingPoint[registerV1];
//                        System.out.println("vj:" + mul.sf[j].vj);
                        mul.sf[j].qj = "0";
                    } else {
                        mul.sf[j].qj = registerFile.floatingPointQi[registerV1];
                    }
                    if (registerFile.floatingPointQi[registerV2].equals("0")) {
                        mul.sf[j].vk = registerFile.floatingPoint[registerV2];
                        mul.sf[j].qk = "0";
                    } else {
                        mul.sf[j].qk = registerFile.floatingPointQi[registerV2];
                    }
                    if (!registerFile.floatingPointQi[resultRegisterNumber].equals("0")) {
                        mul.sf[j].qz = registerFile.floatingPointQi[resultRegisterNumber];
                    } else {
                        mul.sf[j].qz = "0";
                    }
                    mul.sf[j].result = resultRegisterNumber;
                    mul.sf[j].cycles = i.op.equals("MUL.D") ? MulLatency : DivLatency;
                    mul.sf[j].issuedAt = currentCycle;
                    if (resultRegisterNumber != 0) {
                        registerFile.floatingPointQi[resultRegisterNumber] = mul.sf[j].tag;
                    }
                    instructionUnit.instructionQueue.poll();

                    fetched = "M";
                    fetchedIndex = j;
                    break;
                } else {
                    fetched = "0";
                }
            }
        }

        if (i.op.equals("BNEZ")) {
            for (int j = 0; j < add.size; ++j) {
                if (add.sf[j].busy.equals("0")) {
                    add.sf[j].busy = "1";
                    tmp.issued = currentCycle;
                    int resultRegisterNumber = Integer.parseInt(i.result);
                    int registerV1 = Integer.parseInt(i.V1.substring(1));
                    if (registerFile.generalPurposeQi[registerV1].equals("0")) {
                        add.sf[j].vj = registerFile.generalPurpose[registerV1];
                        add.sf[j].qj = "0";
                    } else {
                        add.sf[j].qj = registerFile.generalPurposeQi[registerV1];
                    }
                    add.sf[j].result = resultRegisterNumber;
                    add.sf[j].qz = "0";
                    add.sf[j].op = i.op;
                    add.sf[j].cycles = 1;
                    add.sf[j].issuedAt = currentCycle;
                    instructionUnit.instructionQueue.poll();
                    fetched = "A";
                    fetchedIndex = j;
                    branchFlag = true;
                    break;
                } else {
                    fetched = "0";
                    branchFlag = false;
                }
            }
        }
        if (tmp.issued != -1) {
            ans.add(tmp);
        }
    }

    public static void execute() {
        for (int i = 0; i < add.size; ++i) {
            if (add.sf[i].busy.equals("1") && add.sf[i].qj.equals("0") && add.sf[i].qk.equals("0") && add.sf[i].cycles >= 0) {
                if (fetched.equals("A") && fetchedIndex == i) {
                    continue;
                }
                for (Instruction an : ans) {
                    if (an.issued == add.sf[i].issuedAt && an.stExec == -1) {
                        an.stExec = currentCycle;
                    }
                }
                --add.sf[i].cycles;
                for (Instruction an : ans) {
                    if (an.issued == add.sf[i].issuedAt && add.sf[i].cycles == 0 && an.endExec == -1) {
                        an.endExec = currentCycle;
                    }
                }
            }
        }
        for (int i = 0; i < mul.size; ++i) {
            if (mul.sf[i].busy.equals("1") && mul.sf[i].qj.equals("0") && mul.sf[i].qk.equals("0") && mul.sf[i].cycles >= 0) {
                if (fetched.equals("M") && fetchedIndex == i) {
                    continue;
                }
                for (Instruction an : ans) {
                    if (an.issued == mul.sf[i].issuedAt && an.stExec == -1) {
                        an.stExec = currentCycle;
                    }
                }
                --mul.sf[i].cycles;
                for (Instruction an : ans) {
                    if (an.issued == mul.sf[i].issuedAt && mul.sf[i].cycles == 0 && an.endExec == -1) {
                        an.endExec = currentCycle;
                    }
                }
            }
        }
        boolean memoryAccess = true;
        for (int i = 0; i < load.size; ++i) {
            if (load.loadBuffer[i].busy.equals("1") && load.loadBuffer[i].cycles >= 0) {
                if (fetched.equals("L") && fetchedIndex == i) {
                    continue;
                }
                if (load.loadBuffer[i].cycles == 1) {
                    if (!memoryAccess) continue;
                    memoryAccess = false;
                }
                for (Instruction an : ans) {
                    if (an.issued == load.loadBuffer[i].issuedAt && an.stExec == -1) {
                        an.stExec = currentCycle;
                    }
                }
                --load.loadBuffer[i].cycles;
                for (Instruction an : ans) {
                    if (an.issued == load.loadBuffer[i].issuedAt && load.loadBuffer[i].cycles == 0 && an.endExec == -1) {
                        an.endExec = currentCycle;
                    }
                }
            }
        }
        for (int i = 0; i < store.size; ++i) {
            if (store.storeBuffer[i].busy.equals("1") && store.storeBuffer[i].q.equals("0") && store.storeBuffer[i].cycles >= 0) {
                if (fetched.equals("S") && fetchedIndex == i) {
                    continue;
                }
                if (store.storeBuffer[i].cycles == 1) {
                    if (!memoryAccess) continue;
                    memoryAccess = false;
                }
                for (Instruction an : ans) {
                    if (an.issued == store.storeBuffer[i].issuedAt && an.stExec == -1) {
                        an.stExec = currentCycle;
                    }
                }
                --store.storeBuffer[i].cycles;
                for (Instruction an : ans) {
                    if (an.issued == store.storeBuffer[i].issuedAt && store.storeBuffer[i].cycles == 0 && an.endExec == -1) {
                        an.endExec = currentCycle;
                    }
                }
                if (store.storeBuffer[i].cycles == 0) {
                    cache.cache[store.storeBuffer[i].address] = store.storeBuffer[i].v;
                }
            }
        }
        fetched = "0";
    }

    public static String favouring() {
        int min = Integer.MAX_VALUE;
        String tag = "0";
        for (int i = 0; i < add.size; ++i) {
            if (add.sf[i].busy.equals("1") && add.sf[i].cycles == -1 && add.sf[i].qz.equals("0") && add.sf[i].issuedAt < min) {
                min = add.sf[i].issuedAt;
                tag = add.sf[i].tag;
            }
        }
        for (int i = 0; i < mul.size; ++i) {
            if (mul.sf[i].busy.equals("1") && mul.sf[i].cycles == -1 && mul.sf[i].qz.equals("0") && mul.sf[i].issuedAt < min) {
                min = mul.sf[i].issuedAt;
                tag = mul.sf[i].tag;
            }
        }
        for (int i = 0; i < load.size; ++i) {
            if (load.loadBuffer[i].busy.equals("1") && load.loadBuffer[i].cycles == -1 && load.loadBuffer[i].qz.equals("0") && load.loadBuffer[i].issuedAt < min) {
                min = load.loadBuffer[i].issuedAt;
                tag = load.loadBuffer[i].tag;
            }
        }
        for (int i = 0; i < store.size; ++i) {
            if (store.storeBuffer[i].busy.equals("1") && store.storeBuffer[i].cycles == -1 && store.storeBuffer[i].issuedAt < min) {
                min = store.storeBuffer[i].issuedAt;
                tag = store.storeBuffer[i].tag;
            }
        }
        return tag;
    }

    public static String favouringUltraMax() {
        ArrayList<String> writingBack = new ArrayList<>();
        ArrayList<Integer> issuedAt = new ArrayList<>();
        for (int i = 0; i < add.size; ++i) {
            if (add.sf[i].busy.equals("1") && add.sf[i].cycles == -1 && add.sf[i].qz.equals("0")) {
                writingBack.add(add.sf[i].tag);
                issuedAt.add(add.sf[i].issuedAt);
            }
        }
        for (int i = 0; i < mul.size; ++i) {
            if (mul.sf[i].busy.equals("1") && mul.sf[i].cycles == -1 && mul.sf[i].qz.equals("0")) {
                writingBack.add(mul.sf[i].tag);
                issuedAt.add(mul.sf[i].issuedAt);
            }
        }
        for (int i = 0; i < load.size; ++i) {
            if (load.loadBuffer[i].busy.equals("1") && load.loadBuffer[i].cycles == -1 && load.loadBuffer[i].qz.equals("0")) {
                writingBack.add(load.loadBuffer[i].tag);
                issuedAt.add(load.loadBuffer[i].issuedAt);
            }
        }
        for (int i = 0; i < store.size; ++i) {
            if (store.storeBuffer[i].busy.equals("1") && store.storeBuffer[i].cycles == -1) {
                writingBack.add(store.storeBuffer[i].tag);
                issuedAt.add(store.storeBuffer[i].issuedAt);
            }
        }
        if (writingBack.size() == 0) return "0";
        int[] waiting = new int[writingBack.size()];
        for (int j = 0; j < writingBack.size(); ++j) {
            String tag = writingBack.get(j);
            for (int i = 0; i < add.size; ++i) {
                if (add.sf[i].busy.equals("1") && (add.sf[i].qj.equals(tag) || add.sf[i].qk.equals(tag) || add.sf[i].qz.equals(tag))) {
                    waiting[j] += 1;
                }
            }
            for (int i = 0; i < mul.size; ++i) {
                if (mul.sf[i].busy.equals("1") && (mul.sf[i].qj.equals(tag) || mul.sf[i].qk.equals(tag) || mul.sf[i].qz.equals(tag))) {
                    waiting[j] += 1;
                }
            }
            for (int i = 0; i < load.size; ++i) {
                if (load.loadBuffer[i].busy.equals("1") && load.loadBuffer[i].qz.equals(tag)) {
                    waiting[j] += 1;
                }
            }
            for (int i = 0; i < store.size; ++i) {
                if (store.storeBuffer[i].busy.equals("1") && store.storeBuffer[i].q.equals(tag)) {
                    waiting[j] += 1;
                }
            }
        }
        int max = -1;
        int issued = -1;
        String tag = "0";
        for (int i = 0; i < writingBack.size(); ++i) {
            if (waiting[i] > max) {
                max = waiting[i];
                issued = issuedAt.get(i);
                tag = writingBack.get(i);
            }
            if (waiting[i] == max) {
                if (issuedAt.get(i) < issued) {
                    max = waiting[i];
                    issued = issuedAt.get(i);
                    tag = writingBack.get(i);
                }
            }
        }
        return tag;
    }

    public static void writeBack() {
        commonDataBus.tag = "0"; // otherwise broadCast() repeats the last tag on idle cycles
        String writeBack = favouringUltraMax();
        if (writeBack.equals("0")) return;
        for (int i = 0; i < add.size; ++i) {
            if (add.sf[i].busy.equals("1") && add.sf[i].cycles == -1 && add.sf[i].qz.equals("0") && add.sf[i].tag.equals(writeBack)) {
                commonDataBus.tag = add.sf[i].tag;
                for (Instruction an : ans) {
                    if (an.issued == add.sf[i].issuedAt) {
                        an.wb = currentCycle;
                    }
                }
                if (add.sf[i].op.equals("ADD.D")) {
                    if (add.sf[i].result != 0) {
                        commonDataBus.value = add.sf[i].vj + add.sf[i].vk;
                        registerFile.floatingPoint[add.sf[i].result] = commonDataBus.value;
                    } else {
                        commonDataBus.tag = "0";
                        commonDataBus.value = 0;
                    }
                } else if (add.sf[i].op.equals("DADD") || add.sf[i].op.equals("ADD") || add.sf[i].op.equals("ADDI")) {
                    if (add.sf[i].result != 0) {
                        commonDataBus.value = add.sf[i].vj + add.sf[i].vk;
                        registerFile.generalPurpose[add.sf[i].result] = (long) commonDataBus.value;
                    } else {
                        commonDataBus.tag = "0";
                        commonDataBus.value = 0;
                    }
                } else if (add.sf[i].op.equals("SUB.D")) {
                    if (add.sf[i].result != 0) {
                        commonDataBus.value = add.sf[i].vj - add.sf[i].vk;
                        registerFile.floatingPoint[add.sf[i].result] = commonDataBus.value;
                    } else {
                        commonDataBus.tag = "0";
                        commonDataBus.value = 0;
                    }
                } else if (add.sf[i].op.equals("SUB") || add.sf[i].op.equals("SUBI") || add.sf[i].op.equals("DSUB")) {
                    if (add.sf[i].result != 0) {
                        commonDataBus.value = add.sf[i].vj - add.sf[i].vk;
                        registerFile.generalPurpose[add.sf[i].result] = (long) commonDataBus.value;
                    } else {
                        commonDataBus.tag = "0";
                        commonDataBus.value = 0;
                    }
                } else if (add.sf[i].op.equals("BNEZ")) {
                    if (add.sf[i].vj != 0) {
                        instructionUnit.instructionQueue = new LinkedList<>();
                        for (int j = add.sf[i].result; j < instructionUnit.instructionList.size(); j++) {
                            instructionUnit.instructionQueue.add(instructionUnit.instructionList.get(j));
                        }
                    }
                    branchFlag = false;
                }
                // for BNEZ, result is a branch target rather than a register
                if (!add.sf[i].op.equals("BNEZ")) {
                    if (registerFile.floatingPointQi[add.sf[i].result].equals(add.sf[i].tag)) {
                        registerFile.floatingPointQi[add.sf[i].result] = "0";
                    }
                    if (registerFile.generalPurposeQi[add.sf[i].result].equals(add.sf[i].tag)) {
                        registerFile.generalPurposeQi[add.sf[i].result] = "0";
                    }
                }
                add.sf[i].op = "0";
                add.sf[i].vk = 0;
                add.sf[i].vj = 0;
                add.sf[i].result = 0;
                add.sf[i].busy = "0";
                add.sf[i].cycles = 0;
                return;
            }
        }
        for (int i = 0; i < mul.size; ++i) {
            if (mul.sf[i].busy.equals("1") && mul.sf[i].cycles == -1 && mul.sf[i].qz.equals("0") && mul.sf[i].tag.equals(writeBack)) {
                for (Instruction an : ans) {
                    if (an.issued == mul.sf[i].issuedAt) {
                        an.wb = currentCycle;
                    }
                }
                if (mul.sf[i].op.equals("MUL.D")) {
                    if (mul.sf[i].result != 0) {
                        commonDataBus.tag = mul.sf[i].tag;
                        commonDataBus.value = mul.sf[i].vj * mul.sf[i].vk;
                    } else {
                        commonDataBus.tag = "0";
                        commonDataBus.value = 0;
                    }

                } else if (mul.sf[i].op.equals("DIV.D")) {
                    if (mul.sf[i].result != 0) {
                        commonDataBus.tag = mul.sf[i].tag;
                        commonDataBus.value = mul.sf[i].vj / mul.sf[i].vk;
                    } else {
                        commonDataBus.tag = "0";
                        commonDataBus.value = 0;
                    }
                }
                registerFile.floatingPoint[mul.sf[i].result] = commonDataBus.value;
                if (registerFile.floatingPointQi[mul.sf[i].result].equals(mul.sf[i].tag)) {
                    registerFile.floatingPointQi[mul.sf[i].result] = "0";
                }
                mul.sf[i].vk = 0;
                mul.sf[i].vj = 0;
                mul.sf[i].result = 0;
                mul.sf[i].op = "0";
                mul.sf[i].busy = "0";
                mul.sf[i].cycles = 0;
                return;
            }
        }
        for (int i = 0; i < load.size; ++i) {
            if (load.loadBuffer[i].busy.equals("1") && load.loadBuffer[i].cycles == -1 && load.loadBuffer[i].qz.equals("0") && load.loadBuffer[i].tag.equals(writeBack)) {
                for (Instruction an : ans) {
                    if (an.issued == load.loadBuffer[i].issuedAt) {
                        an.wb = currentCycle;
                    }
                }
                if (load.loadBuffer[i].result != 0) {
                    commonDataBus.tag = load.loadBuffer[i].tag;
                    commonDataBus.value = cache.cache[load.loadBuffer[i].address];
                } else {
                    commonDataBus.tag = "0";
                    commonDataBus.value = 0;
                }
                registerFile.floatingPoint[load.loadBuffer[i].result] = commonDataBus.value;
                if (registerFile.floatingPointQi[load.loadBuffer[i].result].equals(load.loadBuffer[i].tag)) {
                    registerFile.floatingPointQi[load.loadBuffer[i].result] = "0";
                }
                load.loadBuffer[i].result = 0;
                load.loadBuffer[i].address = 0;
                load.loadBuffer[i].busy = "0";
                load.loadBuffer[i].cycles = 0;
                return;
            }
        }
        for (int i = 0; i < store.size; ++i) {
            if (store.storeBuffer[i].busy.equals("1") && store.storeBuffer[i].cycles == -1 && store.storeBuffer[i].tag.equals(writeBack)) {
                for (Instruction an : ans) {
                    if (an.issued == store.storeBuffer[i].issuedAt) {
                        an.wb = currentCycle;
                    }
                }
                commonDataBus.tag = "0";
                commonDataBus.value = 0;
                store.storeBuffer[i].v = 0;
                store.storeBuffer[i].q = "0";
                store.storeBuffer[i].address = 0;
                store.storeBuffer[i].busy = "0";
                store.storeBuffer[i].cycles = 0;
                return;
            }
        }
    }

    public static void broadCast() {
        if (commonDataBus.tag.equals("0")) return;
        for (int i = 0; i < add.size; ++i) {
            if (add.sf[i].busy.equals("1")) {
                if (add.sf[i].qj.equals(commonDataBus.tag)) {
                    add.sf[i].qj = "0";
                    add.sf[i].vj = commonDataBus.value;
                }
                if (add.sf[i].qk.equals(commonDataBus.tag)) {
                    add.sf[i].qk = "0";
                    add.sf[i].vk = commonDataBus.value;
                }
                if (add.sf[i].qz.equals(commonDataBus.tag)) {
                    add.sf[i].qz = "0";
                }
            }
        }
        for (int i = 0; i < mul.size; ++i) {
            if (mul.sf[i].busy.equals("1")) {
                if (mul.sf[i].qj.equals(commonDataBus.tag)) {
                    mul.sf[i].qj = "0";
                    mul.sf[i].vj = commonDataBus.value;
                }
                if (mul.sf[i].qk.equals(commonDataBus.tag)) {
                    mul.sf[i].qk = "0";
                    mul.sf[i].vk = commonDataBus.value;
                }
                if (mul.sf[i].qz.equals(commonDataBus.tag)) {
                    mul.sf[i].qz = "0";
                }
            }
        }
        for (int i = 0; i < store.size; ++i) {
            if (store.storeBuffer[i].busy.equals("1")) {
                if (store.storeBuffer[i].q.equals(commonDataBus.tag)) {
                    store.storeBuffer[i].q = "0";
                    store.storeBuffer[i].v = commonDataBus.value;
                }
            }
        }
        for (int i = 0; i < load.size; ++i) {
            if (load.loadBuffer[i].busy.equals("1")) {
                if (load.loadBuffer[i].qz.equals(commonDataBus.tag)) {
                    load.loadBuffer[i].qz = "0";
                }
            }
        }
    }


    // Blank line or end of input takes the default; anything invalid asks again.
    static int readInt(String prompt, int defaultValue, int min) {
        while (true) {
            System.out.print(prompt + " [" + defaultValue + "]: ");
            if (!in.hasNextLine()) return defaultValue;
            String line = in.nextLine().trim();
            if (line.isEmpty()) return defaultValue;
            try {
                int value = Integer.parseInt(line);
                if (value >= min) return value;
            } catch (NumberFormatException ignored) {
                // falls through to the message below
            }
            System.out.println("Enter a whole number of at least " + min + ".");
        }
    }

    public static void initialize(String programFile) throws IOException {
        in = new Scanner(System.in);
        instructionUnit = new InstructionUnit(programFile);
        ans.clear();
        currentCycle = 0;
        fetched = "0";
        fetchedIndex = 0;
        branchFlag = false;

        MulLatency = readInt("MUL.D latency", 6, 1);
        DivLatency = readInt("DIV.D latency", 6, 1);
        AddLatency = readInt("ADD latency (ADD.D, ADD, DADD)", 4, 1);
        SubLatency = readInt("SUB latency (SUB.D, SUB, DSUB)", 4, 1);
        LoadLatency = readInt("L.D latency", 1, 1);
        StoreLatency = readInt("S.D latency", 1, 1);

        int addStationSize = readInt("Add/sub station size", 3, 1);
        add = new ReservationStation(addStationSize, "Add and Sub");
        for (int i = 0; i < addStationSize; ++i) {
            add.sf[i] = new StationFields("A" + (i + 1));
        }
        int mulStationSize = readInt("Mul/div station size", 2, 1);
        mul = new ReservationStation(mulStationSize, "Multiply and Divide");
        for (int i = 0; i < mulStationSize; ++i) {
            mul.sf[i] = new StationFields("M" + (i + 1));
        }

        int loadBufferSize = readInt("Load buffer size", 2, 1);
        load = new LoadBuffer(loadBufferSize);
        for (int i = 0; i < loadBufferSize; ++i) {
            load.loadBuffer[i] = new LoadBufferFields("L" + (i + 1));
        }
        int storeBufferSize = readInt("Store buffer size", 2, 1);
        store = new StoreBuffer(storeBufferSize);
        for (int i = 0; i < storeBufferSize; ++i) {
            store.storeBuffer[i] = new StoreBufferFields("S" + (i + 1));
        }
        int minCacheSize = instructionUnit.maxAddress() + 1;
        int cacheSize = readInt("Cache size (the program needs at least " + minCacheSize + ")", Math.max(16, minCacheSize), minCacheSize);
        cache = new Cache(cacheSize);
        registerFile = new RegisterFile();
        commonDataBus = new CommonDataBus();

        System.out.print("Pause after each cycle? (y/N): ");
        stepping = in.hasNextLine() && in.nextLine().trim().equalsIgnoreCase("y");
    }

    public static boolean check() {
        boolean ans = true;
        for (int i = 0; i < add.size; ++i) {
            ans &= add.sf[i].busy.equals("0");
        }
        for (int i = 0; i < mul.size; ++i) {
            ans &= mul.sf[i].busy.equals("0");
        }
        for (int i = 0; i < load.size; ++i) {
            ans &= load.loadBuffer[i].busy.equals("0");
        }
        for (int i = 0; i < store.size; ++i) {
            ans &= store.storeBuffer[i].busy.equals("0");
        }
        ans &= instructionUnit.instructionQueue.isEmpty();
        return ans;
    }
    public static void loadRegistersAndMemory(){

        for(int i = 0 ; i < registerFile.floatingPoint.length ; i++){
            registerFile.floatingPoint[i] =i;
        }
        for(int i = 0 ; i < registerFile.generalPurpose.length ; i++){
            registerFile.generalPurpose[i] =i;
        }
        for(int i = 0 ; i < cache.cache.length ; i++){
            cache.cache[i] =i;
        }
    }
    public static void main(String[] args) {
        try {
            initialize(args.length > 0 ? args[0] : "assembly.txt");
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }

        registerFile.generalPurpose[1] = 2;
        //loadRegistersAndMemory();

        Tomasulo();
    }
}
