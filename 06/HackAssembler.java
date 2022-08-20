import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class HackAssembler {
    private Parser parser;
    private SymbolTable symbolTable;
    private PrintWriter codeWriter;
    private int staticVarIndex;
    
    private String processedInput;
    private final String outputFilePath;

    public static void main(String[] args) {
        if (args.length != 1) {    
            System.out.println("Usage:java HackAssembler [filename|directory]");
        } else {
            ArrayList<File> asmFiles = new ArrayList<>();
            File inputFile = new File(args[0]);
            if (inputFile.isFile()) {
                // only a single file.
                asmFiles.add(inputFile);
            } else {
                // Directory
                asmFiles = getASMFiles(inputFile);
            }
            Code.initCode();
            for (File f : asmFiles) {
                HackAssembler assembler = new HackAssembler(f);
                assembler.assemble();
                assembler.close();
            }
        }  
    }

    public HackAssembler(File inputFile) {
        try {
            processedInput = preProcessInput(inputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        {
            String inputFilePath = inputFile.getAbsolutePath();
            int index = inputFilePath.lastIndexOf('.');
            outputFilePath = inputFilePath.substring(0, index) + ".hack";
            File outputFile = new File(outputFilePath);
            initArgs(outputFile);
        }
    }

    public void assemble() {
        firstPass();
        secondPass();
        System.out.println("File created : " + outputFilePath);
    }

    //--------------------------------------------------------------
    private String preProcessInput(File inputFile) throws FileNotFoundException {
        Scanner sc = new Scanner(inputFile);
        StringBuilder sb = new StringBuilder();
        while (sc.hasNext()) {
            String line = sc.nextLine().trim();
            if (!isCommentOrBlank(line)) {
                sb.append(line).append("\n");
            }
        }
        sc.close();
        return sb.toString();
    }

    private boolean isCommentOrBlank(String commandLine) {
        return commandLine.length() <= 1 || commandLine.startsWith("//");
    }

    private void initArgs(File outputFile) {
        symbolTable = new SymbolTable();
        parser = new Parser(processedInput);
        try {
            codeWriter = new PrintWriter(outputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        staticVarIndex = 16;
    }

    // first pass is used to find all the labels.
    private void firstPass() {
        Scanner scanner = new Scanner(processedInput);

        for (int lineIndex = 0; scanner.hasNextLine(); ) {
            String currentInstr = scanner.nextLine().split("[ \t\r\b]+")[0];
            if (currentInstr.charAt(0) == '(') {
                // this is a label, add it to the symbol table.
                String symbol = currentInstr.substring(1, currentInstr.lastIndexOf(')'));
                symbolTable.addEntry(symbol, lineIndex);
            } else {
                ++lineIndex;
            }
        }
        scanner.close();
    }

    // second pass is used to generate code.
    private void secondPass() {
        while(parser.hasMoreLines()) {
            parser.advance();
            switch (parser.instructionType()) {
                case A_INSTRUCTION:
                    handleAInstr(); break;
                case C_INSTRUCTION:
                    handleCInstr(); break;
                default:
                    break;
            }
        }
    }

    // address instruction -> @address
    private void handleAInstr() {
        String symbol = parser.symbol();
        int address;

        if (symbol.matches("0|[1-9]\\d*")) {
            // number
            address = Integer.parseInt(symbol);
        } else {
            // symbol
            if (symbolTable.contains(symbol)) {
                address = symbolTable.getAddress(symbol);
            } else {
                // not find in symbol table.
                address = staticVarIndex;
                symbolTable.addEntry(symbol, address);
                ++staticVarIndex;
            }
        }
        String bitAddress = Integer.toBinaryString(address);
        // adding zeros.
        codeWriter.println("0" + addingZeros(bitAddress));
    }

    // compute instruction.
    private void handleCInstr() {
        String comp = Code.comp(parser.comp());
        String dest = Code.dest(parser.dest());
        String jump = Code.jump(parser.jump());
        codeWriter.println("111" + comp + dest + jump);
    }

    private void close() {
        parser.close();
        codeWriter.close();
    }

    private static String addingZeros(String strIn){
        StringBuilder strInBuilder = new StringBuilder(strIn);
        for (int i = strInBuilder.length(); i < 15; i++){
            strInBuilder.insert(0, "0");
        }
        strIn = strInBuilder.toString();
        return strIn;
    }

    private static ArrayList<File> getASMFiles(File dir) {
        File[] files = dir.listFiles();
        assert files != null;
        ArrayList<File> res = new ArrayList<>();
        
        for (File f : files) {
            if (f.getName().endsWith(".asm")) {
                res.add(f);
            }
        }
        return res;
    }
}
