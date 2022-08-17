import java.io.File;
import java.util.ArrayList;

public class VMTranslator {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage:java VMTranslator [filename|directory]");
        } else {
            String fileInName = args[0];
            String outputFilePath;
            File inputFile = new File(fileInName);
            File outputFile;
            ArrayList<File> VMFiles = new ArrayList<>();

            if (inputFile.isFile()) {
                String path = inputFile.getAbsolutePath();
                VMFiles.add(inputFile);
                outputFilePath = path.substring(0, path.lastIndexOf('.')) + ".asm";
            } else {
                // Directory
                VMFiles = getVMFiles(inputFile);
                assert VMFiles.size() != 0;
                outputFilePath = inputFile.getAbsolutePath() + "/" + inputFile.getName() + ".asm";
            }
            outputFile = new File(outputFilePath);
            VMTranslator.init();
            CodeWriter codeWriter = new CodeWriter(outputFile);

            for(File f : VMFiles) {
                Parser parser = new Parser(f);
                codeWriter.setClassName(f);
                translate(parser, codeWriter);
            }
            codeWriter.close();
            System.out.println("File created : " + outputFilePath);
        }
    }

    private static ArrayList<File> getVMFiles(File dir) {
        File[] files = dir.listFiles();
        assert files != null;
        ArrayList<File> res = new ArrayList<>();
        
        for (File f : files) {
            if (f.getName().endsWith(".vm")) {
                res.add(f);
            }
        }
        return res;
    }

    private static void init() {
        CodeWriter.initCodeWriter();
        Parser.initParser();
    }

    private static void translate(Parser parser, CodeWriter codeWriter) {
        while (parser.hasMoreLines()) {
            parser.advance();
            codeWriter.writeComment(parser.getCommandLine());
            CommandType type = parser.commandType();

            switch (type) {
                case C_NONE:
                    System.err.println("Unknown command : "+ parser.getCommandLine());
                    System.exit(-1);
                case C_ARITHMETIC:
                    codeWriter.writeArithmetic(parser.arg1());  break;
                case C_PUSH:
                case C_POP:
                    codeWriter.writePushPop(type, parser.arg1(), parser.arg2());    break;
                case C_IF:
                    codeWriter.writeIf(parser.arg1());      break;
                case C_LABEL:
                    codeWriter.writeLabel(parser.arg1());   break;
                case C_GOTO:
                    codeWriter.writeGoto(parser.arg1());    break;
                case C_FUNCTION:
                    codeWriter.writeFunction(parser.arg1(), parser.arg2()); break;
                case C_CALL:
                    codeWriter.writeCall(parser.arg1(), parser.arg2());     break;
                case C_RETURN:
                    codeWriter.writeReturn();               break;
            }
        }
    }
}