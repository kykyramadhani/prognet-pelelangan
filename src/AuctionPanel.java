import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class AuctionPanel extends JPanel {
    private JPanel logPanel;
    private RoundedTextField bidField;
    private JButton bidButton;
    private JLabel itemLabel, holderLabel;

    private RoundedPanel priceDisplayPanel;
    private JLabel priceValueLabel;
    private Timer highlightTimer;

    // --- komponen buat chatting ---
    private JTextArea chatArea;
    private RoundedTextField chatInputField;
    private JButton chatButton;
    // --- akhir komponen chat ---

    private final Color DARK_ACCENT_COLOR = new Color(0, 105, 92);
    private final Color DARK_SUCCESS_COLOR = new Color(30, 150, 80);
    private final Color REJECT_COLOR = new Color(231, 76, 60);
    private final Color ACCEPT_COLOR = new Color(39, 174, 96);
    private final Color UPDATE_COLOR = new Color(52, 152, 219);
    private final Color CHAT_COLOR = new Color(41, 128, 185);
    private final int RADIUS = 15;
    private final int CARD_RADIUS = 10;

    private Socket socket;
    private PrintWriter networkOutput;
    private AuctionClientGUI parentFrame;
    private String selectedAuctionId = null;
    private String username; 

    private JButton createRoundedButton(String text, Color bgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), RADIUS, RADIUS));
                super.paintComponent(g);
                g2.dispose();
            }
            @Override
            protected void paintBorder(Graphics g) {}
        };
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        return button;
    }

    public AuctionPanel(AuctionClientGUI parent) {
        this.parentFrame = parent;
        setBackground(new Color(240, 240, 240));
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel headerPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        headerPanel.setOpaque(false);

        RoundedPanel itemDetailsPanel = new RoundedPanel(new GridLayout(2, 1), RADIUS);
        itemDetailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        itemDetailsPanel.setBackground(Color.WHITE);
        itemLabel = new JLabel("Barang: -");
        itemLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        holderLabel = new JLabel("Penawar Tertinggi: -");
        itemDetailsPanel.add(itemLabel);
        itemDetailsPanel.add(holderLabel);

        priceDisplayPanel = new RoundedPanel(new BorderLayout(), RADIUS);
        priceDisplayPanel.setBackground(new Color(255, 255, 204));
        priceDisplayPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        priceValueLabel = new JLabel("Rp 0", SwingConstants.CENTER);
        priceValueLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        priceValueLabel.setForeground(new Color(192, 57, 43));
        JLabel priceTitle = new JLabel("HARGA TERTINGGI SAAT INI", SwingConstants.CENTER);
        priceTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        priceDisplayPanel.add(priceTitle, BorderLayout.NORTH);
        priceDisplayPanel.add(priceValueLabel, BorderLayout.CENTER);
        headerPanel.add(itemDetailsPanel);
        headerPanel.add(priceDisplayPanel);
        add(headerPanel, BorderLayout.NORTH);

        // --- log lelang (kiri) ---
        logPanel = new JPanel();
        logPanel.setLayout(new BoxLayout(logPanel, BoxLayout.Y_AXIS));
        logPanel.setBackground(new Color(240, 240, 240));

        JScrollPane scrollPane = new JScrollPane(logPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                "Log Aktivitas Real-time",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.PLAIN, 12), DARK_ACCENT_COLOR));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // --- panel dibagi dua buat log ama chat ---
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, createChatPanel());
        centerSplit.setResizeWeight(0.7); 
        centerSplit.setContinuousLayout(true);
        centerSplit.setDividerLocation(700);

        add(centerSplit, BorderLayout.CENTER);

        // --- panel input (tawaran & chat) ---
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        bidField = new RoundedTextField(15);
        bidField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        bidField.setBackground(Color.WHITE);

        bidButton = createRoundedButton("Kirim Tawaran!", DARK_SUCCESS_COLOR);
        bidButton.setEnabled(false);
        bidButton.addActionListener(e -> sendBid());
        bidField.addActionListener(e -> sendBid());

        inputPanel.add(new JLabel("Masukkan Nilai Tawaran (Rp):"), BorderLayout.WEST);
        inputPanel.add(bidField, BorderLayout.CENTER);
        inputPanel.add(bidButton, BorderLayout.EAST);
        
        // --- panel input bawah (gabung tawaran ama chat) ---
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1, 0, 5)); 
        bottomPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        bottomPanel.add(inputPanel);
        bottomPanel.add(createChatInputPanel());
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        highlightTimer = new Timer(500, e -> resetPriceDisplayColor());
        highlightTimer.setRepeats(false);
    }
    
    private JComponent createChatPanel() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder(
             BorderFactory.createLineBorder(Color.LIGHT_GRAY),
             "Obrolan (Admin & Broadcast)",
             TitledBorder.LEFT, TitledBorder.TOP,
             new Font("SansSerif", Font.PLAIN, 12), CHAT_COLOR)); 
        
        appendChat("Sistem", "Selamat datang di ruang obrolan. Pesan CHAT akan dikirim ke Admin.");

        return chatScroll;
    }
    
    private JComponent createChatInputPanel() {
        JPanel inputChatPanel = new JPanel(new BorderLayout(10, 0));

        chatInputField = new RoundedTextField(25);
        chatInputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        chatInputField.setBackground(Color.WHITE);
        chatInputField.setEnabled(false);

        chatButton = createRoundedButton("Kirim Chat ke Admin", CHAT_COLOR);
        chatButton.setEnabled(false);
        
        chatButton.addActionListener(e -> sendChat());
        chatInputField.addActionListener(e -> sendChat());

        inputChatPanel.add(chatInputField, BorderLayout.CENTER);
        inputChatPanel.add(chatButton, BorderLayout.EAST);
        
        return inputChatPanel;
    }
    
    public void appendChat(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append("[" + sender + "] " + message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void sendChat() {
        String chatText = chatInputField.getText().trim();
        if (chatText.isEmpty()) return;
        
        if (networkOutput != null && !socket.isClosed()) {
            networkOutput.println("CHAT:" + chatText);
            
            // nampilin pesan sendiri di chat area (feedback doang)
            appendChat("Anda (Pribadi)", chatText); 
            
            chatInputField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "koneksi server putus.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void addLogCard(String message, String type) {
        Color cardColor;
        String headerText;
         switch (type) {
            case "ACCEPTED":
                cardColor = ACCEPT_COLOR;
                headerText = "✅ TAWARAN DITERIMA";
                break;
            case "REJECTED":
                cardColor = REJECT_COLOR;
                headerText = "❌ TAWARAN DITOLAK";
                break;
            case "ERROR":
                cardColor = REJECT_COLOR;
                headerText = "⚠️ ERROR SERVER";
                break;
            case "UPDATE":
                cardColor = UPDATE_COLOR;
                headerText = "🔔 UPDATE HARGA";
                break;
            default:
                cardColor = DARK_ACCENT_COLOR;
                headerText = "ℹ️ INFO";
                break;
        }

        RoundedPanel card = new RoundedPanel(new BorderLayout(), CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 0, 5, 0),
            BorderFactory.createLineBorder(cardColor, 1, true)
        ));

        JLabel header = new JLabel("  " + headerText);
        header.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.setForeground(Color.WHITE);
        header.setBackground(cardColor);
        header.setOpaque(true);
        card.add(header, BorderLayout.NORTH);

        JTextArea content = new JTextArea(message);
        content.setEditable(false);
        content.setLineWrap(true);
        content.setWrapStyleWord(true);
        content.setFont(new Font("SansSerif", Font.PLAIN, 12));
        content.setBorder(new EmptyBorder(8, 8, 8, 8));
        card.add(content, BorderLayout.CENTER);

        logPanel.add(card);
        logPanel.revalidate();

        SwingUtilities.invokeLater(() -> {
            logPanel.getParent().getParent().validate();
            JScrollBar vertical = ((JScrollPane) logPanel.getParent().getParent()).getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    public void connectAndStartListener(String ip, int port, String username, String idToken) throws IOException {
    this.username = username; // simpen username buat chatting
    try {
        this.socket = new Socket(ip, port);
        this.networkOutput = new PrintWriter(socket.getOutputStream(), true);
        
        // protokol tetep login:username:idtoken (dimana idtoken = username)
        this.networkOutput.println("LOGIN:" + username + ":" + idToken); 
        
        addLogCard("Mencoba bergabung sebagai: " + username, "INFO");
        new ServerListenerTask(socket, this).execute();
        bidButton.setEnabled(true);
        chatButton.setEnabled(true); 
        chatInputField.setEnabled(true); 
    } catch (IOException e) {
        closeConnection();
        throw e;
    }
}

    public void sendSelectAuction(String auctionId) {
        if (networkOutput != null) {
            networkOutput.println("SELECT:" + auctionId);
            addLogCard("Memilih lelang: " + auctionId, "INFO");
            this.selectedAuctionId = auctionId;
        } else {
            addLogCard("Belum terhubung ke server.", "ERROR");
        }
    }

    public void setSelectedItem(String itemName, String auctionId) {
        this.selectedAuctionId = auctionId;
        itemLabel.setText("Barang: " + itemName + " (ID: " + auctionId + ")");
        holderLabel.setText("Penawar Tertinggi: -");
        priceValueLabel.setText("Rp 0");
    }

    private void sendBid() {
        if (networkOutput == null || socket == null || socket.isClosed()) {
            JOptionPane.showMessageDialog(this, "koneksi putus.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String bidText = bidField.getText().trim();
        if (bidText.isEmpty()) return;

        try {
            Long.parseLong(bidText);
            networkOutput.println(bidText);
            bidField.setText("");
            addLogCard("Anda mengirim tawaran: Rp " + bidText, "INFO");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Tawaran wajib berupa angka.", "Input Salah", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            addLogCard("Gagal mengirim tawaran.", "ERROR");
            closeConnection();
        }
    }

    public void updateStatus(String item, String price, String holder) {
        itemLabel.setText("Barang: " + item);
        holderLabel.setText("Penawar Tertinggi: " + holder);

        String currentPriceStr = priceValueLabel.getText().replace("Rp ", "").replaceAll(",", "");
        if (!currentPriceStr.equals(price)) {
            priceValueLabel.setText("Rp " + price);
            highlightNewBid();
        }
    }

    private void highlightNewBid() {
        priceDisplayPanel.setBackground(new Color(255, 99, 71));
        priceValueLabel.setForeground(Color.WHITE);

        if (highlightTimer.isRunning()) {
            highlightTimer.restart();
        } else {
            highlightTimer.start();
        }
    }

    private void resetPriceDisplayColor() {
        priceDisplayPanel.setBackground(new Color(255, 255, 204));
        priceValueLabel.setForeground(new Color(192, 57, 43));
    }

    public void closeAuction(String message) {
        closeConnection();
        parentFrame.backToLoginView();
        JOptionPane.showMessageDialog(parentFrame, message, "Lelang Ditutup", JOptionPane.INFORMATION_MESSAGE);
    }

    public void closeConnection() {
        bidButton.setEnabled(false);
        chatButton.setEnabled(false); 
        chatInputField.setEnabled(false); 
        addLogCard("Koneksi terputus/Lelang berakhir.", "INFO");
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ioEx) {}
    }

    private class ServerListenerTask extends SwingWorker<Void, String> {
        private Scanner networkInput;
        private AuctionPanel panel;

        public ServerListenerTask(Socket socket, AuctionPanel panel) throws IOException {
            this.panel = panel;
            this.networkInput = new Scanner(socket.getInputStream());
        }

        @Override
        protected Void doInBackground() throws Exception {
            try {
                while (!isCancelled() && networkInput.hasNextLine()) {
                    String serverMessage = networkInput.nextLine();
                    publish(serverMessage);
                }
            } catch (Exception e) {
                publish("DISCONNECT:Koneksi ke server terputus.");
            }
            return null;
        }

        @Override
        protected void process(java.util.List<String> chunks) {
            for (String serverMessage : chunks) {
                if (serverMessage.startsWith("AUCTIONS:")) {
                    parentFrame.updateSelectItemPanel(serverMessage);
                    continue;
                }

                String[] parts = serverMessage.split(":", 2);
                String command = parts[0];
                String data = (parts.length > 1) ? parts[1] : "";

                switch (command) {
                    case "STATUS":
                        String[] statusData = data.split(":");
                        panel.updateStatus(statusData[0], statusData[1], statusData[2]);
                        panel.addLogCard("Status lelang saat ini dimuat. Barang: " + statusData[0], "UPDATE");
                        break;

                    case "UPDATE":
                        String[] updateData = data.split(":");
                        panel.updateStatus(itemLabel.getText(), updateData[0], updateData[1]);
                        panel.addLogCard("Harga baru: Rp " + updateData[0] + " oleh " + updateData[1], "UPDATE");
                        break;

                    case "REJECTED":
                        panel.addLogCard("Tawaran Anda ditolak. Alasan: " + data, "REJECTED");
                        break;

                    case "ACCEPTED":
                        panel.addLogCard("Tawaran Anda diterima! Harga saat ini: " + data, "ACCEPTED");
                        break;

                    case "CLOSED":
                        panel.closeAuction("Lelang telah ditutup. " + data);
                        break;

                    case "ERROR":
                        panel.addLogCard("Error dari server: " + data, "ERROR");
                        break;
                        
                    case "CHAT_BROADCAST": 
                        // formatnya: chat_broadcast:namaadmin:pesan
                        String[] broadcastParts = data.split(":", 2);
                        if (broadcastParts.length == 2) {
                            panel.appendChat(broadcastParts[0] + " (All)", broadcastParts[1]); 
                        } else {
                            panel.appendChat("Admin (All)", data); 
                        }
                        break;
                        
                    case "CHAT_PRIVATE": 
                        // formatnya: chat_private:namaadmin:pesan
                        String[] privateParts = data.split(":", 2);
                        if (privateParts.length == 2) {
                            panel.appendChat("💬 " + privateParts[0], privateParts[1]);
                        } else {
                            panel.appendChat("💬 Admin", data); 
                        }
                        break;

                    case "DISCONNECT":
                        panel.closeConnection();
                        JOptionPane.showMessageDialog(parentFrame, data, "Koneksi Terputus", JOptionPane.ERROR_MESSAGE);
                        parentFrame.backToLoginView();
                        break;

                    default:
                        panel.addLogCard("Pesan Server: " + serverMessage, "INFO");
                }
            }
        }

        @Override
        protected void done() {
            panel.closeConnection();
        }
    }
}