# Tomasulo Simulator

A command-line, cycle-by-cycle simulator of Tomasulo's dynamic scheduling algorithm, written in Java. It was built for CSEN 702: Microprocessors (GUC, Winter 2023).

It reads MIPS-style assembly from a text file. For every cycle, it prints the instruction queue, reservation stations, load and store buffers, cache, and register file. It ends with a timing table for all instructions.

## Getting started

You need a JDK. The IntelliJ project is set to Java 16, and the simulator has also been run on Java 23. There are no other dependencies.

Run these commands from this directory, because `assembly.txt` is loaded relative to the working directory:

```sh
javac -d out/production/Tomasulo src/*.java
java -cp out/production/Tomasulo Tomasulo
```

You can also open the folder in IntelliJ IDEA and run `Tomasulo.main`. To run a different program, pass its path as an argument: `java -cp out/production/Tomasulo Tomasulo my_program.txt`.

The simulator asks for these values, in this order. Press Enter to accept the default.

| # | Prompt | Default |
|---|--------|---------|
| 1 | `MUL.D` latency | `6` |
| 2 | `DIV.D` latency | `6` |
| 3 | Add latency (`ADD.D`, `ADD`, `DADD`) | `4` |
| 4 | Subtract latency (`SUB.D`, `SUB`, `DSUB`) | `4` |
| 5 | `L.D` latency | `1` |
| 6 | `S.D` latency | `1` |
| 7 | Add/sub station size | `3` |
| 8 | Mul/div station size | `2` |
| 9 | Load buffer size | `2` |
| 10 | Store buffer size | `2` |
| 11 | Cache size (number of addresses) | `16`, or more if the program needs it |
| 12 | Pause after each cycle? | No |

If you choose to pause, press Enter for the next cycle, `r` to run to the end, or `q` to quit and see the timing table so far.

Without pausing, the full trace is long, so you may want to save it to a file. Input that ends early takes the remaining defaults:

```sh
java -cp out/production/Tomasulo Tomasulo < /dev/null > trace.txt
```

To run the regression check:

```sh
javac -d out/check src/*.java test/Check.java
java -cp out/check Check
```

## Writing a program

Edit `assembly.txt`, with one instruction on each line. Operands can be separated by spaces or commas, and case doesn't matter. A label ends with a colon and can sit on its own line or before an instruction. Text after `#` or `;` is a comment.

The program is checked before the simulation starts, and any error is reported with its line number.

```
LOOP:
L.D F1 1
ADD.D F3 F1 F1
S.D F3 3
SUBI R1 R1 1
BNEZ R1 LOOP
ADDI R1 R1 5
```

| Instructions | Format | Station | Execution cycles |
|---|---|---|---|
| `ADD.D`, `SUB.D` | `OP Fd Fs Ft` | Add/sub | Add or subtract latency |
| `MUL.D`, `DIV.D` | `OP Fd Fs Ft` | Mul/div | `MUL.D` or `DIV.D` latency |
| `ADD`, `SUB`, `DADD`, `DSUB` | `OP Rd Rs Rt` | Add/sub | Add or subtract latency |
| `ADDI`, `SUBI` | `OP Rd Rs imm` | Add/sub | 1 |
| `BNEZ` | `BNEZ Rs LABEL` | Add/sub | 1 |
| `L.D` | `L.D Fd address` | Load buffer | `L.D` latency |
| `S.D` | `S.D Fs address` | Store buffer | `S.D` latency |

- **Addresses:** these are effective addresses. The cache size prompt won't accept a size too small for the program.
- **Registers:** there are 32 of each type, `F0`–`F31` and `R0`–`R31`. Writes to `F0` and `R0` are discarded.
- **Initial values:** everything starts at 0, except `R1`, which `main` sets to 2 so the sample loop runs twice. To preload more values, edit `main` or uncomment its call to `loadRegistersAndMemory()`, which sets each register and cache address to its own index.

## How it works

Each cycle runs four steps in `Tomasulo.java`: `issue()`, `execute()`, `writeBack()`, and `broadCast()`.

- **Issue:** one instruction is issued per cycle, in order, and only if a matching station is free. Issue also stops after a `BNEZ` until the branch resolves. There is no branch prediction.
- **Execute:** an instruction starts no earlier than the cycle after it issues, and only once its operands are ready. Only one load or store can finish its memory access per cycle, with loads going first, and any other that is ready waits. Stores write to the cache at the end of execution, and loads read the cache at write-back.
- **Write-back:** one instruction writes back per cycle, no earlier than the cycle after it finishes executing. Stores and branches also use this slot.
- **Broadcast:** the result goes out on the common data bus to every waiting station and buffer.

### Bus arbitration

When several instructions are ready to write back in the same cycle, `favouringUltraMax()` picks one:

1. The instruction with the most stations and buffers waiting on its tag wins.
2. If that is tied, the instruction issued earliest wins.

### Hazards

| Hazard | Handling |
|---|---|
| RAW | The instruction waits on the producer's tag (`Qj`, `Qk`, or `Q`) until it is broadcast. |
| WAR | Available source values are copied into the station at issue. |
| WAW | The later writer records the earlier writer's tag in `Qz` and can't write back until that tag is broadcast. |

The WAW handling is stricter than classic Tomasulo, where the earlier write would simply not update the register. Here, writes to the same register finish in program order.

### Branches

A taken `BNEZ` rebuilds the instruction queue from the label's position. If the branch isn't taken, the queue continues as it was. A run stops after 10,000 cycles in case a loop never exits.

## Example output

The sample program, using the default inputs, ends with this timing table. `BNEZ` finishes executing in cycle 6 but writes back in cycle 8, because `ADD.D` has a store waiting on it and wins the bus in cycle 7.

```
Op        Result    V1        V2        Issued    Execution Write Back
L.D       F1        1         -         0         1...1     2
ADD.D     F3        F1        F1        1         3...6     7
S.D       F3        3         -         2         8...8     9
SUBI      R1        R1        1         3         4...4     5
BNEZ      0         R1        -         4         6...6     8
L.D       F1        1         -         9         10...10   11
ADD.D     F3        F1        F1        10        12...15   16
S.D       F3        3         -         11        17...17   18
SUBI      R1        R1        1         12        13...13   14
BNEZ      0         R1        -         13        15...15   17
ADDI      R1        R1        5         18        19...19   20
```

For `BNEZ`, the `Result` column shows the index of the branch target.

## Project layout

```
assembly.txt                  Default program to simulate
test/
  Check.java                  Regression check
src/
  Tomasulo.java               Entry point, prompts, and the simulation loop
  InstructionUnit.java        Parses and validates the program, and holds the instruction queue
  Instruction.java            One instruction and its timing
  ReservationStation.java     Add/sub and mul/div stations
  StationFields.java          One reservation station entry
  LoadBuffer.java             Load buffer
  LoadBufferFields.java       One load buffer entry
  StoreBuffer.java            Store buffer
  StoreBufferFields.java      One store buffer entry
  RegisterFile.java           F and R registers with their Qi tags
  Cache.java                  Memory, with no misses modelled
  CommonDataBus.java          Current bus tag and value
  Result.java                 Not used
```

## Limitations

- **Initial values:** registers and the cache can't be set from the prompts. Change them in `main`, as described in [Writing a program](#writing-a-program).
