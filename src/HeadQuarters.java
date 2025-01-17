import java.util.*;

public class HeadQuarters {
    public static final long REPORT_EXPIRATION_TIME = 100;
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
    private int gridWidth;
    private int gridHeight;
    private Map<Integer, List<FireSpot>> robotReports = new HashMap<>();
    private Map<FireSpot, Long> reportTimes = new HashMap<>();
    private List<Firefighter> firefighters = new ArrayList<>();
    
    public HeadQuarters(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.gridWidth = width;
        this.gridHeight = height;
        this.globalFireMap = new double[width][height];
    }

    public Firefighter checkAndAddFirefighter() {
        int activeFireCount = 0;
        for (int i = 0; i < gridWidth; i++) {
            for (int j = 0; j < gridHeight; j++) {
                if (globalFireMap[i][j] > FireGrid.INTENSITY_THRESHOLD) {
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

    public void receiveFireReport(int robotId, List<FireSpot> spots) {
        long currentTime = System.currentTimeMillis();
    
        List<FireSpot> updatedSpots = new ArrayList<>(spots);
        robotReports.put(robotId, updatedSpots);
        
        spots.forEach(spot -> reportTimes.put(spot, currentTime));
        
        updateGlobalFireMap();
        
        cleanupOldReports();
    }

    private void updateGlobalFireMap() {
        for (int i = 0; i < gridWidth; i++) {
            for (int j = 0; j < gridHeight; j++) {
                globalFireMap[i][j] = 0;
            }
        }

        robotReports.values().forEach(spots -> spots.forEach(spot -> 
            globalFireMap[spot.x][spot.y] = spot.intensity));
    }

    private void cleanupOldReports() {
        long currentTime = System.currentTimeMillis();
    
        reportTimes.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > REPORT_EXPIRATION_TIME);
        
        robotReports.values().forEach(spots -> 
            spots.removeIf(spot -> 
                !reportTimes.containsKey(spot) || 
                globalFireMap[spot.x][spot.y] <= FireGrid.INTENSITY_THRESHOLD
            )
        );
        
        robotReports.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        updateGlobalFireMap();
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public double[][] getGlobalMap() {
        double[][] copy = new double[gridWidth][gridHeight];
        for (int i = 0; i < gridWidth; i++) {
            System.arraycopy(globalFireMap[i], 0, copy[i], 0, gridHeight);
        }
        return copy;
    }

    public static int getGridWidth() { return GRID_WIDTH; }
    public static int getGridHeight() { return GRID_HEIGHT; }
    public static int getHqX() { return HQ_X; }
    public static int getHqY() { return HQ_Y; }
    public static int getSafeDistance() { return SAFE_DISTANCE; }
}