package util;

import platfomer.genetics.Genome;
import platfomer.genetics.Population;
import util.gui.NetworkViewer;
import platfomer.networks.Network;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Visualiser extends JFrame
{
    private int winnerNodes;
    private int winnerGenes;

    private int popsize;
    private int runs;
    private int inputSize;
    private int outputSize;
    private double[][] inputs;
    private double[][] answers;

    private Population population;
    private int orgIndex;

    private JFileChooser fc = new JFileChooser();
    private NetworkViewer visual;
    private boolean loaded = false;

    private Timer runner;
    private JButton runButton;

    private JPanel labelPanel;
    private JLabel fitnessLabel;
    private JLabel orgIDLabel;


    // Service to run epochs in the background
    private ExecutorService epochQueue;

    int generation = 0;

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> new Visualiser("Network Evolution"));
    }

    public Visualiser(String title)
    {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        fc.setCurrentDirectory(Paths.get("").toAbsolutePath().toFile());
        visual = new NetworkViewer();
        getContentPane().add(visual);
        createButtons();
        createLabelPanel();

        runner = new Timer(1000 / 60, ae -> pushEpochTask());

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);

        epochQueue = Executors.newSingleThreadExecutor();
    }

    public void createButtons()
    {
        JPanel buttonPanel = new JPanel(new FlowLayout());
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        JButton fcbutton = new JButton("Load Data File");
        JButton popButton = new JButton("Load Pop File");
        JButton nextButton = new JButton("Next organism");
        JButton prevButton = new JButton("Previous organism");
        JButton oneGenButton = new JButton("Run once");
        runButton = new JButton("Start");

        fcbutton.setFocusPainted(false);
        popButton.setFocusPainted(false);
        nextButton.setFocusPainted(false);
        prevButton.setFocusPainted(false);
        oneGenButton.setFocusPainted(false);

        fcbutton.addActionListener(ae ->
        {
            if (selectFile() == JFileChooser.APPROVE_OPTION)
            {
                File dataFile = getSelectedFile();
                parseInitData(dataFile);
                createPop();
                loaded = true;
            }
        });
        popButton.addActionListener(ae ->
        {
            if (selectFile() == JFileChooser.APPROVE_OPTION)
            {
                loadPop(fc.getSelectedFile());
                popsize = population.size();
                loaded = true;
            }
        });
        nextButton.addActionListener(ae -> nextOrg());
        prevButton.addActionListener(ae -> prevOrg());
        oneGenButton.addActionListener(ae -> pushEpochTask());
        runButton.addActionListener(a ->
        {
            if (loaded)
            {
                if (runner.isRunning())
                {
                    stop();
                }
                else
                {
                    start();
                }

            }
            else
            {
                stop();
            }
        });

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(a ->
        {
            if (selectFile() == JFileChooser.APPROVE_OPTION)
            {
                File save = fc.getSelectedFile();
                savePop(save);
            }
        });

        buttonPanel.add(fcbutton, BorderLayout.SOUTH);
        buttonPanel.add(popButton, BorderLayout.SOUTH);
        buttonPanel.add(prevButton, BorderLayout.SOUTH);
        buttonPanel.add(oneGenButton, BorderLayout.SOUTH);
        buttonPanel.add(runButton, BorderLayout.SOUTH);
        buttonPanel.add(nextButton, BorderLayout.SOUTH);
        buttonPanel.add(saveButton, BorderLayout.SOUTH);
    }

    public void createLabelPanel()
    {
        labelPanel = new JPanel(new GridLayout(1, 2));
        getContentPane().add(labelPanel, BorderLayout.NORTH);

        fitnessLabel = new JLabel("Fitness:");
        orgIDLabel = new JLabel("Organism #");

        fitnessLabel.setHorizontalAlignment(JLabel.CENTER);
        orgIDLabel.setHorizontalAlignment(JLabel.CENTER);

        labelPanel.add(fitnessLabel);
        labelPanel.add(orgIDLabel);
    }

    public boolean eval(Genome g)
    {
        double[][] out = new double[answers.length][outputSize];

        Network net = g.createNetwork();
        int netDepth = net.maxDepth();
//        if (net.disconnected)
//        {
//            g.fitness = 0.0;
//            return false;
//        }
        for (int i = 0; i < inputs.length; i++)
        {
            net.loadInputs(inputs[i]);

//            success = net.activate();
            net.activate(netDepth);


//            for (int r = 0; r <= netDepth; r++)
//            {
////                success = net.activate();
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
        //System.out.println("Error: " + errorsum);
//            g.fitness = new BigDecimal(Math.pow(answers.length - errorsum, 2));
        g.fitness = Math.pow(answers.length - errorsum, 2);
//            g.error = errorsum;

        //The minimum fitness needed to "win" is (0.5 * answers.length) ^ 2, since at that point,
        // if the outputs were being rounded to the nearest integer, the network would pass.
        // Test for slightly higher performance
        if (g.fitness >= Math.pow(answers.length * 0.60, 2))
        {
            g.winner = true;
            //System.out.println("Winner!");
            winnerNodes = g.getNumNodes();
            winnerGenes = g.getNumLinks();
            return true;
        }
        g.winner = false;
        return false;
    }

    public void runOneGen()
    {
        for (int i = 0; i < population.getGenomes().size(); i++)
        {
            Genome g = population.getGenomes().get(i);
            if (eval(g))
            {
                orgIndex = i;
            }
        }

//        for (Species s : population.getSpecies())
//        {
//            s.computeAveFitness();
//            s.computeMaxFitness();
//        }

        showGenome();
        generation++;
//        population.epoch(generation);
        population.nextGeneration();
    }

    public void createPop()
    {
        population = new Population(popsize, inputSize, outputSize);

        orgIndex = 0;
        showGenome();
    }

    public void nextOrg()
    {
        orgIndex++;
        showGenome();
    }

    public void prevOrg()
    {
        orgIndex--;
        showGenome();
    }

    private void showGenome()
    {
        if (population == null) return;
        if (orgIndex < 0)
            orgIndex = popsize - 1;
        if (orgIndex >= popsize)
            orgIndex = 0;
        Genome g = population.getGenomes().get(orgIndex);
        visual.loadGenome(g);
        fitnessLabel.setText("Fitness: " + population.getGenomes().get(orgIndex).fitness);
        orgIDLabel.setText("Organism #" + orgIndex);

    }

    //Add an epoch to the queue, to run in the background
    public void pushEpochTask()
    {
        if (loaded)
            epochQueue.submit(this::runOneGen);
    }

    public void start()
    {
        if (!runner.isRunning())
        {
            runner.start();
            runButton.setText("Stop");
        }
    }

    public void stop()
    {
        if (runner.isRunning())
        {
            runner.stop();
            runButton.setText("Start");
        }
    }

    //File Chooser methods
    public int selectFile()
    {
        return fc.showOpenDialog(getContentPane());
    }

    public File getSelectedFile()
    {
        return fc.getSelectedFile();
    }

    //IO methods

    /**
     * Save the current population to a file
     *
     * @param file
     */
    public void savePop(File file)
    {
        if (population != null)
        {
            population.printToFileBySpecies(file.getAbsolutePath());
        }
    }

    /**
     * Load a population from a file
     *
     * @param file
     */
    public void loadPop(File file)
    {
        try
        {
            population = new Population(file.getAbsolutePath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return;
        }

        orgIndex = 0;
        Genome gen = population.getGenomes().get(0);
        visual.loadGenome(gen);
    }


    /**
     * Parse a data file for initialization values
     *
     * @param file
     */
    public void parseInitData(File file)
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