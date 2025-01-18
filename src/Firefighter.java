import java.util.ArrayList;
import java.util.List;

public class Firefighter extends Robot {
    // Declaration de constantes
    private static final int VISION_RANGE = 3;
    // Rayon d'extinction du feu
    private static final int EXTINGUISH_RADIUS = 10;
    // Quantité d'eau utilisée pour éteindre le feu
    private static final double EXTINGUISH_AMOUNT = 35.0;
    // Capacité maximale d'eau du robot
    private static final double MAX_WATER = 100.0;
    // Taux d'utilisation de l'eau
    private static final double WATER_USE_RATE = 50.0;
    // Temps de recharge de l'eau
    private static final long WATER_REFILL_TIME = 2000;

    // Declaration des attributs
    private double currentWater;
    private long waterRefillStartTime;
    private FireGrid fireGrid;
    private int targetX = -1;
    private int targetY = -1;

    public Firefighter(int id, int x, int y, int gridWidth, int gridHeight) {
        super(id, x, y, gridWidth, gridHeight);
        this.currentWater = MAX_WATER;
    }

    @Override
    public String getType() {
        return TYPE_FIREFIGHTER;
    }

    // ===== Mettre à jour le status =====
    @Override
    public void updateState(HeadQuarters hq) {
        // Mettre à jour l'état de l'électricité
        if (handleChargingElectricityState()) {
            return;
        }

        // Mettre à jour l'état de l'eau
        if (handleChargingWaterState()) {
            return;
        }

        // Vérifier si le robot a besoin de se recharger en électricité
        if (handleNeedsElectricityRecharge()) {
            return;
        }

        // Vérifier si le robot a besoin de se recharger en eau
        if (handleNeedsWaterRefill()) {
            return;
        }

        // Mettre à jour les connaissances locales
        if (isAtHQ()) {
            localKnowledge = hq.getGlobalMap();
            handleAtHQState();
            operationStartTime = System.currentTimeMillis();
        }

        if (currentState == State.MOVING_TO_FIRE) {
            handleMovingToFireState(hq);
        } else if (currentState == State.EXTINGUISHING) {
            handleExtinguishingState(hq);
        } else if (currentState == State.MOVING_TO_HQ) {
            handleMovingToHQState(hq);
        }
    }

    private boolean handleChargingElectricityState() {
        if (currentState == State.RECHARGING_ELECTRICITY) {
            if (isRechargeComplete()) {
                finishRecharge();
            }
            return true;
        }
        return false;
    }

    private boolean handleNeedsElectricityRecharge() {
        if (needsRecharge() && currentState != State.MOVING_TO_HQ && !isAtHQ()) {
            currentState = State.MOVING_TO_HQ;
            returnToHQ();
            return true;
        }
        return false;
    }

    private boolean handleChargingWaterState() {
        if (currentState == State.RECHARGING_WATER) {
            if (isWaterRefillComplete()) {
                finishWaterRefill();
                currentState = State.AT_HQ;
            }
            return true;
        }
        return false;
    }
    
    private boolean handleNeedsWaterRefill() {
        if (currentWater < WATER_USE_RATE && currentState != State.MOVING_TO_HQ && !isAtHQ()) {
            currentState = State.MOVING_TO_HQ;
            returnToHQ();
            return true;
        }
        return false;
    }

    private void handleAtHQState() {
        if (needsRecharge() || currentWater < MAX_WATER*0.5) {
            if (needsRecharge()) {
                startRecharge();
            }
            if (currentWater < MAX_WATER*0.5) {
                startWaterRefill();
            }
        } else {
            int[] fireLocation = findNearestFireInLocalMap();
            if (fireLocation != null) {
                targetX = fireLocation[0];
                targetY = fireLocation[1];
                currentState = State.MOVING_TO_FIRE;
                moveSmartlyTowards(targetX, targetY);
            }
        }
    }

    private void handleMovingToFireState(HeadQuarters hq) {
        if (isNearFireDirect()) {
            currentState = State.EXTINGUISHING;
            extinguishFire(hq);
        } else {
            moveSmartlyTowards(targetX, targetY);
        }
    }
    
    private void handleExtinguishingState(HeadQuarters hq) {
        if (isNearFireDirect()) {
            extinguishFire(hq);
        } else {
            returnToHQ();
        }
    }

