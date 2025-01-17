import java.util.ArrayList;
import java.util.List;

public abstract class Robot {

    protected enum State {
        AT_HQ, MOVING_TO_FIRE, MOVING_TO_HQ, EXTINGUISHING, SCOUTING
    }

    protected int id;
    protected int x;
    protected int y;
    protected List<FireSpot> discoveredFires;
    protected double[][] localKnowledge;
    protected State currentState = State.AT_HQ;
    
    // Robot types
    public static final String TYPE_SCOUT = "scout";
    public static final String TYPE_FIREFIGHTER = "firefighter";
    

    public Robot(int id, int x, int y, int gridWidth, int gridHeight) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.discoveredFires = new ArrayList<>();
        this.localKnowledge = new double[gridWidth][gridHeight];
    }

    // Abstract method to be implemented by specific robot types
    public abstract void updateState(HeadQuarters hq);
    public abstract String getType();

    protected boolean isAtHQ() {
        boolean atHQ = x == 15 && y == 15;
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
        }
    }

    protected boolean isValidPosition(int x, int y) {
        return x >= 0 && x < localKnowledge.length && y >= 0 && y < localKnowledge[0].length;
    }
    
    // Getters and setters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getId() { return id; }

}

