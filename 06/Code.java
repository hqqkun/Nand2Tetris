import java.util.HashMap;

public class Code {
    private static final HashMap<String, String> compOpcodeHashMap = new HashMap<>();
    private static final HashMap<String, String> destOpcodeHashMap = new HashMap<>();
    private static final HashMap<String, String> jumpOpcodeHashMap = new HashMap<>();

    public static void initCode() {
        // comp part.
            // a == 0
        compOpcodeHashMap.put("0",      "0101010");
        compOpcodeHashMap.put("1",      "0111111");
        compOpcodeHashMap.put("-1",     "0111010");
        compOpcodeHashMap.put("D",      "0001100");
        compOpcodeHashMap.put("A",      "0110000");
        compOpcodeHashMap.put("!D",     "0001101");
        compOpcodeHashMap.put("!A",     "0110001");
        compOpcodeHashMap.put("-D",     "0001111");
        compOpcodeHashMap.put("-A",     "0110011");
        compOpcodeHashMap.put("D+1",    "0011111");
        compOpcodeHashMap.put("A+1",    "0110111");
        compOpcodeHashMap.put("D-1",    "0001110");
        compOpcodeHashMap.put("A-1",    "0110010");
        compOpcodeHashMap.put("D+A",    "0000010");
        compOpcodeHashMap.put("D-A",    "0010011");
        compOpcodeHashMap.put("A-D",    "0000111");
        compOpcodeHashMap.put("D&A",    "0000000");
        compOpcodeHashMap.put("D|A",    "0010101");

            // a == 1
        compOpcodeHashMap.put("M",      "1110000");
        compOpcodeHashMap.put("!M",     "1110001");
        compOpcodeHashMap.put("-M",     "1110011");
        compOpcodeHashMap.put("M+1",    "1110111");
        compOpcodeHashMap.put("M-1",    "1110010");
        compOpcodeHashMap.put("D+M",    "1000010");
        compOpcodeHashMap.put("D-M",    "1010011");
        compOpcodeHashMap.put("M-D",    "1000111");
        compOpcodeHashMap.put("D&M",    "1000000");
        compOpcodeHashMap.put("D|M",    "1010101");

        // dest part.
        destOpcodeHashMap.put("",       "000");
        destOpcodeHashMap.put("M",      "001");
        destOpcodeHashMap.put("D",      "010");
        destOpcodeHashMap.put("A",      "100");
        destOpcodeHashMap.put("DM",     "011");
        destOpcodeHashMap.put("MD",     "011");
        destOpcodeHashMap.put("AM",     "101");
        destOpcodeHashMap.put("MA",     "101");
        destOpcodeHashMap.put("DA",     "110");
        destOpcodeHashMap.put("AD",     "110");
        destOpcodeHashMap.put("ADM",    "111");
        destOpcodeHashMap.put("AMD",    "111");
        destOpcodeHashMap.put("DAM",    "111");
        destOpcodeHashMap.put("DMA",    "111");
        destOpcodeHashMap.put("MAD",    "111");
        destOpcodeHashMap.put("MDA",    "111");

        // jump part.
        jumpOpcodeHashMap.put("",       "000");
        jumpOpcodeHashMap.put("JGT",    "001");
        jumpOpcodeHashMap.put("JEQ",    "010");
        jumpOpcodeHashMap.put("JGE",    "011");
        jumpOpcodeHashMap.put("JLT",    "100");
        jumpOpcodeHashMap.put("JNE",    "101");
        jumpOpcodeHashMap.put("JLE",    "110");
        jumpOpcodeHashMap.put("JMP",    "111");
    }

    public static String dest(String destField) {
        return Code.destOpcodeHashMap.get(destField);
    }

    public static String comp(String compField) {
        return Code.compOpcodeHashMap.get(compField);
    }

    public static String jump(String jumpField) {
        return Code.jumpOpcodeHashMap.get(jumpField);
    }
}