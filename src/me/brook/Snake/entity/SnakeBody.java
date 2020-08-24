package me.brook.Snake.entity;

public class SnakeBody {
	
	// 0=up, 1=right, 2=down, 3=left
	private int direction = 0;
	
	public int x, y;

	public SnakeBody(int direction, int x, int y) {
		this.direction = direction;
		this.x = x;
		this.y = y;
	}
	
	public int getOppositeDirection() {
		return getOppositeDirection(direction);
	}

	private static int getOppositeDirection(int direction) {
		direction += 2;
		direction %= 4;
		return direction;
	}

	public int getDirection() {
		return direction;
	}
	
	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}
	
	

}
