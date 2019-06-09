package platfomer.sim;

import platfomer.Main;
import platfomer.entity.Entities;
import platfomer.entity.Entity;
import platfomer.entity.EntityFactory;
import platfomer.entity.component.PlayerComponent;
import platfomer.genetics.Genome;
import platfomer.genetics.Population;
import platfomer.gfx.Text;
import platfomer.level.LevelChanger;
import platfomer.networks.Link;
import platfomer.networks.Network;
import platfomer.networks.Node;
import platfomer.registry.Registry;
import platfomer.util.FastRandom;
import platfomer.util.NEATUtil;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

public class Simulation
{
    //Evaluation variables
    private int currentFrame = 0;
    private int timeout;
    private int timeoutBonus;
    private int timeoutConstant = 15;
    private int rightmost = 15;
    private int maxRightmost, maxRightmostThisGen;
    private int levels = 0;
    private double fitness;
    private boolean simulationRunning = false;
    private boolean winner = false;

    private int genomeIndex;
    private int oldGenome = -1;
    private Genome currentGenome;
    private boolean playBest = false;
    private boolean runOneGeneration = false;
    private boolean drawNetwork = false;
    private boolean turbo = false;

    private Population population;
    private int popSize = 100;

    private Network network;
    private Network.NetworkGraph networkGraph;

    private Entity player;

    private Scanner input;

    public void init()
    {
        input = new Scanner(System.in);
        levels = 0;
        initPopulation();
        newLevel();
    }

    private void newPlayer()
    {
        player = EntityFactory.createPlayer(1, LevelChanger.curLevel.getTerrainHeight(1) - 1);
    }

    private void newLevel()
    {
        Entities.clear();
        LevelChanger.newLevel();
        newPlayer();
        Entities.add(player);

        winner = false;
        population.resetFitness();
        genomeIndex = 0;
        maxRightmost = 0;
    }

    private void initPopulation()
    {
//        population = new Population(Genome.fullyConnected(NEATUtil.inputWidth * NEATUtil.inputHeight, 3), popSize);
        population = new Population(popSize, NEATUtil.inputWidth * NEATUtil.inputHeight, 3);
    }

    private void reset()
    {
        if (simulationRunning || playBest)
        {
            resetNetPlayer();
        }
        else
        {
            resetPlayer();
        }
    }

    private void resetPlayer()
    {
        newPlayer();
        resetLevel();
    }

    private void resetNetPlayer()
    {
        currentGenome = population.getGenomes().get(genomeIndex);
        rightmost = 15;
        timeout = timeoutConstant;
        timeoutBonus = 0;
        currentFrame = 0;
        //fitness = 0;

        network = currentGenome.createNetwork();
        if (drawNetwork)
        {
            networkGraph = network.createGraph();
        }
        player = EntityFactory.createNeuralPlayer(15, LevelChanger.curLevel.getTerrainHeight(15) - 1, network);

        resetLevel();
    }

    private void resetLevel()
    {
        Entities.clear();
        Entities.add(player);
        Entities.cameraControlSystem.tick(player);
        Entities.levelLoadSystem.tick(player);
        LevelChanger.curLevel.resetEnemies();
    }

    private double calculateFitness()
    {
        currentFrame++;
        timeout--;
        timeoutBonus = (int) (currentFrame / 25.0);
        if ((int) player.x > rightmost)
        {
            rightmost = (int) player.x;
            timeout = timeoutConstant * 6;
        }
        double positionBonus = rightmost - 15.0;

        double timePenalty = currentFrame / 40.0;

        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        double stompBonus = pc.stomps;

        double total = positionBonus + stompBonus;// - timePenalty;
        if(total < 0.0) total = 0.0;


        return total;
    }

