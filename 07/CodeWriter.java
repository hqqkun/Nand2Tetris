import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;

public class CodeWriter {
    private PrintWriter codeWriter;
    private final String className;

    private static final HashMap<String, String> segmentNameHashMap = new HashMap<>();
    private static final HashMap<String, String[]> arithWithLabelHashMap = new HashMap<>();
    private int labelIndex;

    public static void initCodeWriter() {
        segmentNameHashMap.put("local", "LCL");
        segmentNameHashMap.put("argument", "ARG");
        segmentNameHashMap.put("this", "THIS");
        segmentNameHashMap.put("that", "THAT");
        arithWithLabelHashMap.put("eq", new String[]{"EQ_ELSE_", "EQ_DONE_", "D;JNE"});
        arithWithLabelHashMap.put("lt", new String[]{"LT_ELSE_", "LT_DONE_", "D;JGE"});
        arithWithLabelHashMap.put("gt", new String[]{"GT_ELSE_", "GT_DONE_", "D;JLE"});
    }

    public CodeWriter(File outputFile) {
        try {
            codeWriter = new PrintWriter(outputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        {
            int index = outputFile.getName().lastIndexOf('.');
            className = outputFile.getName().substring(0, index);
            labelIndex = 0;
        }
    }

    public void writeArithmetic(String command) {
        switch (command) {
            case "add":
            case "sub":
            case "and":
            case "or":
                        handleArithWithTwoArgs(command);    break;
            case "neg":
            case "not":
                        handleArithWithOneArg(command);     break;
            case "eq":
            case "gt":
            case "lt":
                        handleArithWithBranch(command);     break;
        }
    }

    public void writePushPop(CommandType type, String segment, int index) {
        if (type == CommandType.C_PUSH) {
            handlePush(segment, index);
        } else {
            handlePop(segment, index);
        }
    }

    public void close() {
        codeWriter.close();
    }

    /*-------------------------------------------------*/
    private void handlePush(String segment, int index) {
        String command = "";
        String intVal = "@" + index;

        if (segmentNameHashMap.containsKey(segment)) {
            // local, argument, this, that
            command = ("@" + segmentNameHashMap.get(segment) + "\n" + "D=M\n");
            command += (intVal + "\n" + "A=D+A\nD=M\n");
        } else {
            switch (segment) {
                case "constant":
                    command = (intVal + "\n" + "D=A\n");   break;
                case "static":
                    command = handleStaticVar(true, index);     break;
                case "pointer":
                    command = handlePointer(true, index);       break;
                case "temp":
                    command = handleTempSegment(true, index); break;
            }
        }

        // assume data has already in D reg.
        command += "@SP\nA=M\nM=D\n@SP\nM=M+1\n";
        codeWriter.print(command);
    }

    /* when we pop an element from the stack, R13 holds variable's address. */
    private void handlePop(String segment, int index) {
        String command = "";

        // before the real "pop", make sure the variable's address is in R13.
        if (segmentNameHashMap.containsKey(segment)) {
            // local, argument, this, that
            command = "@" + segmentNameHashMap.get(segment) + "\nD=M\n";
            command += ("@" + index + "\nD=D+A\n@R13\nM=D\n");
        } else {
            switch (segment) {
                case "static" : command = handleStaticVar(false, index); break;
                case "pointer": command = handlePointer(false, index);  break;
                case "temp":    command = handleTempSegment(false, index);  break;

            }
        }

        command += ("@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");
        codeWriter.print(command);
    }

    // sometimes, we need to pop to R13 ~ R15.
    private void handlePopTempRegs(String reg) {
        codeWriter.print("@SP\nAM=M-1\nD=M\n@"+ reg + "\nM=D\n");
    }

    // x op y
    private void handleArithWithTwoArgs(String command) {
        codeWriter.print("@SP\nAM=M-1\nD=M\nA=A-1\n");  // D = y
        switch (command) {
            case "add": codeWriter.println("M=M+D");    return;
            case "sub": codeWriter.println("M=M-D");    return;
            case "and": codeWriter.println("M=M&D");    return;
            case "or":  codeWriter.println("M=M|D");
        }
    }

    private void handleArithWithOneArg(String command) {
        codeWriter.print("@SP\nA=M-1\n");
        switch (command) {
            case "neg": codeWriter.println("M=-M"); return;
            case "not": codeWriter.println("M=!M");
        }
    }

    private void handleArithWithBranch(String command) {
        makeDRegWithValue();
        // assume D already has value x - y.
        String[] labelArray = arithWithLabelHashMap.get(command);
        String labelElse = labelArray[0] + labelIndex;
        String labelDone = labelArray[1] + labelIndex;
        String cmd = labelArray[2];
        {
            codeWriter.print("@" + labelElse + "\n" + cmd + "\n");
            handleStackAssignTrueOrFalse(true);
            codeWriter.print("@" + labelDone + "\n0;JMP\n");
            codeWriter.print("(" + labelElse + ")\n");
            handleStackAssignTrueOrFalse(false);
            codeWriter.print("(" + labelDone + ")\n");
        }
        ++labelIndex;
    }

    private void makeDRegWithValue() {
        codeWriter.print("@SP\nAM=M-1\nD=M\nA=A-1\nD=M-D\n");
    }

    private void handleStackAssignTrueOrFalse(boolean trueFlag) {
        String flag = trueFlag? ("M=-1") : ("M=0");
        codeWriter.print("@SP\nA=M-1\n" + flag + "\n");
    }

    private String handleStaticVar(boolean isPush, int index) {
        String staticVarName = className + "." + index;
        if (isPush) {
            return "@" + staticVarName + "\n" + "D=M\n";
        } else {
            return "@" + staticVarName + "\n" + "D=A\n@R13\nM=D\n";

        }
    }

    private String handlePointer(boolean isPush, int index) {
        if (isPush) {
            return (index == 0) ? ("@THIS\nD=M\n") : ("@THAT\nD=M\n");
        } else {
            String preFix = (index == 0) ? ("@THIS\n") : ("@THAT\n");
            return preFix + "D=A\n@R13\nM=D\n";
        }
    }

    private String handleTempSegment(boolean isPush, int index) {
        String preFix = "@R5\nD=A\n" + "@" + index;
        return isPush ? (preFix + "\nA=D+A\nD=M\n") : (preFix + "\nD=D+A\n@R13\nM=D\n");
    }

    public void writeComment(String comment) {
        codeWriter.println("//" + comment);
    }
}
