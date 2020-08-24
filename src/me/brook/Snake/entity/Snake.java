package me.brook.Snake.entity;

import java.awt.Color;
import java.awt.Point;
import java.util.Random;

import me.brook.NetworkLibrary.Maths;
import me.brook.NetworkLibrary.Network;
import me.brook.NetworkLibrary.tech.Matrix;
import me.brook.NetworkLibrary.tools.Tools;
import me.brook.Snake.Mechanics;

public class Snake {

	private static int STARVE_STEPS = 100;

	private SnakeBody[] body;
	public int movementDirection;

	private Color bodyColor;
	private boolean isSnakeAlive = true;

	private Network ai;
	private int total_movements = 0;
	private int turnsUntilStarvation = STARVE_STEPS, movementLeftOvers;
	private int ticksSinceDeath;

	public Snake(int species, int spawnX, int spawnY, Color color, Network ai) {
		this.ai = ai;
		this.bodyColor = color;

		Random rnd = Maths.generateRandom();
		int direction = rnd.nextInt(4);

		body = new SnakeBody[2];
		body[0] = new SnakeBody(direction, spawnX, spawnY);
		Point p = locationInDirection(body[0], direction);
		;
		body[1] = new SnakeBody(direction, p.x, p.y);
		movementDirection = direction;
	}

	/**
	 * 
	 * @return if two segments share a position
	 */
	public Point update(Point berry, int number) {
		if(!isSnakeAlive) {
			ticksSinceDeath++;
			return new Point(getHeadSegment().x, getHeadSegment().y);
		}

		int move = Tools.arrayToNumber(ai.forward(getInputs(berry)), -1.0f);
		
		// The snake has X movements until it dies.
		if(turnsUntilStarvation-- <= 0) {
			kill();
		}

		if(move != getHeadSegment().getOppositeDirection() && move != getHeadSegment().getDirection() && move != -1) {
			movementDirection = move;
		}

		move();

		total_movements++;

		return new Point(getHeadSegment().x, getHeadSegment().y);
	}

	private void move() {
		SnakeBody[] temp = new SnakeBody[body.length];
		System.arraycopy(body, 0, temp, 0, body.length);

		SnakeBody cHead = body[0];
		SnakeBody cEnd = body[getLength() - 1];

		Point p = locationInDirection(cHead, movementDirection);
		cEnd.x = p.x;
		cEnd.y = p.y;
		temp[0] = cEnd; // Now the end is the head
		cEnd.setDirection(movementDirection);

		// Move the pieces. 0 goes to 1, 1 goes to 2, etc
		for(int i = 1; i < temp.length; i++) {
			temp[i] = body[i - 1];
		}

		body = temp;

		for(SnakeBody alpha : body) {
			// If segment is outside the screen
			int x = alpha.getX();
			int y = alpha.getY();

			if(x < 0 || x >= Mechanics.tiles ||
					y < 0 || y >= Mechanics.tiles) {
				kill();
			}

			// If two segments share a position
			for(SnakeBody beta : body) {
				if(alpha != beta && x == beta.getX() && y == beta.getY()) {
					kill();
				}
			}
		}
	}

	private boolean isTileOOB(Point tile) {
		return tile.x < 0 || tile.x >= Mechanics.tiles ||
				tile.y < 0 || tile.y >= Mechanics.tiles;
	}

	/**
	 * 
	 * @return null if nothing found otherwise returns segment on tile
	 */
	private boolean isBodyOnTile(Point point) {
		for(int i = 0; i < body.length; i++) {
			SnakeBody seg = body[i];
			if(point.x == seg.x && point.y == seg.y) {
				return true;
			}
		}
		return false;
	}

	public void addSegment() {
		// Create new body
		SnakeBody[] extended = new SnakeBody[body.length + 1];
		// Fill new body with old segments
		for(int i = 0; i < body.length; i++) {
			extended[i] = body[i];
		}

		Point p = locationInDirection(getEndSegment(), getEndSegment().getOppositeDirection());
		extended[extended.length - 1] = new SnakeBody(getEndSegment().getDirection(), p.x, p.y);

		body = extended;
		movementLeftOvers += turnsUntilStarvation;
		turnsUntilStarvation = STARVE_STEPS;
	}

