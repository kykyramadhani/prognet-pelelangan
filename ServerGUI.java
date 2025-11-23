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
        setTitle("Server Pelelangan");
        setSize(520, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Modern background
        getContentPane().setBackground(new Color(245, 247, 250));
        setLayout(new BorderLayout());

        // Panel utama (card)
        RoundedPanel card = new RoundedPanel(20);
        card.setBackground(Color.WHITE);
        card.setLayout(new GridLayout(4, 2, 12, 12));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("SERVER PELELANGAN", SwingConstants.CENTER);
        title.setFont(new Font("Roboto", Font.BOLD, 20));
        title.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        add(title, BorderLayout.NORTH);

        // FONT MODERN
        Font font = new Font("Roboto", Font.PLAIN, 14);

        JLabel labelNama = new JLabel("Nama Barang:");
        labelNama.setFont(font);
        fieldNamaBarang = new JTextField();
        fieldNamaBarang.setFont(font);

        JLabel labelHarga = new JLabel("Harga Awal (Rp):");
        labelHarga.setFont(font);
        fieldHargaAwal = new JTextField();
        fieldHargaAwal.setFont(font);

        JLabel labelDurasi = new JLabel("Durasi Lelang (detik):");
        labelDurasi.setFont(font);
        fieldDurasi = new JTextField();
        fieldDurasi.setFont(font);

        // Tambahkan ke card panel
        card.add(labelNama);
        card.add(fieldNamaBarang);

        card.add(labelHarga);
        card.add(fieldHargaAwal);

        card.add(labelDurasi);
        card.add(fieldDurasi);

        // Modern button
        btnStartServer = createModernButton("Mulai Server");
        card.add(new JLabel()); // filler
        card.add(btnStartServer);

        add(card, BorderLayout.CENTER);

        // Area log modern
        areaLog = new JTextArea();
        areaLog.setFont(new Font("Consolas", Font.PLAIN, 13));
        areaLog.setEditable(false);
        areaLog.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(areaLog);
        scroll.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(scroll, BorderLayout.SOUTH);

        // Event button
        btnStartServer.addActionListener((ActionEvent e) -> startServer());
    }

    // ========== BUTTON STYLE ==========
    private JButton createModernButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(66, 133, 244));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(52, 113, 210));
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(66, 133, 244));
            }
        });
        return btn;
    }

    // ========== PANEL ROUNDED ==========
    class RoundedPanel extends JPanel {
        private int radius;

        public RoundedPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Dimension arcs = new Dimension(radius, radius);
            int width = getWidth();
            int height = getHeight();
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, width - 1, height - 1, arcs.width, arcs.height);
        }
    }

    // ========== LOGIC ==========
    private void startServer() {
        String namaBarang = fieldNamaBarang.getText().trim();
        String hargaStr = fieldHargaAwal.getText().trim();
        String durasiStr = fieldDurasi.getText().trim();

        if (namaBarang.isEmpty() || hargaStr.isEmpty() || durasiStr.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Semua field harus diisi!",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int hargaAwal, durasi;
        try {
            hargaAwal = Integer.parseInt(hargaStr);
            durasi = Integer.parseInt(durasiStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Harga awal dan durasi harus angka!",
                    "Format Salah",
                    JOptionPane.ERROR_MESSAGE);
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
        SwingUtilities.invokeLater(() -> new ServerGUI().setVisible(true));
    }
}
