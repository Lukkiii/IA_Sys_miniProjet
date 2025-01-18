import java.util.ArrayList;
import java.util.List;

public abstract class Robot {

    // Enumération des états possibles pour un robot
    protected enum State {
        AT_HQ, MOVING_TO_FIRE, MOVING_TO_HQ, EXTINGUISHING, SCOUTING, RECHARGING_WATER, RECHARGING_ELECTRICITY
    }
    // Temps d'activité d'un robot
    protected static final long MAX_OPERATION_TIME = 3000;
    // Temps de recharge d'un robot
    protected static final long RECHARGE_TIME = 2000;

    // Types de robots
    public static final String TYPE_SCOUT = "scout";
    public static final String TYPE_FIREFIGHTER = "firefighter";
    
    protected int id;
    protected int x;
    protected int y;
    // Liste des feux découverts par le robot
    protected List<FireSpot> discoveredFires;
    // Connaissance locale du robot
    protected double[][] localKnowledge;
    protected State currentState = State.AT_HQ;
    protected long operationStartTime;
    protected long rechargeStartTime;

    public Robot(int id, int x, int y, int gridWidth, int gridHeight) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.discoveredFires = new ArrayList<>();
        this.localKnowledge = new double[gridWidth][gridHeight];
        this.operationStartTime = System.currentTimeMillis();
    }

    public abstract void updateState(HeadQuarters hq);
    public abstract String getType();

    // Déplace le robot vers une position cible
    protected void moveTowards(int targetX, int targetY) {
        if (x < targetX) {
            x++;
        } else if (x > targetX) {
            x--;
        } else if (y < targetY) {
            y++;
        } else if (y > targetY) {
            y--;
        }
    }

    // Vérifie si le robot est au QG
    protected boolean isAtHQ() {
        boolean atHQ = x == HeadQuarters.HQ_X  && y == HeadQuarters.HQ_Y;
        if (atHQ) {
            currentState = State.AT_HQ;
        }
        return atHQ;
    }

    protected boolean isValidPosition(int x, int y) {
        return x >= 0 && x < localKnowledge.length && y >= 0 && y < localKnowledge[0].length;
    }

    // Vérifie si le robot a besoin de se recharger
    protected boolean needsRecharge() {
        if (currentState == State.RECHARGING_ELECTRICITY) {
            return false;
        }
        return System.currentTimeMillis() - operationStartTime >= MAX_OPERATION_TIME;
    }

    // Vérifie si la recharge est terminée
    protected boolean isRechargeComplete() {
        return System.currentTimeMillis() - rechargeStartTime >= RECHARGE_TIME;
    }

    // Démarre la recharge
    protected void startRecharge() {
        rechargeStartTime = System.currentTimeMillis();
        currentState = State.RECHARGING_ELECTRICITY;
    }

    // Termine la recharge
    protected void finishRecharge() {
        operationStartTime = System.currentTimeMillis();
        currentState = State.AT_HQ;
    }

    // Getter
    public double getEnergyPercentage() {
        if (currentState == State.RECHARGING_ELECTRICITY) {
            long rechargingTime = System.currentTimeMillis() - rechargeStartTime;
            return Math.min(100.0, (rechargingTime * 100.0) / RECHARGE_TIME);
        } else {
            long operationTime = System.currentTimeMillis() - operationStartTime;
            return Math.max(0.0, 100.0 - (operationTime * 100.0) / MAX_OPERATION_TIME);
        }
    }

    public String getStatusDescription() {
        if (currentState == State.RECHARGING_ELECTRICITY) {
            return String.format("Recharging (%.0f%%)", getEnergyPercentage());
        }
        return currentState.toString();
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getId() { return id; }

}

