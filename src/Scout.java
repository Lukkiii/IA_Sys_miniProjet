import java.util.*;

/**
 * Classe Scout - Robot éclaireur pour la détection des incendies
 * Cette classe gère un robot qui explore la zone pour détecter et signaler les incendies
 */
public class Scout extends Robot {
    // ==== Constantes ====
    private static final int VISION_RANGE = 5;
    private static final int MAX_PREVIOUS_TARGETS = 5;
    private static final long FIRE_RECHECK_INTERVAL = 5000;
    private static final double FIRE_RECHECK_PROBABILITY = 0.5;

    // ==== Variables d'instance ====
    private int targetX;
    private int targetY;
    private Random random;
    private FireGrid fireGrid;
    private boolean[][] exploredAreas;
    private List<int[]> previousTargets;
    private Map<Point, Long> fireLocations;

    /**
     * Constructeur du Scout
     */
    public Scout(int id, int x, int y, int gridWidth, int gridHeight) {
        super(id, x, y, gridWidth, gridHeight);
        this.random = new Random();
        this.exploredAreas = new boolean[gridWidth][gridHeight];
        this.fireLocations = new HashMap<>();
        this.previousTargets = new ArrayList<>();
        setNewExplorationTarget();
    }

    @Override
    public String getType() {
        return TYPE_SCOUT;
    }

    // ==== Méthodes de mise à jour d'état ====
    @Override
    public void updateState(HeadQuarters hq) {
        // Gestion de la recharge
        if (handleRecharging(hq)) {
            return;
        }

        // Rapport des incendies détectés
        reportFiresIfFound(hq);

        // Mise à jour des connaissances si au QG
        if (isAtHQ()) {
            localKnowledge = hq.getGlobalMap();
        }

        // Mise à jour de l'exploration
        updateExploration();

        // Nettoyage des anciennes positions d'incendie
        cleanupOldFireLocations();
    }

    /**
     * Gère l'état de recharge du robot
     * @return true si le robot est en cours de recharge
     */
    private boolean handleRecharging(HeadQuarters hq) {
        if (currentState == State.RECHARGING_ELECTRICITY) {
            if (isRechargeComplete()) {
                finishRecharge();
            }
            return true;
        }

        if (needsRecharge() && !isAtHQ()) {
            currentState = State.MOVING_TO_HQ;
            moveTowards(hq.getX(), hq.getY());
            return true;
        }

        if (isAtHQ() && needsRecharge()) {
            startRecharge();
            return true;
        }

        return false;
    }

    // ==== Méthodes de gestion des incendies ====
    /**
     * Signale les incendies détectés au QG
     */
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

    /**
     * Scan la zone autour du robot pour détecter les incendies
     */
    private List<FireSpot> scanArea() {
        for (int dx = -VISION_RANGE; dx <= VISION_RANGE; dx++) {
            for (int dy = -VISION_RANGE; dy <= VISION_RANGE; dy++) {
                int newX = x + dx;
                int newY = y + dy;
                
                if (isValidPosition(newX, newY)) {
                    double intensity = observeFireIntensity(newX, newY);
                    if (intensity > FireGrid.INTENSITY_THRESHOLD) {
                        discoveredFires.add(new FireSpot(newX, newY, intensity, System.currentTimeMillis()));
                    }
                }
            }
        }
        return new ArrayList<>(discoveredFires);
    }

    // ==== Méthodes de gestion de l'exploration ====
    /**
     * Met à jour l'exploration du robot
     */
    private void updateExploration() {
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
    }

    /**
     * Marque les zones explorées sur la carte
     */
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

    // ==== Méthodes de déplacement ====
    /**
     * Déplace le robot vers sa cible actuelle
     */
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

    /**
     * Évalue la qualité d'un déplacement possible
     */
    private double evaluateMove(int newX, int newY) {
        double distanceToTarget = -Math.sqrt(Math.pow(newX - targetX, 2) + Math.pow(newY - targetY, 2));
        double explorationBonus = exploredAreas[newX][newY] ? -5 : 5;
        return distanceToTarget + explorationBonus;
    }

    // ==== Méthodes de gestion des cibles ====
    /**
     * Définit une cible pour revérifier un ancien incendie
     */
    private void setFireRecheckTarget() {
        long currentTime = System.currentTimeMillis();
        Point bestTarget = null;
        long oldestCheck = currentTime;
        
        // Recherche l'incendie le plus ancien
        for (Map.Entry<Point, Long> entry : fireLocations.entrySet()) {
            if (entry.getValue() < oldestCheck) {
                oldestCheck = entry.getValue();
                bestTarget = entry.getKey();
            }
        }
        
        // Si un incendie assez ancien est trouvé, on le définit comme cible
        if (bestTarget != null && (currentTime - oldestCheck) >= FIRE_RECHECK_INTERVAL) {
            targetX = bestTarget.x;
            targetY = bestTarget.y;
        } else {
            setNewExplorationTarget();
        }
    }

    /**
     * Définit une nouvelle cible d'exploration
     */
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
            int candidateX = random.nextInt(HeadQuarters.getGridWidth());
            int candidateY = random.nextInt(HeadQuarters.getGridHeight());
            
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

    /**
     * Évalue la qualité d'une cible potentielle
     */
    private double evaluateTarget(int tx, int ty) {
        double unexploredScore = countUnexploredAround(tx, ty);
        double previousTargetsPenalty = getPreviousTargetsPenalty(tx, ty);
        double hqDistancePenalty = -Math.sqrt(Math.pow(tx - 15, 2) + Math.pow(ty - 15, 2)) / 10;
        
        return unexploredScore + previousTargetsPenalty + hqDistancePenalty;
    }

    /**
     * Compte le nombre de cases inexplorées autour d'une position
     */
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

    /**
     * Calcule la pénalité basée sur la proximité aux cibles précédentes
     */
    private double getPreviousTargetsPenalty(int tx, int ty) {
        double penalty = 0;
        for (int[] prev : previousTargets) {
            double distance = Math.sqrt(Math.pow(tx - prev[0], 2) + Math.pow(ty - prev[1], 2));
            penalty -= 10.0 / (distance + 1);
        }
        return penalty;
    }

    // ==== Méthodes utilitaires ====
    private boolean hasReachedTarget() {
        return Math.abs(x - targetX) <= 1 && Math.abs(y - targetY) <= 1;
    }

    private boolean shouldChangeTarget() {
        return random.nextDouble() < 0.05;
    }

    private double observeFireIntensity(int x, int y) {
        if (fireGrid != null) {
            return fireGrid.getIntensityAt(x, y);
        }
        return 0.0;
    }

    private void cleanupOldFireLocations() {
        fireLocations.entrySet().removeIf(entry -> {
            Point p = entry.getKey();
            return fireGrid.getIntensityAt(p.x, p.y) <= FireGrid.INTENSITY_THRESHOLD;
        });
    }

    // ==== Getters et Setters ====
    public void setFireGrid(FireGrid fireGrid) {
        this.fireGrid = fireGrid;
    }

    public FireGrid getFireGrid() {
        return fireGrid;
    }
}