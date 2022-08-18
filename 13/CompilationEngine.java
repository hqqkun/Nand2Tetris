import javafx.util.Pair;
import java.io.File;
import java.util.HashMap;

public class CompilationEngine {
    private final JackTokenizer jackTokenizer;
    private final VMWriter vmWriter;
    private final SymbolTable symbolTable;
    private String thisClassName;
    private String currentSubroutineName;
    private Keyword currentSubroutineType;      // cons, function or method.

    // label used when if-else, while
    private int labelIndex;

    private static final HashMap<Character, Operator> opMap = new HashMap<>();
    private static final HashMap<Kind, Segment> KindSegmentHashMap = new HashMap<>();
    private static final HashMap<Operator, ArithCmd> opAriCmdHashMap = new HashMap<>();

    public CompilationEngine(File inputFile, File outputFile) {
        jackTokenizer = new JackTokenizer(inputFile);
        symbolTable = new SymbolTable();
        vmWriter = new VMWriter(outputFile);
        currentSubroutineName = null;
        labelIndex = 0;
    }

    private static void initHashMap() {
        initKindSegmentHashMap();
        initOpArithCmdHashMap();
        initOpMap();
    }

    private static void initOpArithCmdHashMap() {
        opAriCmdHashMap.put(Operator.GT, ArithCmd.GT);
        opAriCmdHashMap.put(Operator.LT, ArithCmd.LT);
        opAriCmdHashMap.put(Operator.EQ, ArithCmd.EQ);
        opAriCmdHashMap.put(Operator.PLUS, ArithCmd.ADD);
        opAriCmdHashMap.put(Operator.MINUS, ArithCmd.SUB);
        opAriCmdHashMap.put(Operator.AND, ArithCmd.AND);
        opAriCmdHashMap.put(Operator.OR, ArithCmd.OR);
    }
    private static void initKindSegmentHashMap() {
        KindSegmentHashMap.put(Kind.ARG, Segment.ARG);
        KindSegmentHashMap.put(Kind.VAR, Segment.LOCAL);
        KindSegmentHashMap.put(Kind.STATIC, Segment.STATIC);
        KindSegmentHashMap.put(Kind.FIELD, Segment.THIS);
    }
    private static void initOpMap() {
        opMap.put('+', Operator.PLUS);
        opMap.put('-', Operator.MINUS);
        opMap.put('*', Operator.MULTIPLE);
        opMap.put('/', Operator.DIVIDE);
        opMap.put('&', Operator.AND);
        opMap.put('|', Operator.OR);
        opMap.put('<', Operator.LT);
        opMap.put('>', Operator.GT);
        opMap.put('=', Operator.EQ);
    }

    public static void init() {
        JackTokenizer.init();
        VMWriter.init();
        initHashMap();
    }

    public void compileClass() {
        safeAdvance();
        // parsing start now.

        eatKeyword("class");
        thisClassName = handleClassName();
        eatSymbol('{');
        while (isClassVarDec()) {
            compileClassVarDec();
        }
        while (isSubroutineDec()) {
            compileSubroutineDec();
            vmWriter.writeNewLine();
        }
        eatSymbol('}');
        close();    // close writing file.
    }

    private void compileParameterList() {
        final Kind kind = Kind.ARG;

        if (!isSymbolMatchInput(')')) {
            // has at least one argument.
            symbolTableAddEntry(kind, null);
            while(isSymbolMatchInput(',')) {
                eatSymbol(',');
                symbolTableAddEntry(kind, null);
            }
        }
    }

    private void compileClassVarDec() {
        Kind kind = null;

        switch (jackTokenizer.keyword()) {
            case STATIC:    kind = Kind.STATIC; eatKeyword("static");   break;
            case FIELD:     kind = Kind.FIELD;  eatKeyword("field");    break;
        }
        assert kind != null;

        String type = symbolTableAddEntry(kind, null);
        while(isSymbolMatchInput(',')) {
            eatSymbol(',');
            symbolTableAddEntry(kind, type);
        }
        eatSymbol(';');
    }

    private void compileExpression() {
        compileTerm();
        while(isOP(jackTokenizer.symbol())) {
            Operator op = handleOp();
            compileTerm();
            writeOp(op);
        }
    }

    private void compileStatements() {
        while(!isSymbolMatchInput('}')) {
            handleStatement();
        }
    }

    private void compileDo() {
        eatKeyword("do");
        handleSubroutineCall();
        eatSymbol(';');
        vmWriter.writePop(Segment.TEMP, 0);
    }

    private void compileReturn() {
        eatKeyword("return");
        // return (expression)?;
        if (!isSymbolMatchInput(';')) {
            compileExpression();    // has expression.
        } else {
            vmWriter.writePush(Segment.CONST, 0);
        }
        eatSymbol(';');
        vmWriter.writeReturn();
    }

