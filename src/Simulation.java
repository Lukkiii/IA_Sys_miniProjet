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

    public Simulation() {
        this.fire = new Fire(GRID_WIDTH, GRID_HEIGHT);
        this.gui = new SimulationGUI(GRID_WIDTH, GRID_HEIGHT, HQ_X, HQ_Y);
        this.executor = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        isRunning = true;
        executor.scheduleAtFixedRate(() -> {
            if (isRunning) {
                fire.spread();
                timeStep++;
                updateGUI();
            }
        }, 0, 800, TimeUnit.MILLISECONDS);
    }

    private String generateSimulationInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== Simulation Status ===\n");
        info.append("Time Step: ").append(timeStep).append("\n");
        info.append("Grid Size: ").append(GRID_WIDTH).append("x").append(GRID_HEIGHT).append("\n");
        info.append("HQ Position: [").append(HQ_X).append(",").append(HQ_Y).append("]\n\n");

        // Count active fires
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
            gui.updateDisplay(fire.getIntensityMap(), generateSimulationInfo());
        });
    }

    public static void main(String[] args) {
        Simulation simulation = new Simulation();
        simulation.start();
    }
}