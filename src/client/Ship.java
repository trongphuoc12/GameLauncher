package client;

public class Ship {
    public String name; 
    public int length;
    public int x, y; 
    public boolean isHorizontal;
    public int hitCount = 0; 

    public Ship(String name, int length) {
        this.name = name;
        this.length = length;
        this.x = -1; 
        this.y = -1;
        this.isHorizontal = true;
    }

    public void rotate() {
        this.isHorizontal = !this.isHorizontal;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean contains(int px, int py) {
        if (x == -1) return false; 

        if (isHorizontal) {
            return px == this.x && py >= this.y && py < (this.y + this.length);
        } else {
            return py == this.y && px >= this.x && px < (this.x + this.length);
        }
    }

    public void registerHit() {
        hitCount++;
    }
    
    public boolean isSunk() {
        return hitCount >= length;
    }

    public String getProtocolString() {
        return String.format("%d,%s,%d,%d",
                this.length,
                (this.isHorizontal ? "H" : "V"),
                this.x,
                this.y);
    }
}