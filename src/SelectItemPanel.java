import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// panel buat nampilin daftar barang lelang dalam bentuk card
public class SelectItemPanel extends JPanel {

    // referensi ke frame utama, dipake buat pindah ke halaman lelang
    private AuctionClientGUI parentFrame;

    // container utama buat nyimpen semua card barang
    private JPanel cardsContainer; 

    // scroll biar list barang bisa digeser
    private JScrollPane scroll;

    // warna utama aplikasi
    private final Color PRIMARY_COLOR = new Color(0, 105, 92); 

    // warna button pas di-hover
    private final Color HOVER_COLOR = new Color(0, 80, 70);

    // warna background utama panel
    private final Color BG_COLOR = new Color(245, 245, 245);

    // radius sudut card
    private final int CARD_RADIUS = 16;

    // radius sudut tombol
    private final int BUTTON_RADIUS = 12;

    // constructor panel: nerima data barang dan langsung ditampilin
    public SelectItemPanel(AuctionClientGUI parent, String[] ids, String[] items, String[] prices) {
        this.parentFrame = parent;

        setLayout(new BorderLayout());
        setBackground(BG_COLOR); 

        // judul di bagian atas halaman
        JLabel title = new JLabel("BARANG LELANG AKTIF", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(PRIMARY_COLOR);
        title.setBorder(new EmptyBorder(30, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        // panel buat nampung semua card barang
        cardsContainer = new JPanel();
        cardsContainer.setLayout(new FlowLayout(FlowLayout.CENTER, 30, 30)); 
        cardsContainer.setBackground(BG_COLOR);
        cardsContainer.setBorder(new EmptyBorder(20, 40, 20, 40));

        // scroll pane biar kontennya bisa discroll
        scroll = new JScrollPane(cardsContainer);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_COLOR);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0)); 
        
        add(scroll, BorderLayout.CENTER);

        // langsung render data barang pas panel dibuat
        updateItems(ids, items, prices);
    }

    // method buat refresh isi card berdasarkan data terbaru
    public void updateItems(String[] ids, String[] items, String[] prices) {
        // bersihin card lama dulu
        cardsContainer.removeAll();

        // kondisi kalau data barang kosong
        if (ids == null || ids.length == 0) {
            JLabel empty = new JLabel("Tidak ada barang lelang yang tersedia saat ini.", SwingConstants.CENTER);
            empty.setForeground(Color.GRAY);
            empty.setFont(new Font("SansSerif", Font.ITALIC, 18));
            
            // layout tengah biar teksnya pas di tengah
            cardsContainer.setLayout(new GridBagLayout()); 
            cardsContainer.add(empty);
        } else {
            // layout grid biar card rapi dan sejajar
            cardsContainer.setLayout(new GridLayout(0, 4, 30, 30)); 
            
            // loop semua barang dan bikin card satu-satu
            for (int i = 0; i < ids.length; i++) {
                cardsContainer.add(createModernItemCard(ids[i], items[i], prices[i]));
            }
        }

        // refresh tampilan
        cardsContainer.revalidate();
        cardsContainer.repaint();

        // scroll balik ke atas tiap data di-update
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    // method buat bikin satu card barang lelang
    private JPanel createModernItemCard(String auctionId, String itemName, String price) {
        int CARD_W = 300;
        int CARD_H = 250;

        // card utama dengan sudut membulat
        RoundedPanel card = new RoundedPanel(new BorderLayout(), CARD_RADIUS);
        card.setBackground(Color.WHITE);
        
        // ukuran card dibikin fix biar konsisten
        Dimension fixed = new Dimension(CARD_W, CARD_H);
        card.setPreferredSize(fixed);
        card.setMaximumSize(fixed);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        // wrapper konten card (teks + tombol)
        JPanel contentWrapper = new JPanel();
        contentWrapper.setLayout(new BoxLayout(contentWrapper, BoxLayout.Y_AXIS));
        contentWrapper.setBackground(Color.WHITE);

        // header kecil buat id lelang
        JLabel itemHeader = new JLabel("ID: " + auctionId);
        itemHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
        itemHeader.setForeground(new Color(150, 150, 150));
        itemHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // nama barang
        JLabel lblItem = new JLabel(itemName);
        lblItem.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblItem.setForeground(Color.BLACK);
        lblItem.setBorder(new EmptyBorder(4, 0, 15, 0));
        lblItem.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // label keterangan harga
        JLabel priceTitle = new JLabel("Harga Tertinggi Saat Ini:");
        priceTitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        priceTitle.setForeground(new Color(60, 60, 60));
        priceTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        // teks harga utama
        JLabel lblPrice = new JLabel("Rp" + formatHarga(price)); 
        lblPrice.setFont(new Font("SansSerif", Font.BOLD, 28));
        lblPrice.setForeground(new Color(192, 57, 43));
        lblPrice.setBorder(new EmptyBorder(4, 0, 15, 0));
        lblPrice.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // tombol buat masuk ke sesi lelang
        JButton btnJoin = new RoundedButton("Ikut Lelang!", BUTTON_RADIUS, PRIMARY_COLOR, HOVER_COLOR); 
        btnJoin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnJoin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        // pas tombol diklik, pindah ke halaman lelang
        btnJoin.addActionListener(e -> parentFrame.startAuctionAfterChoose(auctionId, itemName));
        
        // susun semua komponen ke dalam card
        contentWrapper.add(itemHeader);
        contentWrapper.add(lblItem);
        contentWrapper.add(priceTitle);
        contentWrapper.add(lblPrice);
        
        // spacer biar tombol nempel di bawah
        contentWrapper.add(Box.createVerticalGlue()); 
        contentWrapper.add(btnJoin);

        card.add(contentWrapper, BorderLayout.CENTER);
        
        return card;
    }

    // method buat format harga biar ada titik ribuan
    private String formatHarga(String val) {
        try {
            if (val == null || val.isEmpty()) return " 0";
            long value = Long.parseLong(val);
            return String.format("%,d", value).replace(',', '.');
        } catch (NumberFormatException e) {
            // fallback kalau data harganya aneh
            return " " + val;
        }
    }
}
