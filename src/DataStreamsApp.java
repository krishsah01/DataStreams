import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * IT 2045C – Lab 09: Java Data Streams
 * Swing GUI that loads a plain‑text file (.txt, .csv, .log, etc.),
 * searches it with Java Stream API, and shows original + filtered
 * results in side‑by‑side panes.
 */
public class DataStreamsApp extends JFrame {

    private final JTextArea originalArea = new JTextArea();
    private final JTextArea filteredArea = new JTextArea();
    private final JTextField searchField = new JTextField(20);
    private final JButton loadButton = new JButton("Load File");
    private final JButton searchButton = new JButton("Search");
    private final JButton quitButton = new JButton("Quit");

    private Path currentFile;
    private List<String> originalLines;

    public DataStreamsApp() {
        super("Java Data Stream Search");
        initUI();
    }

    private void initUI() {
        originalArea.setEditable(false);
        filteredArea.setEditable(false);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(originalArea),
                new JScrollPane(filteredArea));
        splitPane.setResizeWeight(0.5);

        JPanel controls = new JPanel();
        controls.add(new JLabel("Search:"));
        controls.add(searchField);
        controls.add(loadButton);
        controls.add(searchButton);
        controls.add(quitButton);

        add(splitPane, BorderLayout.CENTER);
        add(controls, BorderLayout.SOUTH);

        loadButton.addActionListener(e -> loadFile());
        searchButton.addActionListener(e -> searchFile());
        quitButton.addActionListener(e -> dispose());
        searchButton.setEnabled(false);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt", "csv", "log"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        currentFile = chooser.getSelectedFile().toPath();

        try {
            originalLines = readAllLinesSmart(currentFile);
            originalArea.setText(String.join("\n", originalLines));
            filteredArea.setText("");
            searchButton.setEnabled(true);
        } catch (IOException | UncheckedIOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not read the file as text. It may be binary or use an exotic encoding.\n" + ex.getMessage(),
                    "Unsupported File",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<String> readAllLinesSmart(Path file) throws IOException {
        List<Charset> charsets = Arrays.asList(StandardCharsets.UTF_8,
                Charset.defaultCharset(),
                StandardCharsets.ISO_8859_1,
                StandardCharsets.US_ASCII);

        UncheckedIOException lastFail = null;
        for (Charset cs : charsets) {
            try (Stream<String> lines = Files.lines(file, cs)) {
                return lines.collect(Collectors.toList());
            } catch (UncheckedIOException uioe) {
                if (uioe.getCause() instanceof MalformedInputException) {
                    lastFail = uioe;
                } else {
                    throw uioe; 
                }
            }
        }
        throw new IOException("Unable to decode file with common charsets", lastFail);
    }

    private void searchFile() {
        String term = searchField.getText();
        if (term.isBlank() || originalLines == null) return;

        List<String> matched = originalLines.stream()
                .filter(line -> line.contains(term))
                .collect(Collectors.toList());
        filteredArea.setText(String.join("\n", matched));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DataStreamsApp::new);
    }
}
