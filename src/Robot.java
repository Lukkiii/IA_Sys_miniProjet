public class Robot {
    private int x;
    private int y;
    private String state;
    private static final int EXTINGUISH_RADIUS = 10;
    private static final double EXTINGUISH_AMOUNT = 35.0;

    public Robot(int x, int y) {
        this.x = x;
        this.y = y;
        this.state = "searching";
    }

    public void moveTowards(int targetX, int targetY) {
        if (x < targetX) x++;
        else if (x > targetX) x--;

        if (y < targetY) y++;
        else if (y > targetY) y--;
    }

    public void extinguishFire(FireGrid grid) {
        int gridWidth = grid.getWidth();
        int gridHeight = grid.getHeight();
        
        for (int dx = -EXTINGUISH_RADIUS; dx <= EXTINGUISH_RADIUS; dx++) {
            for (int dy = -EXTINGUISH_RADIUS; dy <= EXTINGUISH_RADIUS; dy++) {
                int newX = x + dx;
                int newY = y + dy;
                
                if (newX >= 0 && newX < gridWidth && newY >= 0 && newY < gridHeight) {
                    
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance <= EXTINGUISH_RADIUS) {
                        
                        double effect = EXTINGUISH_AMOUNT * (1.0 - (distance / (EXTINGUISH_RADIUS + 1)));
                        grid.decreaseIntensity(newX, newY, effect);
                    }
                }
            }
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public void setState(String state) { this.state = state; }
    public String getState() { return state; }
}

