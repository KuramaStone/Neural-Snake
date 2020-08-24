package me.brook.Snake;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;

import me.brook.NetworkLibrary.Maths;
import me.brook.NetworkLibrary.Network;
import me.brook.NetworkLibrary.tools.Tools;
import me.brook.Snake.entity.Berry;
import me.brook.Snake.entity.Snake;
import me.brook.Snake.entity.SnakeBody;
import me.brook.Snake.tools.ButtonValues;

public class Mechanics {

	public static int tiles = 32;
	public static int tile_size = GraphicsManager.WIDTH / tiles;

	private Snake[][] snakes;
	private Network[][] networks;
	private Berry[][] berries;
	private Color[][] colors;
	private List<Long>[] scores;
	private int[][] berryCoords;

	private Random rnd;
	private List<Long>[] averagePerGeneration;
	public long averageScoreOfBest[], highestScoreEver;
	private boolean displayBest;
	private boolean slowDown = true;
	private boolean idleEnabled;
	private int[] bestSpecimenPerSpecies;

	private int renderingMode;
	private int offsetX, offsetY;

	private long totalIteration, scale = -1;
	private int speciesToDisplay = -1;
	private boolean showFullScale = true;
	private int x, y;
	
	public long berrySeed = 0;

	@SuppressWarnings("unchecked")
	public Mechanics(SnakeEngine engine, Network[][] networks) {
		this.networks = networks;
		this.rnd = Maths.generateRandom();

		scores = new ArrayList[Info.SPECIES];
		averagePerGeneration = new ArrayList[Info.SPECIES];
		averageScoreOfBest = new long[Info.SPECIES];
		berries = new Berry[Info.SPECIES][];
		snakes = new Snake[Info.SPECIES][];
		colors = new Color[Info.SPECIES][];
		bestSpecimenPerSpecies = new int[Info.SPECIES];

		for(int s = 0; s < Info.SPECIES; s++) {
			int startingStep = rnd.nextInt(10000);
			colors[s] = new Color[Info.POPULATION[s]];
			for(int i = 0; i < Info.POPULATION[s]; i++) {
				colors[s][i] = Maths.generateVividColor(startingStep + (s * colors[s].length + i * 3));

				scores[s] = new ArrayList<>();
				averagePerGeneration[s] = new ArrayList<>();
			}
			if(Info.POPULATION[s] % 2 == 0) {
				startingStep++;
			}
		}

		this.rnd = new Random(berrySeed = rnd.nextLong());
		// Use the decoy snake to stop intersections
		x = rnd.nextInt(tiles - 2) + 1;
		y = rnd.nextInt(tiles - 2) + 1;
		Snake decoy = new Snake(-1, x, y, colors[0][0], networks[0][0]);
		berryCoords = new int[1000][2];
		for(int b = 0; b < berryCoords.length; b++) {
			int bx = rnd.nextInt(tiles);
			int by = rnd.nextInt(tiles);

			// If berry is on body, find a new spot
			while(onBody(decoy, bx, by)) {
				bx = rnd.nextInt(tiles);
				by = rnd.nextInt(tiles);
			}

			berryCoords[b][0] = bx;
			berryCoords[b][1] = by;
		}

		restart();
	}

	public void assignFitness() {
		for(int s = 0; s < Info.SPECIES; s++) {
			for(int i = 0; i < Info.POPULATION[s]; i++) {
				networks[s][i].setFitness(networks[s][i].getFitness() + snakes[s][i].getFitness());
			}
		}
	}

	public void restart() {

		for(int species = 0; species < Info.SPECIES; species++) {
			this.snakes[species] = new Snake[Info.POPULATION[species]];
			this.berries[species] = new Berry[Info.POPULATION[species]];
			this.rnd = Maths.generateRandom();

			for(int i = 0; i < Info.POPULATION[species]; i++) {
				snakes[species][i] = new Snake(species, x, y,
						colors[species][i],
						networks[species][i]);
				networks[species][i].obj = snakes[species][i];
				spawnBerry(species, i, snakes[species][i].getBodyColor(),
						berryCoords[snakes[species][i].getLength()][0], berryCoords[snakes[species][i].getLength()][1]);
			}
		}

	}

	private boolean onBody(Snake decoy, int bx, int by) {
		for(SnakeBody body : decoy.getBody()) {
			if(body.x == bx && body.y == by) {
				return true;
			}
		}
		return false;
	}

	private void spawnBerry(int species, int i, Color color, int bx, int by) {
		Point p = new Point(bx, by);
		berries[species][i] = new Berry(p, color);
	}

	public void updateGame(InputManager input) {
		inputListener(input);

		bestSpecimenPerSpecies = new int[Info.SPECIES];
		for(int s = 0; s < Info.SPECIES; s++) {

			int best = 0;
			for(int i = 0; i < snakes[s].length; i++) {
				Snake snake = snakes[s][i];

				if(snake.getLength() > best && snake.isSnakeAlive()) {
					best = snake.getLength();
					bestSpecimenPerSpecies[s] = i;
				}

				Point head = snake.update(berries[s][i].getPoint(), i);
				if(head.equals(berries[s][i].getPoint())) {
					consumeBerry(s, i);
				}
			}
		}

	}

