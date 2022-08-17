import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;

public class CodeWriter {
    private PrintWriter codeWriter;
    // when compile a file, use this file name, or it's class name. Used when handle static variable.
    private String className;   
    private String functionName;

    /* when compile a function, this should be true. 
     * normally it is always true, unless we compile a naive file without any function.
     */ 
    private boolean isInFuncDec;    
    private static final HashMap<String, String> segmentNameHashMap = new HashMap<>();
    private static final HashMap<String, String[]> arithWithLabelHashMap = new HashMap<>();
    // used when calls a function. Value means next index which can be used.
    private HashMap<String, Integer> functionIndexHashMap;  
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
        initArgs();
        //writeBootstrap();
    }

    private void initArgs() {
        className = "";
        functionName = "";
        labelIndex = 0;
        isInFuncDec = false;
        functionIndexHashMap = new HashMap<>();
    }

    // set current className to be the current processing file name.
    public void setClassName(File file) {
        this.className = file.getName();
    }

    private void writeBootstrap() {
        // init stack pointer.
        codeWriter.print("@256\nD=A\n@SP\nM=D\n");
        // call Sys.init 0
        writeCall("Sys.init", 0);
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

    // label someLabel
    public void writeLabel(String label) {
        if (isInFuncDec) {
             label = functionName + "$" + label;
        }
        codeWriter.print("(" + label + ")\n");
    }

    // goto label
    public void writeGoto(String label) {
        if (isInFuncDec) {
            label = functionName + "$" + label;
        }
        codeWriter.print("@" + label + "\n0;JMP\n");
    }

    // if-goto label
    public void writeIf(String label) {
        if (isInFuncDec) {
            label = functionName + "$" + label;
        }
        PopSnippet();
        // new Data is in D reg.
        codeWriter.print("@" + label + "\nD;JNE\n");
    }

    /* first write a label to indicate that we are writing a function.
     * then make room for nArgs.
     */
    public void writeFunction(String functionName, int nArgs) {
        this.isInFuncDec = true;
        this.functionName = functionName;
        codeWriter.print("(" + functionName + ")\n");
        for (int i = 0; i != nArgs; ++i) {
            handlePushConstantZero();
        }
    }

    public void writeCall(String functionName, int nArgs) {
        String returnAddress;
        // use hashmap to use next available index.
        int index = functionIndexHashMap.getOrDefault(functionName, 0);
        functionIndexHashMap.put(functionName, index + 1);

        returnAddress = functionName + "$ret." + index;
        // push return address, LCL, ARG, THIS, THAT.
        pushRoutine(returnAddress);
        // ARG = SP - 5 - nArgs.
        codeWriter.print("@SP\nD=M\n@5\nD=D-A\n@" + nArgs + "\nD=D-A\n");   // D = SP - 5 - nArgs.
        codeWriter.print("@ARG\nM=D\n");
        // LCL = SP
        codeWriter.print("@SP\nD=M\n@LCL\nM=D\n");
        // goto functionName
        codeWriter.print("@" + functionName + "\n0;JMP\n");
        // (returnAddress)
        codeWriter.print("(" + returnAddress + ")\n");
    }


    // assume R13 := LCL, R14 := retAddress.
    public void writeReturn() {
        // frame = LCL, let R13 stores LCL.
        codeWriter.print("@LCL\nD=M\n@R13\nM=D\n");
        // retAddress = *(frame - 5)
        codeWriter.print("@R13\nD=M\n@5\nA=D-A\nD=M\n@R14\nM=D\n");
        // *ARG = pop()
        PopSnippet();
        codeWriter.print("@ARG\nA=M\nM=D\n");
        // SP = ARG + 1
        codeWriter.print("@ARG\nD=M\n@SP\nM=D+1\n");
        // Pop THIS, THAT, ARG, LCL.
        backToSegmentRoutine();
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
            command += (intVal + "\nA=D+A\nD=M\n");
        } else {
            switch (segment) {
                case "constant":
                    command = (intVal + "\nD=A\n");   break;
                case "static":
                    command = handleStaticVar(true, index);     break;
                case "pointer":
                    command = handlePointer(true, index);       break;
                case "temp":
                    command = handleTempSegment(true, index); break;
            }
        }
        codeWriter.print(command);
        // assume data has already in D reg.
        pushSnippet();
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

    // eq, lt, gt
    private void handleArithWithBranch(String command) {
        makeDRegWithValue();
        // assume D already has value x - y.
        String[] labelArray = arithWithLabelHashMap.get(command);
        String labelElse = labelArray[0] + labelIndex;
        String labelDone = labelArray[1] + labelIndex;
        String cmd = labelArray[2];
        {
            codeWriter.print("@" + labelElse + "\n" + cmd + "\n");
            handleStackAssignTrueOrFalse(true); // push true
            codeWriter.print("@" + labelDone + "\n0;JMP\n");
            codeWriter.print("(" + labelElse + ")\n");
            handleStackAssignTrueOrFalse(false);    // push false
            codeWriter.print("(" + labelDone + ")\n");
        }
        ++labelIndex;
    }

    // pop y and overlap x - y on x.s
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
            return "@" + staticVarName + "\nD=M\n";
        } else {
            return "@" + staticVarName + "\nD=A\n@R13\nM=D\n";

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
        String preFix = "@R5\nD=A\n@" + index;
        return isPush ? (preFix + "\nA=D+A\nD=M\n") : (preFix + "\nD=D+A\n@R13\nM=D\n");
    }

    private void handlePushConstantZero() {
        codeWriter.print("@SP\nA=M\nM=0\n@SP\nM=M+1\n");
    }

    // assume data need to push is in D reg.
    private void pushSnippet() {
        codeWriter.print("@SP\nA=M\nM=D\n@SP\nM=M+1\n");
    }

    /* Pop item from stack, data is now in D reg. */
    private void PopSnippet() {
        codeWriter.print("@SP\nAM=M-1\nD=M\n");
    }

    // used when calling a function needs to push several segments.
    private void pushRoutine(String returnAddress) {
        // push return address.
        pushWithAddress(returnAddress, false);
        // push LCL
        pushWithAddress("LCL", true);
        // push ARG
        pushWithAddress("ARG", true);
        // push THIS
        pushWithAddress("THIS", true);
        // push THAT
        pushWithAddress("THAT", true);
    }

    // if content is true, then we want to push a value, which means D=M.
    private void pushWithAddress(String address, boolean content) {
        String value = "@" + address + "\n";
        value = content ? (value + "D=M\n") : (value + "D=A\n");
        codeWriter.print(value);
        pushSnippet();
    }

    // assume R13 holds frame.
    private void backToSegmentRoutine() {
        // THAT = *(frame - 1)
        R13BasePointerPop("THAT");
        // THIS = *(frame - 2)
        R13BasePointerPop("THIS");
        // ARG = *(frame - 3)
        R13BasePointerPop("ARG");
        // LCL = *(frame - 4)
        R13BasePointerPop("LCL");
        // goto retAddress
        codeWriter.print("@R14\nA=M\n0;JMP\n");
    }

    // just to make life easier.
    private void R13BasePointerPop(String address) {
        codeWriter.print("@R13\nAM=M-1\nD=M\n@" + address + "\nM=D\n");
    }

    public void writeComment(String comment) {
        codeWriter.println("//" + comment);
    }
}
