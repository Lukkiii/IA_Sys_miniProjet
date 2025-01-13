import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

public class Simulation {

    private static final int FIRE_UPDATE_INTERVAL = 800;
    private static final int ROBOT_UPDATE_INTERVAL = 400;
    private static final int GRID_WIDTH = 30;
    private static final int GRID_HEIGHT = 30;
    private static final int HQ_X = 15;
    private static final int HQ_Y = 15;

    private Fire fire;
    private SimulationGUI gui;
    private boolean isRunning;
    private ScheduledExecutorService fireExecutor;
    private ScheduledExecutorService robotExecutor;
    private int timeStep = 0;

    private List<Robot> robots;
    private Map<Robot, int[]> robotTargets = new HashMap<>();
    private static final int INITIAL_ROBOTS = 1;
    private static final int MAX_ROBOTS = 8;
    private static final double FIRE_THRESHOLD_PER_ROBOT = 8.0;
    private static final int CHECK_INTERVAL = 10;

    public Simulation() {
        this.fire = new Fire(GRID_WIDTH, GRID_HEIGHT);
        this.gui = new SimulationGUI(GRID_WIDTH, GRID_HEIGHT, HQ_X, HQ_Y);
        this.isRunning = false;
        this.fireExecutor = Executors.newScheduledThreadPool(1);
        this.robotExecutor = Executors.newScheduledThreadPool(1);
        this.robots = new ArrayList<>();
        addNewRobot();
    }

    private void addNewRobot() {
        if (robots.size() < MAX_ROBOTS) {
            robots.add(new Robot(HQ_X, HQ_Y));
        }
    }