	private void consumeBerry(int species, int number) {
		snakes[species][number].addSegment();
		spawnBerry(species, number, snakes[species][number].getBodyColor(),
				berryCoords[snakes[species][number].getLength()][0],
				berryCoords[snakes[species][number].getLength()][1]);
	}

	private void inputListener(InputManager input) {

		if(input.buttons[ButtonValues.SPACE]) {
			displayBest = !displayBest;
		}
		if(input.buttons[ButtonValues.S_KEY]) {
			slowDown = !slowDown;
		}
		if(input.buttons[ButtonValues.R_KEY]) {
			renderingMode++;
			renderingMode %= 3;
		}

		if(input.buttons[ButtonValues.UP]) {
			offsetY += 20;
		}
		if(input.buttons[ButtonValues.DOWN]) {
			offsetY -= 20;
		}
		if(input.buttons[ButtonValues.LEFT]) {
			offsetX += 20;
		}
		if(input.buttons[ButtonValues.RIGHT]) {
			offsetX -= 20;
		}
		if(input.buttons[ButtonValues.ESCAPE]) {
			setSpeciesToDisplay(-1);
			offsetX = 0;
			offsetY = 0;
		}

		if(input.buttons[ButtonValues.I_KEY]) {
			idleEnabled = !idleEnabled;
		}
		if(input.buttons[ButtonValues.F_KEY]) {
			showFullScale = !showFullScale;
		}

		if(input.buttons[ButtonValues.KEY_1]) {
			setSpeciesToDisplay(0);
		}
		else if(input.buttons[ButtonValues.KEY_2]) {
			setSpeciesToDisplay(1);
		}
		else if(input.buttons[ButtonValues.KEY_3]) {
			setSpeciesToDisplay(2);
		}
		else if(input.buttons[ButtonValues.KEY_4]) {
			setSpeciesToDisplay(3);
		}
		else if(input.buttons[ButtonValues.KEY_5]) {
			setSpeciesToDisplay(4);
		}
		else if(input.buttons[ButtonValues.KEY_6]) {
			setSpeciesToDisplay(5);
		}
		else if(input.buttons[ButtonValues.KEY_7]) {
			setSpeciesToDisplay(6);
		}
		else if(input.buttons[ButtonValues.KEY_8]) {
			setSpeciesToDisplay(7);
		}
		else if(input.buttons[ButtonValues.KEY_9]) {
			setSpeciesToDisplay(8);
		}
		else if(input.buttons[ButtonValues.KEY_0]) {
			setSpeciesToDisplay(10);
		}

		if(input.buttons[ButtonValues.SCALE_KEY]) {
			String str = JOptionPane.showInputDialog(null, "Scale: ", scale);
			str = Tools.stripLetters(str);

			if(str.isEmpty()) {
				scale = -1;
			}
			else {
				try {
					scale = Long.parseLong(str);
				}
				catch(Exception e) {
					scale = -1;
				}
			}
		}

		input.reset();
	}

	public Snake[][] getSnakes() {
		return snakes;
	}

	public boolean areAllSnakesDead() {
		for(int s = 0; s < Info.SPECIES; s++) {
			for(int i = 0; i < Info.POPULATION[s]; i++) {
				Snake snake = snakes[s][i];
				if(snake.isSnakeAlive()) {
					return false;
				}
			}
		}
		return true;
	}

	public Network[][] getNetworks() {
		return networks;
	}

	public Berry[][] getBerries() {
		return berries;
	}

	public void addScore(int species, long fitness) {
		scores[species].add(fitness);
	}

	public List<Long>[] getScores() {
		return scores;
	}

	public void addAverageScore(int species, long fitness) {
		averagePerGeneration[species].add(fitness);
	}

	public List<Long>[] getAverageScores() {
		return averagePerGeneration;
	}

	public boolean shouldBringNewPopulation() {
		return totalIteration % Info.REPLAY_NETWORKS == 0;
	}

	public boolean isSlowDownEnabled() {
		return slowDown;
	}

	public boolean isIdleEnabled() {
		return idleEnabled;
	}

	public boolean shouldDisplayBest() {
		return displayBest;
	}

	public int getRenderingMode() {
		return renderingMode;
	}

	public void setRenderingMode(int i) {
		this.renderingMode = i;
	}

	public Color[][] getColor() {
		return colors;
	}

	public void setSpeciesToDisplay(int speciesToDisplay) {
		this.speciesToDisplay = speciesToDisplay;
	}

	public int getSpeciesToDisplay() {
		return speciesToDisplay;
	}

	public void addIteration() {
		totalIteration++;
	}

	public void setNetwork(int species, Network[] newGeneration) {
		this.networks[species] = newGeneration;
	}

	public long getHighestFitnessEver() {
		return highestScoreEver;
	}

	public long getScale() {
		return scale;
	}

	public boolean showFullScale() {
		return showFullScale;
	}

	public int getBestSpecimen(int species) {
		return bestSpecimenPerSpecies[species];
	}
	
	public int getOffsetX() {
		return offsetX;
	}
	
	public int getOffsetY() {
		return offsetY;
	}

}
