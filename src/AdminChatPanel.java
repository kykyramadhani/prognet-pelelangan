import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class AdminChatPanel extends JPanel {
    private final MultiServerPelelangan server;
    private final JTextArea areaLog;
    
    private JList<String> clientList;
    private DefaultListModel<String> clientListModel;
    private JTextArea chatHistory;
    private RoundedTextField chatInput;
    private RoundedButton btnKirimPesan;
    private RoundedButton btnBroadcast;

    private String selectedClient = null;
    private final String BROADCAST_KEY = "Semua Klien (Broadcast)";

    private final Map<String, String> privateChatHistoryMap = new ConcurrentHashMap<>();

    public AdminChatPanel(MultiServerPelelangan server, JTextArea areaLog) {
        this.server = server;
        this.areaLog = areaLog;

        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 245, 245));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        clientListModel = new DefaultListModel<>();
        clientListModel.addElement(BROADCAST_KEY);
        clientList = new JList<>(clientListModel);
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clientList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        clientList.setSelectedIndex(0);

        clientList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateChatView();
        });

        JScrollPane clientScroll = new JScrollPane(clientList);
        clientScroll.setPreferredSize(new Dimension(200, 0));
        clientScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0, 105, 92)),
                "Klien Aktif",
                0, 0,
                new Font("SansSerif", Font.BOLD, 12),
                new Color(0, 105, 92)
        ));
        add(clientScroll, BorderLayout.WEST);

        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        chatHistory = new JTextArea();
        chatHistory.setEditable(false);
        chatHistory.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane historyScroll = new JScrollPane(chatHistory);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        chatInput = new RoundedTextField(20);

        Color greenBtn = new Color(67, 160, 71);

        btnKirimPesan = createButton("Kirim Pribadi", greenBtn);
        btnKirimPesan.addActionListener(e -> sendPrivateOrBroadcastChat(false));

        btnBroadcast = createButton("Broadcast", greenBtn);
        btnBroadcast.addActionListener(e -> sendPrivateOrBroadcastChat(true));

        chatInput.addActionListener(e -> sendPrivateOrBroadcastChat(false));

        // tombol cm ambil space dikit aja
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonWrapper.add(btnKirimPesan);
        buttonWrapper.add(btnBroadcast);

        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(buttonWrapper, BorderLayout.EAST);

        chatPanel.add(historyScroll, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        add(chatPanel, BorderLayout.CENTER);
        updateChatView();
    }

    private RoundedButton createButton(String text, Color bgColor) {
        RoundedButton btn = new RoundedButton(text, 5, bgColor, bgColor.darker());
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setBackground(bgColor);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);

        // efek pas mouse lewat (hover)
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(bgColor.darker());
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(bgColor);
            }
        });

        return btn;
    }

    private void updateChatView() {
        selectedClient = clientList.getSelectedValue();
        if (selectedClient == null) return;

        chatHistory.setText("");

        if (selectedClient.equals(BROADCAST_KEY)) {
            chatHistory.append("=== Mode Broadcast: Pesan terkirim ke semua client ===\n\n");

            btnBroadcast.setVisible(true);
            btnBroadcast.setEnabled(true);
            btnKirimPesan.setVisible(false);

        } else {
            chatHistory.append("=== Chat Pribadi dengan: " + selectedClient + " ===\n\n");
            chatHistory.append(privateChatHistoryMap.getOrDefault(selectedClient, ""));

            btnBroadcast.setVisible(false);
            btnKirimPesan.setVisible(true);
            btnKirimPesan.setEnabled(true);
        }

        chatHistory.setCaretPosition(chatHistory.getDocument().getLength());
    }

    private void sendPrivateOrBroadcastChat(boolean forceBroadcast) {
        String msg = chatInput.getText().trim();
        if (msg.isEmpty()) return;

        if (forceBroadcast || selectedClient.equals(BROADCAST_KEY)) {
            server.broadcastChatMessage("Admin Lelang", msg);
            areaLog.append("[Admin BROADCAST]: " + msg + "\n");
        } else {
            if (server.sendPrivateMessage(selectedClient, msg)) {
                appendChatHistory(selectedClient, "[Admin]", msg);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Klien tidak ditemukan atau telah terputus.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        updateChatView();
        chatInput.setText("");
    }

    public void updateClientList(List<String> names) {
        SwingUtilities.invokeLater(() -> {
            String oldSelection = clientList.getSelectedValue();

            clientListModel.clear();
            clientListModel.addElement(BROADCAST_KEY);

            for (String name : names) {
                clientListModel.addElement(name);
                privateChatHistoryMap.putIfAbsent(name, "");
            }

            if (oldSelection != null)
                clientList.setSelectedValue(oldSelection, true);
            else
                clientList.setSelectedIndex(0);

            privateChatHistoryMap.keySet().removeIf(
                    key -> !key.equals(BROADCAST_KEY) && !names.contains(key)
            );

            updateChatView();
        });
    }

    public void appendClientChat(String client, String msg) {
        appendChatHistory(client, "[" + client + "]", msg);
        if (client.equals(selectedClient)) updateChatView();
    }

    private void appendChatHistory(String key, String sender, String msg) {
        privateChatHistoryMap.put(key,
                privateChatHistoryMap.getOrDefault(key, "") + sender + ": " + msg + "\n");
    }
}