    private void handleMovingToHQState(HeadQuarters hq) {
        moveSmartlyTowards(hq.getX(), hq.getY());
        if (isAtHQ()) {
            if (needsRecharge() || currentWater < MAX_WATER*0.5) {
                if (needsRecharge()) {
                    startRecharge();
                }
                if (currentWater < MAX_WATER*0.5) {
                    startWaterRefill();
                }
            } else {
                currentState = State.AT_HQ;
            }
        }
    }

    // Trouver le feu le plus proche dans la connaissance locale
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

    // Vérifier si le feu est proche
    private boolean isNearFireDirect() {
        for (int dx = -VISION_RANGE; dx <= VISION_RANGE; dx++) {
            for (int dy = -VISION_RANGE; dy <= VISION_RANGE; dy++) {
                int newX = x + dx;
                int newY = y + dy;
                if (isValidPosition(newX, newY) && 
                    fireGrid.getIntensity(newX, newY) > FireGrid.INTENSITY_THRESHOLD) {
                    return true;
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

    // Déplacer le robot vers la cible de manière intelligente
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
    
    // Calculer le risque de mouvement
    private double calculateMovementRisk(int x, int y, int targetX, int targetY) {
        double distanceToTarget = Math.sqrt(Math.pow(x - targetX, 2) + Math.pow(y - targetY, 2));
        double fireRisk = localKnowledge[x][y] > FireGrid.INTENSITY_THRESHOLD ? 
                         localKnowledge[x][y] : 0;
        
        return distanceToTarget + (fireRisk * 0.5);
    }

    // Éteindre le feu
    public void extinguishFire(HeadQuarters hq) {
        if (needsRecharge() || currentWater <= 0) {
            returnToHQ();
            return;
        }

        double waterNeeded = WATER_USE_RATE * (300.0 / 1000.0);
        if (currentWater < waterNeeded) {
            returnToHQ();
            return;
        }

        currentWater = Math.max(0, currentWater - waterNeeded);

        List<FireSpot> extinguishedFires = new ArrayList<>();
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
                            if (fireGrid.getIntensity(newX, newY) <= FireGrid.INTENSITY_THRESHOLD) {
                                extinguishedFires.add(new FireSpot(newX, newY, 0, System.currentTimeMillis()));
                            }
                        }
                    }
                }
            }
        }

        if (!extinguishedFires.isEmpty()) {
            hq.receiveFireReport(id, extinguishedFires);
        }
    }

    // ===== Gestion de l'eau =====
    // Début de la recharge de l'eau
    private void startWaterRefill() {
        waterRefillStartTime = System.currentTimeMillis();
        currentState = State.RECHARGING_WATER;
    }

    // Vérifier si la recharge de l'eau est terminée
    private boolean isWaterRefillComplete() {
        if (currentState != State.RECHARGING_WATER) {
            return false;
        }
        return System.currentTimeMillis() - waterRefillStartTime >= WATER_REFILL_TIME;
    }

    // Fin de la recharge de l'eau
    private void finishWaterRefill() {
        currentWater = MAX_WATER;
    }

    // ===== Getters et setters =====
    public double getWaterPercentage() {
        return (currentWater / MAX_WATER) * 100.0;
    }

    private double getWaterRefillPercentage() {
        long refillTime = System.currentTimeMillis() - waterRefillStartTime;
        return Math.min(100.0, (refillTime * 100.0) / WATER_REFILL_TIME);
    }


    @Override
    public String getStatusDescription() {
        StringBuilder status = new StringBuilder();
        
        boolean isCharging = currentState == State.RECHARGING_ELECTRICITY;
        boolean isRefilling = currentWater < MAX_WATER*0.5;
        
        if (isCharging && isRefilling) {
            status.append(String.format("Recharging & Refilling (E:%.0f%% W:%.0f%%)", 
                getEnergyPercentage(), getWaterRefillPercentage()));
        } else if (isCharging) {
            status.append(String.format("Recharging (%.0f%%)", getEnergyPercentage()));
        } else if (isRefilling) {
            status.append(String.format("Refilling water (%.0f%%)", getWaterRefillPercentage()));
        } else {
            status.append(currentState.toString());
        }
    
        status.append(String.format(" [E:%.0f%% W:%.0f%%]", getEnergyPercentage(), getWaterPercentage()));
        
        return status.toString();
    }

    public void setFireGrid(FireGrid fireGrid) {
        this.fireGrid = fireGrid;
    }
}