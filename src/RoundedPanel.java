import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

public class RoundedPanel extends JPanel {
    private int cornerRadius = 15; // Jari-jari sudut default

    public RoundedPanel(LayoutManager layout, int radius) {
        super(layout);
        this.cornerRadius = radius;
        setOpaque(false); // Penting: Agar background transparan
    }
    
    // Konstruktor tanpa layout
    public RoundedPanel(int radius) {
        this(new FlowLayout(), radius);
    }
    
    // Default constructor
    public RoundedPanel() {
        this(new FlowLayout(), 15);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Menggambar background panel yang dibulatkan
        g2.setColor(getBackground());
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));
        
        g2.dispose();
        // Penting: Panggil super.paintComponent(g) setelah gambar background
        super.paintComponent(g); 
    }
}