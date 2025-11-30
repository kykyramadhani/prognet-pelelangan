import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

public class LoginPanel extends JPanel {
    private JTextField ipField, portField, emailField;
    private JPasswordField passwordField;
    private JButton connectButton;
    private AuctionClientGUI parentFrame;
    
    private final Color INPUT_BG = new Color(255, 255, 255);
    // Tombol lebih gelap
    private final Color DARK_ACCENT_COLOR = new Color(0, 105, 92); // Teal yang lebih gelap

    public LoginPanel(AuctionClientGUI parent) {
        this.parentFrame = parent;
        setLayout(new GridBagLayout());
        setBackground(new Color(245, 245, 245)); 
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Judul
        // ... (Tidak perlu dibulatkan karena hanya JLabel)
        
        // --- Input Server (Menggunakan RoundedTextField) ---
        gbc.gridwidth = 1;
        gbc.gridy = 1; add(new JLabel("IP Server:"), gbc);
        gbc.gridx = 1; ipField = createStyledTextField(""); add(ipField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; add(new JLabel("Port:"), gbc);
        gbc.gridx = 1; portField = createStyledTextField(""); add(portField, gbc);
        
        // --- Input Kredensial ---
        gbc.gridx = 0; gbc.gridy = 3; add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; emailField = createStyledTextField(""); add(emailField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; add(new JLabel("Password:"), gbc);
        // Untuk JPasswordField, kita bisa extend RoundedTextField, atau membuat kustomisasi sendiri
        // Untuk kesederhanaan, kita gunakan JPasswordField biasa dengan setting border
        passwordField = new RoundedPasswordField(20);
        passwordField.setBackground(INPUT_BG);
        gbc.gridx = 1; add(passwordField, gbc);
        
        // --- Tombol Connect (Tombol Gelap + Sudut Membulat) ---
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        connectButton = createRoundedButton("Gabung & Otentikasi", DARK_ACCENT_COLOR);
        connectButton.addActionListener(e -> attemptConnect());
        add(connectButton, gbc);
    }
    
    // Helper method untuk styling RoundedTextField
    private RoundedTextField createStyledTextField(String text) {
        RoundedTextField field = new RoundedTextField(text, 20);
        field.setBackground(INPUT_BG);
        return field;
    }
    
    // Helper method untuk membuat JButton dengan sudut membulat
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
            protected void paintBorder(Graphics g) {
                // Tidak menggambar border default
            }
        };
        button.setForeground(Color.WHITE); 
        button.setFocusPainted(false);
        button.setContentAreaFilled(false); // Penting agar paintComponent bekerja
        return button;
    }

    // --- Logika Otentikasi (Sama dengan sebelumnya, dijalankan di SwingWorker) ---
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
        
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Email dan Password harus diisi.", 
                "Peringatan", JOptionPane.WARNING_MESSAGE);
            connectButton.setEnabled(true);
            return;
        }

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
               
                Thread.sleep(1000); 
                if (email.equals("user@lelang.com") && password.equals("password123")) {
                    return ": " + email; 
                } else {
                    return null;
                }
            }

            @Override
            protected void done() {
                connectButton.setEnabled(true);
                try {
                    String idToken = get();
                    if (idToken != null) {
                        parentFrame.startConnectionForSelection(ip, port, email, idToken); 
                    } else {
                        JOptionPane.showMessageDialog(parentFrame, 
                            "Login Gagal. Email atau Password salah.", 
                            "Kesalahan Otentikasi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(parentFrame, 
                        "Kesalahan saat otentikasi: " + ex.getMessage(), 
                        "Kesalahan Jaringan", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}