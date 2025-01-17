import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

public class Simulation {

    private static final int FIRE_UPDATE_INTERVAL = 2000;
    private static final int ROBOT_UPDATE_INTERVAL = 300;
    private static final int GRID_WIDTH = 30;
    private static final int GRID_HEIGHT = 30;
    private static final int HQ_X = 15;
    private static final int HQ_Y = 15;

    private static final int INITIAL_SCOUTS = 2;

    private Fire fire;
    private HeadQuarters hq;
    private SimulationGUI gui;
    private boolean isRunning;
    private ScheduledExecutorService fireExecutor;
    private ScheduledExecutorService robotExecutor;
    private int timeStep = 0;

    private List<Robot> robots;

    public Simulation() {
        this.fire = new Fire(GRID_WIDTH, GRID_HEIGHT);
        this.hq = new HeadQuarters(HQ_X, HQ_Y, GRID_WIDTH, GRID_HEIGHT, fire);
        this.gui = new SimulationGUI(GRID_WIDTH, GRID_HEIGHT, HQ_X, HQ_Y);
        this.isRunning = false;
        this.fireExecutor = Executors.newScheduledThreadPool(1);
        this.robotExecutor = Executors.newScheduledThreadPool(1);
        this.robots = new CopyOnWriteArrayList<>();
        initializeRobots();
    }

    private void initializeRobots() {
        int id = 0;
        // Add initial scouts
        for (int i = 0; i < INITIAL_SCOUTS; i++) {
            robots.add(new Scout(id++, HQ_X, HQ_Y, GRID_WIDTH, GRID_HEIGHT));
        }
    }

    // Mettre à jour l'interface graphique
    private void updateGUI() {
        SwingUtilities.invokeLater(() -> {
            gui.updateDisplay(fire.getIntensityMap(), generateSimulationInfo(), robots);
        });
    }

    // Mettre à jour les robots
    private void updateRobots() {
        double[][] intensityMap = fire.getIntensityMap();

        Firefighter newRobot = hq.checkAndAddFirefighter();
        if (newRobot != null) {
            newRobot.setFireGrid(fire.getFireGrid());
            robots.add(newRobot);
        }
        
        for (Robot robot : robots) {
            if (robot instanceof Scout) {
                ((Scout)robot).updateState(hq, intensityMap);
            } else if (robot instanceof Firefighter) {
                ((Firefighter)robot).updateState(hq, intensityMap);
            }
        }
        
        hq.updateGlobalMap(intensityMap);
    }

    // Démarrer la simulation
    public void start() {
        isRunning = true;

        // Scheduler les mises à jour de feu à un rythme régulier
        fireExecutor.scheduleAtFixedRate(() -> {
            if (isRunning) {
                fire.spread();
                timeStep++;
                updateGUI();
            }
        }, 0, FIRE_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);

        // Scheduler les mises à jour des robots à un rythme régulier
        robotExecutor.scheduleAtFixedRate(() -> {
            if (isRunning) {
                updateRobots();
                updateGUI();
            }
        }, 0, ROBOT_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    // Arrêter la simulation
    public void stop() {
        isRunning = false;
        fireExecutor.shutdown();
        robotExecutor.shutdown();
        try {
            fireExecutor.awaitTermination(1, TimeUnit.SECONDS);
            robotExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String generateSimulationInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== Simulation Status ===\n");
        info.append("Time Step: ").append(timeStep).append("\n");
        info.append("Grid Size: ").append(GRID_WIDTH).append("x").append(GRID_HEIGHT).append("\n");
        info.append("HQ Position: [").append(HQ_X).append(",").append(HQ_Y).append("]\n\n");
        
        info.append("=== Robots Status ===\n");
        long scoutCount = robots.stream().filter(r -> r instanceof Scout).count();
        long firefighterCount = robots.stream().filter(r -> r instanceof Firefighter).count();
        info.append("Scouts: ").append(scoutCount).append("\n");
        info.append("Firefighters: ").append(firefighterCount).append("\n\n");
        
        for (Robot robot : robots) {
            info.append(String.format("%s %d: [%d,%d]\n",
                robot instanceof Scout ? "Scout" : "Firefighter",
                robot.getId() + 1, robot.getX(), robot.getY()));
        }
        info.append("\n");

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

    public static void main(String[] args) {
        Simulation simulation = new Simulation();
        simulation.start();
    }
}