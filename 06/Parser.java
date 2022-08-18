import java.util.Scanner;

public class Parser {
    private final Scanner scanner;
    private InstrType currentInstrType;
    private String currentInstr;
    private String symbol;
    private String dest;
    private String comp;
    private String jump;

    Parser(String processedInput) {
        scanner = new Scanner(processedInput);
        initArgs();
    }

    public boolean hasMoreLines()  {
        return scanner.hasNextLine();
    }

    public InstrType instructionType() {
        return currentInstrType;
    }

    public void advance() {
        currentInstr = scanner.nextLine().split("[ \t\b\r]+")[0];
        currentInstrType = getCurrentInstrType(currentInstr);
        switch (currentInstrType) {
            case A_INSTRUCTION:
            case L_INSTRUCTION:
                handleSymbol(); break;
            case C_INSTRUCTION:
                handleThreeParts(); break;
            case NONE:
            System.err.println("Uknown instruction : " + currentInstr);
            System.exit(-1);
        }
    }

    // assume instruction type is A or L.
    public String symbol() {
        return symbol;
    }

    public String dest() {
        return dest;
    }

    public String comp() {
        return comp;
    }

    public String jump() {
        return jump;
    }

    public void close() {
        scanner.close();
    }

    //------------------------------------
    private void initArgs() {
        currentInstrType = InstrType.NONE;
        currentInstr = "";
        symbol = "";
        dest = "";
        comp = "";
        jump = "";
    }

    private void handleSymbol() {
        if(currentInstrType == InstrType.A_INSTRUCTION) {
            // @someAddress
            symbol = currentInstr.substring(1);
        } else {
            // (label)
            symbol = currentInstr.substring(1, currentInstr.lastIndexOf(')'));
        }
    }

    // find dest, comp, jump.
    private void handleThreeParts() {
        // find '=' and ';' position.
        int eqIndex = currentInstr.indexOf('=');
        int semicolonIndex = currentInstr.lastIndexOf(';');

        dest = jump = "";
        if (eqIndex != -1) {
            dest = currentInstr.substring(0, eqIndex);
            if (semicolonIndex != -1) {
                // dest = comp ; jump
                comp = currentInstr.substring(eqIndex + 1, semicolonIndex);
                jump = currentInstr.substring(semicolonIndex + 1);
            } else {
                // dest = comp
                comp = currentInstr.substring(eqIndex + 1);
            }
        } else {
            // comp ; jump
            comp = currentInstr.substring(0, semicolonIndex);
            jump = currentInstr.substring(semicolonIndex + 1);
        }
    }

    private InstrType getCurrentInstrType(String currentInstr) {
        switch (currentInstr.charAt(0)) {
            // assume we are handling error free asm file.
            case '@':
                return InstrType.A_INSTRUCTION;
            case '(':
                return InstrType.L_INSTRUCTION;
            default:
                return InstrType.C_INSTRUCTION;
        }
    }
}