    public void start() {
        isRunning = true;

        // Schedule fire spread updates
        fireExecutor.scheduleAtFixedRate(() -> {
            if (isRunning) {
                fire.spread();
                timeStep++;
                updateGUI();
            }
        }, 0, FIRE_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);

        // Schedule robot updates at a faster rate
        robotExecutor.scheduleAtFixedRate(() -> {
            if (isRunning) {
                checkAndAdjustRobots();
                updateRobots();
                updateGUI();
            }
        }, 0, ROBOT_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
    }

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

    private void updateGUI() {
        SwingUtilities.invokeLater(() -> {
            gui.updateDisplay(fire.getIntensityMap(), generateSimulationInfo(), robots);
        });
    }

    private void updateRobots() {
        double[][] intensityMap = fire.getIntensityMap();
        
        // Clean up completed targets
        robotTargets.entrySet().removeIf(entry -> {
            Robot robot = entry.getKey();
            int[] target = entry.getValue();
            return robot.getState().equals("extinguishing") || 
                   intensityMap[target[0]][target[1]] <= FireGrid.INTENSITY_THRESHOLD;
        });

        for (Robot robot : robots) {
            if (robot.getState().equals("searching")) {
                // Check if robot is already near fire
                if (isNearFire(robot, intensityMap)) {
                    robot.setState("extinguishing");
                    robotTargets.remove(robot);
                    continue;
                }

                // Assign new target if robot doesn't have one
                if (!robotTargets.containsKey(robot)) {
                    int[] nearestFire = findBestFireTarget(intensityMap, robot);
                    if (nearestFire != null) {
                        robotTargets.put(robot, nearestFire);
                    }
                }
                
                // Move towards target if one exists
                int[] target = robotTargets.get(robot);
                if (target != null) {
                    // Start extinguishing if near fire
                    if (isNearFire(robot, intensityMap)) {
                        robot.setState("extinguishing");
                        robotTargets.remove(robot);
                    } else {
                        robot.moveTowards(target[0], target[1]);
                    }
                }
            } else if (robot.getState().equals("extinguishing")) {
                // Return to searching if no fire nearby
                if (!isNearFire(robot, intensityMap)) {
                    robot.setState("searching");
                } else {
                    robot.extinguishFire(fire.getFireGrid());
                }
            }
        }
    }

    // Check if there's fire in or adjacent to robot's position
    private boolean isNearFire(Robot robot, double[][] intensityMap) {
        int x = robot.getX();
        int y = robot.getY();
        
        // Check 3x3 area around robot
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int newX = x + dx;
                int newY = y + dy;
                
                if (newX >= 0 && newX < GRID_WIDTH && 
                    newY >= 0 && newY < GRID_HEIGHT) {
                    if (intensityMap[newX][newY] > FireGrid.INTENSITY_THRESHOLD) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void checkAndAdjustRobots() {
        if (timeStep % CHECK_INTERVAL != 0) return;

        double[][] intensityMap = fire.getIntensityMap();
        int activeFireCount = 0;
        double totalIntensity = 0;
        double maxIntensity = 0;

        // Count active fires and calculate intensities
        for (int i = 0; i < GRID_WIDTH; i++) {
            for (int j = 0; j < GRID_HEIGHT; j++) {
                if (intensityMap[i][j] > FireGrid.INTENSITY_THRESHOLD) {
                    activeFireCount++;
                    totalIntensity += intensityMap[i][j];
                    maxIntensity = Math.max(maxIntensity, intensityMap[i][j]);
                }
            }
        }

        double averageIntensity = activeFireCount > 0 ? totalIntensity / activeFireCount : 0;
        double intensityFactor = averageIntensity / FireGrid.MAX_INTENSITY;
        double basedOnCount = Math.ceil(activeFireCount / FIRE_THRESHOLD_PER_ROBOT);
        double basedOnIntensity = Math.ceil(basedOnCount * (1 + intensityFactor));

        int neededRobots = (int) Math.min(MAX_ROBOTS,
                Math.max(INITIAL_ROBOTS, basedOnIntensity));

        // Adjust robot count
        if (robots.size() < neededRobots) {
            while (robots.size() < neededRobots) {
                addNewRobot();
            }
        } else if (robots.size() > neededRobots) {
            while (robots.size() > neededRobots) {
                Iterator<Robot> iterator = robots.iterator();
                boolean removed = false;
                while (iterator.hasNext()) {
                    Robot robot = iterator.next();
                    if (robot.getState().equals("searching")) {
                        iterator.remove();
                        robotTargets.remove(robot);
                        removed = true;
                        break;
                    }
                }
                if (!removed && robots.size() > neededRobots) {
                    Robot removedRobot = robots.remove(robots.size() - 1);
                    robotTargets.remove(removedRobot);
                }
            }
        }
    }


    private int[] findBestFireTarget(double[][] intensityMap, Robot currentRobot) {
        List<FirePoint> firePoints = new ArrayList<>();
        
        for (int i = 0; i < GRID_WIDTH; i++) {
            for (int j = 0; j < GRID_HEIGHT; j++) {
                if (intensityMap[i][j] > FireGrid.INTENSITY_THRESHOLD) {
                    double distance = Math.sqrt(Math.pow(i - currentRobot.getX(), 2) + 
                                             Math.pow(j - currentRobot.getY(), 2));

                    boolean isTargeted = false;
                    for (Map.Entry<Robot, int[]> entry : robotTargets.entrySet()) {
                        int[] target = entry.getValue();
                        if (target[0] == i && target[1] == j) {
                            isTargeted = true;
                            break;
                        }
                    }

                    for (Robot otherRobot : robots) {
                        if (otherRobot != currentRobot && 
                            otherRobot.getState().equals("extinguishing") &&
                            otherRobot.getX() == i && otherRobot.getY() == j) {
                            isTargeted = true;
                            break;
                        }
                    }
                    
                    if (!isTargeted) {
                        firePoints.add(new FirePoint(i, j, distance, intensityMap[i][j]));
                    }
                }
            }
        }
        
        if (firePoints.isEmpty()) {
            return null;
        }
        
        Collections.sort(firePoints, (a, b) -> {
            double scoreA = (1000/a.distance) * Math.pow(a.intensity, 0.5);
            double scoreB = (1000/b.distance) * Math.pow(b.intensity, 0.5);
            return Double.compare(scoreB, scoreA);
        });
        int choiceRange = Math.min(3, firePoints.size());
        int chosen = new Random().nextInt(choiceRange);
        FirePoint target = firePoints.get(chosen);
        
        return new int[]{target.x, target.y};
    }
    
    private static class FirePoint {
        int x, y;
        double distance;
        double intensity;
        
        FirePoint(int x, int y, double distance, double intensity) {
            this.x = x;
            this.y = y;
            this.distance = distance;
            this.intensity = intensity;
        }
    }

    // Générer les informations de simulation
    private String generateSimulationInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== Simulation Status ===\n");
        info.append("Time Step: ").append(timeStep).append("\n");
        info.append("Grid Size: ").append(GRID_WIDTH).append("x").append(GRID_HEIGHT).append("\n");
        info.append("HQ Position: [").append(HQ_X).append(",").append(HQ_Y).append("]\n\n");
        info.append("\nRobots Status:\n");
        for (int i = 0; i < robots.size(); i++) {
            Robot robot = robots.get(i);
            info.append("Robot ").append(i + 1)
                .append(": [").append(robot.getX()).append(",").append(robot.getY()).append("] ")
                .append(robot.getState()).append("\n");
        }
        info.append("\n");

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

    public static void main(String[] args) {
        Simulation simulation = new Simulation();
        simulation.start();
    }
}