package me.brook.Snake;

import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import me.brook.NetworkLibrary.Maths;
import me.brook.NetworkLibrary.Network;
import me.brook.NetworkLibrary.tech.Matrix;
import me.brook.Snake.entity.Snake;

public class SnakeEngine implements Runnable {

	private JFrame frame;
	private InputManager input;
	private GraphicsManager graphics;
	private Mechanics mech;
	private Thread thread;
	private File saveFile;
	private Random rnd;

	private boolean isRunning;

	private long[] totalFitnessOfBest;
	private int[] staleness;

	public SnakeEngine() {
		rnd = Maths.generateRandom();
		input = new InputManager();
		staleness = new int[Info.SPECIES];

		frame = new JFrame("Snake");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setResizable(false);
		frame.addKeyListener(input);
		frame.pack();
		Insets insets = frame.getInsets();
		frame.setSize(GraphicsManager.WIDTH + insets.left + insets.right,
				GraphicsManager.HEIGHT + insets.top + insets.bottom);
		frame.setLocationRelativeTo(null);
		frame.setLocation((int) frame.getLocation().getX(), 0);

		String[] readFrom = {
		};

		Network[][] networks = new Network[Info.SPECIES][];
		totalFitnessOfBest = new long[Info.SPECIES];

		boolean error = false;
		if(Info.SPECIES > Info.NEURONS.length) {
			System.out.println("[ERROR] Not enough network designs...");
			error = true;
		}
		if(Info.SPECIES > readFrom.length && readFrom.length != 0) {
			System.out.println("[ERROR] Not enough network loadstates.");
		}
		if(Info.SPECIES > Info.MUTATION_RATE.length) {
			System.out.println("[ERROR] Not enough mutation rates.");
			error = true;
		}

		if(error) {
			System.exit(1);
		}

		for(int species = 0; species < Info.SPECIES; species++) {
			networks[species] = new Network[Info.POPULATION[species]];
			for(int i = 0; i < networks[species].length; i++) {
				String save = null;
				if(readFrom.length > 0) {
					// Read from the appropriate save, otherwise continue loading the last one.
					if(species < readFrom.length) {
						save = readFrom[species];
					}
					else {
						save = readFrom[readFrom.length - 1];
					}
				}
				networks[species][i] = new Network(Info.NEURONS[species], save);
			}
		}

		mech = new Mechanics(this, networks);
		graphics = new GraphicsManager(this, mech, frame);
		frame.add(graphics);

		thread = new Thread(this);
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				SnakeEngine.this.save();
			}

		});

		int i = 0;
		while(saveFile == null || saveFile.exists()) {
			saveFile = new File(Info.SAVE_FILE, String.valueOf(i++));
		}
		saveFile.mkdirs();
	}

	@Override
	public void run() {
		int milliSecondsPerUpdate = 0;
		long update = System.currentTimeMillis();

		/*
		 * Not rendering can drastically increase speed by orders of magnitude
		 */
		while(isRunning && !thread.isInterrupted() &&
				(Info.MAX_GENERATION == -1 || Info.GENERATION <= Info.MAX_GENERATION)) {

			if(mech.isSlowDownEnabled()) {
				milliSecondsPerUpdate = 50;
			}
			else {
				milliSecondsPerUpdate = 0;
			}
			long now = System.currentTimeMillis();

			if(now - update >= milliSecondsPerUpdate) {
				/*
				 * Run the game multiple times to get the average for each network.
				 * This minimizes random chance from artificially favoring a weaker network.
				 */
				if(mech.areAllSnakesDead()) {
					mech.assignFitness();
					mech.addIteration();

					if(mech.shouldBringNewPopulation()) {
						Info.GENERATION++;
						frame.setTitle("Snake - Generation: " + Info.GENERATION);
						for(int species = 0; species < Info.SPECIES; species++) {
							mech.getNetworks()[species] = newGeneration(species, mech.getNetworks()[species]);
						}
						mech.restart();
					}
					else {
						mech.restart();
					}

				}

				mech.updateGame(input);

				if(frame.getState() != Frame.ICONIFIED) {
					graphics.repaint();
				}

				update = now;
			}
		}

		frame.dispose();
	}

	protected void save() {
		File parent = new File("C:\\Users\\Stone\\Documents\\Training\\Snake\\images");

		BufferedImage graph = new BufferedImage(GraphicsManager.WIDTH, GraphicsManager.HEIGHT, BufferedImage.TYPE_INT_RGB);
		mech.setSpeciesToDisplay(-1);
		mech.setRenderingMode(1);
		graphics.paintComponent(graph.getGraphics());

		File png = null;
		int copy = 0;
		while(png == null || (png != null && png.exists())) {
			png = new File(parent, Info.SESSION_ID + "_" + copy++ + ".png");
		}

		try {
			png.mkdirs();
			ImageIO.write(graph, "png", png);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		isRunning = false;

		System.exit(0);
	}

	private long highestAverageFitness;

	private Network[] newGeneration(int species, Network[] unsorted) {
		Network[] newGeneration = new Network[Info.POPULATION[species]];

		// Average the total fitnesses
		for(int i = 0; i < unsorted.length; i++) {
			unsorted[i].setFitness(unsorted[i].getFitness() / Info.REPLAY_NETWORKS);
		}

		Network[] population = new Network[unsorted.length];
		System.arraycopy(unsorted, 0, population, 0, unsorted.length);

		// Sort in descending order
		Arrays.sort(population, Maths.SORT_FITNESS_H2L);
		updateGraph(species, population);
		saveSpecies(species, population);
		updateAverages(species, population);

		selection(species, population, newGeneration);
		// selectionByElitism(species, population, newGeneration);

		return newGeneration;
	}

	private void selection(int species, Network[] population, Network[] newGeneration) {

		// Kill bottom half of the species by setting their fitness to 0. 0 fitness stops them from breeding
		for(int i = population.length / 2; i < population.length; i++) {
			population[i].setFitness(0);
		}

		int count = 0;
		newGeneration[count++] = population[0]; // Keep best
		while(count < Info.POPULATION[species]) {
			Network child = (Network) Network.breed(getByRoulette(species, population), getByRoulette(species, population));

			child.mutate(Info.MUTATION_RATE[species]);

			newGeneration[count++] = child;
		}
		newGeneration[0].setFitness(0);

		if(staleness[species] > 20) {
			for(int i = Info.POPULATION[species] / 2; i < Info.POPULATION[species]; i++) {
				newGeneration[i].mutate(0.9);
			}
			staleness[species] = 0;
		}

	}

	private void updateAverages(int species, Network[] population) {
		long sum = 0;
		for(int i = 0; i < population.length; i++) {
			// System.out.println(i + " " + possibleParents[i]);
			sum += population[i].getFitness();
		}
		sum /= population.length;

		mech.addAverageScore(species, sum);
	}

	private long[] lastSave = new long[Info.SPECIES];

	private void saveSpecies(int species, Network[] population) {

		// If the current time is a multiple of the save rate, then save
		if(System.currentTimeMillis() - lastSave[species] > Info.SAVE_RATE) {
			try {
				population[0].save(saveFile, false, Info.GENERATION, mech.averageScoreOfBest[species], species);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			lastSave[species] = System.currentTimeMillis();
		}
	}

	private void updateGraph(int species, Network[] population) {

		Snake obj = ((Snake) population[0].obj);
		// Highest fitness is for the graph's Y-Scale
		if(obj.getFitness() > mech.highestScoreEver) {
			mech.highestScoreEver = obj.getFitness();
			staleness[species] = 0;
		}
		else {
			staleness[species]++;
		}

		totalFitnessOfBest[species] += obj.getFitness();

		// Add average score to graph
		if(Info.GENERATION % Info.GRAPH_SCALE == 0) {
			mech.averageScoreOfBest[species] = totalFitnessOfBest[species];
			if(mech.averageScoreOfBest[species] > highestAverageFitness) {
				highestAverageFitness = mech.averageScoreOfBest[species];
			}
			totalFitnessOfBest[species] = 0;
			mech.addScore(species, (long) mech.averageScoreOfBest[species]);
		}
	}

	public Network getByRoulette(int species, Network[] possibleParents) {
		long eFitness = 0;
		for(int i = 0; i < possibleParents.length; i++) {
			// System.out.println(i + " " + possibleParents[i].getFitness());
			eFitness += possibleParents[i].getFitness() * Math.pow(2, -i);
		}

		long rand = (long) (rnd.nextDouble() * eFitness);
		long runningSum = 0;
		for(int i = 0; i < possibleParents.length; i++) {
			runningSum += possibleParents[i].getFitness() * Math.pow(2, -i);
			if(runningSum > rand) {
				// System.out.println("chosen: " + i + ": " + possibleParents[i].getFitness());
				return possibleParents[i];
			}
		}

		return possibleParents[0];
	}

	public void start() {
		if(isRunning) {
			return;
		}

		isRunning = true;
		thread.start();
	}

	@SuppressWarnings("deprecation")
	public void stop(boolean force) {
		if(!isRunning) {
			return;
		}

		if(force) {
			thread.stop();
		}
		isRunning = false;
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		Matrix.SCALE = Info.ORIG_SCALE;
		SnakeEngine engine = new SnakeEngine();
		engine.start();
	}

	public JFrame getFrame() {
		return frame;
	}

	public GraphicsManager getGraphics() {
		return graphics;
	}

}
