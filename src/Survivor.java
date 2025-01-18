public class Survivor {
    int id;
    int x, y;
    boolean rescued;
    
    public Survivor(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.rescued = false;
    }

    public int getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isRescued() { return rescued; }
    public void setRescued(boolean rescued) { this.rescued = rescued; }
}