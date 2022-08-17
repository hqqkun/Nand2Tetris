import java.util.HashMap;

public class SymbolTable {
    private final HashMap<String, SymbolTuple> classSymbols;
    private HashMap<String, SymbolTuple> subroutineSymbols;
    private final HashMap<Kind, Integer> KindIndexes;             // which index should be used.

    public SymbolTable() {
        classSymbols = new HashMap<>();
        subroutineSymbols = new HashMap<>();
        KindIndexes = new HashMap<>();

        KindIndexes.put(Kind.STATIC, 0);
        KindIndexes.put(Kind.FIELD, 0);
        KindIndexes.put(Kind.VAR, 0);
        KindIndexes.put(Kind.ARG, 0);
    }

    public void startSubroutine() {
        subroutineSymbols = new HashMap<>();
        KindIndexes.replace(Kind.VAR, 0);
        KindIndexes.replace(Kind.ARG, 0);
    }

    public void define(String name, String type, Kind kind) {
        int index = varCount(kind);
        KindIndexes.replace(kind, index + 1);

        switch (kind) {
            case STATIC:
            case FIELD:
                classSymbols.put(name, new SymbolTuple(type, kind, index));
                break;
            case ARG:
            case VAR:
                subroutineSymbols.put(name, new SymbolTuple(type, kind, index));
                break;
        }
    }

    public int varCount(Kind kind) {
        return KindIndexes.get(kind);
    }

    public Kind kindOf(String name) {
        SymbolTuple st = lookupTable(name);
        return (st == null)? Kind.NONE : st.getKind();
    }

    public String typeOf(String name) {
        SymbolTuple st = lookupTable(name);
        return (st == null)? null : st.getType();
    }

    public int indexOf(String name) {
        SymbolTuple st = lookupTable(name);
        return (st == null)? -1 : st.getIndex();
    }

    private SymbolTuple lookupTable(String name) {
        SymbolTuple st;
        if ((st = classSymbols.get(name)) != null)
            return st;
        st = subroutineSymbols.get(name);
        return st;
    }
}