	public int getLength() {
		return body.length;
	}

	public SnakeBody getHeadSegment() {
		return body[0];
	}

	public SnakeBody getEndSegment() {
		return body[body.length - 1];
	}

	public SnakeBody[] getBody() {
		return body;
	}

	public Point locationInDirection(SnakeBody body, int direction) {
		return locationInDirection(body.getX(), body.getY(), direction);
	}

	public Point locationInDirection(int x, int y, int direction) {
		int nx = x;
		int ny = y;
		switch(direction) {
			case 0 :
				ny -= 1;
				break;
			case 1 :
				nx += 1;
				break;
			case 2 :
				ny += 1;
				break;
			case 3 :
				nx -= 1;
				break;
		}

		return new Point(nx, ny);
	}

	public Network getNetwork() {
		return ai;
	}

	public Color getBodyColor() {
		return bodyColor;
	}

	private Matrix getInputs(Point berry) {
		Matrix inputs = new Matrix(new double[ai.neurons[0]][1], 0.0f);

		Point head = new Point(getHeadSegment().getX(), getHeadSegment().getY());

		int start = getHeadSegment().getDirection();
		boolean first = true;
		int i = 0;
		
		// Get distance to food, then wall, then direction
//		for(int direction = start; direction != start || first; direction = (direction + 1) % vectors.length) {
		for(int direction = 0; direction < vectors.length; direction++) {
			first = false;
			// Check if the direction is opposite of the first. If so, then skip it.
			if(direction == getHeadSegment().getOppositeDirection()) {
				continue;
			}
			int[] vector = vectors[direction];

			double foodDistance = -1;
			double bodyDistance = -1;
			double wallDistance = 0;
			
			for(int d = 1; d < Mechanics.tiles + 1; d++) { // To get the wall distance.
				Point tile = new Point(vector[0] * d + head.x, vector[1] * d + head.y);
				double distance = head.distance(tile);
				
				if(berry.equals(tile)) {
					foodDistance = 1.0 / distance;
				}

				if(isBodyOnTile(tile)) {
					bodyDistance = 1.0 / distance;
				}

				if(isTileOOB(tile)) {
					wallDistance = 1.0 / distance;
					break;
				}

			}

			inputs.matrix[i * 3 + 0][0] = foodDistance;
			inputs.matrix[i * 3 + 1][0] = bodyDistance;
			inputs.matrix[i * 3 + 2][0] = wallDistance;

			i++;
		}

		return inputs;
	}

	public static int[][] vectors = {
			{ 0, -1 }, // up
			// { 1, -1 }, // up right
			{ 1, 0 }, // right
			// { 1, 1 }, // down right
			{ 0, 1 }, // down
			// { -1, 1 }, // down left
			{ -1, 0 }, // left
			// { -1, -1 }, // up left
	};

	public void setBodyColor(Color color) {
		this.bodyColor = color;
	}

	public int getSpareMovements() {
		return movementLeftOvers;
	}

	public boolean isSnakeAlive() {
		return isSnakeAlive;
	}

	public int getX() {
		return getHeadSegment().x;
	}

	public int getY() {
		return getHeadSegment().y;
	}

	public int getTotalMovements() {
		return total_movements;
	}

	public int getTicksSinceDeath() {
		return ticksSinceDeath;
	}

	public void kill() {
		this.isSnakeAlive = false;
	}

	public long getFitness() {
		long fitness = 0;

		fitness = getTotalMovements();
		fitness *= getLength();
		fitness *= Math.max(getSpareMovements(), 1);

		return fitness;
	}

	/**
	 * 
	 * @return the amount of movements per step
	 */
	public double getEffiency() {
		return getLength() / getTotalMovements();
	}

}
