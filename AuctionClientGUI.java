import java.awt.*;
import javax.swing.*;
import java.io.IOException;

public class AuctionClientGUI extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;

    public static final String LOGIN_VIEW = "Login";
    public static final String SELECT_VIEW = "SelectItem";
    public static final String AUCTION_VIEW = "Auction";

    private SelectItemPanel selectItemPanel; // dibuat/diupdate dinamis
    private AuctionPanel auctionPanel;
    private LoginPanel loginPanel;

    // simpan connection state di AuctionPanel (network handled there)
    public AuctionClientGUI() {
        super("Multi-Client Lelang Interaktif - Card UI");

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // gunakan LoginPanel dan AuctionPanel yang sudah ada (atau versi kamu)
        loginPanel = new LoginPanel(this);
        auctionPanel = new AuctionPanel(this);

        // inisialisasi SelectItemPanel sementara kosong (akan diupdate saat server kirim AUCTIONS)
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

    // Dipanggil oleh LoginPanel setelah otentikasi berhasil:
    // koneksikan ke server (auctionPanel menangani koneksi/networking)
    public void startConnectionForSelection(String ip, int port, String username, String idToken) {
        try {
            auctionPanel.connectAndStartListener(ip, port, username, idToken);
            // tunggu server kirim AUCTIONS; tapi langsung tampilkan panel daftar (yang akan di-update)
            showView(SELECT_VIEW);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Gagal terhubung ke server: " + e.getMessage(),
                    "Kesalahan Koneksi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Dipanggil oleh AuctionPanel ketika menerima pesan AUCTIONS:... dari server
    // message berformat "AUCTIONS:ID1,Name,Price|ID2,Name,Price|..."
    public void updateSelectItemPanel(String auctionsMessage) {
        if (!auctionsMessage.startsWith("AUCTIONS:")) return;
        String payload = auctionsMessage.substring("AUCTIONS:".length());
        if (payload.trim().isEmpty()) {
            // tidak ada auction
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
        // tampilkan daftar barang
        showView(SELECT_VIEW);
    }

    // Dipanggil dari SelectItemPanel ketika user klik "Ikut Lelang"
    // auctionId dipakai untuk kirim SELECT:, itemName hanya untuk UI
    public void startAuctionAfterChoose(String auctionId, String itemName) {
        // kirim SELECT ke server melalui auctionPanel
        auctionPanel.sendSelectAuction(auctionId);
        // beri tahu auctionPanel barang yang dipilih (agar tampil di UI)
        auctionPanel.setSelectedItem(itemName, auctionId);
        showView(AUCTION_VIEW);
    }

    // Dibutuhkan AuctionPanel untuk memanggil kembali ke GUI (misal setelah disconnect)
    public void backToLoginView() {
        showView(LOGIN_VIEW);
    }

    public static void main(String[] args) {
        // tema look & feel default system
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) { /* ignore */ }
        SwingUtilities.invokeLater(AuctionClientGUI::new);
    }
}
