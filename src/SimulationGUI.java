import javax.swing.*;
import java.awt.*;

public class SimulationGUI extends JFrame {
    private SimulationPanel simulationPanel;
    private JTextArea infoPanel;
    private final int hqX;
    private final int hqY;

    public SimulationGUI(int width, int height, int hqX, int hqY) {
        setTitle("Fire Spread Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        this.hqX = hqX;
        this.hqY = hqY;
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        simulationPanel = new SimulationPanel(width, height);
        mainPanel.add(simulationPanel, BorderLayout.CENTER);
        
        infoPanel = new JTextArea();
        infoPanel.setEditable(false);
        infoPanel.setPreferredSize(new Dimension(250, 600));
        infoPanel.setFont(new Font("Monospace", Font.PLAIN, 14));
        infoPanel.setBackground(new Color(240, 240, 240));
        infoPanel.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(infoPanel);
        mainPanel.add(scrollPane, BorderLayout.EAST);
        
        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void updateDisplay(double[][] intensityMap, String info) {
        simulationPanel.updateState(intensityMap);
        infoPanel.setText(info);
        simulationPanel.repaint();
    }

    private class SimulationPanel extends JPanel {
        private double[][] intensityMap;
        private final int width;
        private final int height;
        private final int cellSize = 20;

        public SimulationPanel(int width, int height) {
            this.width = width;
            this.height = height;
            setPreferredSize(new Dimension(width * cellSize, height * cellSize));
            setBackground(Color.WHITE);
        }

        public void updateState(double[][] intensityMap) {
            this.intensityMap = intensityMap;
        }

        private Color getFireColor(double intensity) {
            if (intensity <= FireGrid.INTENSITY_THRESHOLD) return Color.WHITE;

            // Normalize intensity to 0-1 range
            double normalized = (intensity - FireGrid.INTENSITY_THRESHOLD) / 
                              (FireGrid.MAX_INTENSITY - FireGrid.INTENSITY_THRESHOLD);
            
            // Create color gradient from yellow to orange to red
            if (normalized < 0.3) {
                // Yellow to Orange
                return new Color(255, 
                               (int)(255 * (1 - normalized/0.3)), 
                               0, 200);
            } else if (normalized < 0.7) {
                // Orange to Red
                normalized = (normalized - 0.3) / 0.4;
                return new Color(255, 
                               (int)(140 * (1 - normalized)), 
                               0, 200);
            } else {
                // Deep Red
                normalized = (normalized - 0.7) / 0.3;
                return new Color(255 - (int)(55 * normalized),
                               0,
                               0, 200);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw grid
            g2d.setColor(new Color(220, 220, 220));
            for (int i = 0; i <= width; i++) {
                g2d.drawLine(i * cellSize, 0, i * cellSize, height * cellSize);
            }
            for (int j = 0; j <= height; j++) {
                g2d.drawLine(0, j * cellSize, width * cellSize, j * cellSize);
            }

            // Draw fires with intensity-based colors
            if (intensityMap != null) {
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        if (intensityMap[i][j] > FireGrid.INTENSITY_THRESHOLD) {
                            g2d.setColor(getFireColor(intensityMap[i][j]));
                            g2d.fillRect(i * cellSize + 1, j * cellSize + 1, 
                                       cellSize - 2, cellSize - 2);
                        }
                    }
                }
            }

            // Draw HQ
            g2d.setColor(new Color(0, 150, 0));
            g2d.fillRect(hqX * cellSize, hqY * cellSize, cellSize, cellSize);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            String hqText = "HQ";
            int textX = hqX * cellSize + (cellSize - fm.stringWidth(hqText)) / 2;
            int textY = hqY * cellSize + (cellSize + fm.getAscent()) / 2;
            g2d.drawString(hqText, textX, textY);
        }
    }
}