public class Firefighter extends Robot {
    private static final int VISION_RANGE = 3;
    private static final int EXTINGUISH_RADIUS = 10;
    private static final double EXTINGUISH_AMOUNT = 40.0;
    private static final int MAX_EXTINGUISHING_TIME = 20;
    private static final int MAX_MOVING_TIME = 20;
    private static final double MAX_WATER = 100.0;
    private static final double WATER_USE_RATE = 50.0;
    private static final long WATER_REFILL_TIME = 2000;

    private double currentWater;
    private long waterRefillStartTime;
    private FireGrid fireGrid;
    private int targetX = -1;
    private int targetY = -1;
    private int extinguishingTime = 0;
    private int movingTime = 0;

    public Firefighter(int id, int x, int y, int gridWidth, int gridHeight) {
        super(id, x, y, gridWidth, gridHeight);
        this.currentWater = MAX_WATER;
    }

    @Override
    public String getType() {
        return TYPE_FIREFIGHTER;
    }

    private int[] findNearestFireInLocalMap() {
        int nearestX = -1;
        int nearestY = -1;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < localKnowledge.length; i++) {
            for (int j = 0; j < localKnowledge[0].length; j++) {
                if (localKnowledge[i][j] > FireGrid.INTENSITY_THRESHOLD) {
                    double distance = Math.sqrt(Math.pow(i - x, 2) + Math.pow(j - y, 2));
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestX = i;
                        nearestY = j;
                    }
                }
            }
        }

        return (nearestX != -1) ? new int[]{nearestX, nearestY} : null;
    }

    @Override
    public void updateState(HeadQuarters hq) {

        if (currentState == State.RECHARGING_ELECTRICITY) {
            if (isRechargeComplete()) {
                finishRecharge();
            }
            return;
        }

        if (needsRecharge() && currentState != State.MOVING_TO_HQ && !isAtHQ()) {
            currentState = State.MOVING_TO_HQ;
            returnToHQ();
            return;
        }

        if (isAtHQ()) {
            localKnowledge = hq.getGlobalMap();
        }

        if (currentState == State.AT_HQ) {
            if (needsRecharge()) {
                startRecharge();
            } else {
                int[] fireLocation = findNearestFireInLocalMap();
                if (fireLocation != null) {
                    targetX = fireLocation[0];
                    targetY = fireLocation[1];
                    extinguishingTime = 0;
                    movingTime = 0;
                    currentState = State.MOVING_TO_FIRE;
                    moveSmartlyTowards(targetX, targetY);
                }
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
                if (needsRecharge()) {
                    startRecharge();
                } else {
                    currentState = State.AT_HQ;
                }
            }
        }
        
    }

    private void startWaterRefill() {
        waterRefillStartTime = System.currentTimeMillis();
        currentState = State.RECHARGING_WATER;
    }

    private boolean isWaterRefillComplete() {
        if (currentState != State.RECHARGING_WATER) {
            return false;
        }
        return System.currentTimeMillis() - waterRefillStartTime >= WATER_REFILL_TIME;
    }

    private void finishWaterRefill() {
        currentWater = MAX_WATER;
    }

    @Override
    public String getStatusDescription() {
        StringBuilder status = new StringBuilder();
        
        if (currentState == State.RECHARGING_ELECTRICITY) {
            status.append(String.format("Recharging (%.0f%%)", getEnergyPercentage()));
        } else if (currentState == State.RECHARGING_WATER) {
            status.append(String.format("Refilling water (%.0f%%)", getWaterRefillPercentage()));
        } else {
            status.append(currentState.toString());
        }

        status.append(String.format(" [E:%.0f%% W:%.0f%%]", getEnergyPercentage(), getWaterPercentage()));
        
        return status.toString();
    }

    private double getWaterRefillPercentage() {
        long refillTime = System.currentTimeMillis() - waterRefillStartTime;
        return Math.min(100.0, (refillTime * 100.0) / WATER_REFILL_TIME);
    }

    public double getWaterPercentage() {
        return (currentWater / MAX_WATER) * 100.0;
    }

    private boolean isNearFireDirect() {
        for (int dx = -VISION_RANGE; dx <= VISION_RANGE; dx++) {
            for (int dy = -VISION_RANGE; dy <= VISION_RANGE; dy++) {
                int newX = x + dx;
                int newY = y + dy;
                if (isValidPosition(newX, newY)) {
                    if (localKnowledge[newX][newY] > FireGrid.INTENSITY_THRESHOLD) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void returnToHQ() {
        targetX = HeadQuarters.getHqX();
        targetY = HeadQuarters.getHqY();
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
        double fireRisk = localKnowledge[x][y] > FireGrid.INTENSITY_THRESHOLD ? 
                         localKnowledge[x][y] : 0;
        
        return distanceToTarget + (fireRisk * 0.5);
    }

    public void extinguishFire() {
        if (needsRecharge()) {
            returnToHQ();
            return;
        }

        // double waterUsed = WATER_USE_RATE * (300.0 / 1000.0);
        // currentWater = Math.max(0, currentWater - waterUsed);

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