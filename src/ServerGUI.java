import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener; 
import java.beans.PropertyChangeEvent; 
import java.util.ArrayList;
import java.util.List;

public class ServerGUI extends JFrame implements PropertyChangeListener {

    // field input data barang
    private JTextField fieldNamaBarang;
    private JTextField fieldHargaAwal;
    private JTextField fieldDurasi;

    // tombol buat nambah barang lelang
    private RoundedButton btnTambahBarang;

    // area log aktivitas server (bukan area chat ya tolong)
    private JTextArea areaLog; 
    
    // ini baru panel chat admin
    private AdminChatPanel chatPanel;
    
    // server utama pelelangan
    private MultiServerPelelangan server;

    // nyimpen data barang sebelum dikirim ke server
    private List<BarangInput> barangList = new ArrayList<>();

    public ServerGUI() {
        setTitle("Auction Server");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 247, 250));
        setLayout(new BorderLayout());

        JLabel title = new JLabel("AUCTION SERVER - PENGELOLA LELANG", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(20, 10, 15, 10));
        add(title, BorderLayout.NORTH);

  
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

        gc.gridx = 0; gc.gridy = 0;
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
        btnTambahBarang = createModernButton("Tambah Barang & Persiapan");
        card.add(btnTambahBarang, gc);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        leftPanel.add(card, BorderLayout.NORTH);

        //  (log server + chat admin)
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setFont(new Font("SansSerif", Font.PLAIN, 13));
        areaLog.setBackground(Color.WHITE);
        areaLog.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 15, 10, 15),
                BorderFactory.createLineBorder(new Color(225, 225, 225), 1)
        ));
        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        RoundedPanel logCard = new RoundedPanel(25);
        logCard.setOpaque(false); 

        logCard.setBackground(Color.WHITE);
        logCard.setLayout(new BorderLayout());
        logCard.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        logCard.add(scrollLog, BorderLayout.CENTER);

        server = new MultiServerPelelangan(); // server harus nyala dulu sebelum chat
        chatPanel = new AdminChatPanel(server, areaLog); 
        areaLog.addPropertyChangeListener(this); // dengerin event dari server

        JPanel rightPanelContent = new JPanel(new GridLayout(2, 1, 0, 10));
        rightPanelContent.add(logCard);
        rightPanelContent.add(chatPanel); 

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        rightPanel.setBackground(new Color(245, 247, 250));
        rightPanel.add(rightPanelContent, BorderLayout.CENTER); 

        // split layar kiri (form) dan kanan (log + chat)
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(360);
        split.setResizeWeight(0.3);
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(true);

        add(split, BorderLayout.CENTER);

        // klik tombol = tambah barang
        btnTambahBarang.addActionListener((ActionEvent e) -> tambahBarang());

        // jalanin server di thread terpisah biar gui ga freeze
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

   
    private RoundedButton createModernButton(String text) {
        RoundedButton btn = new RoundedButton(text, 10, new Color(66, 133, 244), new Color(66, 133, 200));
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(66, 133, 244));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // efek hover biar tombol keliatan hidup
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(52, 113, 210)); }
            public void mouseExited(MouseEvent e) { btn.setBackground(new Color(66, 133, 244)); }
        });
        return btn;
    }

    // logic utama buat nambah barang lelang dari form
    private void tambahBarang() {
        String nama = fieldNamaBarang.getText().trim();
        String hargaStr = fieldHargaAwal.getText().trim();
        String durasiStr = fieldDurasi.getText().trim();

        // validasi input kosong
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

        // simpen barang ke list sementara
        barangList.add(new BarangInput(nama, harga, durasi));
        areaLog.append("Barang ditambahkan: " + nama + " | Harga: " + harga + " | Durasi: " + durasi + " detik\n");

        // reset field input
        fieldNamaBarang.setText("");
        fieldHargaAwal.setText("");
        fieldDurasi.setText("");

        // nanya mau nambah barang lagi atau engga
        int pilihan = JOptionPane.showConfirmDialog(this, "Tambah barang lelang lagi?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (pilihan == JOptionPane.NO_OPTION) {
            // kirim semua barang ke server
            for (BarangInput b : barangList) {
                server.addAuctionFromGUI(b.nama, b.hargaAwal, b.durasi, areaLog);
            }
            areaLog.append("\n=== Semua barang sudah dimasukkan. Server berjalan... ===\n");
            btnTambahBarang.setEnabled(false);
        }
    }
    
   
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("clientListUpdate")) {
            // update list client di panel chat
            chatPanel.updateClientList(server.getClientNames());
            
        } else if (evt.getPropertyName().equals("newClientChat")) {
            // nerima chat baru dari client
            String clientName = (String) evt.getOldValue();
            String message = (String) evt.getNewValue();
            chatPanel.appendClientChat(clientName, message);
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
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            ServerGUI gui = new ServerGUI();
            gui.setVisible(true);
            gui.setExtendedState(JFrame.MAXIMIZED_BOTH);
        });
    }
}
