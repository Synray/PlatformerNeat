package platfomer.genetics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

//Contains a group of similar organisms
public class Species
{
    public final int id;
    public int age;
    public int ageLastImprovement;
    public double maxFitnessEver;

    public boolean novel;

    public ArrayList<Genome> genomes;

    public Species(int id)
    {
        this(id, true);
    }

    public Species(int id, boolean n)
    {
        this.id = id;
        novel = n;
        genomes = new ArrayList<>();
    }

    public void addGenome(Genome g)
    {
        genomes.add(g);
        g.species = this;
    }

    public void remove(Genome g)
    {
        genomes.remove(g);
    }

    public int size()
    {
        return genomes.size();
    }

    public Genome first()
    {
        return genomes.get(0);
    }

    public double calcMaxFitness()
    {
        double max = 0.0;
        for (Genome genome : genomes)
        {
            if (max < genome.fitness)
            {
                max = genome.fitness;
            }
        }
        return max;
    }

    public double calcAveFitness()
    {
        double total = 0.0;
        for (Genome genome : genomes)
        {
            total += genome.fitness;
        }
        return total / genomes.size();
    }

    public void printToFile(BufferedWriter bw) throws IOException
    {
        bw.write("/* Species: " + id + "*/\n");
        for (Genome genome : genomes)
        {
            genome.printToFile(bw);
        }
    }

    public int lastImproved()
    {
        return age - ageLastImprovement;
    }
    public void setNovel(boolean novel)
    {
        this.novel = novel;
    }
}