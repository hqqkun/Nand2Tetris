import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Parser {
    private Scanner scanner;
    private String arg1;    // current command argument 1.
    private int arg2;
    private String[] commandLineSpilt;
    private CommandType currentCommentType;
    private String commandLine;

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
        if (currentCommentType == CommandType.C_ARITHMETIC) {
            arg1 = commandLineSpilt[0];
            arg2 = 0;
        } else {
            arg1 = commandLineSpilt[1];
            arg2 = Integer.parseInt(commandLineSpilt[2]);
        }
    }

    private boolean isCommentOrBlank(String commandLine) {
        return commandLine.length() <= 1 || commandLine.startsWith("//");
    }

    private CommandType getCommandType(String commandLine) {
        commandLineSpilt = commandLine.split(" +");
        switch (commandLineSpilt[0]) {
            case "push":    return CommandType.C_PUSH;
            case "pop":     return CommandType.C_POP;
            case "add":
            case "sub":
            case "neg":
            case "not":
            case "and":
            case "or":
            case "eq":
            case "lt":
            case "gt":
                            return CommandType.C_ARITHMETIC;
            default:        return CommandType.C_NONE;
        }
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
