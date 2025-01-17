public class Firefighter extends Robot {

    private static final int VISION_RANGE = 3;
    private static final int EXTINGUISH_RADIUS = 10;
    private static final double EXTINGUISH_AMOUNT = 15.0;
    private FireGrid fireGrid;
    private int targetX = -1;
    private int targetY = -1;

    public Firefighter(int id, int x, int y, int gridWidth, int gridHeight) {
        super(id, x, y, gridWidth, gridHeight);
    }

    @Override
    public String getType() {
        return TYPE_FIREFIGHTER;
    }

    @Override
public void updateState(HeadQuarters hq, double[][] intensityMap) {

    if (isAtHQ()) {
        int[] target = hq.getFirefighterAssignment(this);
        if (target != null) {
            targetX = target[0];
            targetY = target[1];
            currentState = State.MOVING_TO_FIRE;
            moveTowards(targetX, targetY);
        }
    } else if (currentState == State.MOVING_TO_FIRE) {
        if (isNearFire(intensityMap)) {
            currentState = State.EXTINGUISHING;
            extinguishFire(intensityMap);
        } else {
            moveTowards(targetX, targetY);
        }
    } else if (currentState == State.EXTINGUISHING) {
        if (isNearFire(intensityMap)) {
            extinguishFire(intensityMap);
        } else {
            targetX = hq.getX();
            targetY = hq.getY();
            currentState = State.MOVING_TO_HQ;
            moveTowards(targetX, targetY);
        }
    } else if (currentState == State.MOVING_TO_HQ) {
        moveTowards(hq.getX(), hq.getY());
        if (isAtHQ()) {
            currentState = State.AT_HQ;
        }
    }
}
    private boolean isNearFire(double[][] intensityMap) {
        for (int dx = -VISION_RANGE; dx <= VISION_RANGE; dx++) {
            for (int dy = -VISION_RANGE; dy <= VISION_RANGE; dy++) {
                int newX = x + dx;
                int newY = y + dy;
                
                if (isValidPosition(newX, newY, intensityMap)) {
                    if (intensityMap[newX][newY] > FireGrid.INTENSITY_THRESHOLD) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    

    private void moveSmartlyTowards(int targetX, int targetY, double[][] intensityMap) {
        int bestDx = 0, bestDy = 0;
        double lowestRisk = Double.MAX_VALUE;
    
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int newX = x + dx;
                int newY = y + dy;
    
                if (isValidPosition(newX, newY, intensityMap)) {
                    double currentRisk = calculateMovementRisk(newX, newY, targetX, targetY, intensityMap);
                    
                    if (currentRisk < lowestRisk) {
                        lowestRisk = currentRisk;
                        bestDx = dx;
                        bestDy = dy;
                    }
                }
            }
        }
    
        x += bestDx;
        y += bestDy;
    }
    
    private double calculateMovementRisk(int x, int y, int targetX, int targetY, double[][] intensityMap) {
        double distanceToTarget = Math.sqrt(Math.pow(x - targetX, 2) + Math.pow(y - targetY, 2));
        
        double fireRisk = intensityMap[x][y] > FireGrid.INTENSITY_THRESHOLD ? 
                          intensityMap[x][y] : 0;
        
        return distanceToTarget + fireRisk;
    }

    public void extinguishFire(double[][] intensityMap) {
        for (int dx = -EXTINGUISH_RADIUS; dx <= EXTINGUISH_RADIUS; dx++) {
            for (int dy = -EXTINGUISH_RADIUS; dy <= EXTINGUISH_RADIUS; dy++) {
                int newX = x + dx;
                int newY = y + dy;
                
                if (isValidPosition(newX, newY, intensityMap)) {
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance <= EXTINGUISH_RADIUS) {
                        double effect = EXTINGUISH_AMOUNT * (1.0 - (distance / (EXTINGUISH_RADIUS + 1)));
                        if (fireGrid != null) {
                            fireGrid.decreaseIntensity(newX, newY, effect);
                        }
                    }
                }
            }
        }
    }

    public void setFireGrid(FireGrid fireGrid) {
        this.fireGrid = fireGrid;
    }

}