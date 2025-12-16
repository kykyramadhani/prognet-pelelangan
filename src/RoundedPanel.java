import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

public class RoundedPanel extends JPanel {

    private int cornerRadius = 20;

    // constructor utama: bisa atur layout + radius sudut
    public RoundedPanel(LayoutManager layout, int radius) {
        super(layout);
        this.cornerRadius = radius;
        setOpaque(false); // biar background default panel ga nutupin rounded effect
    }

    public RoundedPanel(int radius) {
        this(new FlowLayout(), radius);
    }

    // constructor default: flowlayout + radius 20
    public RoundedPanel() {
        this(new FlowLayout(), 20);
    }


    // di sini kita custom cara gambar panelnya
    @Override
    protected void paintComponent(Graphics g) {
  
        Graphics2D g2 = (Graphics2D) g.create();

        // aktifin anti-aliasing biar pinggirannya halus
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

  
        g2.setColor(getBackground());

        // gambar rectangle dengan sudut membulat
        g2.fill(new RoundRectangle2D.Float(
                0, 0,
                getWidth(), getHeight(),
                cornerRadius, cornerRadius
        ));

        // buang object graphics biar ga boros resource
        g2.dispose();

     
        super.paintComponent(g);
    }
}
