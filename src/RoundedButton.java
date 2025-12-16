import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

class RoundedButton extends JButton {
    private final int arc; // buat ngatur seberapa bulet sudut tombolnya
    private Color defaultBg; // warna default button
    private final Color hoverBg; // warna pas mouse hover

    public RoundedButton(String text, int arc, Color bgColor, Color hoverColor) {
        super(text);
        this.arc = arc;
        this.defaultBg = bgColor;
        this.hoverBg = hoverColor;
        
        setBackground(defaultBg);
        setForeground(Color.WHITE);
        setFont(new Font("SansSerif", Font.BOLD, 14));

        // biar kita gambar tombolnya sendiri
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // ngatur efek hover mouse
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { 
                // pas mouse masuk, ganti warna
                setBackground(hoverBg); 
            }
            @Override
            public void mouseExited(MouseEvent e) { 
                // pas mouse keluar, balikin ke warna awal
                setBackground(defaultBg); 
            }
        });
    }

    @Override
    public void setBackground(Color bg) {
        // jaga warna default biar hover tetep jalan normal
        if (defaultBg != null && !bg.equals(defaultBg)) {
            this.defaultBg = bg;
        }
        super.setBackground(bg);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        // method utama buat gambar tombol rounded
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // gambar background rounded
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

        // posisi text biar pas di tengah
        FontMetrics fm = g2.getFontMetrics();
        int textX = (getWidth() - fm.stringWidth(getText())) / 2;
        int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(getForeground());
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }
}
