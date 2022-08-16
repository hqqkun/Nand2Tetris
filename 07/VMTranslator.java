import java.io.File;

public class VMTranslator {

    public static void main(String[] args) {
        File inputFile = new File(args[0]);
        String fileName = args[0].substring(0, args[0].lastIndexOf('.'));
        File outputFile = new File(fileName + ".asm");
        CodeWriter.initCodeWriter();

        Parser parser = new Parser(inputFile);
        CodeWriter codeWriter = new CodeWriter(outputFile);
        while (parser.hasMoreLines()) {
            parser.advance();
            codeWriter.writeComment(parser.getCommandLine());
            CommandType type = parser.commandType();
            switch (type) {
                case C_ARITHMETIC:
                    codeWriter.writeArithmetic(parser.arg1());
                    break;
                case C_PUSH:
                case C_POP:
                    codeWriter.writePushPop(type, parser.arg1(), parser.arg2());
            }
        }

        codeWriter.close();
    }
}
