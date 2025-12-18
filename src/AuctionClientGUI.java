import java.awt.*;
import java.io.IOException;
import javax.swing.*;

public class AuctionClientGUI extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;

    public static final String LOGIN_VIEW = "Login";
    public static final String SELECT_VIEW = "SelectItem";
    public static final String AUCTION_VIEW = "Auction";

    private SelectItemPanel selectItemPanel; 
    private AuctionPanel auctionPanel;
    private LoginPanel loginPanel;

    public AuctionClientGUI() {
        super("Multi-Client Lelang Interaktif - Card UI");

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        loginPanel = new LoginPanel(this);
        auctionPanel = new AuctionPanel(this); 

        selectItemPanel = new SelectItemPanel(this, new String[0], new String[0], new String[0]);

        mainPanel.add(loginPanel, LOGIN_VIEW);
        mainPanel.add(selectItemPanel, SELECT_VIEW);
        mainPanel.add(auctionPanel, AUCTION_VIEW);

        add(mainPanel);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        showView(LOGIN_VIEW);
        setVisible(true);
    }

    public void showView(String viewName) {
        cardLayout.show(mainPanel, viewName);
    }

    public void startConnectionForSelection(String ip, int port, String username, String idToken) {
    // skrg idtoken isinya username, aman kok
        try {
            // auctionpanel tetep nerima 4 param ya
            auctionPanel.connectAndStartListener(ip, port, username, idToken);
            showView(SELECT_VIEW);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "gagal coonect ke server: " + e.getMessage(),
                    "kesalahan koneksi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void updateSelectItemPanel(String auctionsMessage) {
        if (!auctionsMessage.startsWith("AUCTIONS:")) return;
        String payload = auctionsMessage.substring("AUCTIONS:".length());
        if (payload.trim().isEmpty()) {
            selectItemPanel.updateItems(new String[0], new String[0], new String[0]);
            return;
        }

        String[] entries = payload.split("\\|");
        String[] ids = new String[entries.length];
        String[] names = new String[entries.length];
        String[] prices = new String[entries.length];

        for (int i = 0; i < entries.length; i++) {
            String[] parts = entries[i].split(",", 3);
            ids[i] = (parts.length > 0) ? parts[0] : "ID?";
            names[i] = (parts.length > 1) ? parts[1] : "Nama?";
            prices[i] = (parts.length > 2) ? parts[2] : "0";
        }

        selectItemPanel.updateItems(ids, names, prices);
        showView(SELECT_VIEW);
    }

    public void startAuctionAfterChoose(String auctionId, String itemName) {
        auctionPanel.sendSelectAuction(auctionId);
        auctionPanel.setSelectedItem(itemName, auctionId);
        showView(AUCTION_VIEW);
    }

    public void backToLoginView() {
        showView(LOGIN_VIEW);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) { /* biarin aja */ }
        SwingUtilities.invokeLater(AuctionClientGUI::new);
    }
}