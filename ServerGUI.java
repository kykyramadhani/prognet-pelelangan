import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class ServerGUI extends JFrame {

    private JTextField fieldNamaBarang;
    private JTextField fieldHargaAwal;
    private JTextField fieldDurasi;
    private JButton btnTambahBarang;
    private JTextArea areaLog;

    private MultiServerPelelangan server;
    private List<BarangInput> barangList = new ArrayList<>();

    public ServerGUI() {
        setTitle("Auction Server");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 247, 250));
        setLayout(new BorderLayout());

        JLabel title = new JLabel("AUCTION SERVER", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(20, 10, 15, 10));
        add(title, BorderLayout.NORTH);

        // Left card (form)
        RoundedPanel card = new RoundedPanel(16);
        card.setBackground(Color.WHITE);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        card.setAlignmentY(TOP_ALIGNMENT);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(10, 10, 10, 10);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.anchor = GridBagConstraints.NORTHWEST;

        Font font = new Font("SansSerif", Font.PLAIN, 14);

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
        btnTambahBarang = createModernButton("Tambah Barang / Selesai");
        card.add(btnTambahBarang, gc);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        leftPanel.add(card, BorderLayout.NORTH);

        // Right panel (log)
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setFont(new Font("SansSerif", Font.PLAIN, 13));
        areaLog.setBackground(Color.WHITE);
        areaLog.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 15, 10, 15),
                BorderFactory.createLineBorder(new Color(225, 225, 225), 1)
        ));
        JScrollPane scroll = new JScrollPane(areaLog);
        scroll.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        RoundedPanel logCard = new RoundedPanel(16);
        logCard.setBackground(Color.WHITE);
        logCard.setLayout(new BorderLayout());
        logCard.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        logCard.add(scroll, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        rightPanel.setBackground(new Color(245, 247, 250));
        rightPanel.add(logCard, BorderLayout.CENTER);

        // Split pane
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(360);
        split.setResizeWeight(0.3);
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(true);

        add(split, BorderLayout.CENTER);

        btnTambahBarang.addActionListener((ActionEvent e) -> tambahBarang());

        // Inisialisasi server instance (tetap belum jalan)
        server = new MultiServerPelelangan();

        // start server thread langsung di GUI (tetap non-blocking)
        new Thread(() -> {
            try {
                server.startServer();
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> areaLog.append("ERROR Server: " + ex.getMessage() + "\n"));
            }
        }).start();
    }

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return lbl;
    }

    private JButton createModernButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
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

    private void tambahBarang() {
        String nama = fieldNamaBarang.getText().trim();
        String hargaStr = fieldHargaAwal.getText().trim();
        String durasiStr = fieldDurasi.getText().trim();

        if (nama.isEmpty() || hargaStr.isEmpty() || durasiStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Semua field harus diisi!", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int harga, durasi;
        try {
            harga = Integer.parseInt(hargaStr);
            durasi = Integer.parseInt(durasiStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Harga dan durasi harus angka!", "Format Salah", JOptionPane.ERROR_MESSAGE);
            return;
        }

        barangList.add(new BarangInput(nama, harga, durasi));
        areaLog.append("Barang ditambahkan: " + nama + " | Harga: " + harga + " | Durasi: " + durasi + " detik\n");

        fieldNamaBarang.setText("");
        fieldHargaAwal.setText("");
        fieldDurasi.setText("");

        int pilihan = JOptionPane.showConfirmDialog(this, "Tambah barang lelang lagi?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (pilihan == JOptionPane.NO_OPTION) {
            // Mendaftarkan auction ke server dan memberikan reference areaLog
            for (BarangInput b : barangList) {
                server.addAuctionFromGUI(b.nama, b.hargaAwal, b.durasi, areaLog);
            }
            areaLog.append("\n=== Semua barang sudah dimasukkan. Server berjalan... ===\n");
            btnTambahBarang.setEnabled(false);
        }
    }

    private static class BarangInput {
        String nama;
        int hargaAwal;
        int durasi;

        BarangInput(String nama, int hargaAwal, int durasi) {
            this.nama = nama;
            this.hargaAwal = hargaAwal;
            this.durasi = durasi;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI();
            gui.setVisible(true);
            gui.setExtendedState(JFrame.MAXIMIZED_BOTH);
        });
    }
}

/* Simple helper classes for rounded UI components below.
   You can keep these in the same file or separate files. */
class RoundedPanelServer extends JPanel {
    private int arc;

    RoundedPanelServer(int arc) {
        this.arc = arc;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}