import java.util.*;

public class HeadQuarters {

    public static final long REPORT_EXPIRATION_TIME = 10000;
    public static final int MAX_ROBOTS = 7;
    public static final int INITIAL_SCOUTS = 2;
    public static final int GRID_WIDTH = 24;
    public static final int GRID_HEIGHT = 24;
    public static final int HQ_X = 12;
    public static final int HQ_Y = 12;
    public static final int SAFE_DISTANCE = 4;

    private int x;
    private int y;
    private double[][] globalFireMap;
    private Fire fire;
    private int gridWidth;
    private int gridHeight;
    private Map<Integer, List<FireSpot>> robotReports = new HashMap<>();
    private Map<FireSpot, Firefighter> fireAssignments = new HashMap<>();
    private Map<FireSpot, Long> reportTimes = new HashMap<>();
    private List<Firefighter> firefighters = new ArrayList<>();
    
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
                                        (int)Math.ceil(activeFireCount / 10.0));
    
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
        long currentTime = System.currentTimeMillis();

        List<FireSpot> updatedSpots = new ArrayList<>();
        for (FireSpot spot : spots) {
            if (fire.getIntensityMap()[spot.x][spot.y] > FireGrid.INTENSITY_THRESHOLD) {
                updatedSpots.add(spot);
                reportTimes.put(spot, currentTime);
                globalFireMap[spot.x][spot.y] = spot.intensity;
            }
        }

        if (!updatedSpots.isEmpty()) {
            robotReports.put(robotId, updatedSpots);
            updateFirefighterAssignments();
        }
        
        cleanupOldReports();
    }

    private void cleanupOldReports() {
        long currentTime = System.currentTimeMillis();
        
        reportTimes.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > REPORT_EXPIRATION_TIME);
            
        robotReports.values().forEach(spots -> spots.removeIf(spot -> 
            !reportTimes.containsKey(spot) || 
            fire.getIntensityMap()[spot.x][spot.y] <= FireGrid.INTENSITY_THRESHOLD));
            
        robotReports.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        updateFirefighterAssignments();
    }
    
    public int[] getFirefighterAssignment(Firefighter firefighter) {
        cleanupOldReports();

        List<FireSpot> unassignedFires = new ArrayList<>();
        for (List<FireSpot> spots : robotReports.values()) {
            for (FireSpot spot : spots) {
                if (!fireAssignments.containsKey(spot) && 
                    fire.getIntensityMap()[spot.x][spot.y] > FireGrid.INTENSITY_THRESHOLD) {
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
            return fire.getIntensityMap()[spot.x][spot.y] <= FireGrid.INTENSITY_THRESHOLD ||
                   !reportTimes.containsKey(spot);
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

    public static int getGridWidth() { return GRID_WIDTH; }
    public static int getGridHeight() { return GRID_HEIGHT; }
    public static int getHqX() { return HQ_X; }
    public static int getHqY() { return HQ_Y; }
    public static int getSafeDistance() { return SAFE_DISTANCE; }

}