    private void compileIf() {
        String L1 = freshLabel();

        eatKeyword("if");
        eatSymbol('(' );
        compileExpression();
        vmWriter.writeArithmetic(ArithCmd.NOT);
        eatSymbol(')');
        vmWriter.writeIf(L1);
        eatSymbol('{');
        compileStatements();    // if statements
        eatSymbol('}');
        // handle else
        if (jackTokenizer.hasMoreTokens() && isKeywordMatchInput(Keyword.ELSE)) {
            String L2 = freshLabel();
            vmWriter.writeGoto(L2);
            vmWriter.writeLabel(L1);
            handleElse();
            vmWriter.writeLabel(L2);
        } else {
            // just if clause, no else branch
            vmWriter.writeLabel(L1);
        }
    }

    private void compileLet() {
        eatKeyword("let");
        String varName = handleVarName();
        boolean isArray = false;
        // ([expression])?
        if (isSymbolMatchInput('[')) {
            // arr[exp1] = exp2
            isArray = true;
            pushOrPopToVarByName(varName, true);
            eatSymbol('[');
            compileExpression();    // exp1
            eatSymbol(']');
            vmWriter.writeArithmetic(ArithCmd.ADD);
        }
        eatSymbol('=');
        compileExpression();    // exp2
        eatSymbol(';');
        if (isArray) {
            writeArrayAssign();
        } else {
            pushOrPopToVarByName(varName, false);
        }
    }

    private void compileWhile() {
        String loopLabel = freshLabel();
        String doneLabel = freshLabel();
        vmWriter.writeLabel(loopLabel);

        eatKeyword("while");
        eatSymbol('(');
        compileExpression();
        vmWriter.writeArithmetic(ArithCmd.NOT);
        vmWriter.writeIf(doneLabel);
        eatSymbol(')');
        eatSymbol('{');
        compileStatements();
        eatSymbol('}');
        vmWriter.writeGoto(loopLabel);
        vmWriter.writeLabel(doneLabel);
    }

    private void compileSubroutineDec() {
        symbolTable.startSubroutine();
        currentSubroutineType = jackTokenizer.keyword();
        switch (currentSubroutineType) {
            case CONSTRUCTOR:   eatKeyword("constructor");  break;
            case FUNCTION:      eatKeyword("function");     break;
            case METHOD:        eatKeyword("method");
                                symbolTable.define("this", thisClassName, Kind.ARG);
                                break;
        }
        if (isKeywordMatchInput(Keyword.VOID)) {
            eatKeyword("void");
        } else {
            handleType();
        }
        currentSubroutineName = handleSubroutineName();
        eatSymbol('(');
        compileParameterList();
        eatSymbol(')');
        compileSubroutineBody();
    }

    private void compileSubroutineBody() {
        eatSymbol('{');
        // handle varDec
        while (isKeywordMatchInput(Keyword.VAR)) {
            compileVarDec();
        }
        // writeVMWriter
        writeFuncDec(thisClassName, currentSubroutineName, symbolTable.varCount(Kind.VAR));
        switch (currentSubroutineType) {
            case CONSTRUCTOR:   writeConstructorHeader();  break;
            case METHOD:        writeThisPointToObject();   break;
        }
        compileStatements();
        eatSymbol('}');
    }

    private void compileVarDec() {
        final Kind kind = Kind.VAR;

        eatKeyword("var");
        String type = symbolTableAddEntry(kind, null);
        while(isSymbolMatchInput(',')) {
            eatSymbol(',');
            symbolTableAddEntry(kind, type);
        }
        eatSymbol(';');
    }

    private void compileTerm() {
        switch (jackTokenizer.tokenType()) {
            case INT_CONST_HEX:
            case INT_CONST_DEC:     writeIntegerConstant();    break;
            case STRING_CONST:  writeStringConstant();     break;
            case KEYWORD:       writeKeywordConstant();    break;
            case IDENTIFIER:    writeIdentifierInTerm();   break;
            case SYMBOL:        writeSymbolInTerm();       break;
        }
    }

    private int compileExpressionList() {
        int nArgs = 0;
        if (!isSymbolMatchInput(')')) {
            compileExpression();
            ++nArgs;
            while(isSymbolMatchInput(',')) {
                eatSymbol(',');
                compileExpression();
                ++nArgs;
            }
        }
        return nArgs;
    }

    // goto label
    private void compileGoto() {
        eatKeyword("goto");
        String label = handleIdentifier();
        label = thisClassName + "." + currentSubroutineName + "_" + label;
        eatSymbol(';');
        vmWriter.writeGoto(label);
    }

