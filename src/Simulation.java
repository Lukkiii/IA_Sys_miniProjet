import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

public class Simulation {

    // Temps de mise à jour du feu
    private static final int FIRE_UPDATE_INTERVAL = 2000;
    // Temps de mise à jour des robots
    private static final int ROBOT_UPDATE_INTERVAL = 300;
    // Nombre maximum de survivants
    private static final int MAX_SURVIVORS = 7;

    private Fire fire;
    private FireGrid fireGrid;
    private HeadQuarters hq;
    private SimulationGUI gui;
    private boolean isRunning;
    private ScheduledExecutorService fireExecutor;
    private ScheduledExecutorService robotExecutor;
    private int timeStep = 0;
    private FireStatistics statistics;

    private List<Robot> robots;
    private List<Survivor> survivors;

    public Simulation() {
        initializeSimulation();
    }

    private void initializeSimulation() {
        FireScenario.Parameters scenario = FireScenario.CHEMICAL;
        this.fireGrid = new FireGrid(HeadQuarters.getGridWidth(), 
                                   HeadQuarters.getGridHeight(), 
                                   scenario);
        this.fire = new Fire(fireGrid);
        this.hq = new HeadQuarters(HeadQuarters.getHqX(), HeadQuarters.getHqY(), 
                                 HeadQuarters.getGridWidth(), HeadQuarters.getGridHeight(), fireGrid);
        this.isRunning = false;
        this.timeStep = 0;
        this.robots = new CopyOnWriteArrayList<>();
        this.survivors = new CopyOnWriteArrayList<>();
        this.statistics = new FireStatistics();
        initializeRobots();       
    }

    // Initialiser les robots
    private void initializeRobots() {
        int id = 0;
        for (int i = 0; i < HeadQuarters.INITIAL_SCOUTS; i++) {
            robots.add(new Scout(id++, HeadQuarters.getHqX(), HeadQuarters.getHqY(), 
                               HeadQuarters.getGridWidth(), HeadQuarters.getGridHeight()));
        }
    }

    // Ajouter les survivants
    private void spawnSurvivor() {
        int id = 0;
        double[][] intensityMap = fire.getIntensityMap();
        List<Point> allFireLocations = new ArrayList<>();
        
        for (int i = 0; i < HeadQuarters.getGridWidth(); i++) {
            for (int j = 0; j < HeadQuarters.getGridHeight(); j++) {
                if (intensityMap[i][j] > fireGrid.getIntensityThreshold()) {
                    allFireLocations.add(new Point(i, j));
                }
            }
        }
    
        if (!allFireLocations.isEmpty()) {
            List<Point> selectedFireLocations = new ArrayList<>(allFireLocations);
            for (int i = 0; i < MAX_SURVIVORS && !selectedFireLocations.isEmpty(); i++) {
                int index = (int)(Math.random() * selectedFireLocations.size());
                Point p = selectedFireLocations.get(index);
                
                List<Point> nearbyFirePoints = new ArrayList<>();
                for (Point firePoint : allFireLocations) {
                    if (Math.abs(firePoint.x - p.x) <= 2 && Math.abs(firePoint.y - p.y) <= 2) {
                        nearbyFirePoints.add(firePoint);
                    }
                }
                
                int survivorsInThisFire = 1 + (int)(Math.random() * 3);
                for (int j = 0; j < survivorsInThisFire && id < MAX_SURVIVORS && !nearbyFirePoints.isEmpty(); j++) {
                    int firePointIndex = (int)(Math.random() * nearbyFirePoints.size());
                    Point survivorPoint = nearbyFirePoints.get(firePointIndex);
                    survivors.add(new Survivor(id++, survivorPoint.x, survivorPoint.y));
                    nearbyFirePoints.remove(firePointIndex);
                }
                
                selectedFireLocations.remove(index);
            }
        }
    }

    // Mettre à jour l'interface graphique
    private void updateGUI() {
        statistics.updateStatistics(
            fire.getIntensityMap(),
            survivors,
            fireGrid.getIntensityThreshold()
        );

        SwingUtilities.invokeLater(() -> {
            gui.updateDisplay(fire.getIntensityMap(), generateSimulationInfo(), robots, survivors);
        });
    }

    // Mettre à jour les survivants
    private void updateSurvivors() {
        double[][] intensityMap = fire.getIntensityMap();
        for (Survivor survivor : survivors) {
            if (!survivor.isRescued() && !survivor.isDead()) {
                survivor.updateStatus(intensityMap[survivor.getX()][survivor.getY()]);
            }
        }
    }

    // Mettre à jour les robots
    private void updateRobots() {
        Firefighter newRobot = hq.checkAndAddFirefighter();
        if (newRobot != null) {
            newRobot.setFireGrid(fireGrid);
            robots.add(newRobot);
        }
        
        for (Robot robot : robots) {
            if (robot instanceof Scout) {
                if (((Scout)robot).getFireGrid() == null) {
                    ((Scout)robot).setFireGrid(fireGrid);
                }
                ((Scout)robot).updateState(hq);
            } else if (robot instanceof Firefighter) {
                ((Firefighter)robot).updateState(hq);
            }
        }

        // Mettre à jour les survivants
        updateSurvivors();
    }

    // Créer l'interface graphique
    public void createGUI() {
        this.gui = new SimulationGUI(HeadQuarters.getGridWidth(), HeadQuarters.getGridHeight(), 
                                   HeadQuarters.getHqX(), HeadQuarters.getHqY(), this);
    }

    public FireGrid getFireGrid() {
        return fireGrid;
    }

    public FireStatistics getStatistics() {
        return statistics;
    }

    // Réinitialiser la simulation
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

        // Scheduler l'apparition des survivants après 5 secondes de démarrage de la simulation
        ScheduledExecutorService survivorExecutor = Executors.newScheduledThreadPool(1);
        survivorExecutor.schedule(() -> {
            if (isRunning) {
                spawnSurvivor();
                updateGUI();
            }
            survivorExecutor.shutdown();
        }, 5, TimeUnit.SECONDS); 

        // Scheduler les mises à jour de feu
        fireExecutor.scheduleAtFixedRate(() -> {
            if (isRunning) {
                fire.spread();
                timeStep++;
                updateGUI();
            }
        }, 0, FIRE_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);

        // Scheduler les mises à jour des robots
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

    // Générer les informations de simulation
    private String generateSimulationInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== Simulation Status ===\n");
        info.append("Time Step: ").append(timeStep).append("\n");
        info.append("Grid Size: ").append(HeadQuarters.GRID_WIDTH).append("x").append(HeadQuarters.GRID_HEIGHT).append("\n");
        info.append("HQ Position: [").append(HeadQuarters.HQ_X).append(",").append(HeadQuarters.HQ_Y).append("]\n\n");
        
        info.append("=== Fire Status ===\n");
        info.append("Max Intensity: ").append(fireGrid.getMaxIntensity()).append("\n");
        info.append("Intensity Threshold: ").append(fireGrid.getIntensityThreshold()).append("\n");
        info.append("Spread Probability: ").append(fireGrid.getSpreadProbability()).append("\n");
        info.append("Initial Intensity: ").append(fireGrid.getInitialIntensity()).append("\n");
        info.append("Fire Type: ").append(fireGrid.getScenarioDescription()).append("\n\n");

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

        info.append("=== Statistics ===\n");
        info.append(statistics.getFormattedStatistics()).append("\n");

        return info.toString();
    }

    public static void main(String[] args) {
        Simulation simulation = new Simulation();
        simulation.createGUI();
    }
}