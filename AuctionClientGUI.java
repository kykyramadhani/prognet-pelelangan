import java.awt.*;
import java.io.IOException;
import javax.swing.*;

public class AuctionClientGUI extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;

    public static final String LOGIN_VIEW = "Login";
    public static final String AUCTION_VIEW = "Auction";

    public AuctionClientGUI() {
        super("Multi-Client Lelang Interaktif - Card UI");
        
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Inisialisasi Panel
        LoginPanel loginPanel = new LoginPanel(this);
        AuctionPanel auctionPanel = new AuctionPanel(this);

        mainPanel.add(loginPanel, LOGIN_VIEW);
        mainPanel.add(auctionPanel, AUCTION_VIEW);

        add(mainPanel);

        // Pengaturan Frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null); 
        showView(LOGIN_VIEW);
        setVisible(true);
    }

    public void showView(String viewName) {
        cardLayout.show(mainPanel, viewName);
    }

    public void startAuction(String ip, int port, String username, String idToken) {
        AuctionPanel auctionPanel = (AuctionPanel) mainPanel.getComponent(1);
        try {
            auctionPanel.connectAndStartListener(ip, port, username, idToken);
            showView(AUCTION_VIEW);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Gagal terhubung ke server: " + e.getMessage(), 
                "Kesalahan Koneksi", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void closeAuction(String message) {
        JOptionPane.showMessageDialog(this, message, 
            "Lelang Ditutup", JOptionPane.INFORMATION_MESSAGE);
        showView(LOGIN_VIEW); 
    }

    public static void main(String[] args) {
        // Tema Material/Modern Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(AuctionClientGUI::new);
    }
}