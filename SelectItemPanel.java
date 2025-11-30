import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
// Pastikan RoundedPanel dan RoundedButton dapat diakses (di package yang sama).

public class SelectItemPanel extends JPanel {

    private AuctionClientGUI parentFrame;
    private JPanel cardsContainer; 
    private JScrollPane scroll;

    // ⭐ KONSTANTA DESAIN BARU
    private final Color PRIMARY_COLOR = new Color(0, 105, 92); // Teal
    private final Color HOVER_COLOR = new Color(0, 80, 70);
    private final Color BG_COLOR = new Color(245, 245, 245);
    private final int CARD_RADIUS = 16;
    private final int BUTTON_RADIUS = 12;

    // Konstruktor awal: pass arrays (boleh kosong)
    public SelectItemPanel(AuctionClientGUI parent, String[] ids, String[] items, String[] prices) {
        this.parentFrame = parent;

        setLayout(new BorderLayout());
        setBackground(BG_COLOR); 

        // Header Title
        JLabel title = new JLabel("BARANG LELANG AKTIF", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(PRIMARY_COLOR);
        title.setBorder(new EmptyBorder(30, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        // ⭐ CARDS CONTAINER: Menggunakan FlowLayout sementara, akan diubah di updateItems
        // saat kartu ada (menjadi GridLayout) atau saat kosong (menjadi GridBagLayout).
        cardsContainer = new JPanel();
        cardsContainer.setLayout(new FlowLayout(FlowLayout.CENTER, 30, 30)); 
        cardsContainer.setBackground(BG_COLOR);
        cardsContainer.setBorder(new EmptyBorder(20, 40, 20, 40));

        scroll = new JScrollPane(cardsContainer);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_COLOR);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0)); 
        
        add(scroll, BorderLayout.CENTER);

        updateItems(ids, items, prices);
    }

    // Dipanggil dari AuctionClientGUI.updateSelectItemPanel(...)
    public void updateItems(String[] ids, String[] items, String[] prices) {
        cardsContainer.removeAll();

        if (ids == null || ids.length == 0) {
            JLabel empty = new JLabel("Tidak ada barang lelang yang tersedia saat ini.", SwingConstants.CENTER);
            empty.setForeground(Color.GRAY);
            empty.setFont(new Font("SansSerif", Font.ITALIC, 18));
            
            // Layout untuk centering tulisan "kosong"
            cardsContainer.setLayout(new GridBagLayout()); 
            cardsContainer.add(empty);
        } else {
            // ⭐ INI YANG PENTING: Gunakan GridLayout(0, 4, ...) untuk wrapping!
            // 0: Baris otomatis; 4: Kolom tetap; 30, 30: Gap horizontal & vertical
            cardsContainer.setLayout(new GridLayout(0, 4, 30, 30)); 
            
            for (int i = 0; i < ids.length; i++) {
                cardsContainer.add(createModernItemCard(ids[i], items[i], prices[i]));
            }
        }

        cardsContainer.revalidate();
        cardsContainer.repaint();
        // Menggeser scroll ke atas setiap kali list diupdate
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    // ⭐ METODE BARU: createModernItemCard
    private JPanel createModernItemCard(String auctionId, String itemName, String price) {
        int CARD_W = 300;
        int CARD_H = 250;

        // Gunakan RoundedPanel
        RoundedPanel card = new RoundedPanel(new BorderLayout(), CARD_RADIUS);
        card.setBackground(Color.WHITE);
        
        Dimension fixed = new Dimension(CARD_W, CARD_H);
        card.setPreferredSize(fixed);
        card.setMaximumSize(fixed);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Content Wrapper (BoxLayout Y_AXIS)
        JPanel contentWrapper = new JPanel();
        contentWrapper.setLayout(new BoxLayout(contentWrapper, BoxLayout.Y_AXIS));
        contentWrapper.setBackground(Color.WHITE);

        // --- ID Barang ---
        JLabel itemHeader = new JLabel("ID: " + auctionId);
        itemHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
        itemHeader.setForeground(new Color(150, 150, 150));
        itemHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // --- Nama Barang ---
        JLabel lblItem = new JLabel(itemName);
        lblItem.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblItem.setForeground(Color.BLACK);
        lblItem.setBorder(new EmptyBorder(4, 0, 15, 0));
        lblItem.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // --- Harga Saat Ini ---
        JLabel priceTitle = new JLabel("Harga Tertinggi Saat Ini:");
        priceTitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        priceTitle.setForeground(new Color(60, 60, 60));
        priceTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblPrice = new JLabel("Rp" + formatHarga(price)); 
        lblPrice.setFont(new Font("SansSerif", Font.BOLD, 28));
        lblPrice.setForeground(new Color(192, 57, 43));
        lblPrice.setBorder(new EmptyBorder(4, 0, 15, 0));
        lblPrice.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // --- Tombol ---
        JButton btnJoin = new RoundedButton("Ikut Lelang!", BUTTON_RADIUS, PRIMARY_COLOR, HOVER_COLOR); 
        btnJoin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnJoin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        btnJoin.addActionListener(e -> parentFrame.startAuctionAfterChoose(auctionId, itemName));
        
        // Susun komponen
        contentWrapper.add(itemHeader);
        contentWrapper.add(lblItem);
        contentWrapper.add(priceTitle);
        contentWrapper.add(lblPrice);
        
        contentWrapper.add(Box.createVerticalGlue()); 
        contentWrapper.add(btnJoin);

        card.add(contentWrapper, BorderLayout.CENTER);
        
        return card;
    }

    // ⭐ METODE BARU: formatHarga
    private String formatHarga(String val) {
        try {
            if (val == null || val.isEmpty()) return " 0";
            long value = Long.parseLong(val);
            return String.format("%,d", value).replace(',', '.');
        } catch (NumberFormatException e) {
            return " " + val;
        }
    }
}