public class Survivor {
    private static final double DEATH_THRESHOLD = 80.0;
    private static final double RESCUE_THRESHOLD = 20.0;

    int id;
    int x, y;
    boolean rescued;
    private boolean dead;
    
    public Survivor(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.rescued = false;
        this.dead = false;
    }

    public void updateStatus(double fireIntensity) {
        if (!rescued && !dead) {
            if (fireIntensity >= DEATH_THRESHOLD) {
                dead = true;
                System.out.println("Survivor " + (id+1) + " died at: [" + x + "," + y + "]");
            } else if (fireIntensity <= RESCUE_THRESHOLD) {
                rescued = true;
                System.out.println("Survivor " + (id+1) + " rescued at: [" + x + "," + y + "]");
            }
        }
    }

    public int getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isRescued() { return rescued; }
    public boolean isDead() { return dead; }
    
}