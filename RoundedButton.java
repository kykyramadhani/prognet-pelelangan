import java.awt.*;
import javax.swing.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// File: RoundedButton.java
class RoundedButton extends JButton {
    private final int arc;
    private Color defaultBg;
    private final Color hoverBg;

    public RoundedButton(String text, int arc, Color bgColor, Color hoverColor) {
        super(text);
        this.arc = arc;
        this.defaultBg = bgColor;
        this.hoverBg = hoverColor;
        
        setBackground(defaultBg);
        setForeground(Color.WHITE);
        setFont(new Font("SansSerif", Font.BOLD, 14));

        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { setBackground(hoverBg); }
            @Override
            public void mouseExited(MouseEvent e) { setBackground(defaultBg); }
        });
    }

    @Override
    public void setBackground(Color bg) {
        if (defaultBg != null) {
             this.defaultBg = bg;
        }
        super.setBackground(bg);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

        FontMetrics fm = g2.getFontMetrics();
        int textX = (getWidth() - fm.stringWidth(getText())) / 2;
        int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(getForeground());
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }
}