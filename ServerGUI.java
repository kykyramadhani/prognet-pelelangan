import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ServerGUI extends JFrame {

    private JTextField fieldNamaBarang;
    private JTextField fieldHargaAwal;
    private JTextField fieldDurasi;
    private JButton btnStartServer;
    private JTextArea areaLog;

    public ServerGUI() {

        setTitle("Auction Server");
        setSize(520, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 247, 250));
        setLayout(new BorderLayout());

        JLabel title = new JLabel("AUCTION SERVER", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(20, 10, 15, 10));
        add(title, BorderLayout.NORTH);

        RoundedPanel card = new RoundedPanel(22);
        card.setBackground(Color.WHITE);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(10, 10, 10, 10);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        Font font = new Font("Segoe UI", Font.PLAIN, 14);

        gc.gridx = 0;
        gc.gridy = 0;
        card.add(makeLabel("Nama Barang:"), gc);

        gc.gridy = 1;
        fieldNamaBarang = new RoundedTextField(20);
        fieldNamaBarang.setFont(font);
        card.add(fieldNamaBarang, gc);

        gc.gridy = 2;
        card.add(makeLabel("Harga Awal (Rp):"), gc);

        gc.gridy = 3;
        fieldHargaAwal = new RoundedTextField(20);
        fieldHargaAwal.setFont(font);
        card.add(fieldHargaAwal, gc);

        gc.gridy = 4;
        card.add(makeLabel("Durasi Lelang (detik):"), gc);

        gc.gridy = 5;
        fieldDurasi = new RoundedTextField(20);
        fieldDurasi.setFont(font);
        card.add(fieldDurasi, gc);

        gc.gridy = 6;
        gc.insets = new Insets(18, 10, 10, 10);
        btnStartServer = createModernButton("Mulai Server");
        card.add(btnStartServer, gc);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        leftPanel.add(card, BorderLayout.NORTH);

        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setFont(new Font("Consolas", Font.PLAIN, 13));
        areaLog.setBackground(Color.WHITE);
        areaLog.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 15, 10, 15),
                BorderFactory.createLineBorder(new Color(225, 225, 225), 1)
        ));

        JScrollPane scroll = new JScrollPane(areaLog);
        scroll.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        rightPanel.add(scroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(400);
        split.setResizeWeight(0.3);
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(true);

        add(split, BorderLayout.CENTER);

        btnStartServer.addActionListener((ActionEvent e) -> startServer());
    }

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return lbl;
    }

    private JButton createModernButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(66, 133, 244));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(52, 113, 210)); }
            public void mouseExited(MouseEvent e) { btn.setBackground(new Color(66, 133, 244)); }
        });

        return btn;
    }

    private void startServer() {
        String namaBarang = fieldNamaBarang.getText().trim();
        String hargaStr = fieldHargaAwal.getText().trim();
        String durasiStr = fieldDurasi.getText().trim();

        if (namaBarang.isEmpty() || hargaStr.isEmpty() || durasiStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Semua field harus diisi!", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int hargaAwal, durasi;
        try {
            hargaAwal = Integer.parseInt(hargaStr);
            durasi = Integer.parseInt(durasiStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Harga awal dan durasi harus angka!", "Format Salah", JOptionPane.ERROR_MESSAGE);
            return;
        }

        btnStartServer.setEnabled(false);
        areaLog.append("=== SERVER DIMULAI ===\n");
        areaLog.append("Barang: " + namaBarang + "\n");
        areaLog.append("Harga Awal: Rp " + hargaAwal + "\n");
        areaLog.append("Durasi: " + durasi + " detik\n\n");

        new Thread(() -> {
            try {
                MultiServerPelelangan.startFromGUI(namaBarang, hargaAwal, durasi, areaLog);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> areaLog.append("ERROR: " + ex.getMessage() + "\n"));
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI();
            gui.setVisible(true);
            gui.setExtendedState(JFrame.MAXIMIZED_BOTH);
        });
    }
}
