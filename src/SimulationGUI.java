import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SimulationGUI extends JFrame {
    private SimulationPanel simulationPanel;
    private JTextArea infoPanel;
    private final int hqX;
    private final int hqY;
    private JButton startButton;
    private JButton stopButton;
    private JButton resetButton;
    private Simulation simulation;

    public SimulationGUI(int width, int height, int hqX, int hqY, Simulation simulation) {
        setTitle("Fire Spread Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        this.hqX = hqX;
        this.hqY = hqY;
        this.simulation = simulation;
        
        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel controlPanel = new JPanel();
        createControlButtons(controlPanel);
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        
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

    // Créer les boutons de contrôle
    private void createControlButtons(JPanel panel) {
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        resetButton = new JButton("Reset");

        for (JButton button : new JButton[]{startButton, stopButton, resetButton}) {
            button.setPreferredSize(new Dimension(100, 30));
            button.setFont(new Font("Arial", Font.BOLD, 14));
        }

        startButton.setBackground(new Color(50, 205, 50));
        startButton.setForeground(Color.BLACK);
        stopButton.setBackground(new Color(220, 20, 60));
        stopButton.setForeground(Color.BLACK);
        resetButton.setBackground(new Color(30, 144, 255));
        resetButton.setForeground(Color.BLACK);

        startButton.addActionListener(e -> {
            simulation.start();
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            resetButton.setEnabled(false);
        });

        stopButton.addActionListener(e -> {
            simulation.stop();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            resetButton.setEnabled(true);
        });

        resetButton.addActionListener(e -> {
            simulation.reset();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            resetButton.setEnabled(true);
        });

        stopButton.setEnabled(false);
        resetButton.setEnabled(false);

        panel.add(startButton);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(stopButton);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(resetButton);
    }


    // Mettre à jour l'affichage
    public void updateDisplay(double[][] intensityMap, String info, List<Robot> robots, List<Survivor> survivors) {
        simulationPanel.updateState(intensityMap);
        simulationPanel.updateRobots(robots);
        simulationPanel.updateSurvivors(survivors);
        infoPanel.setText(info);
        simulationPanel.repaint();
    }

    private class SimulationPanel extends JPanel {
        private double[][] intensityMap;
        private FireGrid fireGrid;
        private final int width;
        private final int height;
        private final int cellSize = 20;
        private List<Robot> robots = new ArrayList<>();
        private List<Survivor> survivors = new ArrayList<>();

        public SimulationPanel(int width, int height) {
            this.width = width;
            this.height = height;
            setPreferredSize(new Dimension(width * cellSize, height * cellSize));
            setBackground(Color.WHITE);
        }

        public void updateState(double[][] intensityMap) {
            this.intensityMap = intensityMap;
            this.fireGrid = simulation.getFireGrid();
        }

        public void updateRobots(List<Robot> robots) {
            this.robots = robots;
        }

        public void updateSurvivors(List<Survivor> survivors) {
            this.survivors = survivors;
        }

        private Color getFireColor(double intensity) {
            if (intensity <= fireGrid.getIntensityThreshold()) return Color.WHITE;

            // Normaliser l'intensité pour obtenir une valeur entre 0 et 1
            double normalized = (intensity - fireGrid.getIntensityThreshold()) / 
                              (fireGrid.getMaxIntensity() - fireGrid.getIntensityThreshold());
            
            // Définir les couleurs en fonction de l'intensité normalisée
            if (normalized < 0.3) {
                // Jaune à Orange
                return new Color(255, 
                               (int)(255 * (1 - normalized/0.3)), 
                               0, 200);
            } else if (normalized < 0.7) {
                // Orange à Rouge
                normalized = (normalized - 0.3) / 0.4;
                return new Color(255, 
                               (int)(140 * (1 - normalized)), 
                               0, 200);
            } else {
                // Rouge foncé
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

            // Dessiner la grille
            g2d.setColor(new Color(220, 220, 220));
            for (int i = 0; i <= width; i++) {
                g2d.drawLine(i * cellSize, 0, i * cellSize, height * cellSize);
            }
            for (int j = 0; j <= height; j++) {
                g2d.drawLine(0, j * cellSize, width * cellSize, j * cellSize);
            }

            // Dessiner les cases en feu avec des couleurs basées sur l'intensité
            if (intensityMap != null) {
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        if (intensityMap[i][j] > fireGrid.getIntensityThreshold()) {
                            g2d.setColor(getFireColor(intensityMap[i][j]));
                            g2d.fillRect(i * cellSize + 1, j * cellSize + 1, 
                                       cellSize - 2, cellSize - 2);
                        }
                    }
                }
            }

            // Dessiner le quartier général
            g2d.setColor(new Color(0, 150, 0));
            g2d.fillRect(hqX * cellSize, hqY * cellSize, cellSize, cellSize);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            String hqText = "HQ";
            int textX = hqX * cellSize + (cellSize - fm.stringWidth(hqText)) / 2;
            int textY = hqY * cellSize + (cellSize + fm.getAscent()) / 2;
            g2d.drawString(hqText, textX, textY);

            // Dessiner les survivants
            for (Survivor survivor : survivors) {
                if (!survivor.isRescued() && !survivor.isDead()) {
                    int x = survivor.getX() * cellSize;
                    int y = survivor.getY() * cellSize;
                    
                    g2d.setColor(Color.MAGENTA);
                    int survivorSize = cellSize/2;
                    int xOffset = (cellSize - survivorSize)/2;
                    int yOffset = (cellSize - survivorSize)/2;
                    g2d.fillOval(x + xOffset, y + yOffset, survivorSize, survivorSize);
                    
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.BOLD, 10));
                    String survivorNum = String.valueOf(survivor.getId() + 1);
                    int textX_survivor = x + cellSize/2 - fm.stringWidth(survivorNum)/2;
                    int textY_survivor = y + cellSize/2 + fm.getAscent()/2;
                    g2d.drawString(survivorNum, textX_survivor, textY_survivor);
                }
            }

            // Dessiner le robot
            for (Robot robot : robots) {
                Color robotColor;
                String robotStatus = "";
                String statusText = "";
    
                if (robot instanceof Scout) {
                    if (robot.currentState == Robot.State.RECHARGING_ELECTRICITY) {
                        robotColor = Color.GRAY;
                        robotStatus = "Recharging electricity";
                    } else if (robot.currentState == Robot.State.MOVING_TO_HQ) {
                        robotColor = Color.GRAY;
                        robotStatus = "need recharge";
                    } else {
                        robotColor = Color.BLUE;
                        robotStatus = "Scouting";
                    }
                } else if (robot instanceof Firefighter) {
                    if (robot.currentState == Robot.State.RECHARGING_ELECTRICITY && 
                        ((Firefighter)robot).getWaterPercentage() < 100.0) {
                        robotColor = Color.GRAY;
                        robotStatus = "Recharging & Refilling";
                    } else if (robot.currentState == Robot.State.RECHARGING_ELECTRICITY) {
                        robotColor = Color.GRAY;
                        robotStatus = "Recharging electricity";
                    } else if (robot.currentState == Robot.State.RECHARGING_WATER) {
                        robotColor = Color.GRAY;
                        robotStatus = "Refilling water";
                    } else if (robot.isAtHQ()) {
                        robotColor = Color.LIGHT_GRAY;
                        robotStatus = "waiting...";
                    } else if (robot.currentState == Robot.State.MOVING_TO_FIRE) {
                        robotColor = Color.PINK;
                        robotStatus = "Moving to fire";
                    } else if (robot.currentState == Robot.State.EXTINGUISHING) {
                        robotColor = Color.RED;
                        robotStatus = "Extinguishing fire";
                    } else if (robot.currentState == Robot.State.MOVING_TO_HQ) {
                        robotColor = Color.GRAY;
                        robotStatus = "Returning to HQ";
                    } else {
                        robotColor = Color.GRAY;
                    }
                    statusText = String.format("FF: %s", robot.getStatusDescription());
                } else {
                    robotColor = Color.GRAY;
                }
    
                int robotSize = cellSize - 4;
                int x = robot.getX() * cellSize + (cellSize - robotSize) / 2;
                int y = robot.getY() * cellSize + (cellSize - robotSize) / 2;
    
                g2d.setColor(robotColor);
                g2d.fillOval(x, y, robotSize, robotSize);

                int barWidth = robotSize;
                int barHeight = 3;
                int barY = y + robotSize + 2;

                g2d.setColor(Color.GRAY);
                g2d.fillRect(x, barY, barWidth, barHeight);

                g2d.setColor(Color.GREEN);
                int energyWidth = (int)(barWidth * robot.getEnergyPercentage() / 100.0);
                g2d.fillRect(x, barY, energyWidth, barHeight);

                if (robot instanceof Firefighter) {
                    Firefighter ff = (Firefighter) robot;
                    g2d.setColor(Color.GRAY);
                    g2d.fillRect(x, barY + barHeight + 1, barWidth, barHeight);

                    g2d.setColor(Color.BLUE);
                    int waterWidth = (int)(barWidth * ff.getWaterPercentage() / 100.0);
                    g2d.fillRect(x, barY + barHeight + 1, waterWidth, barHeight);
                }
        
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                String robotNum = String.valueOf(robot.getId() + 1);
                int textX_robot = x + (robotSize - fm.stringWidth(robotNum)) / 2;
                int textY_robot = y + (robotSize + fm.getAscent()) / 2;
                g2d.drawString(robotNum, textX_robot, textY_robot);
    
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.BOLD, 8));
                int statusX = x;
                int baseStatusY = y + robotSize + 12;
                g2d.drawString(robotStatus, statusX, baseStatusY + 10);

                if (robot instanceof Firefighter) {
                    g2d.drawString(statusText, statusX, baseStatusY + 20);
                }
            }
        }
    }
}
