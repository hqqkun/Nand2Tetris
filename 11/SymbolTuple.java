public class SymbolTuple {
    private final String type;
    private final Kind kind;
    private final int index;

    public SymbolTuple(String type, Kind kind, int index) {
        this.type = type;
        this.kind = kind;
        this.index = index;
    }

    public Kind getKind() {
        return kind;
    }

    public String getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }
}
