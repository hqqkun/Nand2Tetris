import javafx.util.Pair;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JackTokenizer {
    private String currentToken;
    private Type currentTokenType;
    private Queue<Pair<Type, String>> tokens;
    // regular expressions
    private static Pattern tokenPatterns;
    private static String intRegex;         // integer constant
    private static String idRegex;          // identifier
    private static String stringRegex;      // string constant
    private static String symbolRegex;      // single character
    private static String keywordRegex;     // keyword
    private static String whiteSpaceRegex; // ignore white space
    private static String commentRegex;     // ignore comments.
    // keywordMap
    private static final HashMap<String, Keyword> keywordMap = new HashMap<>();

    //---------------------------------------------------------------------------
    /* public interface */
    /*
    * keyword(), symbol(), intVal(), stringVal(), identifier(), user check the type is correct.
    * */
    public char symbol() {
        return currentToken.charAt(0);
    }

    public int intVal() {
        return Integer.parseInt(currentToken);
    }

    public String stringVal() {
        return currentToken;
    }

    public String identifier() {
        return currentToken;
    }

    public Type tokenType() {
        return currentTokenType;
    }

    public boolean hasMoreTokens() {
        return !tokens.isEmpty();
    }

    public Keyword keyword() {
        return keywordMap.get(currentToken);
    }

    // only used when hasMoreToken() is true.
    public void advance() {
        Pair<Type, String> curToken = tokens.remove();
        assert curToken != null;
        currentTokenType = curToken.getKey();
        currentToken = curToken.getValue();
    }

    private void initPrivateArgs() {
        this.currentToken = null;
        this.currentTokenType = Type.ERROR;
        this.tokens = new LinkedList<>();
    }

    private static void initKeywordMap() {
        keywordMap.put("class", Keyword.CLASS);
        keywordMap.put("method", Keyword.METHOD);
        keywordMap.put("int", Keyword.INT);
        keywordMap.put("function", Keyword.FUNCTION);
        keywordMap.put("boolean", Keyword.BOOLEAN);
        keywordMap.put("constructor", Keyword.CONSTRUCTOR);
        keywordMap.put("char", Keyword.CHAR);
        keywordMap.put("void", Keyword.VOID);
        keywordMap.put("var", Keyword.VAR);
        keywordMap.put("static", Keyword.STATIC);
        keywordMap.put("field", Keyword.FIELD);
        keywordMap.put("let", Keyword.LET);
        keywordMap.put("do", Keyword.DO);
        keywordMap.put("if", Keyword.IF);
        keywordMap.put("else", Keyword.ELSE);
        keywordMap.put("while", Keyword.WHILE);
        keywordMap.put("return", Keyword.RETURN);
        keywordMap.put("true", Keyword.TRUE);
        keywordMap.put("false", Keyword.FALSE);
        keywordMap.put("null", Keyword.NULL);
        keywordMap.put("this", Keyword.THIS);
    }

    // init all the regular expressions, then compile patten.
    private static void initRegexes() {
        {
            StringBuilder sb = new StringBuilder();
            for (String key : keywordMap.keySet()) {
                sb.append(key).append("|");
            }
            keywordRegex = sb.toString();
        }
        {
            int length = keywordRegex.length();
            keywordRegex = keywordRegex.substring(0, length - 1);
        }
        intRegex = "(0|([1-9]\\d*))";                   // integer constant
        idRegex = "[A-Za-z_]\\w*";                      // identifier
        stringRegex = "\"[^\"\n]*\"";                   // string constant
        symbolRegex = "[{}()\\[\\].,;+\\-*/&|<>=~]";    // single char symbol

        whiteSpaceRegex = "[ \n\t\r\b]+";               // white spaces
        commentRegex = "(/[*]([^*]|([*]+[^*/]))*[*]+/)|(//.*(\n)?)";    // comments
        // everything not in above.
        String errorRegex = ".";

        String finalRegex = commentRegex + "|"
                            + whiteSpaceRegex + "|"
                            + idRegex + "|"
                            + intRegex + "|"
                            + keywordRegex + "|"
                            + stringRegex + "|"
                            + symbolRegex + "|"
                            + errorRegex;
        tokenPatterns = Pattern.compile(finalRegex);
    }

    // initialize JackTokenizer.
    public static void init()
    {
        initKeywordMap();
        initRegexes();
    }

    // Constructor
    public JackTokenizer(File inputFile) {
        initPrivateArgs();  // init three arguments.
        try {
            String preprocessed = preProcessInput(inputFile);

            Matcher m = tokenPatterns.matcher(preprocessed);
            while (m.find()) {
                String rawToken = m.group();
                Pair<Type, String> token = processRawToken(rawToken);
                if (token.getKey() != Type.NONE)   // we ignore white spaces and comments.
                    tokens.add(token);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Type findTypeFromName(String tokenName) {
        if (tokenName.matches(keywordRegex)) {
            return Type.KEYWORD;
        } else if (tokenName.matches(symbolRegex)) {
            return Type.SYMBOL;
        } else if (tokenName.matches(intRegex)) {
            return Type.INT_CONST;
        } else if (tokenName.matches(stringRegex)) {
            return Type.STRING_CONST;
        } else if (tokenName.matches(idRegex)) {
            return Type.IDENTIFIER;
        } else if (tokenName.matches(whiteSpaceRegex)) {
            return Type.NONE;
        } else if (tokenName.matches(commentRegex)) {
            return Type.NONE;
        } else {
            return Type.ERROR;
        }
    }

    public String getCurrentTokenVal() {
        return String.valueOf(currentToken);
    }

    public Pair<Type, String> peekNextToken() {
        return tokens.peek();
    }

    private String preProcessInput(File inputFile) throws FileNotFoundException {
        Scanner scanner = new Scanner(inputFile);
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNext()) {
            String line = scanner.nextLine().trim();
            if (line.length() > 0) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /* rawToken is just what regular expressions matches.
     * first find what type does this token have, then process the raw string.
     * */
    private Pair<Type, String> processRawToken(String rawToken) {
        Type type = findTypeFromName(rawToken);
        switch (type) {
            case STRING_CONST:
                // "hello" -> hello, we don't care the symbol '"'.
                rawToken = rawToken.substring(1, rawToken.length() - 1);
                break;
            case ERROR:
                throw new IllegalArgumentException("Undefined token: " + rawToken);
            case NONE:
                rawToken = null;
                break;
        }
        return new Pair<>(type, rawToken);
    }
}