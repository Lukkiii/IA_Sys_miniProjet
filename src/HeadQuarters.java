import java.util.*;

public class HeadQuarters {
    private int x;
    private int y;
    private double[][] globalFireMap;
    private Map<Integer, List<FireSpot>> robotReports = new HashMap<>();
    private Map<FireSpot, Firefighter> fireAssignments = new HashMap<>();

    private static final int MAX_ROBOTS = 7;
    private static final int INITIAL_SCOUTS = 2;
    private List<Firefighter> firefighters = new ArrayList<>();
    private Fire fire;
    private int gridWidth;
    private int gridHeight;

    public HeadQuarters(int x, int y, int width, int height, Fire fire) {
        this.x = x;
        this.y = y;
        this.gridWidth = width;
        this.gridHeight = height;
        this.fire = fire;
        this.globalFireMap = new double[width][height];
    }

    public Firefighter checkAndAddFirefighter() {
        double[][] intensityMap = fire.getIntensityMap();
        int activeFireCount = 0;
        
        for (int i = 0; i < gridWidth; i++) {
            for (int j = 0; j < gridHeight; j++) {
                if (intensityMap[i][j] > FireGrid.INTENSITY_THRESHOLD) {
                    activeFireCount++;
                }
            }
        }

        int neededFirefighters = Math.min(MAX_ROBOTS - INITIAL_SCOUTS,
                                        (int)Math.ceil(activeFireCount / 50.0));
    
        if (firefighters.size() < neededFirefighters) {
            Firefighter newFirefighter = createNewFirefighter();
            return newFirefighter;
        }
        
        return null;
    }

    private Firefighter createNewFirefighter() {
        int id = firefighters.size() + INITIAL_SCOUTS;
        Firefighter ff = new Firefighter(id, x, y, gridWidth, gridHeight);
        firefighters.add(ff);
        return ff;
    }

    // Used by Scouts to report fire locations
    public void receiveFireReport(int robotId, List<FireSpot> spots) {
        robotReports.put(robotId, spots);
        // Update global map with new fire information
        for (FireSpot spot : spots) {
            globalFireMap[spot.x][spot.y] = spot.intensity;
        }
        
        // Immediately update assignments when new fires are reported
        updateFirefighterAssignments();
    }
    
    public int[] getFirefighterAssignment(Firefighter firefighter) {
        List<FireSpot> unassignedFires = new ArrayList<>();
        for (List<FireSpot> spots : robotReports.values()) {
            for (FireSpot spot : spots) {
                if (!fireAssignments.containsKey(spot)) {
                    unassignedFires.add(spot);
                }
            }
        }

        if (!unassignedFires.isEmpty()) {
            FireSpot nearestFire = findNearestFire(firefighter, unassignedFires);
            if (nearestFire != null) {
                fireAssignments.put(nearestFire, firefighter);
                return new int[]{nearestFire.x, nearestFire.y};
            }
        }
        return null;
    }

    private FireSpot findNearestFire(Firefighter firefighter, List<FireSpot> fires) {
        FireSpot nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (FireSpot fire : fires) {
            double distance = Math.sqrt(
                Math.pow(firefighter.getX() - fire.x, 2) + 
                Math.pow(firefighter.getY() - fire.y, 2)
            );
            if (distance < minDistance) {
                minDistance = distance;
                nearest = fire;
            }
        }
        return nearest;
    }

    public void updateFirefighterAssignments() {
        fireAssignments.entrySet().removeIf(entry -> {
            FireSpot spot = entry.getKey();
            return globalFireMap[spot.x][spot.y] <= FireGrid.INTENSITY_THRESHOLD;
        });
    }


    public int getX() { return x; }
    public int getY() { return y; }

    public void updateGlobalMap(double[][] currentMap) {
        for (int i = 0; i < globalFireMap.length; i++) {
            for (int j = 0; j < globalFireMap[0].length; j++) {
                globalFireMap[i][j] = currentMap[i][j];
            }
        }
    }

    public double[][] getGlobalMap() {
        return globalFireMap.clone();
    }

}