    /*****************************************************************************/
    private Operator handleOp() {
        Character ch = jackTokenizer.symbol();
        Operator op = opMap.get(ch);
        safeAdvance();
        return op;
    }

    private void handleSubroutineCall() {
        // yet use LL(2) again.
        Pair<Type, String> nextToken = jackTokenizer.peekNextToken();
        switch (nextToken.getValue().charAt(0)) {
            case '(':
                writeThisClassMethodCall();
                break;
            case '.':
                writeWithNameFunctionCall();
                break;
        }
    }

    private void handleStatement() {
        Type type = jackTokenizer.tokenType();
        assert type == Type.KEYWORD || type == Type.LABEL;
        if (type == Type.LABEL) {
            // label:
            handleLabel();
        } else {
            switch (jackTokenizer.keyword()) {
                case IF:        compileIf();        break;
                case LET:       compileLet();       break;
                case DO:        compileDo();        break;
                case WHILE:     compileWhile();     break;
                case RETURN:    compileReturn();    break;
                case GOTO:      compileGoto();      break;
            }
        }
    }

    private void handleLabel() {
        String label = handleIdentifier();
        label = thisClassName + "." + currentSubroutineName + "_" + label;
        vmWriter.writeLabel(label);
    }

    private void handleElse() {
        eatKeyword("else");
        eatSymbol('{');
        compileStatements();
        eatSymbol('}');
    }

    private void writeIdentifierInTerm() {
        // here, we use LL(2), which means lookahead 2 tokens.
        Pair<Type, String> nextToken = jackTokenizer.peekNextToken();
        Type nextTokenType = nextToken.getKey();

        if (nextTokenType == Type.SYMBOL) {
            // varName[exp], subroutineCall
            switch (nextToken.getValue().charAt(0)) {
                case '[':   // array, varName[exp]
                    pushOrPopToVarByName(handleVarName(), true);
                    eatSymbol('[');
                    compileExpression();
                    eatSymbol(']');
                    vmWriter.writeArithmetic(ArithCmd.ADD);
                    vmWriter.writePop(Segment.POINTER, 1);
                    vmWriter.writePush(Segment.THAT, 0);
                    break;
                case '(':
                case '.':
                    handleSubroutineCall();
                    break;
                default:
                    pushOrPopToVarByName(handleVarName(), true);
            }
        } else {
            pushOrPopToVarByName(handleVarName(), true);
        }
    }

    private void writeSymbolInTerm() {
        char symbol = jackTokenizer.symbol();
        ArithCmd cmd = (symbol == '-')? ArithCmd.NEG : ArithCmd.NOT;

        switch (symbol) {
            case '(':
                eatSymbol('(');
                compileExpression();
                eatSymbol(')');
                break;
            case '-':
            case '~':
                // above two are unary op.
                eatSymbol(symbol);
                compileTerm();
                vmWriter.writeArithmetic(cmd);
                break;
        }
    }

    private void writeStringConstant() {
        String str = jackTokenizer.stringVal();
        int length = str.length();
        vmWriter.writePush(Segment.CONST, length);
        vmWriter.writeCall("String.new", 1);
        for(int i = 0; i != length; ++i) {
            vmWriter.writePush(Segment.CONST, str.charAt(i));
            vmWriter.writeCall("String.appendChar", 2);
        }
        safeAdvance();
    }

    private void writeIntegerConstant() {
        vmWriter.writePush(Segment.CONST, jackTokenizer.intVal());
        safeAdvance();
    }

    private void writeKeywordConstant() {
        switch (jackTokenizer.keyword()) {
            case THIS:      vmWriter.writePush(Segment.POINTER, 0); break;
            case TRUE:      vmWriter.writePush(Segment.CONST, 1);
                            vmWriter.writeArithmetic(ArithCmd.NEG);
                            break;
            case FALSE:
            case NULL:      vmWriter.writePush(Segment.CONST, 0);   break;
        }
        eatKeyword(jackTokenizer.getCurrentTokenVal());
    }

    private String handleIdentifier() {
        String id = jackTokenizer.identifier();
        safeAdvance();
        return id;
    }

    private String handleVarName() {
         return handleIdentifier();
    }

    private String handleClassName() {
        return handleIdentifier();
    }

    private String handleSubroutineName() {
        return handleIdentifier();
    }

    // type must be int, char, boolean or some classname.
    private String handleType() {
        if (jackTokenizer.tokenType() == Type.KEYWORD) {
            switch (jackTokenizer.keyword()) {
                case INT:          eatKeyword("int");       break;
                case CHAR:         eatKeyword("char");      break;
                case BOOLEAN:      eatKeyword("boolean");   break;
            }
            return String.valueOf(jackTokenizer.keyword()).toLowerCase();
        } else {
            return handleIdentifier(); // if type is an identifier.
        }
    }

