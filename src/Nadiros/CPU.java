package Nadiros;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class CPU {
    private int regA, regB, regX, regY, regU, regS, regDP;
    private int regPC;
    private boolean flagC, flagV, flagZ, flagN, flagI, flagH, flagF, flagE;
    private static final int RAM_START = 0x0000;
    private static final int RAM_END = 0x7FFF;    // 32K RAM
    private static final int ROM_START = 0x8000;  // ROM commence à 0x8000
    private static final int ROM_END = 0xFFFF;    // ROM jusqu'à 0xFFFF

    // Constantes pour les bits du CCR (Condition Code Register)
    private static final int CC_C = 0x01;  // Carry
    private static final int CC_V = 0x02;  // Overflow
    private static final int CC_Z = 0x04;  // Zero
    private static final int CC_N = 0x08;  // Negative
    private static final int CC_I = 0x10;  // IRQ Mask
    private static final int CC_H = 0x20;  // Half Carry
    private static final int CC_F = 0x40;  // FIRQ Mask
    private static final int CC_E = 0x80;  // Entire Flag

    private byte[] memory;
    private boolean[] isROM;  // Pour marquer les zones ROM
    private Map<String, Integer> labels;
    private Map<String, Integer> opCodes;
    private Map<Integer, String> mnemonics;
    private Map<Integer, String> addressingModes;
    private int romAddress;

    public CPU() {
        memory = new byte[65536];  // 64K memory
        isROM = new boolean[65536];
        labels = new HashMap<>();
        opCodes = new HashMap<>();
        mnemonics = new HashMap<>();
        addressingModes = new HashMap<>();

        // Instructions de chargement (Load)
        // Mode immédiat (#)
        opCodes.put("LDA#", 0x86);
        opCodes.put("LDB#", 0xC6);
        opCodes.put("LDX#", 0x8E);

        // Mode direct (<)
        opCodes.put("LDA<", 0x96);
        opCodes.put("LDB<", 0xD6);
        opCodes.put("LDX<", 0x9E);

        // Mode étendu (>)
        opCodes.put("LDA>", 0xB6);
        opCodes.put("LDB>", 0xF6);
        opCodes.put("LDX>", 0xBE);

        // Instructions de stockage (Store)
        // Mode direct (<)
        opCodes.put("STA<", 0x97);
        opCodes.put("STB<", 0xD7);
        opCodes.put("STX<", 0x9F);

        // Mode étendu (>)
        opCodes.put("STA>", 0xB7);
        opCodes.put("STB>", 0xF7);
        opCodes.put("STX>", 0xBF);

        // Instructions arithmétiques
        // Mode immédiat (#)
        opCodes.put("ADDA#", 0x8B);
        opCodes.put("ADDB#", 0xCB);
        opCodes.put("ADDD#", 0xC3);

        // Mode direct (<)
        opCodes.put("ADDA<", 0x9B);
        opCodes.put("ADDB<", 0xDB);
        opCodes.put("ADDD<", 0xD3);

        // Mode étendu (>)
        opCodes.put("ADDA>", 0xBB);
        opCodes.put("ADDB>", 0xFB);
        opCodes.put("ADDD>", 0xF3);

        // Nouvelles instructions
        // Mode inhérent
        opCodes.put("CLRA", 0x4F);
        opCodes.put("CLRB", 0x5F);
        opCodes.put("DECA", 0x4A);
        opCodes.put("DECB", 0x5A);
        opCodes.put("INCA", 0x4C);
        opCodes.put("INCB", 0x5C);
        opCodes.put("PSHS", 0x34);
        opCodes.put("PSHU", 0x36);
        opCodes.put("PULS", 0x35);
        opCodes.put("PULU", 0x37);

        // Instructions de chargement 16 bits
        // Mode immédiat
        opCodes.put("LDD#", 0xCC);
        opCodes.put("LDY#", 0x108E);
        opCodes.put("LDS#", 0x10CE);
        opCodes.put("LDU#", 0xCE);

        // Mode direct
        opCodes.put("LDD<", 0xDC);
        opCodes.put("LDY<", 0x109E);
        opCodes.put("LDS<", 0x10DE);
        opCodes.put("LDU<", 0xDE);

        // Mode étendu
        opCodes.put("LDD>", 0xFC);
        opCodes.put("LDY>", 0x10BE);
        opCodes.put("LDS>", 0x10FE);
        opCodes.put("LDU>", 0xFE);

        // Mode indexé
        opCodes.put("LDD,X", 0xEC);
        opCodes.put("LDY,X", 0x10AE);
        opCodes.put("LDS,X", 0x10EE);
        opCodes.put("LDU,X", 0xEE);

        // Instructions de stockage 16 bits
        // Mode direct
        opCodes.put("STD<", 0xDD);
        opCodes.put("STY<", 0x109F);
        opCodes.put("STS<", 0x10DF);
        opCodes.put("STU<", 0xDF);

        // Mode étendu
        opCodes.put("STD>", 0xFD);
        opCodes.put("STY>", 0x10BF);
        opCodes.put("STS>", 0x10FF);
        opCodes.put("STU>", 0xFF);

        // Mode indexé
        opCodes.put("STD,X", 0xED);
        opCodes.put("STY,X", 0x10AF);
        opCodes.put("STS,X", 0x10EF);
        opCodes.put("STU,X", 0xEF);

        // Instructions de comparaison
        // Mode immédiat
        opCodes.put("CMPA#", 0x81);
        opCodes.put("CMPB#", 0xC1);

        // Mode direct
        opCodes.put("CMPA<", 0x91);
        opCodes.put("CMPB<", 0xD1);

        // Mode étendu
        opCodes.put("CMPA>", 0xB1);
        opCodes.put("CMPB>", 0xF1);

        // Mode indexé
        opCodes.put("CMPA,X", 0xA1);
        opCodes.put("CMPB,X", 0xE1);

        reset();
    }

    public byte[] assembleInstruction(String opcode, String operand) {
        // Vérifier si c'est la fin du programme
        if (opcode.equalsIgnoreCase("END")) {
            return new byte[] { 0x3F };
        }

        // Déterminer le mode d'adressage
        String key = opcode;
        if (operand.startsWith("#")) {
            key += "#";  // Mode immédiat
        } else if (operand.startsWith("<")) {
            key += "<";  // Mode direct
        } else if (operand.startsWith(">")) {
            key += ">";  // Mode étendu
        } else if (operand.contains(",X")) {  // Mode indexé
            key += ",X";
        }

        Integer opCodeValue = opCodes.get(key.toUpperCase());
        if (opCodeValue == null) {
            throw new IllegalArgumentException("Opcode invalide: " + opcode + " avec mode " + key);
        }

        byte[] instruction;

        if (operand.startsWith("#")) {  // Mode immédiat
            String hexValue = operand.substring(2); // Skip #$
            if (opcode.endsWith("A") || opcode.endsWith("B")) {
                // Instructions 8 bits
                instruction = encodeInstruction(opCodeValue, new byte[] {
                        (byte) Integer.parseInt(hexValue, 16)
                });
            } else {
                // Instructions 16 bits (X, D)
                int value = Integer.parseInt(hexValue, 16);
                instruction = encodeInstruction(opCodeValue, new byte[] {
                        (byte) (value >> 8),
                        (byte) (value & 0xFF)
                });
            }
        } else if (operand.startsWith("<")) {  // Mode direct
            String hexValue = operand.substring(2); // Skip <$
            instruction = encodeInstruction(opCodeValue, new byte[] {
                    (byte) Integer.parseInt(hexValue, 16)
            });
        } else if (operand.startsWith(">")) {  // Mode étendu
            String hexValue = operand.substring(2); // Skip >$
            int address = Integer.parseInt(hexValue, 16);
            instruction = encodeInstruction(opCodeValue, new byte[] {
                    (byte) (address >> 8),
                    (byte) (address & 0xFF)
            });
        } else if (operand.contains(",X")) {  // Mode indexé
            String hexValue = operand.substring(0, operand.indexOf(","));
            if (hexValue.startsWith("$")) {
                hexValue = hexValue.substring(1);
            }
            instruction = encodeInstruction(opCodeValue, new byte[] {
                    (byte) Integer.parseInt(hexValue, 16)
            });
        } else if (opcode.equals("CLRA") || opcode.equals("CLRB") ||
                opcode.equals("DECA") || opcode.equals("DECB") ||
                opcode.equals("INCA") || opcode.equals("INCB") ||
                opcode.equals("PSHS") || opcode.equals("PSHU") ||
                opcode.equals("PULS") || opcode.equals("PULU")) {
            // Instructions inhérentes - un seul octet
            instruction = encodeInstruction(opCodeValue, new byte[0]);
        } else {
            throw new IllegalArgumentException("Mode d'adressage non supporté: " + operand);
        }

        // Stocker le mnémonique pour cette instruction
        mnemonics.put(romAddress, opcode + " " + operand);
        romAddress += instruction.length;

        return instruction;
    }

    public void executeInstruction() {
        int currentPC = regPC;
        byte opcode = readMemory(currentPC);

        try {
            switch (opcode & 0xFF) {
                // Instructions LDA
                case 0x86: // LDA immediate
                    regA = readMemory(currentPC + 1) & 0xFF;
                    updateCCR(regA, 8);
                    regPC += 2;
                    break;
                case 0x96: // LDA direct
                    regA = readMemory(readMemory(currentPC + 1) & 0xFF) & 0xFF;
                    updateCCR(regA, 8);
                    regPC += 2;
                    break;
                case 0xB6: // LDA extended
                    int ldaAddr = (readMemory(currentPC + 1) << 8) | (readMemory(currentPC + 2) & 0xFF);
                    regA = readMemory(ldaAddr) & 0xFF;
                    updateCCR(regA, 8);
                    regPC += 3;
                    break;

                // Instructions LDB
                case 0xC6: // LDB immediate
                    regB = readMemory(currentPC + 1) & 0xFF;
                    updateCCR(regB, 8);
                    regPC += 2;
                    break;
                case 0xD6: // LDB direct
                    regB = readMemory(readMemory(currentPC + 1) & 0xFF) & 0xFF;
                    updateCCR(regB, 8);
                    regPC += 2;
                    break;
                case 0xF6: // LDB extended
                    int ldbAddr = (readMemory(currentPC + 1) << 8) | (readMemory(currentPC + 2) & 0xFF);
                    regB = readMemory(ldbAddr) & 0xFF;
                    updateCCR(regB, 8);
                    regPC += 3;
                    break;

                // Instructions LDX
                case 0x8E: // LDX immediate
                    setRegX((readMemory(currentPC + 1) << 8) | (readMemory(currentPC + 2) & 0xFF));
                    updateCCR(regX, 16);
                    regPC += 3;
                    break;
                case 0x9E: // LDX direct
                    int ldxDirectAddr = readMemory(currentPC + 1) & 0xFF;
                    setRegX((readMemory(ldxDirectAddr) << 8) | (readMemory(ldxDirectAddr + 1) & 0xFF));
                    updateCCR(regX, 16);
                    regPC += 2;
                    break;
                case 0xBE: // LDX extended
                    int ldxAddr = (readMemory(currentPC + 1) << 8) | (readMemory(currentPC + 2) & 0xFF);
                    setRegX((readMemory(ldxAddr) << 8) | (readMemory(ldxAddr + 1) & 0xFF));
                    updateCCR(regX, 16);
                    regPC += 3;
                    break;

                // Instructions STA
                case 0x97: // STA direct
                    writeMemory(readMemory(currentPC + 1) & 0xFF, (byte)regA);
                    updateCCR(regA, 8);
                    regPC += 2;
                    break;
                case 0xB7: // STA extended
                    int staAddr = (readMemory(currentPC + 1) << 8) | (readMemory(currentPC + 2) & 0xFF);
                    writeMemory(staAddr, (byte)regA);
                    updateCCR(regA, 8);
                    regPC += 3;
                    break;

                // Instructions STB
                case 0xD7: // STB direct
                    writeMemory(readMemory(currentPC + 1) & 0xFF, (byte)regB);
                    updateCCR(regB, 8);
                    regPC += 2;
                    break;
                case 0xF7: // STB extended
                    int stbAddr = (readMemory(currentPC + 1) << 8) | (readMemory(currentPC + 2) & 0xFF);
                    writeMemory(stbAddr, (byte)regB);
                    updateCCR(regB, 8);
                    regPC += 3;
                    break;

                // Instructions STX
                case 0x9F: // STX direct
                    int stxDirectAddr = readMemory(currentPC + 1) & 0xFF;
                    writeMemory(stxDirectAddr, (byte)(regX >> 8));
                    writeMemory(stxDirectAddr + 1, (byte)(regX & 0xFF));
                    updateCCR(regX, 16);
                    regPC += 2;
                    break;
                case 0xBF: // STX extended
                    int stxAddr = (readMemory(currentPC + 1) << 8) | (readMemory(currentPC + 2) & 0xFF);
                    writeMemory(stxAddr, (byte)(regX >> 8));
                    writeMemory(stxAddr + 1, (byte)(regX & 0xFF));
                    updateCCR(regX, 16);
                    regPC += 3;
                    break;

                // Instructions ADDA
                case 0x8B: // ADDA immediate
                    int addaImm = readMemory(currentPC + 1) & 0xFF;
                    regA = (regA + addaImm) & 0xFF;
                    updateCCR(regA, 8);
                    regPC += 2;
                    break;
                case 0x9B: // ADDA direct
                    int addaDirect = readMemory(readMemory(currentPC + 1) & 0xFF) & 0xFF;
                    regA = (regA + addaDirect) & 0xFF;
                    updateCCR(regA, 8);
                    regPC += 2;
                    break;
                case 0xBB: // ADDA extended
                    int addaExt = readMemory((readMemory(currentPC + 1) << 8) | (readMemory(currentPC + 2) & 0xFF)) & 0xFF;
                    regA = (regA + addaExt) & 0xFF;
                    updateCCR(regA, 8);
                    regPC += 3;
                    break;

                // Instructions ADDB similaires à ADDA
                case 0xCB: // ADDB immediate
                    int addbImm = readMemory(currentPC + 1) & 0xFF;
                    regB = (regB + addbImm) & 0xFF;
                    updateCCR(regB, 8);
                    regPC += 2;
                    break;

                case 0x3F: // END instruction
                    return;

                // Instructions inhérentes
                case 0x4F: // CLRA
                    regA = 0;
                    updateCCR(regA, 8);
                    regPC++;
                    break;

                case 0x5F: // CLRB
                    regB = 0;
                    updateCCR(regB, 8);
                    regPC++;
                    break;

                case 0x4A: // DECA
                    regA = (regA - 1) & 0xFF;
                    updateCCR(regA, 8);
                    regPC++;
                    break;

                case 0x5A: // DECB
                    regB = (regB - 1) & 0xFF;
                    updateCCR(regB, 8);
                    regPC++;
                    break;

                case 0x4C: // INCA
                    regA = (regA + 1) & 0xFF;
                    updateCCR(regA, 8);
                    regPC++;
                    break;

                case 0x5C: // INCB
                    regB = (regB + 1) & 0xFF;
                    updateCCR(regB, 8);
                    regPC++;
                    break;

                // Instructions de pile
                case 0x34: // PSHS
                    pushStack('S', regA);
                    regPC++;
                    break;

                case 0x36: // PSHU
                    pushStack('U', regA);
                    regPC++;
                    break;

                case 0x35: // PULS
                    regA = pullStack('S');
                    regPC++;
                    break;

                case 0x37: // PULU
                    regA = pullStack('U');
                    regPC++;
                    break;

                // Instructions LDD
                case 0xCC: // LDD immediate
                    int immValue = (readMemory(currentPC + 1) << 8) | (readMemory(currentPC + 2) & 0xFF);
                    setRegD(immValue);
                    updateCCR(getRegD(), 16);
                    regPC += 3;
                    break;

                case 0xDC: // LDD direct
                    int lddDirectAddr = readMemory(currentPC + 1) & 0xFF;
                    int directValue = (readMemory(lddDirectAddr) << 8) | (readMemory(lddDirectAddr + 1) & 0xFF);
                    setRegD(directValue);
                    updateCCR(getRegD(), 16);
                    regPC += 2;
                    break;

                case 0xFC: // LDD extended
                    int lddAddr = (readMemory(currentPC + 1) << 8) | (readMemory(currentPC + 2) & 0xFF);
                    int extValue = (readMemory(lddAddr) << 8) | (readMemory(lddAddr + 1) & 0xFF);
                    setRegD(extValue);
                    updateCCR(getRegD(), 16);
                    regPC += 3;
                    break;

                case 0xEC: // LDD indexed
                    int lddOffset = readMemory(currentPC + 1) & 0xFF;
                    int lddIndexedAddr = (regX + lddOffset) & 0xFFFF;
                    int indexedValue = (readMemory(lddIndexedAddr) << 8) | (readMemory(lddIndexedAddr + 1) & 0xFF);
                    setRegD(indexedValue);
                    updateCCR(getRegD(), 16);
                    regPC += 2;
                    break;

                // Instructions STD
                case 0xDD: // STD direct
                    int stddAddr = readMemory(currentPC + 1) & 0xFF;
                    writeMemory(stddAddr, (byte)(getRegD() >> 8));
                    writeMemory(stddAddr + 1, (byte)(getRegD() & 0xFF));
                    updateCCR(getRegD(), 16);
                    regPC += 2;
                    break;

                case 0xFD: // STD extended
                    int stdeAddr = (readMemory(currentPC + 1) << 8) | (readMemory(currentPC + 2) & 0xFF);
                    writeMemory(stdeAddr, (byte)(getRegD() >> 8));
                    writeMemory(stdeAddr + 1, (byte)(getRegD() & 0xFF));
                    updateCCR(getRegD(), 16);
                    regPC += 3;
                    break;

                case 0xED: // STD indexed
                    int stdOffset = readMemory(currentPC + 1) & 0xFF;
                    int stdIndexedAddr = (regX + stdOffset) & 0xFFFF;
                    writeMemory(stdIndexedAddr, (byte)(getRegD() >> 8));
                    writeMemory(stdIndexedAddr + 1, (byte)(getRegD() & 0xFF));
                    updateCCR(getRegD(), 16);
                    regPC += 2;
                    break;

                // Instructions LDY (avec préfixe 10)
                case 0x10: // Préfixe pour instructions 16 bits
                    opcode = readMemory(currentPC + 1);
                    switch (opcode & 0xFF) {
                        case 0x8E: // LDY immediate
                            regY = (readMemory(currentPC + 2) << 8) | (readMemory(currentPC + 3) & 0xFF);
                            updateCCR(regY, 16);
                            regPC += 4;
                            break;

                        case 0x9E: // LDY direct
                            int ldyDirectAddr = readMemory(currentPC + 2) & 0xFF;
                            regY = (readMemory(ldyDirectAddr) << 8) | (readMemory(ldyDirectAddr + 1) & 0xFF);
                            updateCCR(regY, 16);
                            regPC += 3;
                            break;

                        case 0xBE: // LDY extended
                            int ldyAddr = (readMemory(currentPC + 2) << 8) | (readMemory(currentPC + 3) & 0xFF);
                            regY = (readMemory(ldyAddr) << 8) | (readMemory(ldyAddr + 1) & 0xFF);
                            updateCCR(regY, 16);
                            regPC += 4;
                            break;

                        case 0xAE: // LDY indexed
                            int ldyOffset = readMemory(currentPC + 2) & 0xFF;
                            int ldyIndexedAddr = (regX + ldyOffset) & 0xFFFF;
                            regY = (readMemory(ldyIndexedAddr) << 8) | (readMemory(ldyIndexedAddr + 1) & 0xFF);
                            updateCCR(regY, 16);
                            regPC += 3;
                            break;

                        case 0xBF: // STY extended
                            int styAddr = (readMemory(currentPC + 2) << 8) | (readMemory(currentPC + 3) & 0xFF);
                            writeMemory(styAddr, (byte)(regY >> 8));
                            writeMemory(styAddr + 1, (byte)(regY & 0xFF));
                            updateCCR(regY, 16);
                            regPC += 4;
                            break;

                        case 0xAF: // STY indexed
                            int styOffset = readMemory(currentPC + 2) & 0xFF;
                            int styIndexedAddr = (regX + styOffset) & 0xFFFF;
                            writeMemory(styIndexedAddr, (byte)(regY >> 8));
                            writeMemory(styIndexedAddr + 1, (byte)(regY & 0xFF));
                            updateCCR(regY, 16);
                            regPC += 3;
                            break;

                        default:
                            throw new IllegalStateException("Instruction préfixée non supportée: " + String.format("%02X", opcode));
                    }
                    break;

                case 0xCE: // LDU immediate
                    regU = (readMemory(currentPC + 1) << 8) | (readMemory(currentPC + 2) & 0xFF);
                    updateCCR(regU, 16);
                    regPC += 3;
                    break;

                case 0xFF: // STU extended
                    int stuAddr = (readMemory(currentPC + 1) << 8) | (readMemory(currentPC + 2) & 0xFF);
                    writeMemory(stuAddr, (byte)(regU >> 8));
                    writeMemory(stuAddr + 1, (byte)(regU & 0xFF));
                    updateCCR(regU, 16);
                    regPC += 3;
                    break;

                default:
                    throw new IllegalStateException("Instruction non supportée: " + String.format("%02X", opcode));
            }

        } catch (Exception e) {
            throw new RuntimeException("Erreur d'exécution à l'adresse " +
                    String.format("%04X", currentPC) + ": " + e.getMessage());
        }
    }

    public void writeMemory(int address, byte value) {
        // Vérifier si l'adresse est dans la ROM
        if (address >= ROM_START && address <= ROM_END) {
            throw new RuntimeException("Tentative d'écriture en ROM à l'adresse: " + String.format("%04X", address));
        }
        if (address < 0 || address > 0xFFFF) {
            throw new IllegalArgumentException("Adresse invalide: " + String.format("%04X", address));
        }
        memory[address] = value;
    }

    public byte readMemory(int address) {
        if (address < 0 || address > 0xFFFF) {
            throw new IllegalArgumentException("Adresse invalide: " + String.format("%04X", address));
        }
        return memory[address];
    }

    public void reset() {
        regA = regB = regX = regY = regDP = 0;
        regS = 0x7FFF;  // Pile S initialisée au sommet de la RAM
        regU = 0x7EFF;  // Pile U initialisée juste en dessous de la pile S
        regPC = ROM_START;  // Le PC démarre au début de la ROM
        romAddress = ROM_START;
        mnemonics.clear();
        labels.clear();
        initializeMemory();
        // Initialiser le CCR avec les interruptions masquées
        setFlagsByte(CC_I | CC_F);
    }

    // Méthode pour mettre à jour le CCR après les opérations
    private void updateCCR(int result, int size) {
        int mask = (size == 8) ? 0xFF : 0xFFFF;

        // Flag Z (Zero)
        flagZ = (result & mask) == 0;

        // Flag N (Negative)
        flagN = (size == 8) ?
                (result & 0x80) != 0 :    // 8 bits
                (result & 0x8000) != 0;   // 16 bits

        // Les flags C (Carry) et V (Overflow) sont gérés spécifiquement
        // dans chaque instruction qui les affecte
    }

    // Méthode pour obtenir une représentation du CCR
    public String getCCRString() {
        return String.format("%c%c%c%c%c%c%c%c",
                flagE ? 'E' : '.',  // Entire Flag
                flagF ? 'F' : '.',  // FIRQ Mask
                flagH ? 'H' : '.',  // Half Carry
                flagI ? 'I' : '.',  // IRQ Mask
                flagN ? 'N' : '.',  // Negative
                flagZ ? 'Z' : '.',  // Zero
                flagV ? 'V' : '.',  // Overflow
                flagC ? 'C' : '.'   // Carry
        );
    }

    // Méthode pour obtenir la valeur du CCR comme un octet
    public int getFlagsByte() {
        return ((flagE ? 1 : 0) << 7) |
                ((flagF ? 1 : 0) << 6) |
                ((flagH ? 1 : 0) << 5) |
                ((flagI ? 1 : 0) << 4) |
                ((flagN ? 1 : 0) << 3) |
                ((flagZ ? 1 : 0) << 2) |
                ((flagV ? 1 : 0) << 1) |
                (flagC ? 1 : 0);
    }

    // Méthode pour définir les flags à partir d'un octet
    public void setFlagsByte(int flags) {
        flagE = (flags & 0x80) != 0;
        flagF = (flags & 0x40) != 0;
        flagH = (flags & 0x20) != 0;
        flagI = (flags & 0x10) != 0;
        flagN = (flags & 0x08) != 0;
        flagZ = (flags & 0x04) != 0;
        flagV = (flags & 0x02) != 0;
        flagC = (flags & 0x01) != 0;
    }

    // Méthode pour charger le programme en ROM
    public void loadProgram(byte[] program, int address) {
        if (address < ROM_START || address + program.length > ROM_END) {
            throw new IllegalArgumentException("Adresse ROM invalide");
        }

        // Effacer d'abord la zone
        for (int i = address; i < address + program.length; i++) {
            memory[i] = 0x00;
        }

        // Copier le programme
        System.arraycopy(program, 0, memory, address, program.length);

        // Marquer la zone comme ROM
        for (int i = address; i < address + program.length; i++) {
            isROM[i] = true;
        }

        // Initialiser PC au début du programme
        regPC = address;
    }

    // Méthode pour obtenir l'état des registres
    public String getRegisterState() {
        return String.format(
                "A: %02X  B: %02X  D: %04X\n" +
                        "X: %04X  Y: %04X\n" +
                        "U: %04X  S: %04X\n" +
                        "DP: %02X  PC: %04X\n" +
                        "CCR: %s",
                regA & 0xFF,
                regB & 0xFF,
                ((regA << 8) | regB) & 0xFFFF,
                regX & 0xFFFF,
                regY & 0xFFFF,
                regU & 0xFFFF,
                regS & 0xFFFF,
                regDP & 0xFF,
                regPC & 0xFFFF,
                getCCRString()
        );
    }

    public int getRegA() { return regA; }
    public int getRegB() { return regB; }
    public int getRegX() { return regX; }
    public int getRegY() { return regY; }
    public int getRegU() { return regU; }
    public int getRegS() { return regS; }
    public int getRegDP() { return regDP; }
    public int getPC() { return regPC; }

    public boolean getFlagC() { return flagC; }
    public boolean getFlagV() { return flagV; }
    public boolean getFlagZ() { return flagZ; }
    public boolean getFlagN() { return flagN; }
    public boolean getFlagI() { return flagI; }
    public boolean getFlagH() { return flagH; }
    public boolean getFlagF() { return flagF; }
    public boolean getFlagE() { return flagE; }

    public void setRomAddress(int address) {
        if (address >= ROM_START && address <= ROM_END) {
            romAddress = address;
        }
    }

    public int getRomAddress() {
        return romAddress;
    }

    // Méthodes utiles pour le debugging
    public void addBreakpoint(int address) {
        // Ajouter un point d'arrêt à l'adresse spécifiée
        breakpoints.add(address);
    }

    public void removeBreakpoint(int address) {
        // Supprimer un point d'arrêt
        breakpoints.remove(address);
    }

    public void clearBreakpoints() {
        // Supprimer tous les points d'arrêt
        breakpoints.clear();
    }

    private Set<Integer> breakpoints = new HashSet<>();

    public void step() {
        // Exécuter une seule instruction
        executeInstruction();
    }

    public void run() {
        // Exécuter jusqu'à un point d'arrêt ou une instruction END
        while (true) {
            if (breakpoints.contains(regPC)) {
                System.out.println("Breakpoint hit at " + String.format("$%04X", regPC));
                break;
            }

            byte opcode = readMemory(regPC);
            if (opcode == 0x3F) { // END instruction
                System.out.println("Program terminated normally");
                break;
            }

            executeInstruction();
        }
    }

    // Méthodes pour la manipulation des registres 16 bits
    public void setRegX(int value) { regX = value & 0xFFFF; }
    public void setRegY(int value) { regY = value & 0xFFFF; }
    public void setRegU(int value) { regU = value & 0xFFFF; }
    public void setRegS(int value) { regS = value & 0xFFFF; }
    public void setRegPC(int value) { regPC = value & 0xFFFF; }

    // Méthodes pour la manipulation des registres 8 bits
    public void setRegA(int value) { regA = value & 0xFF; }
    public void setRegB(int value) { regB = value & 0xFF; }
    public void setRegDP(int value) { regDP = value & 0xFF; }

    // Méthode pour obtenir le registre D (concaténation de A et B)
    public int getRegD() { return (regA << 8) | regB; }
    public void setRegD(int value) {
        regA = (value >> 8) & 0xFF;
        regB = value & 0xFF;
    }

    // Méthodes pour la manipulation des flags
    public void setFlags(boolean c, boolean v, boolean z, boolean n) {
        flagC = c;
        flagV = v;
        flagZ = z;
        flagN = n;
    }

    // Méthode pour le dump mémoire
    public String dumpMemory(int start, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < start + length && i <= 0xFFFF; i++) {
            // Format: AAAA: BB
            sb.append(String.format("%04X: %02X", i, memory[i] & 0xFF));

            // Si c'est une instruction en ROM, ajouter le mnémonique
            if (isROM[i]) {
                String mnemonic = mnemonics.get(i);
                if (mnemonic != null) {
                    sb.append("  ; ").append(mnemonic);
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private void initializeMemory() {
        for (int i = 0; i < memory.length; i++) {
            memory[i] = 0x00;
            isROM[i] = (i >= ROM_START && i <= ROM_END);
        }
    }

    // Méthodes auxiliaires pour la pile
    private void pushStack(char stack, int value) {
        if (stack == 'S') {
            writeMemory(regS, (byte)value);
            regS--;
        } else {
            writeMemory(regU, (byte)value);
            regU--;
        }
    }

    private int pullStack(char stack) {
        if (stack == 'S') {
            return memory[++regS] & 0xFF;
        } else {
            return memory[++regU] & 0xFF;
        }
    }

    private byte[] encodeInstruction(int opCodeValue, byte[] operands) {
        int opCodeSize = opCodeValue > 0xFF ? 2 : 1;
        byte[] encoded = new byte[opCodeSize + operands.length];
        if (opCodeSize == 2) {
            encoded[0] = (byte) ((opCodeValue >> 8) & 0xFF);
            encoded[1] = (byte) (opCodeValue & 0xFF);
            System.arraycopy(operands, 0, encoded, 2, operands.length);
        } else {
            encoded[0] = (byte) (opCodeValue & 0xFF);
            System.arraycopy(operands, 0, encoded, 1, operands.length);
        }
        return encoded;
    }
}
