import java.util.*;

public class Scout extends Robot {
    private static final int VISION_RANGE = 5;
    private static final int GRID_WIDTH = 30;
    private static final int GRID_HEIGHT = 30;
    private static final int MAX_PREVIOUS_TARGETS = 5;
    private static final long FIRE_RECHECK_INTERVAL = 5000;
    private static final double FIRE_RECHECK_PROBABILITY = 0.3;

    private int targetX;
    private int targetY;
    private Random random;
    private FireGrid fireGrid;
    private boolean[][] exploredAreas;
    private List<int[]> previousTargets = new ArrayList<>();
    private Map<Point, Long> fireLocations;
    

    public Scout(int id, int x, int y, int gridWidth, int gridHeight) {
        super(id, x, y, gridWidth, gridHeight);
        this.random = new Random();
        this.exploredAreas = new boolean[gridWidth][gridHeight];
        this.fireLocations = new HashMap<>();
        setNewExplorationTarget();
    }

    @Override
    public String getType() {
        return TYPE_SCOUT;
    }

    @Override
    public void updateState(HeadQuarters hq) {
        if (currentState == State.RECHARGING_ELECTRICITY) {
            if (isRechargeComplete()) {
                finishRecharge();
                currentState = State.AT_HQ;
            } else {
                reportFiresIfFound(hq);
                return;
            }
        }

        if (needsRecharge() && !isAtHQ()) {
            currentState = State.MOVING_TO_HQ;
            moveTowards(hq.getX(), hq.getY());
            reportFiresIfFound(hq);
            return;
        }

        if (isAtHQ() && needsRecharge()) {
            currentState = State.RECHARGING_ELECTRICITY;
            startRecharge();
            reportFiresIfFound(hq);
            return;
        }

        reportFiresIfFound(hq);

        if (isAtHQ()) {
            updateLocalKnowledge(hq);
        }

        markExploredArea();

        if (hasReachedTarget() || shouldChangeTarget()) {
            if (!fireLocations.isEmpty() && random.nextDouble() < FIRE_RECHECK_PROBABILITY) {
                setFireRecheckTarget();
            } else {
                setNewExplorationTarget();
            }
        }

        if (currentState != State.MOVING_TO_HQ && currentState != State.RECHARGING_ELECTRICITY) {
            moveTowardsTarget();
            currentState = State.SCOUTING;
        }

        cleanupOldFireLocations();
    }

    private void reportFiresIfFound(HeadQuarters hq) {
        List<FireSpot> newFires = scanArea();
        if (!newFires.isEmpty()) {
            hq.receiveFireReport(id, newFires);
            discoveredFires.clear();

            for (FireSpot fire : newFires) {
                fireLocations.put(new Point(fire.x, fire.y), System.currentTimeMillis());
            }
        }
    }

    private void setFireRecheckTarget() {
        long currentTime = System.currentTimeMillis();
        Point bestTarget = null;
        long oldestCheck = currentTime;
        
        for (Map.Entry<Point, Long> entry : fireLocations.entrySet()) {
            if (entry.getValue() < oldestCheck) {
                oldestCheck = entry.getValue();
                bestTarget = entry.getKey();
            }
        }
        
        if (bestTarget != null && (currentTime - oldestCheck) >= FIRE_RECHECK_INTERVAL) {
            targetX = bestTarget.x;
            targetY = bestTarget.y;
        } else {
            setNewExplorationTarget();
        }
    }

    private void cleanupOldFireLocations() {
        fireLocations.entrySet().removeIf(entry -> {
            Point p = entry.getKey();
            return fireGrid.getIntensityAt(p.x, p.y) <= FireGrid.INTENSITY_THRESHOLD;
        });
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

    private void markExploredArea() {
        for (int dx = -VISION_RANGE; dx <= VISION_RANGE; dx++) {
            for (int dy = -VISION_RANGE; dy <= VISION_RANGE; dy++) {
                int newX = x + dx;
                int newY = y + dy;
                if (isValidPosition(newX, newY)) {
                    exploredAreas[newX][newY] = true;
                }
            }
        }
    }

    private boolean hasReachedTarget() {
        return Math.abs(x - targetX) <= 1 && Math.abs(y - targetY) <= 1;
    }

    private void moveTowardsTarget() {
        int bestDx = 0, bestDy = 0;
        double bestValue = Double.NEGATIVE_INFINITY;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                
                int newX = x + dx;
                int newY = y + dy;
                if (!isValidPosition(newX, newY)) continue;

                double value = evaluateMove(newX, newY);
                if (value > bestValue) {
                    bestValue = value;
                    bestDx = dx;
                    bestDy = dy;
                }
            }
        }

        x += bestDx;
        y += bestDy;
    }

    private double evaluateMove(int newX, int newY) {
        double distanceToTarget = -Math.sqrt(Math.pow(newX - targetX, 2) + Math.pow(newY - targetY, 2));
        double explorationBonus = exploredAreas[newX][newY] ? -5 : 5;
        return distanceToTarget + explorationBonus;
    }

    private void setNewExplorationTarget() {
        if (targetX != -1 && targetY != -1) {
            previousTargets.add(new int[]{targetX, targetY});
            if (previousTargets.size() > MAX_PREVIOUS_TARGETS) {
                previousTargets.remove(0);
            }
        }

        int bestX = -1, bestY = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        for (int attempts = 0; attempts < 20; attempts++) {
            int candidateX = random.nextInt(GRID_WIDTH);
            int candidateY = random.nextInt(GRID_HEIGHT);
            
            double score = evaluateTarget(candidateX, candidateY);
            if (score > bestScore) {
                bestScore = score;
                bestX = candidateX;
                bestY = candidateY;
            }
        }

        targetX = bestX;
        targetY = bestY;
    }

    private double evaluateTarget(int tx, int ty) {
        double unexploredScore = countUnexploredAround(tx, ty);
        double previousTargetsPenalty = getPreviousTargetsPenalty(tx, ty);
        double hqDistancePenalty = -Math.sqrt(Math.pow(tx - 15, 2) + Math.pow(ty - 15, 2)) / 10;
        
        return unexploredScore + previousTargetsPenalty + hqDistancePenalty;
    }

    private double countUnexploredAround(int tx, int ty) {
        int count = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                int newX = tx + dx;
                int newY = ty + dy;
                if (isValidPosition(newX, newY) && !exploredAreas[newX][newY]) {
                    count++;
                }
            }
        }
        return count;
    }

    private double getPreviousTargetsPenalty(int tx, int ty) {
        double penalty = 0;
        for (int[] prev : previousTargets) {
            double distance = Math.sqrt(Math.pow(tx - prev[0], 2) + Math.pow(ty - prev[1], 2));
            penalty -= 10.0 / (distance + 1);
        }
        return penalty;
    }

    private boolean shouldChangeTarget() {
        return random.nextDouble() < 0.05;
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