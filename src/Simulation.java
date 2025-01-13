import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

public class Simulation {
    private Fire fire;
    private SimulationGUI gui;
    private boolean isRunning;
    private ScheduledExecutorService executor;
    private static final int GRID_WIDTH = 30;
    private static final int GRID_HEIGHT = 30;
    private static final int HQ_X = 15;
    private static final int HQ_Y = 15;
    private int timeStep = 0;
    private Robot robot;

    public Simulation() {
        this.fire = new Fire(GRID_WIDTH, GRID_HEIGHT);
        this.gui = new SimulationGUI(GRID_WIDTH, GRID_HEIGHT, HQ_X, HQ_Y);
        this.executor = Executors.newScheduledThreadPool(1);
        this.robot = new Robot(HQ_X, HQ_Y);
    }

    public void start() {
        isRunning = true;
        // Scheduler le processus de propagation du feu toutes les 800 ms
        executor.scheduleAtFixedRate(() -> {
            if (isRunning) {
                fire.spread();
                updateRobot();
                timeStep++;
                updateGUI();
            }
        }, 0, 800, TimeUnit.MILLISECONDS);
    }

    private void updateRobot() {
        double[][] intensityMap = fire.getIntensityMap();
        
        if (robot.getState().equals("searching")) {
            int[] nearestFire = findNearestFire(intensityMap, robot.getX(), robot.getY());
            if (nearestFire != null) {
                robot.moveTowards(nearestFire[0], nearestFire[1]);
                if (robot.getX() == nearestFire[0] && robot.getY() == nearestFire[1]) {
                    robot.setState("extinguishing");
                }
            }
        } 
        else if (robot.getState().equals("extinguishing")) {
            if (intensityMap[robot.getX()][robot.getY()] <= FireGrid.INTENSITY_THRESHOLD) {
                robot.setState("searching");
            } else {
                robot.extinguishFire(fire.getFireGrid());
            }
        }
    }

    private int[] findNearestFire(double[][] intensityMap, int startX, int startY) {
        int[] nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (int i = 0; i < GRID_WIDTH; i++) {
            for (int j = 0; j < GRID_HEIGHT; j++) {
                if (intensityMap[i][j] > FireGrid.INTENSITY_THRESHOLD) {
                    double distance = Math.sqrt(Math.pow(i - startX, 2) + Math.pow(j - startY, 2));
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearest = new int[]{i, j};
                    }
                }
            }
        }
        
        return nearest;
    }

    // Générer les informations de simulation
    private String generateSimulationInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== Simulation Status ===\n");
        info.append("Time Step: ").append(timeStep).append("\n");
        info.append("Grid Size: ").append(GRID_WIDTH).append("x").append(GRID_HEIGHT).append("\n");
        info.append("HQ Position: [").append(HQ_X).append(",").append(HQ_Y).append("]\n\n");
        info.append("Robot Position: [").append(robot.getX()).append(",").append(robot.getY()).append("]\n");
        info.append("Robot State: ").append(robot.getState()).append("\n\n");

        // Compter le nombre de feux actifs
        double[][] intensityMap = fire.getIntensityMap();
        int fireCount = 0;
        for (int i = 0; i < GRID_WIDTH; i++) {
            for (int j = 0; j < GRID_HEIGHT; j++) {
                if (intensityMap[i][j] > FireGrid.INTENSITY_THRESHOLD) {
                    fireCount++;
                }
            }
        }
        info.append("Active Fires: ").append(fireCount).append("\n");
        info.append("Total Time Steps: ").append(timeStep);

        return info.toString();
    }

    public void stop() {
        isRunning = false;
        executor.shutdown();
    }

    private void updateGUI() {
        SwingUtilities.invokeLater(() -> {
            gui.updateDisplay(fire.getIntensityMap(), generateSimulationInfo(), robot);
        });
    }

    public static void main(String[] args) {
        Simulation simulation = new Simulation();
        simulation.start();
    }
}