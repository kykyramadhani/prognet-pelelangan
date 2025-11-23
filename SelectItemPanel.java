import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class SelectItemPanel extends JPanel {

    private AuctionClientGUI parentFrame;
    private JPanel gridPanel;
    private JScrollPane scroll;

    // Konstruktor awal: pass arrays (boleh kosong)
    public SelectItemPanel(AuctionClientGUI parent, String[] ids, String[] items, String[] prices) {
        this.parentFrame = parent;

        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        JLabel title = new JLabel("DAFTAR BARANG LELANG", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        title.setBorder(new EmptyBorder(20, 10, 20, 10));
        add(title, BorderLayout.NORTH);

        gridPanel = new JPanel(new GridLayout(0, 4, 15, 15));
        gridPanel.setBackground(Color.BLACK);
        gridPanel.setBorder(new EmptyBorder(20, 40, 20, 40));

        scroll = new JScrollPane(gridPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // inisialisasi isi (bisa kosong)
        updateItems(ids, items, prices);
    }

    // Dipanggil dari AuctionClientGUI.updateSelectItemPanel(...)
    public void updateItems(String[] ids, String[] items, String[] prices) {
        gridPanel.removeAll();

        if (ids == null || ids.length == 0) {
            JLabel empty = new JLabel("Tidak ada barang lelang saat ini.", SwingConstants.CENTER);
            empty.setForeground(Color.WHITE);
            empty.setFont(new Font("SansSerif", Font.PLAIN, 16));
            gridPanel.setLayout(new BorderLayout());
            gridPanel.add(empty, BorderLayout.CENTER);
        } else {
            gridPanel.setLayout(new GridLayout(0, 4, 15, 15));
            for (int i = 0; i < ids.length; i++) {
                gridPanel.add(createItemCard(ids[i], items[i], prices[i]));
            }
        }

        revalidate();
        repaint();
    }

    private JPanel createItemCard(String auctionId, String itemName, String price) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                new EmptyBorder(10, 10, 10, 10)));

        JLabel lblItem = new JLabel(itemName, SwingConstants.CENTER);
        lblItem.setFont(new Font("SansSerif", Font.BOLD, 16));

        JLabel lblPrice = new JLabel("Harga Saat Ini: Rp " + price, SwingConstants.CENTER);
        lblPrice.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JButton btnJoin = new JButton("Ikut Lelang");
        btnJoin.setBackground(new Color(0, 105, 92));
        btnJoin.setForeground(Color.WHITE);
        btnJoin.setFocusPainted(false);

        // Ketika dipilih, kirim SELECT ke server dan pindah ke AuctionPanel
        btnJoin.addActionListener(e -> parentFrame.startAuctionAfterChoose(auctionId, itemName));

        card.add(lblItem, BorderLayout.NORTH);
        card.add(lblPrice, BorderLayout.CENTER);
        card.add(btnJoin, BorderLayout.SOUTH);

        return card;
    }
}