    private boolean doNELoop()
    {
        boolean running = true;
        fitness = calculateFitness();
        timeoutBonus = 0;
        if (timeout + timeoutBonus < 0 || player.removed)
        {
            currentGenome.fitness = fitness;
            if (rightmost > LevelChanger.curLevel.levelWidth && !player.removed)
            {
                currentGenome.winner = true;
                winner = true;
            }

            if (rightmost - 15 > maxRightmostThisGen)
            {
                maxRightmostThisGen = rightmost - 15;
            }

            if (++genomeIndex == popSize)
            {
                genomeIndex = 0;
                if (winner && NEATUtil.currentSearchPhase == NEATUtil.SearchPhase.COMPLEXIFY)
                {
                    if (population.currentBestGenome == null || !population.currentBestGenome.winner )
                    {
                        population.nextGeneration();
                        //writeWinner();
                        //population.printToFileBySpecies("winner_" + new String(GameUtil.random.getSeed()) + ".txt");
                    }

                    simulationRunning = false;
                    runOneGeneration = false;
                    resetPlayer();
                    Main.paused = true;
//                    levels++;
//                    newLevel();
//                    reset();
//                    System.out.println("New level! Levels completed: " + levels);
                }
                else
                {
                    population.nextGeneration();
                    for (; genomeIndex < popSize; ++genomeIndex)
                    {
                        if (population.getGenome(genomeIndex).generation == population.getGeneration())
                        {
                            break;
                        }
                    }
                }
                //turbo on: run generations as fast as possible
                if(turbo)
                {
                    running = false;
                }
                maxRightmost = maxRightmostThisGen;
                maxRightmostThisGen = 0;
            }

            reset();
            //turbo off: run one genome per frame
            if(!turbo)
            {
                running = false;
            }

        }
        Entities.tick();
        return running;
    }

    private void playBest()
    {
        fitness = calculateFitness();
        if (timeout + timeoutBonus < 0 || player.removed)
        {
            playBest = false;
            genomeIndex = oldGenome;
            oldGenome = -1;
            reset();
        }
        Entities.tick();
    }

    public void tick()
    {
        if (Registry.input.key(KeyEvent.VK_H).pressed)
        {
            PlayerComponent p = player.getComponent(PlayerComponent.class);
            p.invincible ^= true;
        }

        if (Registry.input.key(KeyEvent.VK_PLUS).pressed || Registry.input.key(KeyEvent.VK_EQUALS).pressed)
        {
            runOneGeneration ^= true;
        }

        if (Registry.input.key(KeyEvent.VK_T).pressed)
        {
            turbo ^= true;
        }

        if (Registry.input.key(KeyEvent.VK_F).pressed)
        {
            if (!playBest)
            {
                simulationRunning ^= true;
            }
            else
            {
                playBest = false;
            }
            reset();
        }

        //Write the population to a file
        if (Registry.input.key(KeyEvent.VK_P).pressed)
        {
            System.out.println("Enter filename: ");
            String filename = input.nextLine();
            Path path = Paths.get(filename);
            if (!Files.exists(path))
            {
                try
                {
                    Files.createFile(path);
                    try (DataOutputStream bw = new DataOutputStream(new FileOutputStream(filename)))
                    {
                        bw.write(LevelChanger.curLevel.getSeed());
                        bw.write('\n');
                    }
                    catch (IOException e)
                    {
                        System.out.println("Failed to write seed to file: " + e.getMessage());
                    }
                    population.printToFileBySpecies(filename);
                }
                catch (IOException e)
                {
                    System.out.println("Failed to create file: " + e.getMessage());
                }
            }
        }

        if (Registry.input.key(KeyEvent.VK_L).pressed)
        {
            System.out.println("Enter filename: ");
            String filename = input.nextLine();
            try (DataInputStream bw = new DataInputStream(new FileInputStream(filename)))
            {
                byte[] seed = new byte[FastRandom.SEED_SIZE_BYTES];
                bw.read(seed);
                LevelChanger.curLevel.seed(seed);
                LevelChanger.newLevel();
            }
            catch (IOException e)
            {
                System.out.println("Failed to read seed from file: " + e.getMessage());
            }
            try
            {
                population = new Population(filename);
                popSize = population.size();
                genomeIndex = 0;
                maxRightmost = 0;
                winner = false;
                reset();
            }
            catch (IOException e)
            {
                population = null;
                e.printStackTrace();
            }
        }

        if (Registry.input.key(KeyEvent.VK_R).pressed)
        {
            if (network != null)
            {
                drawNetwork ^= true;
                if (!drawNetwork)
                {
                    networkGraph = null;
                }
                else if (networkGraph == null)
                {
                    networkGraph = network.createGraph();
                }
            }
        }

        if (Registry.input.key(KeyEvent.VK_ASTERISK).pressed || Registry.input.key(KeyEvent.VK_8).pressed)
        {
            if (!playBest)
            {
                playBest = true;
                oldGenome = genomeIndex;
                genomeIndex = population.currentBestGenome();
                reset();
            }
            else
            {
                playBest = false;
                genomeIndex = oldGenome;
                oldGenome = -1;
                reset();
            }
        }

        ////////////////NE LOOP////////////////////////////////////////
        if (playBest)
        {
            playBest();
        }
        else if (simulationRunning)
        {
            if(Registry.input.control())
            {
                if(NEATUtil.currentSearchPhase == NEATUtil.SearchPhase.COMPLEXIFY && Registry.input.key(KeyEvent.VK_DOWN).held)
                {
                    population.startSimplifying();
                }

                if(NEATUtil.currentSearchPhase == NEATUtil.SearchPhase.SIMPLIFY && Registry.input.key(KeyEvent.VK_UP).pressed)
                {
                    population.startComplexifying();
                }
            }
            if (runOneGeneration)
            {
                while (doNELoop())
                {
                    //Run until the loop finishes
                }
            }
            else
            {
                doNELoop();
            }
        }
        ///////////////////////////////////////////////////////////////
        else
        {
            if (Registry.input.control() && Registry.input.key(KeyEvent.VK_ENTER).pressed)
            {
                init();
            }
            if (Registry.input.control() && Registry.input.key(KeyEvent.VK_SPACE).pressed)
            {
                newLevel();
            }
            Entities.tick();
        }
    }

