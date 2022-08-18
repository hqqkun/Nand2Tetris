import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;

public class VMWriter {
    private PrintWriter vmWriter;
    private static final HashMap<Segment, String> segmentStringHashMap = new HashMap<>();
    private static final HashMap<ArithCmd, String> arithCmdStringHashMap = new HashMap<>();

    public static void init() {
        segmentStringHashMap.put(Segment.CONST, "constant");
        segmentStringHashMap.put(Segment.ARG,"argument");
        segmentStringHashMap.put(Segment.LOCAL,"local");
        segmentStringHashMap.put(Segment.STATIC,"static");
        segmentStringHashMap.put(Segment.THIS,"this");
        segmentStringHashMap.put(Segment.THAT,"that");
        segmentStringHashMap.put(Segment.POINTER,"pointer");
        segmentStringHashMap.put(Segment.TEMP,"temp");

        arithCmdStringHashMap.put(ArithCmd.ADD, "add");
        arithCmdStringHashMap.put(ArithCmd.SUB, "sub");
        arithCmdStringHashMap.put(ArithCmd.NEG, "neg");
        arithCmdStringHashMap.put(ArithCmd.NOT, "not");
        arithCmdStringHashMap.put(ArithCmd.AND, "and");
        arithCmdStringHashMap.put(ArithCmd.OR, "or");
        arithCmdStringHashMap.put(ArithCmd.EQ, "eq");
        arithCmdStringHashMap.put(ArithCmd.LT, "lt");
        arithCmdStringHashMap.put(ArithCmd.GT, "gt");
    }

    public VMWriter(File outputFile) {
        try {
            vmWriter = new PrintWriter(outputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void writePush(Segment segment, int index) {
        writeCommandLine("push", segmentStringHashMap.get(segment), String.valueOf(index));
    }

    public void writePop(Segment segment, int index) {
        writeCommandLine("pop", segmentStringHashMap.get(segment), String.valueOf(index));
    }

    public void writeArithmetic(ArithCmd command) {
        writeCommandLine(arithCmdStringHashMap.get(command), "", "");
    }
    public void writeCall(String name, int nArgs) {
        writeCommandLine("call", name, String.valueOf(nArgs));
    }

    public void writeIf(String label) {
        writeCommandLine("if-goto", label, "");
    }

    public void writeGoto(String label) {
        writeCommandLine("goto", label, "");
    }

    public void writeLabel(String label) {
        writeCommandLine("label", label, "");
    }

    public void writeReturn() {
        writeCommandLine("return", "", "");
    }

    public void writeFunction(String name, int nArgs) {
        writeCommandLine("function", name, String.valueOf(nArgs));
    }
    public void close() {
        vmWriter.close();
    }

    public void writeNewLine() {
        vmWriter.println();
    }
    private void writeCommandLine(String cmd, String arg1, String arg2) {
        vmWriter.println(cmd + ' ' + arg1 + ' ' + arg2);
    }

}