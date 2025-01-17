import java.util.*;

public class Scout extends Robot {
    private static final int VISION_RANGE = 3;
    private static final int GRID_WIDTH = 30;
    private static final int GRID_HEIGHT = 30;
    private int targetX;
    private int targetY;
    private Random random;
    private FireGrid fireGrid;

    public Scout(int id, int x, int y, int gridWidth, int gridHeight) {
        super(id, x, y, gridWidth, gridHeight);
        this.random = new Random();
        setNewExplorationTarget();
    }

    @Override
    public String getType() {
        return TYPE_SCOUT;
    }

    @Override
    public void updateState(HeadQuarters hq) {
        List<FireSpot> newFires = scanArea();
        if (!newFires.isEmpty()) {
            hq.receiveFireReport(id, newFires);
            discoveredFires.clear();
        }

        if (isAtHQ()) {
            updateLocalKnowledge(hq);
        }

        // Move to new exploration target
        if (random.nextDouble() < 0.1) {
            setNewExplorationTarget();
        }
        moveTowards(targetX, targetY);
        currentState = State.SCOUTING;
    }

    private List<FireSpot> scanArea() {
        for (int dx = -VISION_RANGE; dx <= VISION_RANGE; dx++) {
            for (int dy = -VISION_RANGE; dy <= VISION_RANGE; dy++) {
                int newX = x + dx;
                int newY = y + dy;
                
                if (isValidPosition(newX, newY)) {
                    double intensity = observeFireIntensity(newX, newY);
                    if (intensity > FireGrid.INTENSITY_THRESHOLD) {
                        discoveredFires.add(new FireSpot(newX, newY, 
                                          intensity, 
                                          System.currentTimeMillis()));
                    }
                }
            }
        }
        return new ArrayList<>(discoveredFires);
    }

    private double observeFireIntensity(int x, int y) {
        if (fireGrid != null) {
            return fireGrid.getIntensityAt(x, y);
        }
        return 0.0;
    }

    private void setNewExplorationTarget() {
        targetX = random.nextInt(GRID_WIDTH);
        targetY = random.nextInt(GRID_HEIGHT);
    }

    public void setFireGrid(FireGrid fireGrid) {
        this.fireGrid = fireGrid;
    }

    public FireGrid getFireGrid() {
        return fireGrid;
    }

    List<FireSpot> reportDiscoveredFires() {
        List<FireSpot> reports = new ArrayList<>(discoveredFires);
        discoveredFires.clear();
        return reports;
    }
}