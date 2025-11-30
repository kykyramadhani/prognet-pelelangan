import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class AdminChatPanel extends JPanel {
    private final MultiServerPelelangan server;
    private final JTextArea areaLog; // Log GUI utama (di ServerGUI)
    
    private JList<String> clientList;
    private DefaultListModel<String> clientListModel;
    private JTextArea chatHistory;
    private RoundedTextField chatInput;
    private JButton btnKirimPesan;
    private JButton btnBroadcast;

    private String selectedClient = null;
    private final String BROADCAST_KEY = "Semua Klien (Broadcast)";

    // Map untuk menyimpan riwayat chat pribadi (Klien -> Riwayat)
    private final Map<String, String> privateChatHistoryMap = new ConcurrentHashMap<>(); 

    public AdminChatPanel(MultiServerPelelangan server, JTextArea areaLog) {
        this.server = server;
        this.areaLog = areaLog;
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 245, 245));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // 1. Panel Daftar Klien (Kiri)
        clientListModel = new DefaultListModel<>();
        clientListModel.addElement(BROADCAST_KEY);
        clientList = new JList<>(clientListModel);
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clientList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        clientList.setSelectedIndex(0); // Default ke Broadcast
        
        clientList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateChatView();
            }
        });

        JScrollPane clientScroll = new JScrollPane(clientList);
        clientScroll.setPreferredSize(new Dimension(200, 0));
        clientScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0, 105, 92)), 
            "Klien Aktif", 0, 0, new Font("SansSerif", Font.BOLD, 12), new Color(0, 105, 92)));

        add(clientScroll, BorderLayout.WEST);

        // 2. Panel Chat (Tengah/Kanan)
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        chatHistory = new JTextArea();
        chatHistory.setEditable(false);
        chatHistory.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane historyScroll = new JScrollPane(chatHistory);
        
        // 3. Panel Input Chat
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        chatInput = new RoundedTextField(25);
        
        // Tombol Kirim Pesan Pribadi
        btnKirimPesan = createButton("Kirim Pribadi", new Color(41, 128, 185)); // Biru
        btnKirimPesan.addActionListener(e -> sendPrivateOrBroadcastChat(false));

        // Tombol Broadcast
        btnBroadcast = createButton("Broadcast", new Color(30, 150, 80)); // Hijau
        btnBroadcast.addActionListener(e -> sendPrivateOrBroadcastChat(true));

        chatInput.addActionListener(e -> sendPrivateOrBroadcastChat(false));

        JPanel buttonWrapper = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonWrapper.add(btnKirimPesan);
        buttonWrapper.add(btnBroadcast);

        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(buttonWrapper, BorderLayout.EAST);

        chatPanel.add(historyScroll, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        add(chatPanel, BorderLayout.CENTER);
        updateChatView();
    }
    
    private JButton createButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bgColor.darker()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bgColor); }
        });
        return btn;
    }

    private void updateChatView() {
        selectedClient = clientList.getSelectedValue();
        
        if (selectedClient == null) {
            chatHistory.setText("Pilih klien atau broadcast.");
            btnKirimPesan.setEnabled(false);
            btnBroadcast.setEnabled(false);
            chatInput.setEnabled(false);
            return;
        }

        chatHistory.setText(""); 
        
        if (selectedClient.equals(BROADCAST_KEY)) {
            chatHistory.append("--- MODE BROADCAST: Pesan Anda Diterima SEMUA KLIEN ---\n\n");
            btnKirimPesan.setEnabled(false); 
            btnBroadcast.setEnabled(true);
        } else {
            String history = privateChatHistoryMap.getOrDefault(selectedClient, "");
            chatHistory.append("--- CHAT PRIBADI DENGAN: " + selectedClient + " ---\n\n");
            chatHistory.append(history);
            btnKirimPesan.setEnabled(true);
            btnBroadcast.setEnabled(false);
        }
        
        chatInput.setEnabled(true);
        chatHistory.setCaretPosition(chatHistory.getDocument().getLength());
    }

    private void sendPrivateOrBroadcastChat(boolean forceBroadcast) {
        String message = chatInput.getText().trim();
        if (message.isEmpty()) return;

        if (forceBroadcast || clientList.getSelectedValue().equals(BROADCAST_KEY)) {
            // Mode Broadcast
            server.broadcastChatMessage("Admin Lelang", message);
            areaLog.append("[Admin BROADCAST]: " + message + "\n");
            
        } else if (selectedClient != null && !selectedClient.equals(BROADCAST_KEY)) {
            // Mode Pribadi
            if (server.sendPrivateMessage(selectedClient, message)) {
                appendChatHistory(selectedClient, "[Admin]", message);
                // Update tampilan chat yang sedang aktif
                updateChatView();
            } else {
                JOptionPane.showMessageDialog(this, "Klien " + selectedClient + " tidak ditemukan atau sudah disconnect.", "Error Kirim", JOptionPane.ERROR_MESSAGE);
            }
        }
        chatInput.setText("");
    }

    // Metode: Update Daftar Klien (dipanggil dari ServerGUI propertyChange)
    public void updateClientList(List<String> names) {
        SwingUtilities.invokeLater(() -> {
            String currentSelection = clientList.getSelectedValue();
            clientListModel.clear();
            clientListModel.addElement(BROADCAST_KEY);
            
            for (String name : names) {
                clientListModel.addElement(name);
                privateChatHistoryMap.putIfAbsent(name, "");
            }
            
            if (currentSelection != null) {
                clientList.setSelectedValue(currentSelection, true);
            } else {
                clientList.setSelectedIndex(0);
            }
            // Hapus klien yang sudah disconnect dari riwayat
            privateChatHistoryMap.keySet().removeIf(key -> !key.equals(BROADCAST_KEY) && !names.contains(key));
            updateChatView();
        });
    }

    // Metode: Catat dan tampilkan riwayat chat klien (dipanggil dari ServerGUI propertyChange)
    public void appendClientChat(String clientName, String message) {
        // 1. Tambahkan ke Map riwayat
        appendChatHistory(clientName, "[" + clientName + "]", message);
        
        // 2. Jika klien tersebut sedang aktif dipilih, tampilkan langsung
        if (clientName.equals(selectedClient)) {
            updateChatView();
        }
    }
    
    // Internal method untuk mengelola riwayat di map
    private void appendChatHistory(String clientKey, String sender, String message) {
        String currentHistory = privateChatHistoryMap.getOrDefault(clientKey, "");
        String newEntry = sender + ": " + message + "\n";
        privateChatHistoryMap.put(clientKey, currentHistory + newEntry);
    }
}