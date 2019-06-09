package util;

import platfomer.genetics.Genome;
import platfomer.genetics.Population;
import platfomer.networks.Network;
import platfomer.util.NEATUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Test
{
    private static int winnerNodes;
    private static int winnerGenes;

    private static int popsize;
    private static int inputSize;
    private static int outputSize;
    private static double[][] inputs;
    private static double[][] answers;

    private static Population population;

    private static int generation = 0;

    private static String expFile = "xor.txt";

    public static void main(String[] args)
    {
        File xorFile = new File(expFile);
        parseInitData(xorFile);

        double totalGenerations = 0.0;
        double totalComplexity = 0.0;
        double totalSpecies = 0.0;
        double maxGenerations = 0.0;
        double maxComplexity = 0.0;
        double maxSpecies = 0.0;
        int runs = 10000;
        double[] runGens = new double[runs];
        double[] numSpecies = new double[runs];
        for (int run = 1; run <= runs; ++run)
        {
            NEATUtil.reseedRandom();
//            population = new Population(Genome.fullyConnected(inputSize, outputSize), popsize);
            population = new Population(popsize, inputSize, outputSize);

            generation = 0;
            for (int i = 0; i < 50000; ++i)
            {
                System.out.print("Run " + run + " - ");
                if (runOneGen())
                {
                    System.out.println("Winner after " + generation + " gens.");
                    break;
                }
            }
            runGens[run-1] = generation;
            if (generation > maxGenerations)
            {
                maxGenerations = generation;
            }
            numSpecies[run - 1] = population.getSpecies().size();
            if (numSpecies[run - 1] > maxSpecies)
            {
                maxSpecies = numSpecies[run - 1];
            }

            if (population.getMeanComplexity() > maxComplexity)
            {
                maxComplexity = population.getMeanComplexity();
            }

            totalGenerations += generation;
            totalComplexity += population.getMeanComplexity();
            totalSpecies += numSpecies[run - 1];
        }

        double aveGens = totalGenerations / runs;
        double aveSpecies = totalSpecies / runs;
        double variance = 0.0;
        for(int i = 0; i < runs; ++i)
        {
            double diff = runGens[i] - aveGens;
            variance += (diff * diff) / runs;
        }
        double stdDeviation = Math.sqrt(variance);

        System.out.println("-----------------------STATS--------------------------");
        System.out.println(runs + " runs");
//        System.out.println("Complexity MA Length: " + NEATUtil.COMPLEXITY_MOVING_AVERAGE_LENGTH);
        System.out.println("speciesThreshold: " + NEATUtil.speciesThreshold);
        System.out.println("------------------------------------------------------");
        System.out.println("Average num speices: " + aveSpecies);
        System.out.println("Max species: " + maxSpecies);
        System.out.println("Winner after average " + aveGens + " generations");
        System.out.println("longest run: " + maxGenerations);
        System.out.println("Variance: " + variance);
        System.out.println("SD: " + stdDeviation);
        System.out.println("Ave complexity: " + totalComplexity / runs);
        System.out.println("Max complexity: " + maxComplexity);
    }

    private static boolean eval(Genome g)
    {
        double[][] out = new double[answers.length][outputSize];

        Network net = g.createNetwork();
        int netDepth = net.maxDepth();
//        if (netDepth == 0 || net.disconnected)
//        {
////            System.out.println("Disconnected genome");
//
//            g.fitness = 0.0;
//            return false;
//        }

        for (int i = 0; i < inputs.length; i++)
        {
            net.loadInputs(inputs[i]);


            net.activate(netDepth);

//            for (int r = 0; r <= netDepth; r++)
//            {
//                success = net.activate();
//            }
            out[i] = net.getOutputs();
            net.flush();
        }

        double errorsum = 0.0;
        for (int i = 0; i < answers.length; i++)
        {
            for (int j = 0; j < outputSize; j++)
            {
                errorsum += Math.abs(answers[i][j] - out[i][j]);
            }
        }
//        System.out.println("Error: " + errorsum);
//        g.fitness = new BigDecimal(Math.pow(answers.length - errorsum, 2));
        g.fitness = Math.max(0, (answers.length * outputSize) - errorsum);
//        g.error = errorsum;

        //The minimum fitness needed to "win" is (0.5 * answers.length) ^ 2, since at that point,
        // if the outputs were being rounded to the nearest integer, the network would pass.
        // Test for slightly higher performance
        if (g.fitness >= answers.length * outputSize * 0.99)
        {
            g.winner = true;
            System.out.println("Winner!");
            winnerNodes = g.getNumNodes();
            winnerGenes = g.getNumLinks();
            return true;
        }
        g.winner = false;
        return false;
    }

    public static boolean runOneGen()
    {
        for (int i = 0; i < population.getGenomes().size(); i++)
        {
            Genome g = population.getGenomes().get(i);

            if (eval(g))
            {
                return true;
            }
        }

//        for (Species s : population.getSpecies())
//        {
//            s.computeAveFitness();
//            s.computeMaxFitness();
//        }

        generation++;
//        population.epoch(generation);
        population.nextGeneration();
        return false;
    }

    public static void parseInitData(File file)
    {
        final int BUFFER_SIZE = 1000;
        try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr))
        {
            String line;
            br.mark(BUFFER_SIZE);
            while ((line = br.readLine()) != null)
            {
                String[] data = line.split(" ");
                if (data[0].equals("popsize"))
                {
                    popsize = Integer.parseInt(data[1]);
                }
                if (data[0].equals("inputs"))
                {
                    inputSize = Integer.parseInt(data[1]);
                }
                if (data[0].equals("outputs"))
                {
                    outputSize = Integer.parseInt(data[1]);
                }
                if (data[0].equals("startinputdata"))
                {
                    if (inputSize <= 0 || outputSize <= 0)
                    {
                        throw new IllegalArgumentException(
                                "Error while parsing datafile, expected 'inputs' and 'outputs' to be defined before 'startinputdata'");
                    }
                    StringBuilder inData = new StringBuilder();
                    String dataline;
                    int lines = 0;
                    boolean end = false;
                    while ((dataline = br.readLine()) != null)
                    {
                        String[] split = dataline.split(" ");
                        if (split[0].contains("endinputdata"))
                        {
                            end = true;
                            break;
                        }
                        inData.append(dataline).append('\n');
                        lines++;
                    }
                    if (!end)
                        throw new IllegalArgumentException("Expected 'endinputdata'");
                    inputs = new double[lines][inputSize];
                    String[] inLines = inData.toString().split("\n");
                    for (int i = 0; i < lines; i++)
                    {
                        String[] records = (inLines[i]).split(" ");
                        for (int j = 0; j < inputSize; j++)
                        {
                            inputs[i][j] = Double.parseDouble(records[j]);
                        }
                    }
                }
                if (data[0].equals("startoutputdata"))
                {
                    if (inputSize <= 0 || outputSize <= 0)
                    {
                        throw new IllegalArgumentException(
                                "Error while parsing datafile, expected 'inputs' and 'outputs' to be defined before 'startinputdata'");
                    }
                    StringBuilder inData = new StringBuilder();
                    String dataline;
                    int lines = 0;
                    boolean end = false;
                    while ((dataline = br.readLine()) != null)
                    {
                        String[] split = dataline.split(" ");
                        if (split[0].contains("endoutputdata"))
                        {
                            end = true;
                            break;
                        }
                        inData.append(dataline).append('\n');
                        lines++;
                    }
                    if (!end)
                        throw new IllegalArgumentException("Expected 'endoutputdata'");
                    answers = new double[lines][outputSize];
                    String[] inLines = inData.toString().split("\n");
                    for (int i = 0; i < lines; i++)
                    {
                        String[] records = (inLines[i]).split(" ");
                        for (int j = 0; j < outputSize; j++)
                        {
                            answers[i][j] = Double.parseDouble(records[j]);
                        }
                    }
                }
                br.mark(BUFFER_SIZE);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
