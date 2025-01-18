import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

public class Simulation {

    private static final int FIRE_UPDATE_INTERVAL = 2000;
    private static final int ROBOT_UPDATE_INTERVAL = 300;
    private static final double SURVIVOR_SPAWN_PROBABILITY = 0.3;
    private static final int MAX_SURVIVORS = 5;

    private Fire fire;
    private HeadQuarters hq;
    private SimulationGUI gui;
    private boolean isRunning;
    private ScheduledExecutorService fireExecutor;
    private ScheduledExecutorService robotExecutor;
    private int timeStep = 0;

    private List<Robot> robots;
    private List<Survivor> survivors;

    public Simulation() {
        initializeSimulation();
    }

    private void initializeSimulation() {
        this.fire = new Fire(HeadQuarters.getGridWidth(), HeadQuarters.getGridHeight());
        this.hq = new HeadQuarters(HeadQuarters.getHqX(), HeadQuarters.getHqY(), 
                                 HeadQuarters.getGridWidth(), HeadQuarters.getGridHeight());
        this.isRunning = false;
        this.timeStep = 0;
        this.robots = new CopyOnWriteArrayList<>();
        this.survivors = new CopyOnWriteArrayList<>();
        initializeRobots();
        spawnSurvivor();
    }

    private void initializeRobots() {
        int id = 0;
        // Add initial scouts
        for (int i = 0; i < HeadQuarters.INITIAL_SCOUTS; i++) {
            robots.add(new Scout(id++, HeadQuarters.getHqX(), HeadQuarters.getHqY(), 
                               HeadQuarters.getGridWidth(), HeadQuarters.getGridHeight()));
        }
    }

    // Ajouter les survivants
    private void spawnSurvivor() {
        int id = 0;
        for (int i = 0; i < MAX_SURVIVORS; i++) {
            int x = (int)(Math.random() * HeadQuarters.getGridWidth());
            int y = (int)(Math.random() * HeadQuarters.getGridHeight());
            survivors.add(new Survivor(id++, x, y));
        }

    }

    // Mettre à jour l'interface graphique
    private void updateGUI() {
        SwingUtilities.invokeLater(() -> {
            gui.updateDisplay(fire.getIntensityMap(), generateSimulationInfo(), robots, survivors);
        });
    }

    // Mettre à jour les robots
    private void updateRobots() {
        Firefighter newRobot = hq.checkAndAddFirefighter();
        if (newRobot != null) {
            newRobot.setFireGrid(fire.getFireGrid());
            robots.add(newRobot);
        }
        
        for (Robot robot : robots) {
            if (robot instanceof Scout) {
                if (((Scout)robot).getFireGrid() == null) {
                    ((Scout)robot).setFireGrid(fire.getFireGrid());
                }
                ((Scout)robot).updateState(hq);
            } else if (robot instanceof Firefighter) {
                ((Firefighter)robot).updateState(hq);
            }
        }
    }

    public void createGUI() {
        this.gui = new SimulationGUI(HeadQuarters.getGridWidth(), HeadQuarters.getGridHeight(), 
                                   HeadQuarters.getHqX(), HeadQuarters.getHqY(), this);
    }

    public void reset() {
        stop();
        initializeSimulation();
        updateGUI();
    }

    // Démarrer la simulation
    public void start() {
        if (isRunning) return;

        isRunning = true;
        this.fireExecutor = Executors.newScheduledThreadPool(1);
        this.robotExecutor = Executors.newScheduledThreadPool(1);

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
        if (fireExecutor != null) {
            fireExecutor.shutdown();
        }
        if (robotExecutor != null) {
            robotExecutor.shutdown();
        }
        try {
            if (fireExecutor != null) {
                fireExecutor.awaitTermination(1, TimeUnit.SECONDS);
            }
            if (robotExecutor != null) {
                robotExecutor.awaitTermination(1, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String generateSimulationInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== Simulation Status ===\n");
        info.append("Time Step: ").append(timeStep).append("\n");
        info.append("Grid Size: ").append(HeadQuarters.GRID_WIDTH).append("x").append(HeadQuarters.GRID_HEIGHT).append("\n");
        info.append("HQ Position: [").append(HeadQuarters.HQ_X).append(",").append(HeadQuarters.HQ_Y).append("]\n\n");
        
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

        info.append("=== Survivors Status ===\n");
        long activeCount = survivors.stream().filter(s -> !s.isRescued()).count();
        long rescuedCount = survivors.stream().filter(Survivor::isRescued).count();
        info.append("Active Survivors: ").append(activeCount).append("\n");
        info.append("Rescued Survivors: ").append(rescuedCount).append("\n\n");

        double[][] intensityMap = fire.getIntensityMap();
        int fireCount = 0;
        for (int i = 0; i < HeadQuarters.GRID_WIDTH; i++) {
            for (int j = 0; j < HeadQuarters.GRID_HEIGHT; j++) {
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
        simulation.createGUI();
    }
}