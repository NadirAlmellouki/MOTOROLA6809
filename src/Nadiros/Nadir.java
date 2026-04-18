package Nadiros;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.util.ArrayList;
import java.util.List;

public class Nadir extends JFrame {
    private CPU cpu;
    private JTextArea programArea, ramArea, romArea, stackArea;
    private JTextField regA, regB, regD, regX, regY, regU, regS, regDP, regPC, regCCR;
    private JButton stepButton, runButton, resetButton, compileButton;
    private Timer runTimer;
    private boolean isRunning = false;
    private int romAddress = 0x8000;  // Adresse de début pour le code en ROM
    private boolean isCompiled = false;  // Pour vérifier si le programme est compilé
    private final List<Integer> instructionStartAddresses = new ArrayList<>();
    private final List<Integer> instructionLineIndices = new ArrayList<>();
    private Object currentLineHighlightTag;

    public Nadir() {
        cpu = new CPU();
        initializeGUI();

        // Programme de test complet
        String testProgram =
                "LDD #$1234\n" +         // Charge 1234h dans D
                        "STD >$1000\n" +         // Stocke D à l'adresse 1000h
                        "LDX #$2000\n" +         // Charge 2000h dans X pour l'indexation
                        "LDY #$ABCD\n" +         // Charge ABCDh dans Y
                        "STY $10,X\n" +          // Stocke Y à l'adresse X + 10h
                        "LDA #$55\n" +           // Charge 55h dans A
                        "INCA\n" +               // Incrémente A (devient 56h)
                        "PSHS\n" +               // Empile A
                        "CLRA\n" +               // Efface A (devient 00h)
                        "PULS\n" +               // Récupère A depuis la pile (redevient 56h)
                        "STA <$80\n" +           // Stocke A en page zéro
                        "LDB #$AA\n" +           // Charge AAh dans B
                        "DECB\n" +               // Décrémente B (devient A9h)
                        "CMPA #$56\n" +          // Compare A avec 56h
                        "CMPB <$80\n" +          // Compare B avec la valeur à l'adresse 80h
                        "LDS #$3000\n" +         // Initialise la pile S
                        "LDU #$4000\n" +         // Initialise la pile U
                        "STU >$1100\n" +         // Stocke U à l'adresse 1100h
                        "END\n";                 // Fin du programme

        programArea.setText(testProgram);
    }

