import java.util.ArrayList;
import java.util.List;

public abstract class Robot {

    protected enum State {
        AT_HQ, MOVING_TO_FIRE, MOVING_TO_HQ, EXTINGUISHING, SCOUTING, RECHARGING_WATER, RECHARGING_ELECTRICITY
    }

    protected static final long MAX_OPERATION_TIME = 3000;
    protected static final long RECHARGE_TIME = 2000;

    // Robot types
    public static final String TYPE_SCOUT = "scout";
    public static final String TYPE_FIREFIGHTER = "firefighter";
    
    protected int id;
    protected int x;
    protected int y;
    protected List<FireSpot> discoveredFires;
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

    // Abstract method to be implemented by specific robot types
    public abstract void updateState(HeadQuarters hq);
    public abstract String getType();

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

    protected boolean isAtHQ() {
        boolean atHQ = x == HeadQuarters.HQ_X  && y == HeadQuarters.HQ_Y;
        if (atHQ) {
            currentState = State.AT_HQ;
        }
        return atHQ;
    }

    protected void updateLocalKnowledge(HeadQuarters hq) {
        if (isAtHQ()) {
            double[][] globalMap = hq.getGlobalMap();
            for (int i = 0; i < localKnowledge.length; i++) {
                for (int j = 0; j < localKnowledge[0].length; j++) {
                    localKnowledge[i][j] = globalMap[i][j];
                }
            }
            operationStartTime = System.currentTimeMillis();
        }
    }

    protected boolean isValidPosition(int x, int y) {
        return x >= 0 && x < localKnowledge.length && y >= 0 && y < localKnowledge[0].length;
    }

    protected boolean needsRecharge() {
        if (currentState == State.RECHARGING_ELECTRICITY) {
            return false;
        }
        return System.currentTimeMillis() - operationStartTime >= MAX_OPERATION_TIME;
    }

    protected boolean isRechargeComplete() {
        return System.currentTimeMillis() - rechargeStartTime >= RECHARGE_TIME;
    }

    protected void startRecharge() {
        rechargeStartTime = System.currentTimeMillis();
        currentState = State.RECHARGING_ELECTRICITY;
    }

    protected void finishRecharge() {
        operationStartTime = System.currentTimeMillis();
        currentState = State.AT_HQ;
    }

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
    
    // Getters and setters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getId() { return id; }

}

