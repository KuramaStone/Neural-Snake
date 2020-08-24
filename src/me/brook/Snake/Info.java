package me.brook.Snake;

import java.io.File;

import me.brook.NetworkLibrary.Maths;

public class Info {

	public static final long SESSION_ID = Maths.generateRandom().nextLong();

	/*
	 * 4 inputs around head. distance to food
	 * 4 inputs around head, distance to wall/body
	 * 4 inputs around head, distance to body
	 * 
	 * 4 outputs for the direction. Four inputs to ensure this neuron is effective.
	 */
	public static final int[][] NEURONS = {
			{ 12, 20, 20, 4 },
			{ 12, 20, 4 },
	};

	public static final int POPULATION[] = { 100, 100 };
	public static final int SPECIES = POPULATION.length;
	public static double[] MUTATION_RATE = { 0.05, 0.05, 0.05, 0.05 };

	public static final double KEEP_X_FROM_OLD = 0.1;
	public static final double BREED_X_FROM_TOP = 0.9;
	public static final double TOP_POPULATION = KEEP_X_FROM_OLD * 1;

	public static final File SAVE_FILE = new File("C:\\Users\\Stone\\Documents\\Training\\Snake\\files");

	/*
	 * Replay each generation a set number of times to average the greatest fitness
	 */
	public static int GENERATION = 1, REPLAY_NETWORKS = 1;
	public static int MAX_GENERATION = -1;
	public static double ORIG_SCALE = 0.5;
	
	// Temperature being the chance to greatly mutate the population. nextDouble < (temp/10000)
	public static double STARTING_TEMP = 5000, END_TEMP = 100;
	public static double TEMPERATURE_INCREMENT = 0.1;

	public static final int GRAPH_SCALE = 1, SCALE_TO_SHOW = 100;
	public static final int SAVE_RATE = 1000 * 60 * 5; // Save every five minutes

}