    private void initializeGUI() {
        setTitle("Motorola 6809 Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Panel des registres
        JPanel registersPanel = createRegistersPanel();
        mainPanel.add(registersPanel, BorderLayout.NORTH);

        // Panel central avec programme et mémoires
        JPanel centerPanel = new JPanel(new GridLayout(1, 4));

        // Zone de programme
        JPanel programPanel = new JPanel(new BorderLayout());
        programArea = new JTextArea(20, 20);
        programArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        programPanel.setBorder(BorderFactory.createTitledBorder("Programme"));
        programPanel.add(new JScrollPane(programArea), BorderLayout.CENTER);
        centerPanel.add(programPanel);

        // Zone RAM
        JPanel ramPanel = new JPanel(new BorderLayout());
        ramArea = new JTextArea(10, 20);
        ramArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ramArea.setEditable(false);
        ramPanel.setBorder(BorderFactory.createTitledBorder("RAM"));
        ramPanel.add(new JScrollPane(ramArea), BorderLayout.CENTER);
        centerPanel.add(ramPanel);

        // Zone Stack
        JPanel stackPanel = new JPanel(new BorderLayout());
        stackArea = new JTextArea(10, 20);
        stackArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        stackArea.setEditable(false);
        stackPanel.setBorder(BorderFactory.createTitledBorder("Stack"));
        stackPanel.add(new JScrollPane(stackArea), BorderLayout.CENTER);
        centerPanel.add(stackPanel);

        // Zone ROM
        JPanel romPanel = new JPanel(new BorderLayout());
        romArea = new JTextArea(10, 20);
        romArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        romArea.setEditable(false);
        romPanel.setBorder(BorderFactory.createTitledBorder("ROM"));
        romPanel.add(new JScrollPane(romArea), BorderLayout.CENTER);
        centerPanel.add(romPanel);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Panel des boutons
        JPanel buttonPanel = new JPanel();
        stepButton = new JButton("Step");
        runButton = new JButton("Run");
        resetButton = new JButton("Reset");
        compileButton = new JButton("Compile");

        buttonPanel.add(compileButton);
        buttonPanel.add(stepButton);
        buttonPanel.add(runButton);
        buttonPanel.add(resetButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel);

        // Configuration des actions des boutons
        setupButtonActions();

        // Timer pour le mode "Run"
        runTimer = new Timer(500, e -> executeNextInstruction());

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createRegistersPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 5, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Registres"));

        // Création des champs de registres
        regA = createRegisterField("A");
        regB = createRegisterField("B");
        regD = createRegisterField("D");
        regX = createRegisterField("X");
        regY = createRegisterField("Y");
        regU = createRegisterField("U");
        regS = createRegisterField("S");
        regDP = createRegisterField("DP");
        regPC = createRegisterField("PC");
        regCCR = createRegisterField("CCR");

        // Ajout des registres au panel
        panel.add(createRegisterPanel("A", regA));
        panel.add(createRegisterPanel("B", regB));
        panel.add(createRegisterPanel("D", regD));
        panel.add(createRegisterPanel("X", regX));
        panel.add(createRegisterPanel("Y", regY));
        panel.add(createRegisterPanel("U", regU));
        panel.add(createRegisterPanel("S", regS));
        panel.add(createRegisterPanel("DP", regDP));
        panel.add(createRegisterPanel("PC", regPC));
        panel.add(createRegisterPanel("CCR", regCCR));

        return panel;
    }

    private JTextField createRegisterField(String name) {
        JTextField field = new JTextField(8);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setEditable(false);
        return field;
    }

    private JPanel createRegisterPanel(String name, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(name));
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private void setupButtonActions() {
        compileButton.addActionListener(e -> compileProgram());

        stepButton.addActionListener(e -> {
            if (!isCompiled) {
                JOptionPane.showMessageDialog(this,
                        "Veuillez d'abord compiler le programme",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            executeNextInstruction();
        });

        runButton.addActionListener(e -> {
            if (!isCompiled) {
                JOptionPane.showMessageDialog(this,
                        "Veuillez d'abord compiler le programme",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            toggleRun();
        });

        resetButton.addActionListener(e -> {
            cpu.reset();
            updateDisplay();
            highlightCurrentLine();
            isCompiled = false;
        });
    }

    private void executeNextInstruction() {
        if (!isCompiled) {
            return;
        }

        try {
            cpu.step();  // Utilise la méthode step() de CPU

            // Vérifier si on a atteint une instruction END (0x3F)
            if (cpu.readMemory(cpu.getPC()) == 0x3F) {
                isRunning = false;
                runTimer.stop();
                runButton.setText("Run");
                JOptionPane.showMessageDialog(this,
                        "Programme terminé normalement",
                        "Information",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            updateDisplay();

        } catch (Exception ex) {
            isRunning = false;
            runTimer.stop();
            runButton.setText("Run");
            JOptionPane.showMessageDialog(this,
                    "Erreur d'exécution à l'adresse " + String.format("%04X", cpu.getPC()) + ": " + ex.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateDisplay() {
        // Mise à jour des registres
        regA.setText(String.format("%02X", cpu.getRegA()));
        regB.setText(String.format("%02X", cpu.getRegB()));
        regD.setText(String.format("%04X", cpu.getRegD()));
        regX.setText(String.format("%04X", cpu.getRegX()));
        regY.setText(String.format("%04X", cpu.getRegY()));
        regU.setText(String.format("%04X", cpu.getRegU()));
        regS.setText(String.format("%04X", cpu.getRegS()));
        regDP.setText(String.format("%02X", cpu.getRegDP()));
        regPC.setText(String.format("%04X", cpu.getPC()));
        regCCR.setText(cpu.getCCRString());  // Utilise la nouvelle méthode pour afficher le CCR

        // Mise à jour des zones mémoire (un octet par ligne)
        ramArea.setText(cpu.dumpMemory(0x0000, 0x8000));  // RAM: 0x0000-0x7FFF
        romArea.setText(cpu.dumpMemory(0x8000, 0x8000));  // ROM: 0x8000-0xFFFF

        // Afficher la pile autour du pointeur de pile (128 octets centrés sur S)
        int stackPointer = cpu.getRegS();
        int stackStart = Math.max(0, stackPointer - 64);
        int stackLength = Math.min(128, 0x10000 - stackStart);
        stackArea.setText(cpu.dumpMemory(stackStart, stackLength));

        // Mettre en surbrillance l'instruction courante
        highlightCurrentLine();
    }

    private void compileProgram() {
        cpu.reset();  // Réinitialiser le CPU et la mémoire
        isCompiled = false;
        instructionStartAddresses.clear();
        instructionLineIndices.clear();

        String[] lines = sanitizeProgramText(programArea.getText()).split("\\n");
        byte[] program = new byte[0x8000];  // Taille maximale du programme (32KB)
        int programSize = 0;

        try {
            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                String line = lines[lineIndex].trim();
                if (line.isEmpty() || line.startsWith(";")) {
                    continue;
                }

                String[] parts = line.split("\\s+", 2);
                String opcode = parts[0].toUpperCase();
                String operand = parts.length > 1 ? parts[1].trim() : "";

                instructionStartAddresses.add(0x8000 + programSize);
                instructionLineIndices.add(lineIndex);

                // Assembler l'instruction
                byte[] instruction = cpu.assembleInstruction(opcode, operand);
                if (instruction == null || instruction.length == 0) {
                    throw new IllegalArgumentException("Instruction invalide: " + line);
                }

                // Vérifier le dépassement de taille
                if (programSize + instruction.length > program.length) {
                    throw new IllegalArgumentException("Programme trop grand pour la ROM");
                }

                // Copier l'instruction dans le programme
                System.arraycopy(instruction, 0, program, programSize, instruction.length);
                programSize += instruction.length;

                if (opcode.equals("END")) {
                    isCompiled = true;
                    break;
                }
            }

            if (!isCompiled) {
                throw new IllegalArgumentException("Le programme doit se terminer par END");
            }

            // Créer le programme final
            byte[] finalProgram = new byte[programSize];
            System.arraycopy(program, 0, finalProgram, 0, programSize);

            // Charger en ROM
            cpu.loadProgram(finalProgram, 0x8000);
            // Garder l'entrée utilisateur propre (sans préfixes dynamiques)
            programArea.setText(sanitizeProgramText(programArea.getText()));

            updateDisplay();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Erreur de compilation: " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            isCompiled = false;
        }
    }

    private void toggleRun() {
        if (!isRunning) {
            isRunning = true;
            runButton.setText("Stop");
            runTimer.start();
        } else {
            isRunning = false;
            runButton.setText("Run");
            runTimer.stop();
        }
    }

    private void highlightCurrentLine() {
        Highlighter highlighter = programArea.getHighlighter();
        if (currentLineHighlightTag != null) {
            highlighter.removeHighlight(currentLineHighlightTag);
            currentLineHighlightTag = null;
        }

        if (!isCompiled || instructionStartAddresses.isEmpty()) {
            return;
        }

        int currentLine = getCurrentInstructionLine();
        if (currentLine < 0) {
            return;
        }

        String text = programArea.getText();
        int lineStart = 0;
        int line = 0;
        while (line < currentLine && lineStart < text.length()) {
            int nextNewline = text.indexOf('\n', lineStart);
            if (nextNewline < 0) {
                return;
            }
            lineStart = nextNewline + 1;
            line++;
        }

        int lineEnd = text.indexOf('\n', lineStart);
        if (lineEnd < 0) {
            lineEnd = text.length();
        }

        try {
            currentLineHighlightTag = highlighter.addHighlight(
                    lineStart,
                    lineEnd,
                    new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 245, 157))
            );
            programArea.setCaretPosition(Math.min(lineStart, text.length()));
        } catch (Exception ignored) {
        }
    }

    private int getCurrentInstructionLine() {
        int pc = cpu.getPC();
        int bestLine = -1;
        for (int i = 0; i < instructionStartAddresses.size(); i++) {
            if (instructionStartAddresses.get(i) == pc) {
                bestLine = instructionLineIndices.get(i);
                break;
            }
        }
        return bestLine;
    }

    private String sanitizeProgramText(String text) {
        StringBuilder cleaned = new StringBuilder();
        String[] lines = text.split("\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].replaceFirst("^\\s*(?:=>|->)?\\s*\\d+\\s{2,}", "");
            cleaned.append(line);
            if (i < lines.length - 1) {
                cleaned.append("\n");
            }
        }
        return cleaned.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Nadir().setVisible(true);
        });
    }
}
