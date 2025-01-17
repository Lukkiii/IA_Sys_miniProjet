import java.util.*;

public class Scout extends Robot {
    private static final int VISION_RANGE = 3;
    private static final int GRID_WIDTH = 30;
    private static final int GRID_HEIGHT = 30;
    private int targetX;
    private int targetY;
    private Random random;

    public Scout(int id, int x, int y, int gridWidth, int gridHeight) {
        super(id, x, y, gridWidth, gridHeight);
        this.random = new Random();
        // Initial random target
        this.targetX = random.nextInt(gridWidth);
        this.targetY = random.nextInt(gridHeight);
    }

    @Override
    public String getType() {
        return TYPE_SCOUT;
    }

    @Override
    public void updateState(HeadQuarters hq, double[][] intensityMap) {
        // Scan area for fires
        scanArea(intensityMap);

        // Always report fires if found
        List<FireSpot> reports = reportDiscoveredFires();
        if (!reports.isEmpty()) {
            hq.receiveFireReport(id, reports);
        }

        // Move to new exploration target
        if (Math.abs(x - targetX) <= 1 && Math.abs(y - targetY) <= 1 || random.nextDouble() < 0.1) {
            targetX = random.nextInt(GRID_WIDTH);
            targetY = random.nextInt(GRID_HEIGHT);
        }
        moveTowards(targetX, targetY);
        currentState = State.SCOUTING;
    }

    private void scanArea(double[][] intensityMap) {
        for (int dx = -VISION_RANGE; dx <= VISION_RANGE; dx++) {
            for (int dy = -VISION_RANGE; dy <= VISION_RANGE; dy++) {
                int newX = x + dx;
                int newY = y + dy;
                if (isValidPosition(newX, newY, intensityMap)) {
                    if (intensityMap[newX][newY] > FireGrid.INTENSITY_THRESHOLD) {
                        discoveredFires.add(new FireSpot(newX, newY, 
                                          intensityMap[newX][newY], 
                                          System.currentTimeMillis()));
                    }
                }
            }
        }
    }

    List<FireSpot> reportDiscoveredFires() {
        List<FireSpot> reports = new ArrayList<>(discoveredFires);
        discoveredFires.clear();
        return reports;
    }
}