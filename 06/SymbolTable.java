import java.util.HashMap;

public class SymbolTable {
    private final HashMap<String, Integer> symbolAddressHashTable;

    public SymbolTable() {
        symbolAddressHashTable = new HashMap<>();
        for (int i = 0; i != 16; ++i) {
            symbolAddressHashTable.put("R" + i, i);
        }
        symbolAddressHashTable.put("SP", 0);
        symbolAddressHashTable.put("LCL", 1);
        symbolAddressHashTable.put("ARG", 2);
        symbolAddressHashTable.put("THIS", 3);
        symbolAddressHashTable.put("THAT", 4);
        symbolAddressHashTable.put("SCREEN", 0x4000);
        symbolAddressHashTable.put("KBD", 0x6000);
    }

    public boolean contains(String symbol) {
        return symbolAddressHashTable.containsKey(symbol);
    }

    public int getAddress(String symbol) {
        return symbolAddressHashTable.get(symbol);
    }

    public void addEntry(String symbol, int address) {
        symbolAddressHashTable.put(symbol, address);
    }
}