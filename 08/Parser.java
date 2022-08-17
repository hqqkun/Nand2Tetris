import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

public class Parser {
    private Scanner scanner;
    private String arg1;    // current command argument 1.
    private int arg2;
    private String[] commandLineSpilt;
    private CommandType currentCommentType;
    private String commandLine;

    static private final HashMap<String, CommandType> stringCommandTypeHashMap = new HashMap<>();

    static public void initParser() {
        stringCommandTypeHashMap.put("push", CommandType.C_PUSH);
        stringCommandTypeHashMap.put("pop", CommandType.C_POP);
        stringCommandTypeHashMap.put("add", CommandType.C_ARITHMETIC);
        stringCommandTypeHashMap.put("sub", CommandType.C_ARITHMETIC);
        stringCommandTypeHashMap.put("neg", CommandType.C_ARITHMETIC);
        stringCommandTypeHashMap.put("not", CommandType.C_ARITHMETIC);
        stringCommandTypeHashMap.put("and", CommandType.C_ARITHMETIC);
        stringCommandTypeHashMap.put("or", CommandType.C_ARITHMETIC);
        stringCommandTypeHashMap.put("eq", CommandType.C_ARITHMETIC);
        stringCommandTypeHashMap.put("lt", CommandType.C_ARITHMETIC);
        stringCommandTypeHashMap.put("gt", CommandType.C_ARITHMETIC);
        stringCommandTypeHashMap.put("if-goto", CommandType.C_IF);
        stringCommandTypeHashMap.put("label", CommandType.C_LABEL);
        stringCommandTypeHashMap.put("goto", CommandType.C_GOTO);
        stringCommandTypeHashMap.put("function", CommandType.C_FUNCTION);
        stringCommandTypeHashMap.put("call", CommandType.C_CALL);
        stringCommandTypeHashMap.put("return", CommandType.C_RETURN);
    }

    Parser(File inputFile) {
        try {
            scanner = new Scanner(preProcessInput(inputFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        initArgs();
    }

    private String preProcessInput(File inputFile) throws FileNotFoundException {
        Scanner sc = new Scanner(inputFile);
        StringBuilder sb = new StringBuilder();
        while (sc.hasNext()) {
            String line = sc.nextLine().trim();
            if (!isCommentOrBlank(line)) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private void initArgs() {
        commandLineSpilt = null;
        commandLine = "";
        currentCommentType = CommandType.C_NONE;
        arg1 = "";
        arg2 = 0;
    }

    public boolean hasMoreLines() {
        return scanner.hasNextLine();
    }

    public void advance() {
        commandLine = scanner.nextLine();
        currentCommentType = getCommandType(commandLine);
        switch (currentCommentType) {
            case C_NONE:    return;
            case C_ARITHMETIC:
            case C_RETURN:
                            arg1 = commandLineSpilt[0];
                            arg2 = 0;   return;
            case C_LABEL:
            case C_IF:
            case C_GOTO:
                            arg2 = 0;   break;
            default:
                            arg2 = Integer.parseInt(commandLineSpilt[2]);
        }
        arg1 = commandLineSpilt[1];
    }

    private boolean isCommentOrBlank(String commandLine) {
        return commandLine.length() <= 1 || commandLine.startsWith("//");
    }

    private CommandType getCommandType(String commandLine) {
        commandLineSpilt = commandLine.split("[ \t\b\r]+");
        return stringCommandTypeHashMap.getOrDefault(commandLineSpilt[0], CommandType.C_NONE);
    }

    public String arg1() {
        return arg1;
    }

    public int arg2() {
        return arg2;
    }

    public CommandType commandType() {
        return currentCommentType;
    }

    public String getCommandLine() {
        return commandLine;
    }
}