    // check whether current token is class var dec.
    private boolean isClassVarDec() {
        return isKeywordMatchInput(Keyword.FIELD) || isKeywordMatchInput(Keyword.STATIC);
    }

    // check whether current token is subroutine dec.
    private boolean isSubroutineDec() {
        return isKeywordMatchInput(Keyword.CONSTRUCTOR) ||
                isKeywordMatchInput(Keyword.METHOD) ||
                isKeywordMatchInput(Keyword.FUNCTION);
    }

    /* eat keyword, then make the tokenizer advance;
     * since we have already seen keyword, don't need to check the validity.
     * */
    private void eatKeyword(String ignoredKeyword) {
        safeAdvance();
    }

    private void eatSymbol(char ignoredSymbol) {
        safeAdvance();
    }

    private void safeAdvance() {
        if (jackTokenizer.hasMoreTokens()) {
            jackTokenizer.advance();
        }
    }

    private Boolean isSymbolMatchInput(char input) {
        return (jackTokenizer.tokenType() == Type.SYMBOL && jackTokenizer.symbol() == input);
    }

    private Boolean isKeywordMatchInput(Keyword keyword) {
        return (jackTokenizer.tokenType() == Type.KEYWORD && jackTokenizer.keyword() == keyword);
    }

    private boolean isOP(Character op) {
        return opMap.containsKey(op);
    }

    // handle the function header, nVars means how many local variables.
    private void writeFuncDec(String className, String funcName, int nVars) {
        String newName = className + "." + funcName;
        vmWriter.writeFunction(newName, nVars);
    }

    // object constructor, first asks OS to get the proper size
    private void writeConstructorHeader() {
        vmWriter.writePush(Segment.CONST, symbolTable.varCount(Kind.FIELD));
        vmWriter.writeCall("Memory.alloc", 1);
        vmWriter.writePop(Segment.POINTER, 0);
    }

    // if it is a method, first make the "this" pointer valid.
    private void writeThisPointToObject() {
        vmWriter.writePush(Segment.ARG, 0);
        vmWriter.writePop(Segment.POINTER, 0);
    }

    /* add an entry to symbol table, first get type, then get varName.
     * assume kind is already known.
     */
    private String symbolTableAddEntry(Kind kind, String type) {
        if (type == null) {
            type = handleType();
        }
        String varName = handleVarName();
        symbolTable.define(varName, type, kind);
        return type;
    }

    /* find segment and index this variable has.
     * then push/pop by we need.
     */
    private void pushOrPopToVarByName(String varName, boolean pushFlag) {
        int index = symbolTable.indexOf(varName);
        Segment segment = KindSegmentHashMap.get(symbolTable.kindOf(varName));
        if (pushFlag) {
            vmWriter.writePush(segment, index);
        } else {
            vmWriter.writePop(segment, index);
        }
    }


    private void writeWithNameFunctionCall() {
        String name = handleIdentifier();   // could be className or varName.
        String type = name;
        Kind kind = symbolTable.kindOf(name);
        if (kind != Kind.NONE) {
            // varName, get its type, use for call command.
            type = symbolTable.typeOf(name);
            Segment segment = KindSegmentHashMap.get(kind);
            vmWriter.writePush(segment, symbolTable.indexOf(name)); // push the var address.
        }

        eatSymbol('.');
        String subroutineName = handleSubroutineName();
        eatSymbol('(');
        int nArgs = compileExpressionList();
        nArgs = (kind == Kind.NONE) ? nArgs: nArgs + 1;     // method, one more argument for "this".
        eatSymbol(')');
        vmWriter.writeCall(type + "." + subroutineName, nArgs);
    }

    private void writeOp(Operator op) {
        switch (op) {
            case MULTIPLE:
                vmWriter.writeCall("Math.multiply", 2);
                break;
            case DIVIDE:
                vmWriter.writeCall("Math.divide", 2);
                break;
            default:
                vmWriter.writeArithmetic(opAriCmdHashMap.get(op));
        }
    }

    private void writeArrayAssign() {
        vmWriter.writePop(Segment.TEMP, 0);
        vmWriter.writePop(Segment.POINTER, 1);
        vmWriter.writePush(Segment.TEMP, 0);
        vmWriter.writePop(Segment.THAT, 0);
    }

    private void close() {
        vmWriter.close();
    }

    private void writeThisClassMethodCall() {
        String subroutineName = handleSubroutineName();
        eatSymbol('(');
        vmWriter.writePush(Segment.POINTER, 0);
        int nArgs = compileExpressionList() + 1;
        eatSymbol(')');
        vmWriter.writeCall(thisClassName + "." + subroutineName, nArgs);
    }

    private String freshLabel() {
        return "Label_" + labelIndex++;
    }
}