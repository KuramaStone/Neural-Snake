package me.brook.Snake;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import me.brook.NetworkLibrary.Maths;
import me.brook.NetworkLibrary.Network;
import me.brook.Snake.entity.Snake;
import me.brook.Snake.entity.SnakeBody;

public class GraphicsManager extends JPanel {

	private static final long serialVersionUID = 5753780642232693553L;

	private static long MAX_SCORE = 2;
	private static final int BORDER_GAP = 55;
	private static final int Y_HATCH_CNT = 25;
	private static final Stroke graphStroke = new BasicStroke(1);
	private static final Stroke hatchStroke = new BasicStroke(2);

	private static final int tile_size = Mechanics.tile_size;
	// Keep as divisible by the tile_size
	public static final int WIDTH = 800, HEIGHT = 800;

	private Mechanics mech;
	private Font font;
	private NumberFormat nf;

	private Color DEAD_COLOR = new Color(145, 145, 145);
	private long highestScoreLastUpdate = -1;

	public GraphicsManager(SnakeEngine snake, Mechanics mech, JFrame frame) {
		this.mech = mech;

		nf = NumberFormat.getInstance();
		nf.setGroupingUsed(true);
		font = new Font("Verdana", Font.BOLD, 20);
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);

		int mode = mech.getRenderingMode();
		if(mode == 0) {
			renderGame(graphics);
		}
		else if(mode == 1) {
			try {
				renderGraph(graphics);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		else if(mode == 2) {
			drawNetwork(graphics);
		}

		graphics.dispose();
	}

	private int lastGeneration = -1, lastSpecies = -1;
	private Image map;

	private void drawNetwork(Graphics g) {
		if(mech.getSpeciesToDisplay() >= 0 && mech.getSpeciesToDisplay() < Info.SPECIES) {
			if(lastGeneration != Info.GENERATION || lastSpecies != mech.getSpeciesToDisplay()) {
				map = Network.createWeightImage(false, mech.getNetworks()[mech.getSpeciesToDisplay()]);
				// Squash it into window size
				map = map.getScaledInstance(map.getWidth(null), HEIGHT, Image.SCALE_AREA_AVERAGING);
				
				lastGeneration = Info.GENERATION;
				lastSpecies = mech.getSpeciesToDisplay();
			}

			g.setColor(Color.BLACK);
			g.fillRect(0, 0, WIDTH, HEIGHT);
			
			g.drawImage(map, mech.getOffsetX(), mech.getOffsetY(), null);
			
			g.setColor(Color.BLUE);
			g.setFont(font);
			g.drawString("Species: " + mech.getSpeciesToDisplay() + 1, 20, 20);
		}
		else {
			// Request to select a species
			String string = "Please select a species.";
			int width = g.getFontMetrics().stringWidth(string);
			g.setFont(font);
			g.setColor(Color.BLACK);
			g.drawString(string, WIDTH / 2 - width, HEIGHT / 2);
		}
	}

	private void renderGraph(Graphics graphics) {
		Graphics2D g2 = (Graphics2D) graphics;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setFont(font);

		g2.setColor(Color.WHITE);
		g2.fillRect(0, 50, WIDTH, HEIGHT);

		for(int species = 0; species < Info.SPECIES; species++) {
			g2.setColor(Maths.toTransparent(mech.getColor()[species][0], 100));
			int x = (int) Math.ceil(species * WIDTH / Info.SPECIES);
			g2.fillRect(x, 0, WIDTH / Info.SPECIES, 50);
			g2.setColor(Color.BLACK);
			g2.drawString(String.valueOf(species + 1), x + (WIDTH / Info.SPECIES) / 2, 50 - 25);
		}

		g2.setColor(Color.BLACK);
		// create x and y axes
		g2.drawLine(BORDER_GAP, getHeight() - BORDER_GAP, BORDER_GAP, BORDER_GAP);
		g2.drawLine(BORDER_GAP, getHeight() - BORDER_GAP, getWidth() - BORDER_GAP, getHeight() - BORDER_GAP);

		MAX_SCORE = (long) (highestScoreLastUpdate * 2);
		if(mech.getScale() > 0) {
			MAX_SCORE = mech.getScale();
		}

		double xScale = ((double) getWidth() - 2 * BORDER_GAP) / (Info.GENERATION / Info.GRAPH_SCALE);
		if(!mech.showFullScale()) {
			xScale = ((double) getWidth() - 2 * BORDER_GAP) / (Info.SCALE_TO_SHOW / Info.GRAPH_SCALE); 
		}
		double yScale = ((double) getHeight() - 2 * BORDER_GAP) / (MAX_SCORE - 1);

		String[] highestString = new String[Info.SPECIES];
		Point[] highestPoints = new Point[Info.SPECIES];

		highestScoreLastUpdate = 0;
		for(int species = 0; species < Info.SPECIES; species++) {
			if(mech.getSpeciesToDisplay() != -1 && mech.getSpeciesToDisplay() != species) {
				continue;
			}
			List<Long> scores = mech.getAverageScores()[species];

			int locationH = -1;
			long h = Long.MIN_VALUE;

			// Closest generation to mouse. [0] is the generation, [1] is fitness, [2] is the xValue
			long[] cursorInfo = new long[3];
			Point cursor = this.getMousePosition();
			int mouseX = -1;
			if(cursor != null) {
				mouseX = cursor.x - this.getLocation().x;
			}

			List<Point> graphPoints = new ArrayList<>();
			int start = 0;
			if(!mech.showFullScale()) {
				start = scores.size() - Info.SCALE_TO_SHOW;
				start = Math.max(0, start);
			}
			
			for(int i = start; i < scores.size(); i += 1) {
				int x1 = (int) ((i - start) * xScale + BORDER_GAP);
				int y1 = (int) ((MAX_SCORE - Math.max(1, (long) scores.get(i))) * yScale + BORDER_GAP);
				graphPoints.add(new Point(x1, y1));

				if(x1 < mouseX) {
					cursorInfo[0] = i * Info.GRAPH_SCALE;
					cursorInfo[1] = scores.get(i);
					cursorInfo[2] = x1;
				}

				if(scores.get(i) > highestScoreLastUpdate) {
					highestScoreLastUpdate = (long) scores.get(i);
				}

				if(h <= scores.get(i)) {
					locationH = i;
					h = scores.get(i);
				}
			}

			g2.setStroke(graphStroke);
			Color color;
			if(mech.getSpeciesToDisplay() == -1) {
				color = Maths.toTransparent(mech.getColor()[species][0], 255 / Info.SPECIES);
			}
			else {
				color = mech.getColor()[species][0];
			}

			for(int i = 0; i < graphPoints.size(); i++) {
				int x1 = graphPoints.get(i).x;
				int y1 = graphPoints.get(i).y;

				if(i < graphPoints.size() - 1) {
					int x2 = graphPoints.get(i + 1).x;
					int y2 = graphPoints.get(i + 1).y;
					int y3 = getHeight() - BORDER_GAP;

					int[] xPoints = new int[] { x1, x2, x2, x1 };
					int[] yPoints = new int[] { y1, y2, y3, y3 };
					g2.setColor(color);
					g2.fillPolygon(xPoints, yPoints, 4);
				}

				if(i == locationH) {
					g2.setColor(mech.getColor()[species][0]);
					g2.fillOval(x1 - 7, y1 - 7, 15, 15);

					highestString[species] = String.format("(%s) %s", species + 1, nf.format(scores.get(i).longValue()));
					highestPoints[species] = new Point(x1, y1);
				}
			}
			
			if(cursor != null) {
				// Closest generation to mouse. [0] is the generation, [1] is fitness, [2] is the xValue
				g2.setColor(Color.DARK_GRAY);
				String string = String.format("(%s) %s:%s", species + 1, cursorInfo[0], nf.format(cursorInfo[1]));
				g2.drawString(string, cursorInfo[2] - g2.getFontMetrics().stringWidth(string), 100 + species * 50);
				g2.drawLine((int) cursorInfo[2], 100, (int) cursorInfo[2], getHeight() - BORDER_GAP);
			}
		}

		for(int species = 0; species < Info.SPECIES; species++) {
			String fit = highestString[species];
			if(fit != null) {
				int x1 = highestPoints[species].x;
				int y1 = highestPoints[species].y;

				int stringWidth = g2.getFontMetrics().stringWidth(fit);
				int sx = Math.max(x1 - stringWidth / 2, 0);
				int sy = y1 - 7 - (species * 35);
				g2.setColor(Color.BLACK);
				g2.drawLine(sx + stringWidth / 2, sy + 5, x1, y1);
				g2.drawString(fit, sx, Math.max(sy, 20));
			}
		}

		g2.setStroke(hatchStroke);
		// create hatch marks for y axis.
		for(int i = 0; i < Y_HATCH_CNT; i++) {
			if(i % 2 == 0) {
				g2.setColor(Maths.toTransparent(Color.BLACK, 32));
			}
			else {
				g2.setColor(Maths.toTransparent(Color.BLACK, 64));
			}

			int x0 = BORDER_GAP;
			int x1 = getWidth() - BORDER_GAP;
			int y0 = getHeight() - (((i + 1) * (getHeight() - BORDER_GAP * 2)) / Y_HATCH_CNT + BORDER_GAP);
			int y1 = y0;
			g2.drawLine(x0, y0, x1, y1);
		}

		g2.setColor(Color.DARK_GRAY);
		// Draw x-line graph
		String xString = "Generations";
		int stringWidth = g2.getFontMetrics().stringWidth(xString);
		int xLX = getWidth() / 2 - stringWidth / 2;
		int yLX = getHeight() - BORDER_GAP / 2;
		g2.drawString(xString, xLX, yLX);

		String yString = "Fitness";
		stringWidth = g2.getFontMetrics().stringWidth(yString);
		int xLY = BORDER_GAP / 2;
		int yLY = getHeight() / 2 + stringWidth / 2;
		g2.translate(xLY, yLY);
		g2.rotate(Math.toRadians(270));
		g2.drawString(yString, 0, 0);
		g2.rotate(-Math.toRadians(270));
		g2.translate(-xLY, -yLY);

	}

	private void renderGame(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;

		g.setColor(Color.BLACK);
		g.fillRect(0, 0, WIDTH, HEIGHT);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		try {
			drawGrid(g);
			drawSnakes(g);

			drawGeneration(g, new Color(255, 255, 255, 128), 10, 20);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void drawGeneration(Graphics g, Color color, int x, int y) {
		g.setColor(color);
		g.setFont(font);

		String string = "Generation: " + Info.GENERATION;
		g.drawString(string, x, y);
	}

	private void drawGrid(Graphics g) {
		g.setColor(Color.DARK_GRAY);

		for(int x = 0; x < WIDTH; x += tile_size) {
			g.drawLine(x, 0, x, HEIGHT);
		}

		for(int y = 0; y < HEIGHT; y += tile_size) {
			g.drawLine(0, y, WIDTH, y);
		}

	}

	private void drawSnakes(Graphics g) {
		if(mech.getSpeciesToDisplay() == -1) {
			for(int species = 0; species < Info.SPECIES; species++) {
				drawSnakeSpecies(g, species);
			}
		}
		else {
			drawSnakeSpecies(g, mech.getSpeciesToDisplay());
		}
	}

	private void drawSnakeSpecies(Graphics g, int species) {
		if(species > mech.getSnakes().length - 1) {
			return;
		}
		Snake[] snakes = mech.getSnakes()[species];
		if(snakes == null) {
			return;
		}

		if(mech.shouldDisplayBest()) {
			Snake snake = snakes[mech.getBestSpecimen(species)];
			if(snake != null) {
				drawSnake(g, snake, snake.getBodyColor());

				if(snake.isSnakeAlive()) {
					Point berry = mech.getBerries()[species][mech.getBestSpecimen(species)].getPoint();
					g.setColor(snake.getBodyColor());
					g.fillOval(berry.x * tile_size + tile_size / 4, berry.y * tile_size + tile_size / 4,
							tile_size / 2, tile_size / 2);
				}
			}
		}
		else {
			for(int i = snakes.length - 1; i >= 0; i--) {
				Snake snake = snakes[i];
				if(snake != null) {
					drawSnake(g, snake, snake.getBodyColor());

					if(snake.isSnakeAlive()) {
						if(mech.getBerries()[species][i] != null) {
							Point berry = mech.getBerries()[species][i].getPoint();
							g.setColor(snake.getBodyColor());
							g.fillOval(berry.x * tile_size + tile_size / 4, berry.y * tile_size + tile_size / 4,
									tile_size / 2, tile_size / 2);
						}
					}
				}
			}
		}
	}

	private void drawSnake(Graphics g, Snake snake, Color color) {
		Graphics2D g2 = (Graphics2D) g;
		if(snake.getTicksSinceDeath() < 20) {
			SnakeBody[] body = snake.getBody();
			for(int i = 0; i < body.length; i++) {
				SnakeBody part = body[i];

				int x = part.getX() * tile_size;
				int y = part.getY() * tile_size;

				if(snake.isSnakeAlive()) {
					g2.setColor(color);
				}
				else {
					g2.setColor(DEAD_COLOR);
				}
				g2.fillRect(x, y, tile_size, tile_size);
				g2.setColor(Color.BLACK);
				g2.setStroke(new BasicStroke(2f));
				g2.drawRect(x, y, tile_size, tile_size);
				
				
			}
//			drawSnakeSight(g2, snake);
		}
	}

	public void drawSnakeSight(Graphics2D g2, Snake snake) {
		Point head = new Point(snake.getHeadSegment().getX(), snake.getHeadSegment().getY());

		int start = snake.getHeadSegment().getDirection();
		boolean first = true;
		for(int direction = start; direction != start || first; direction = (direction + 1) % Snake.vectors.length) {
			first = false;
			if(direction == snake.getHeadSegment().getOppositeDirection()) {
				continue;
			}
			int[] vector = Snake.vectors[direction];

			for(int d = 1; d < Mechanics.tiles + 1; d++) { // To get the wall distance. The sight distance is in the if-statement
				Point tile = new Point(vector[0] * d + head.x, vector[1] * d + head.y);
				tile.x = tile.x * tile_size;
				tile.y = tile.y * tile_size;


				g2.setColor(Color.WHITE);
				g2.fillRect(tile.x, tile.y, tile_size, tile_size);
				g2.setColor(Color.BLACK);
				g2.setStroke(new BasicStroke(2f));
				g2.drawRect(tile.x, tile.y, tile_size, tile_size);
			}
		}
	}

}
