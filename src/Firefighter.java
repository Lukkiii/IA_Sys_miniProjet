public class Firefighter extends Robot {
    private static final int VISION_RANGE = 3;
    private static final int EXTINGUISH_RADIUS = 10;
    private static final double EXTINGUISH_AMOUNT = 35.0;
    private static final int MAX_EXTINGUISHING_TIME = 20;
    private static final int MAX_MOVING_TIME = 20;

    private FireGrid fireGrid;
    private int targetX = -1;
    private int targetY = -1;
    private int extinguishingTime = 0;
    private int movingTime = 0;
    

    public Firefighter(int id, int x, int y, int gridWidth, int gridHeight) {
        super(id, x, y, gridWidth, gridHeight);
    }

    @Override
    public String getType() {
        return TYPE_FIREFIGHTER;
    }

    @Override
    public void updateState(HeadQuarters hq) {
        if (isAtHQ()) {
            extinguishingTime = 0;
            movingTime = 0;

            updateLocalKnowledge(hq);

            int[] target = hq.getFirefighterAssignment(this);
            if (target != null) {
                targetX = target[0];
                targetY = target[1];
                currentState = State.MOVING_TO_FIRE;
                moveSmartlyTowards(targetX, targetY);
            }
        } else if (currentState == State.MOVING_TO_FIRE) {
            movingTime++;
            if (movingTime >= MAX_MOVING_TIME) {
                returnToHQ();
            } else if (isNearFireDirect()) {
                movingTime = 0;
                currentState = State.EXTINGUISHING;
                extinguishFire();
            } else {
                moveSmartlyTowards(targetX, targetY);
            }
        } else if (currentState == State.EXTINGUISHING) {
            extinguishingTime++;
            if (isNearFireDirect()) {
                if (extinguishingTime >= MAX_EXTINGUISHING_TIME) {
                    returnToHQ();
                } else {
                    extinguishFire();
                }
            } else {
                returnToHQ();
            }
        } else if (currentState == State.MOVING_TO_HQ) {
            moveSmartlyTowards(hq.getX(), hq.getY());
            if (isAtHQ()) {
                currentState = State.AT_HQ;
            }
        }
    }

    private boolean isNearFireDirect() {
        for (int dx = -VISION_RANGE; dx <= VISION_RANGE; dx++) {
            for (int dy = -VISION_RANGE; dy <= VISION_RANGE; dy++) {
                int newX = x + dx;
                int newY = y + dy;
                if (isValidPosition(newX, newY)) {
                    if (fireGrid != null && fireGrid.getIntensityAt(newX, newY) > FireGrid.INTENSITY_THRESHOLD) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void returnToHQ() {
        targetX = 15;
        targetY = 15;
        currentState = State.MOVING_TO_HQ;
    }

    private void moveSmartlyTowards(int targetX, int targetY) {
        int bestDx = 0, bestDy = 0;
        double lowestRisk = Double.MAX_VALUE;
    
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int newX = x + dx;
                int newY = y + dy;
                if (isValidPosition(newX, newY)) {
                    double currentRisk = calculateMovementRisk(newX, newY, targetX, targetY);
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
    
    private double calculateMovementRisk(int x, int y, int targetX, int targetY) {
        double distanceToTarget = Math.sqrt(Math.pow(x - targetX, 2) + Math.pow(y - targetY, 2));
        double fireRisk = 0;
        
        if (fireGrid != null) {
            fireRisk = fireGrid.getIntensityAt(x, y) > FireGrid.INTENSITY_THRESHOLD ? 
                      fireGrid.getIntensityAt(x, y) : 0;
        }
        
        return distanceToTarget + fireRisk;
    }

    public void extinguishFire() {
        for (int dx = -EXTINGUISH_RADIUS; dx <= EXTINGUISH_RADIUS; dx++) {
            for (int dy = -EXTINGUISH_RADIUS; dy <= EXTINGUISH_RADIUS; dy++) {
                int newX = x + dx;
                int newY = y + dy;
                if (isValidPosition(newX, newY)) {
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