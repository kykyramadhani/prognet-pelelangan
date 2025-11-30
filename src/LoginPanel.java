import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

public class LoginPanel extends JPanel {
    private JTextField ipField, portField, nameField; // 'emailField' diganti 'nameField'
    private JButton connectButton;
    private AuctionClientGUI parentFrame;
    
    private final Color INPUT_BG = new Color(255, 255, 255);
    private final Color DARK_ACCENT_COLOR = new Color(0, 105, 92); 

    public LoginPanel(AuctionClientGUI parent) {
        this.parentFrame = parent;
        setLayout(new GridBagLayout());
        setBackground(new Color(245, 245, 245)); 
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // --- Input Server ---
        gbc.gridwidth = 1;
        gbc.gridy = 1; add(new JLabel("IP Server:"), gbc);
        gbc.gridx = 1; ipField = createStyledTextField("127.0.0.1"); add(ipField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; add(new JLabel("Port:"), gbc);
        gbc.gridx = 1; portField = createStyledTextField("1234"); add(portField, gbc);
        
        // --- Input Nama (Pengganti Kredensial) ---
        gbc.gridx = 0; gbc.gridy = 3; add(new JLabel("Nama Panggilan:"), gbc);
        gbc.gridx = 1; nameField = createStyledTextField("Penawar_X"); add(nameField, gbc);
        
        // *** JPasswordField dihapus ***
        
        // --- Tombol Connect ---
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        connectButton = createRoundedButton("Gabung Lelang", DARK_ACCENT_COLOR);
        connectButton.addActionListener(e -> attemptConnect());
        add(connectButton, gbc);
    }
    
    // Helper methods (tidak berubah)
    private RoundedTextField createStyledTextField(String text) {
        RoundedTextField field = new RoundedTextField(text, 20);
        field.setBackground(INPUT_BG);
        return field;
    }
    
    private JButton createRoundedButton(String text, Color bgColor) {
        JButton button = new JButton(text) {
            private int radius = 15;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
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

    // --- Logika Koneksi Sederhana ---
    private void attemptConnect() {
        connectButton.setEnabled(false);
        
        String ip = ipField.getText();
        int port;
        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port harus berupa angka.", 
                "Kesalahan Input", JOptionPane.ERROR_MESSAGE);
            connectButton.setEnabled(true);
            return;
        }
        
        String username = nameField.getText().trim();
        
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama Panggilan harus diisi.", 
                "Peringatan", JOptionPane.WARNING_MESSAGE);
            connectButton.setEnabled(true);
            return;
        }

        // Langsung coba koneksi tanpa simulasi otentikasi
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // Beri waktu sebentar sebelum koneksi nyata (Opsional)
                Thread.sleep(100); 
                return username; // Kembalikan username sebagai token sederhana
            }

            @Override
            protected void done() {
                connectButton.setEnabled(true);
                try {
                    String finalUsername = get();
                    // ID Token sekarang sama dengan username
                    parentFrame.startConnectionForSelection(ip, port, finalUsername, finalUsername); 
                } catch (Exception ex) {
                    // Hanya menangani error dari SwingWorker (biasanya Thread Exception)
                }
            }
        }.execute();
    }
}