    private void writeWinner()
    {
        String filename = "Level_" + levels + "_winner.txt";
        Path path = Paths.get(filename);
        if (!Files.exists(path))
        {
            try
            {
                Files.createFile(path);

            }
            catch (IOException e)
            {
                System.out.println("Failed to create file: " + e.getMessage());
            }
        }
        try (DataOutputStream bw = new DataOutputStream(new FileOutputStream(filename)))
        {
            bw.write(LevelChanger.curLevel.getSeed());
            bw.write('\n');
        }
        catch (IOException e)
        {
            System.out.println("Failed to write seed to file: " + e.getMessage());
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true)))
        {
            population.currentBestGenome.printToFile(bw);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void render()
    {
        LevelChanger.render();
        Entities.render();
        if (playBest)
        {
            Text.drawText(10, 9, "Running Best Genome.");
        }
        else if (simulationRunning)
        {
            Text.drawText(110, 9, "Gen: " + population.getGeneration());
        }

        if (playBest || simulationRunning)
        {
            Text.drawText(200, 9, "Species: " + currentGenome.species.id);
            Text.drawText(200, 18, "(" + (population.getSpecies().indexOf(currentGenome.species) + 1) + '/' + population.getSpecies().size() + ')');
            Text.drawText(300, 9, "Genome: " + genomeIndex);
            Text.drawText(300, 18, "Pop Size: " + popSize);
            Text.drawText(400, 9, "Complexity: " + currentGenome.getComplexity());
            Text.drawText(500, 9, "Levels: " + levels);

            Text.drawText(110, 429, String.format("fitness: %.8f", fitness));
            Text.drawText(260, 429, String.format("max fitness: %.8f", population.getMaxFitness()));
            Text.drawText(110, 438, String.format("distance: %d", rightmost));
            Text.drawText(260, 438, String.format("furthest distance: %d", maxRightmost));

            if (NEATUtil.currentSearchPhase == NEATUtil.SearchPhase.COMPLEXIFY)
            {
                Text.drawText(410, 429, "complexifying");
            }
            else
            {
                Text.drawText(410, 429, "simplifying");
            }
        }
    }

    public void drawGenome(Graphics g)
    {
        if (!drawNetwork || !(simulationRunning || playBest)) return;
        if (network == null || networkGraph == null) return;
        g.setColor(new Color(0x7F7F7F7F, true));
        g.fillRect(20, 20, (Main.WIDTH - 2) * NEATUtil.nodeSize + 1, (Main.HEIGHT - 2) * NEATUtil.nodeSize + 1);
        int xOff = 20 + ((Main.WIDTH - 2) / 2 - NEATUtil.inputWidth / 2) * NEATUtil.nodeSize;
        int yOff = 20 + ((Main.HEIGHT - 2) / 2 - NEATUtil.inputHeight / 2) * NEATUtil.nodeSize;
        g.fillRect(xOff, yOff, NEATUtil.inputWidth * NEATUtil.nodeSize + 1, NEATUtil.inputHeight * NEATUtil.nodeSize + 1);
        for (int i = 0; i < network.dims.ninput; ++i)
        {
            double activation = network.activations[networkGraph._nodes[i]];
            if (activation == 0.0 && i >= network.dims.nbias) continue;

            Point point = networkGraph.nodePoints[i];
            int color = (int) Math.floor((activation + 1) / 2.0 * 256);
            if (color > 255)
            {
                color = 255;
            }
            if (color < 0)
            {
                color = 0;
            }
            int opacity = 0xFF000000;
            if (activation == 0)
            {
                opacity = 0x58000000;
            }
            color = opacity + color * 0x10000 + color * 0x100 + color;
            if (activation == -1.0)
            {
                color = 0xFFFF0000;
            }
            if (activation == -2.0)
            {
                color = 0xFFB56C91;
            }
            if (activation == 2.0)
            {
                color = 0xFF00FF80;
            }
            g.setColor(new Color(color, true));
            g.fillRect(point.x - NEATUtil.nodeSize / 2, point.y - NEATUtil.nodeSize / 2, NEATUtil.nodeSize, NEATUtil.nodeSize);
            g.setColor(new Color((~color) | (0xFF << 24), true));
            g.drawRect(point.x - NEATUtil.nodeSize / 2, point.y - NEATUtil.nodeSize / 2, NEATUtil.nodeSize, NEATUtil.nodeSize);
        }
        for (int i = 0; i < networkGraph._nodes.length; ++i)
        {
            int nodeIdx = networkGraph._nodes[i];
            Node node = network.nodes[nodeIdx];
            for (int l = node.incomingStart; l < node.incomingEnd; l++)
            {
                Link link = network.links[l];
                double activationIn = network.activations[link.in];
                Point outPoint = networkGraph.nodePoints[i];
                Point inPoint;
                if (link.in == link.out)
                {
                    inPoint = outPoint;
                }
                else if (link.in < network.dims.ninput)
                {
                    inPoint = networkGraph.nodePoints[link.in];
                }
                else
                {
                    //binary search for the in node.
                    //the _nodes array is always sorted.
                    int pointIdx = Arrays.binarySearch(networkGraph._nodes, network.dims.ninput, networkGraph._nodes.length, link.in);
                    inPoint = networkGraph.nodePoints[pointIdx];
                }
                assert inPoint != null;


                int opacity = 0xB0000000;
                if (activationIn == 0)
                {
                    opacity = 0x30000000;
                }
                int color = (int) (0x80 - Math.floor(Math.abs(NEATUtil.sigmoid(link.weight)) * 0x80));
                if (link.weight > 0)
                {
                    color = opacity + 0x8000 + 0x10000 * color;
                }
                if (link.weight < 0)
                {
                    color = opacity + 0x800000 + 0x100 * color;
                }
                g.setColor(new Color(color, true));
                if (inPoint == outPoint)
                {
                    g.drawLine(inPoint.x + 1, inPoint.y, inPoint.x + NEATUtil.nodeSize, inPoint.y - NEATUtil.nodeSize / 2);
                    g.drawLine(inPoint.x + NEATUtil.nodeSize, inPoint.y - NEATUtil.nodeSize / 2, inPoint.x + NEATUtil.nodeSize, inPoint.y - (NEATUtil.nodeSize + 1));
                    g.drawLine(inPoint.x + NEATUtil.nodeSize, inPoint.y - (NEATUtil.nodeSize + 1), inPoint.x - NEATUtil.nodeSize, inPoint.y - (NEATUtil.nodeSize + 1));
                    g.drawLine(inPoint.x - NEATUtil.nodeSize, inPoint.y - (NEATUtil.nodeSize + 1), inPoint.x - NEATUtil.nodeSize, inPoint.y);
                    g.drawLine(inPoint.x - NEATUtil.nodeSize, inPoint.y, inPoint.x - NEATUtil.nodeSize / 2, inPoint.y);
                    continue;
                }
                else if (network.depths[link.in] < network.depths[link.out])
                {
                    if (outPoint.y <= inPoint.y)
                    {
                        g.drawLine(inPoint.x + 1, inPoint.y, inPoint.x + NEATUtil.nodeSize, inPoint.y - NEATUtil.nodeSize / 2);
                        g.drawLine(inPoint.x + NEATUtil.nodeSize, inPoint.y - NEATUtil.nodeSize / 2, inPoint.x + NEATUtil.nodeSize, inPoint.y - (NEATUtil.nodeSize + 1));
                        g.drawLine(inPoint.x + NEATUtil.nodeSize, inPoint.y - (NEATUtil.nodeSize + 1), outPoint.x - NEATUtil.nodeSize, outPoint.y - (NEATUtil.nodeSize + 1));
                        g.drawLine(outPoint.x - NEATUtil.nodeSize, outPoint.y - (NEATUtil.nodeSize + 1), outPoint.x - NEATUtil.nodeSize, outPoint.y);
                        g.drawLine(outPoint.x - NEATUtil.nodeSize, outPoint.y, outPoint.x - NEATUtil.nodeSize / 2, outPoint.y);
                    }
                    else
                    {
                        g.drawLine(inPoint.x + 1, inPoint.y, inPoint.x + NEATUtil.nodeSize, inPoint.y + NEATUtil.nodeSize / 2);
                        g.drawLine(inPoint.x + NEATUtil.nodeSize, inPoint.y + NEATUtil.nodeSize / 2, inPoint.x + NEATUtil.nodeSize, inPoint.y + (NEATUtil.nodeSize + 1));
                        g.drawLine(inPoint.x + NEATUtil.nodeSize, inPoint.y + (NEATUtil.nodeSize + 1), outPoint.x - NEATUtil.nodeSize, outPoint.y + (NEATUtil.nodeSize + 1));
                        g.drawLine(outPoint.x - NEATUtil.nodeSize, outPoint.y + (NEATUtil.nodeSize + 1), outPoint.x - NEATUtil.nodeSize, outPoint.y);
                        g.drawLine(outPoint.x - NEATUtil.nodeSize, outPoint.y, outPoint.x - NEATUtil.nodeSize / 2, outPoint.y);
                    }
                    continue;
                }

                g.drawLine(inPoint.x + 1, inPoint.y, outPoint.x - NEATUtil.nodeSize / 2, outPoint.y);
            }
        }
        //Draw the hidden nodes over the links
        for (int i = network.dims.ninput; i < networkGraph.nodePoints.length; ++i)
        {
            double activation = network.activations[networkGraph._nodes[i]];
            if (activation == 0.0 && i >= network.dims.nbias && i < network.dims.ninput) continue;

            Point point = networkGraph.nodePoints[i];
            int color = (int) Math.floor((activation + 1) / 2.0 * 256);
            if (color > 255)
            {
                color = 255;
            }
            if (color < 0)
            {
                color = 0;
            }
            int opacity = 0xFF000000;
            if (activation == 0)
            {
                opacity = 0x58000000;
            }
            color = opacity + color * 0x10000 + color * 0x100 + color;
            g.setColor(new Color(color, true));
            g.fillRect(point.x - NEATUtil.nodeSize / 2, point.y - NEATUtil.nodeSize / 2, NEATUtil.nodeSize, NEATUtil.nodeSize);
            g.setColor(new Color((~color) | (0xFF << 24), true));
            g.drawRect(point.x - NEATUtil.nodeSize / 2, point.y - NEATUtil.nodeSize / 2, NEATUtil.nodeSize, NEATUtil.nodeSize);
        }
        Text.drawText(550, 51, "Left");
        Text.drawText(550, 67, "Jump");
        Text.drawText(550, 83, "Right");
    }
}