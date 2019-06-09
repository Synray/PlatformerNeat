package platfomer.genetics;

import platfomer.util.AverageBuffer;
import platfomer.util.NEATUtil;
import platfomer.util.ProbabilityDistribution;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;

public class Population
{
    //private ArrayList<Organism> organisms;
    private ArrayList<Genome> genomes;
    private ArrayList<Species> species;
    private SpecieStats[] specieStats;

    private int lastSpecies;
    private int bestSpecies;
    public Genome currentBestGenome;

    //Contains the mutations generated each epoch
    //Any organism that generates the same mutation then gets the same number
    private ArrayList<Innovation> innovations;
    private int globalInnovationNum = 1;

    //The last mutated node in the population
    private int curNodeID = 0;

    //Statistical variables for use with noisy data sets
    private double meanFitness;
    private double variance;
    private double standardDeviation;


    private int generation;
    private int winnerGen;

    private double maxFitness;
    private int highestLastChanged;

    private int popSize;

    //Variables to keep track of whether or not the fitness improved during the
    //simplification phase. If not, then the max complexity increases by a small amount
    private double complexifyingFitness;
    private int phasesWithoutImproving;

    private double meanComplexity;
    private double minComplexityThisPhase;

    private double currentComplexityCeiling = -1;
    private int lastTransitionGeneration;

    private AverageBuffer complexityMovingAverage;
    private double prevComplexityMovingAverage;

    {
        complexityMovingAverage = new AverageBuffer(NEATUtil.COMPLEXITY_MOVING_AVERAGE_LENGTH);
        NEATUtil.currentSearchPhase = NEATUtil.SearchPhase.COMPLEXIFY;
        NEATUtil.currentMutationDistribution = NEATUtil.complexifyingMutationDistribution;
        NEATUtil.currentAsexualProportion = NEATUtil.complexifyingAsexualProportion;
    }

    //Generate from a seed genome
    public Population(Genome g, int size)
    {
        this.genomes = new ArrayList<>();
        this.species = new ArrayList<>();
        this.innovations = new ArrayList<>();
        spawn(g, size);

        popSize = size;
    }

    //generate a random population with nin inputs, nout outputs, and up to nmax hidden nodes, connected randomly
    public Population(int size, int nin, int nout)
    {
        this.genomes = new ArrayList<>();
        this.species = new ArrayList<>();
        this.innovations = new ArrayList<>();

        popSize = size;

        Genome g = new Genome(0, nin, nout);
        spawn(g, size);
    }

    //    load a population from a file
    public Population(String filename) throws IOException
    {
        int BUFFER_SIZE = 1024;
        this.genomes = new ArrayList<>();
        this.species = new ArrayList<>();
        this.innovations = new ArrayList<>();

        curNodeID = 0;
        globalInnovationNum = 0;
        try (FileReader fr = new FileReader(filename); BufferedReader br = new BufferedReader(fr))
        {
            String line;
            br.mark(BUFFER_SIZE);
            while ((line = br.readLine()) != null)
            {
                String[] data = line.split(" ");
                if (data[0].equals("genomestart") || data[0].startsWith("/*"))
                {
                    Genome newGenome = new Genome(br);
                    genomes.add(newGenome);
                    if (curNodeID < newGenome.getLastNodeID())
                    {
                        curNodeID = newGenome.getLastNodeID();
                    }
                    if (globalInnovationNum < newGenome.getLastGeneInnovnum())
                    {
                        globalInnovationNum = newGenome.getLastGeneInnovnum();
                    }
                }
                br.mark(BUFFER_SIZE);
            }
            popSize = genomes.size();
            //Normalize each of the genomes
//            for (int i = 0; i < popSize; ++i)
//            {
//                genomes.get(i).normalizeWeights(NEATUtil.weightCap);
//            }


            speciate();
            System.out.println("Population of " + genomes.size() + " organisms loaded successfuly.");
        }
    }

    public void resetFitness()
    {
        for (int i = 0; i < species.size(); i++)
        {
            Species s = species.get(i);
            s.maxFitnessEver = 0;
            s.age = 0;
            s.ageLastImprovement = 0;
        }
        for (Genome g : genomes)
        {
            g.winner = false;
        }
        complexifyingFitness = 0.0;
        phasesWithoutImproving = 0;
        maxFitness = 0.0;
    }

    public void nextGeneration()
    {
        generation++;
        System.out.println("Generation " + generation);
        sortSpecieGenomes();
        updateBestGenome();

        System.out.println("Best fitness: " + currentBestGenome.fitness);

        int numOffspring = calcSpecieStats();
        ArrayList<Genome> offspring = createOffspring(numOffspring);

        boolean emptySpecies = trimToElites();

        if (emptySpecies)
        {
            //Remove empty species, keeping the SpecieStats array aligned
            int speciesIdx = 0;
            for (int i = 0; i < specieStats.length; ++i)
            {
                if (specieStats[i].eliteSizeInt == 0)
                {
                    species.remove(speciesIdx);
                }
                else
                {
                    specieStats[speciesIdx++] = specieStats[i];
                }
            }
        }

        rebuildGenomeList();

        genomes.addAll(offspring);

        //speciate the offspring
        for (Genome genome : offspring)
        {
            for (Species curSpecies : species)
            {
                if (curSpecies.size() > 0)
                {
                    double comp = genome.compatibility(curSpecies.first());
                    if (comp < NEATUtil.speciesThreshold)
                    {
                        genome.species = curSpecies;
                        break;
                    }
                }
            }

            if (genome.species == null)
            {
                genome.species = newSpecies();
                species.add(genome.species);
            }

            genome.species.addGenome(genome);
        }

        updateComplexityStats();
        determineComplexityMode();

        if ((NEATUtil.currentSearchPhase == NEATUtil.SearchPhase.SIMPLIFY && innovations.size() > 20) || innovations.size() >= 20000)// || ((generation-lastTransitionGeneration)+1) % 200 == 0)
        {
//            System.out.println("Clearing innovations. Removed: " + innovations.size());
            innovations.clear();
        }
    }

    private int calcSpecieStats()
    {
        int numSpecies = species.size();
        specieStats = new SpecieStats[numSpecies];
        double totalAveFitness = 0.0;

        for (int i = 0; i < numSpecies; ++i)
        {
            SpecieStats stats = new SpecieStats();
            specieStats[i] = stats;
            Species s = species.get(i);

            s.age++;
            double maxFitness = s.calcMaxFitness();
            if (maxFitness > s.maxFitnessEver)
            {
                s.maxFitnessEver = maxFitness;
                s.ageLastImprovement = s.age;
            }

            stats.aveFitness = s.calcAveFitness();

            //If the search is complexifying, remove species that haven't improved
            //recently, (unless it is the best species).
            //This is skipped during the simplifying phase because it can cause a pathological
            //complexity feedback loop:
            //1. During simplification, fitness will only increase when harmful genes are removed,
            //  or rarely by weight mutations
            //2. After an initial rise in fitness in each species caused by removing
            //  negative genes, fitness will generally stall until the neutral genes are removed
            //3. If the best species is complex enough, then there will be enough
            //  neutral genes that the simplified species will become stale before all of the genes
            //  are removed.
            //4. when the "stale" simplified species are removed, the average complexity
            //  suddenly rises, ending the simplify phase prematurely
            //
            //One run of xor that performed stale pruning in both phases
            // took 15273 generations and ended with a mean complexity of 93.982,
            // compared to averages of 380 and 18!
            if (NEATUtil.dropoff)
            {
                if (NEATUtil.currentSearchPhase == NEATUtil.SearchPhase.COMPLEXIFY)
                {
                    if (s.lastImproved() >= NEATUtil.dropoffAge && i != bestSpecies)
                    {
                        stats.aveFitness *= 0.001;
                    }
                }
//                else
//                {
//                    s.ageLastImprovement = s.age;
//                }
            }

            totalAveFitness += stats.aveFitness;
        }

        //Update the species compatibility threshold
        if (NEATUtil.keepConstantNumSpecies)
        {
            if (generation > 1)
            {
                if (species.size() < NEATUtil.targetNumSpecies)
                    NEATUtil.speciesThreshold -= NEATUtil.speciesThresholdDelta;
                else if (species.size() > NEATUtil.targetNumSpecies)
                    NEATUtil.speciesThreshold += NEATUtil.speciesThresholdDelta;

                if (NEATUtil.speciesThreshold < NEATUtil.speciesThresholdDelta)
                    NEATUtil.speciesThreshold = NEATUtil.speciesThresholdDelta;
            }
        }

        //Calculate the number offspring to give to each of the species
        int totalTargetSizeInt = 0;
        if (totalAveFitness == 0.0)
        {
            // All genomes have 0 fitness, so assign all species an equal number of offspring
            double targetSizeReal = (double) popSize / numSpecies;

            for (int i = 0; i < numSpecies; ++i)
            {
                SpecieStats stats = specieStats[i];
                stats.targetSizeReal = targetSizeReal;

                stats.targetSizeInt = (int) probabilisticRound(targetSizeReal);

                totalTargetSizeInt += stats.targetSizeInt;
            }
        }
        else
        {
            for (int i = 0; i < numSpecies; ++i)
            {
                SpecieStats stats = specieStats[i];
                stats.targetSizeReal = (stats.aveFitness / totalAveFitness) * popSize;
                stats.targetSizeInt = (int) probabilisticRound(stats.targetSizeReal);

                totalTargetSizeInt += stats.targetSizeInt;
            }
        }

        int targetSizeDifference = totalTargetSizeInt - popSize;

        if (targetSizeDifference < 0)
        {
            if (targetSizeDifference == -1)
            {
                specieStats[bestSpecies].targetSizeInt++;
            }
            else
            {
                double[] probabilities = new double[numSpecies];
                for (int i = 0; i < numSpecies; i++)
                {
                    SpecieStats stats = specieStats[i];
                    probabilities[i] = Math.max(0.0, stats.targetSizeReal - stats.targetSizeInt);
                }

                ProbabilityDistribution dist = new ProbabilityDistribution(probabilities);

                targetSizeDifference *= -1;
                for (int i = 0; i < targetSizeDifference; i++)
                {
                    int specieIdx = dist.sample();
                    specieStats[specieIdx].targetSizeInt++;
                }
            }
        }
        else if (targetSizeDifference > 0)
        {
            double[] probabilities = new double[numSpecies];
            for (int i = 0; i < numSpecies; i++)
            {
                SpecieStats stats = specieStats[i];
                probabilities[i] = Math.max(0.0, stats.targetSizeInt - stats.targetSizeReal);
            }

            ProbabilityDistribution dist = new ProbabilityDistribution(probabilities);

            // Probabilistically decrement specie target sizes.
            for (int i = 0; i < targetSizeDifference; )
            {
                int specieIdx = dist.sample();

                // Skip empty species. This can happen because the same species can be selected more than once.
                if (0 != specieStats[specieIdx].targetSizeInt)
                {
                    specieStats[specieIdx].targetSizeInt--;
                    i++;
                }
            }
        }

        //Make sure the best species survives
        if (specieStats[bestSpecies].targetSizeInt == 0)
        {
            specieStats[bestSpecies].targetSizeInt++;

            boolean done = false;
            for (int i = 0; i < numSpecies; ++i)
            {
                if (i != bestSpecies && specieStats[i].targetSizeInt > 0)
                {
                    specieStats[i].targetSizeInt--;
                    done = true;
                    break;
                }
            }

            if (!done)
            {
                throw new IllegalStateException("calcSpecieStats: Error adjusting target population size down.");
            }
        }

        //Determine the eliteSize for each species. This is the number of genomes that will remain from the current
        //generation. Also calculate the number of offspring that need to be made
        int numOffspring = 0;
        for (int i = 0; i < numSpecies; ++i)
        {
            if (specieStats[i].targetSizeInt == 0)
            {
                specieStats[i].eliteSizeInt = 0;
                continue;
            }

            double eliteSizeReal = species.get(i).genomes.size() * NEATUtil.survivalThresh;
            int eliteSizeInt = (int) probabilisticRound(eliteSizeReal);

            SpecieStats stats = specieStats[i];
            stats.eliteSizeInt = Math.min(eliteSizeInt, stats.targetSizeInt);

            //Ensure that the best genome always survives
            if (i == bestSpecies && stats.eliteSizeInt == 0)
            {
                stats.eliteSizeInt = 1;
            }

            stats.offspringCount = stats.targetSizeInt - stats.eliteSizeInt;
            numOffspring += stats.offspringCount;

            double offspringAsexualCountReal = stats.offspringCount * NEATUtil.currentAsexualProportion;
            stats.offspringAsexualCount = (int) probabilisticRound(offspringAsexualCountReal);
            stats.offspringSexualCount = stats.offspringCount - stats.offspringAsexualCount;

            double selectionSizeReal = species.get(i).genomes.size() * NEATUtil.survivalThresh;
            stats.selectionSizeInt = Math.max(1, (int) probabilisticRound(selectionSizeReal));
        }

        return numOffspring;
    }

    private static double probabilisticRound(double val)
    {
        double integerPart = Math.floor(val);
        double fractionalPart = val - integerPart;
        return NEATUtil.randDouble() < fractionalPart ? integerPart + 1.0 : integerPart;
    }

    private ArrayList<Genome> createOffspring(int numOffspring)
    {
        int numSpecies = specieStats.length;
        double[] specieFitness = new double[numSpecies];
        ProbabilityDistribution[] distArr = new ProbabilityDistribution[numSpecies];

        int nonZeroSpecieCount = 0;
        for (int i = 0; i < numSpecies; ++i)
        {
            // Array of probabilities for specie selection. Note that some of these probabilities can be zero, but at least one of them won't be.
            SpecieStats inst = specieStats[i];
            specieFitness[i] = inst.selectionSizeInt;

            if (0 == inst.selectionSizeInt)
            {
                // Skip building a ProbabilityDistribution for species that won't be selected from.
                distArr[i] = null;
                continue;
            }

            nonZeroSpecieCount++;

            // For each specie we build a ProbabilityDistribution for genome selection within
            // that specie. Fitter genomes have higher probability of selection.
            ArrayList<Genome> genomeList = species.get(i).genomes;
            double[] probabilities = new double[inst.selectionSizeInt];
            for (int j = 0; j < inst.selectionSizeInt; j++)
            {
                probabilities[j] = genomeList.get(j).fitness;
            }
            distArr[i] = new ProbabilityDistribution(probabilities);
        }

        // Complete construction of ProbabilityDistribution for specie selection.
        ProbabilityDistribution rwlSpecies = new ProbabilityDistribution(specieFitness);

        // Produce offspring from each specie in turn and store them in offspringList.
        ArrayList<Genome> offspringList = new ArrayList<>(numOffspring);
        for (int specieIdx = 0; specieIdx < numSpecies; specieIdx++)
        {
            SpecieStats inst = specieStats[specieIdx];
            ArrayList<Genome> genomeList = species.get(specieIdx).genomes;

            // Get ProbabilityDistribution for genome selection.
            ProbabilityDistribution dist = distArr[specieIdx];

            //the null entries should never be picked here, because
            //they receive a normalized probability of 0
            assert dist != null;

            // --- Produce the required number of offspring from asexual reproduction.
            for (int i = 0; i < inst.offspringAsexualCount; i++)
            {
                int genomeIdx = dist.sample();
                Genome offspring = genomeList.get(genomeIdx).createOffspring(this, generation);
                offspringList.add(offspring);
            }
            //stats.asexualOffspringCount += (ulong) inst.offspringAsexualCount;

            // --- Produce the required number of offspring from sexual reproduction.
            // Cross-specie mating.
            // If nonZeroSpecieCount is exactly 1 then we skip inter-species mating. One is a special case because
            // for 0 the  species all get an even chance of selection, and for >1 we can just select species normally.
            int crossSpecieMatings = nonZeroSpecieCount == 1 ? 0 :
                    (int) probabilisticRound(NEATUtil.interspeciesMate * inst.offspringSexualCount);

            // An index that keeps track of how many offspring have been produced in total.
            int matingsCount = 0;
            for (; matingsCount < crossSpecieMatings; matingsCount++)
            {
                Genome offspring = createOffspringCrossSpecieMating(dist, distArr, rwlSpecies, specieIdx, genomeList);
                offspringList.add(offspring);
            }

            // For the remainder we use normal intra-specie mating.
            // Test for special case - we only have one genome to select from in the current specie.
            if (1 == inst.selectionSizeInt)
            {
                // Fall-back to asexual reproduction.
                for (; matingsCount < inst.offspringSexualCount; matingsCount++)
                {
                    int genomeIdx = dist.sample();
                    Genome offspring = genomeList.get(genomeIdx).createOffspring(this, generation);
                    offspringList.add(offspring);
                }
            }
            else
            {
                // Remainder of matings are normal within-specie.
                for (; matingsCount < inst.offspringSexualCount; matingsCount++)
                {
                    // Select parent 1.
                    int parent1Idx = dist.sample();
                    Genome parent1 = genomeList.get(parent1Idx);

                    // Remove selected parent from set of possible outcomes.
                    ProbabilityDistribution distTmp = dist.removeOutcome(parent1Idx);

                    // Test for existence of at least one more parent to select.
                    if (0 != distTmp.getProbabilities().length)
                    {   // Get the two parents to mate.
                        int parent2Idx = distTmp.sample();
                        Genome parent2 = genomeList.get(parent2Idx);
                        Genome offspring = parent1.createOffspring(parent2, this, generation);
                        offspringList.add(offspring);
                    }
                    else
                    {   // No other parent has a non-zero selection probability (they all have zero fitness).
                        // Fall back to asexual reproduction of the single genome with a non-zero fitness.
                        Genome offspring = parent1.createOffspring(this, generation);
                        offspringList.add(offspring);
                    }
                }
            }
        }

        return offspringList;
    }

    private Genome createOffspringCrossSpecieMating(ProbabilityDistribution dist, ProbabilityDistribution[] distArr, ProbabilityDistribution rwlSpecies, int currentSpecieIdx, ArrayList<Genome> genomeList)
    {
        // Select parent from current specie.
        int parent1Idx = dist.sample();

        // Select specie other than current one for 2nd parent genome.
        ProbabilityDistribution distSpeciesTmp = rwlSpecies.removeOutcome(currentSpecieIdx);
        int specie2Idx = distSpeciesTmp.sample();

        // Select a parent genome from the second specie.
        int parent2Idx = distArr[specie2Idx].sample();

        // Get the two parents to mate.
        Genome parent1 = genomeList.get(parent1Idx);
        Genome parent2 = species.get(specie2Idx).genomes.get(parent2Idx);
        return parent1.createOffspring(parent2, this, generation);
    }

    private void updateBestGenome()
    {
        Genome bestGenome = genomes.get(0);
        double bestFitness = -1.0;
        int bestSpeciesIdx = 0;
        for (int i = 0; i < species.size(); ++i)
        {
            Genome genome = species.get(i).genomes.get(0);
            if (genome.fitness > bestFitness)
            {
                bestGenome = genome;
                bestFitness = genome.fitness;
                bestSpeciesIdx = i;
            }
            else if (!(genome.fitness < bestFitness) && genome.generation > bestGenome.generation)
            {
                bestGenome = genome;
                bestFitness = genome.fitness;
                bestSpeciesIdx = i;
            }
        }

        currentBestGenome = bestGenome;

        bestSpecies = bestSpeciesIdx;

        if (bestFitness > maxFitness)
        {
            maxFitness = bestFitness;
            highestLastChanged = 0;
        }
        else
        {
            highestLastChanged++;
        }
    }

    //Update mean, max, and moving average complexity stats
    private void updateComplexityStats()
    {
        //Update stats, and change search phases
        double totalComplexity = genomes.get(0).getComplexity();
        for (Genome g : genomes)
        {
            totalComplexity += g.getComplexity();
        }

        meanComplexity = totalComplexity / popSize;
        prevComplexityMovingAverage = complexityMovingAverage.getMean();
        complexityMovingAverage.enqueue(meanComplexity);
    }

    public void startSimplifying()
    {
        if (NEATUtil.SearchPhase.COMPLEXIFY != NEATUtil.currentSearchPhase) return;

        NEATUtil.currentSearchPhase = NEATUtil.SearchPhase.SIMPLIFY;
        NEATUtil.currentMutationDistribution = NEATUtil.simplifyingMutationDistribution;
        NEATUtil.currentAsexualProportion = NEATUtil.simplifyingAsexualProportion;
        int numGens = generation - lastTransitionGeneration;
        lastTransitionGeneration = generation;

        minComplexityThisPhase = meanComplexity;
        if (maxFitness >= complexifyingFitness * 1.01)
        {
            complexifyingFitness = maxFitness;
            if (--phasesWithoutImproving < 0)
            {
                phasesWithoutImproving = 0;
            }
        }
        else
        {
            phasesWithoutImproving++;
        }

        System.out.println("Forced SIMPLIFY @ generation " + generation + " after " + numGens);
    }

    public void startComplexifying()
    {
        if (NEATUtil.SearchPhase.SIMPLIFY != NEATUtil.currentSearchPhase) return;

        NEATUtil.currentSearchPhase = NEATUtil.SearchPhase.COMPLEXIFY;
        NEATUtil.currentMutationDistribution = NEATUtil.complexifyingMutationDistribution;
        NEATUtil.currentAsexualProportion = NEATUtil.complexifyingAsexualProportion;
        int numGens = generation - lastTransitionGeneration;
        lastTransitionGeneration = generation;
        System.out.println("Forced COMPLEXIFY @ generation " + generation + " after " + numGens);

        //If fitness improved during simplfication, a disadvantageous link was removed
        if (maxFitness >= complexifyingFitness * 1.01)
        {
            complexifyingFitness = maxFitness;
            if (--phasesWithoutImproving < 0)
            {
                phasesWithoutImproving = 0;
            }
        }
//                currentComplexityCeiling = meanComplexity * 1.10 + NEATUtil.COMPLEXITY_CEILING + phasesWithoutImproving * (NEATUtil.COMPLEXITY_CEILING / 2.0);
        currentComplexityCeiling = meanComplexity + NEATUtil.COMPLEXITY_CEILING;
        System.out.println("New complexity ceiling: " + currentComplexityCeiling);
    }

    private void determineComplexityMode()
    {
//        currentComplexityCeiling = 118.15666666666667;
//        phasesWithoutImproving = 0;
//        prevComplexityMovingAverage = 0.0;
//        currentComplexityCeiling = meanComplexity * 1.10 + NEATUtil.COMPLEXITY_CEILING + phasesWithoutImproving * (NEATUtil.COMPLEXITY_CEILING / 2.0);
//        System.out.println("New complexity ceiling: " + currentComplexityCeiling);

        if (NEATUtil.SearchPhase.COMPLEXIFY == NEATUtil.currentSearchPhase)
        {
            if (currentComplexityCeiling < 0.0)
            {
                currentComplexityCeiling = meanComplexity + NEATUtil.COMPLEXITY_CEILING;
            }
            // Currently complexifying, test if the complexity ceiling has been reached.
            else if (meanComplexity > currentComplexityCeiling)
            {   // Switch to simplifying mode.
                NEATUtil.currentSearchPhase = NEATUtil.SearchPhase.SIMPLIFY;
                NEATUtil.currentMutationDistribution = NEATUtil.simplifyingMutationDistribution;
                NEATUtil.currentAsexualProportion = NEATUtil.simplifyingAsexualProportion;
                int numGens = generation - lastTransitionGeneration;
                lastTransitionGeneration = generation;

                minComplexityThisPhase = meanComplexity;
                if (maxFitness >= complexifyingFitness * 1.01)
                {
                    complexifyingFitness = maxFitness;
                    if (--phasesWithoutImproving < 0)
                    {
                        phasesWithoutImproving = 0;
                    }
                }
                else
                {
                    phasesWithoutImproving++;
                }

                System.out.println("Phase SIMPLIFY @ generation " + generation + " after " + numGens);
            }
        }
        else
        {   // Currently simplifying. Test if simplification (ongoing reduction in complexity) has stalled.
            // We allow simplification to progress for a few generations before testing if it has stalled, this allows
            // a lead in time for the effects of simplification to occur.
            // In addition we do not switch to complexifying if complexity is above the currently defined ceiling.
            if (meanComplexity < minComplexityThisPhase)
            {
                minComplexityThisPhase = meanComplexity;
            }
//            if ((meanComplexity < currentComplexityCeiling && generation - lastTransitionGeneration > NEATUtil.MIN_SIMPLIFY_GENERATIONS
//                    || generation - lastTransitionGeneration > NEATUtil.MIN_SIMPLIFY_GENERATIONS * 2)
//                    && complexityMovingAverage.getMean() - prevComplexityMovingAverage >= 0.0)
            if (generation - lastTransitionGeneration > NEATUtil.MIN_SIMPLIFY_GENERATIONS
                    && complexityMovingAverage.getMean() - prevComplexityMovingAverage >= 0.0)
            {   // Simplification has stalled. Switch back to complexification.
                NEATUtil.currentSearchPhase = NEATUtil.SearchPhase.COMPLEXIFY;
                NEATUtil.currentMutationDistribution = NEATUtil.complexifyingMutationDistribution;
                NEATUtil.currentAsexualProportion = NEATUtil.complexifyingAsexualProportion;
                int numGens = generation - lastTransitionGeneration;
                lastTransitionGeneration = generation;
                System.out.println("Phase COMPLEXIFY @ generation " + generation + " after " + numGens);

                //If fitness improved during simplfication, a disadvantageous link was removed
                if (maxFitness >= complexifyingFitness * 1.01)
                {
                    complexifyingFitness = maxFitness;
                    if (--phasesWithoutImproving < 0)
                    {
                        phasesWithoutImproving = 0;
                    }
                }
//                currentComplexityCeiling = meanComplexity * 1.10 + NEATUtil.COMPLEXITY_CEILING + phasesWithoutImproving * (NEATUtil.COMPLEXITY_CEILING / 2.0);
                currentComplexityCeiling = meanComplexity + NEATUtil.COMPLEXITY_CEILING;
                System.out.println("New complexity ceiling: " + currentComplexityCeiling);
            }
        }
        System.out.println("Mean complexity: " + meanComplexity + ", Moving average complexity: " + complexityMovingAverage.getMean());
    }

    private void sortSpecieGenomes()
    {
        for (Species s : species)
        {
            s.genomes.sort(genomeComparator);
        }
    }

    //Sort species per highest performance
    private static Comparator<Genome> genomeComparator = (a, b) ->
    {
        // Primarily sort by fitness, highest fitness first
        if (a.fitness > b.fitness)
        {
            return -1;
        }
        else if (a.fitness < b.fitness)
        {
            return 1;
        }

        /* fitnesses are equal, so sort by age
         * youngest first. younger genomes have a higher generation than older genomes.
         *
         *      This means the complexity will drift in the direction of the current search phase.
         * During complexification, the best genomes will be replaced by more and more complex
         * ones. During simplification, the opposite happens.
         *
         *      It's good for the population to drift in this way. if species were only sorted
         * by fitness, then only offspring whose fitness improved over the previous generation
         * would survive. Offspring that evolved neutral genes, genes that did not improve
         * fitness but could be further mutated to become useful, would die off.
         */
        if (a.generation > b.generation)
        {
            return -1;
        }
        else if (a.generation < b.generation)
        {
            return 1;
        }

        return 0;
    };

    private boolean trimToElites()
    {
        boolean emptySpecies = false;
        for (int i = 0; i < species.size(); ++i)
        {
            Species specie = species.get(i);
            SpecieStats stats = specieStats[i];

            specie.genomes.subList(stats.eliteSizeInt, specie.genomes.size()).clear();

            if (stats.eliteSizeInt == 0)
            {
                emptySpecies = true;
            }
        }

        return emptySpecies;
    }

    //Remove the old genomes from the list, only keep the genomes that are in a species
    private void rebuildGenomeList()
    {
        genomes.clear();
        for (Species s : species)
        {
            genomes.addAll(s.genomes);
        }
    }

    /**
     * Seed a population of <b>{@code size}</b> organisms from the genome <b>{@code g}</b>
     */
    private void spawn(Genome g, int size)
    {
        if (size == 0) throw new IllegalArgumentException("Size must be > 0!");

        int count;
        Genome newGenome = g;
        for (count = 0; count < size; count++)
        {
            newGenome = new Genome(g, generation);
            newGenome.mutateLinkWeights(NEATUtil.weightMutPower);
            genomes.add(newGenome);
        }

        curNodeID = newGenome.getLastNodeID();
        globalInnovationNum = newGenome.getLastGeneInnovnum();

        speciate();
    }

    //Separate organisms into species
    private void speciate()
    {
        Species newSpecies;

        int counter = 0;
        for (int g = 0, genomesSize = genomes.size(); g < genomesSize; g++)
        {
            Genome curGenome = genomes.get(g);
            if (species.size() == 0)
            {
                newSpecies = new Species(counter++);
                species.add(newSpecies);
                newSpecies.addGenome(curGenome);
            }
            else
            {
                Genome compGenome = null;
                for (int s = 0, speciesSize = species.size(); s < speciesSize; s++)
                {
                    Species curSpecies = species.get(s);
                    compGenome = curSpecies.first();
                    if (compGenome != null)
                    {
                        if (curGenome.compatibility(compGenome) < NEATUtil.speciesThreshold)
                        {
                            curSpecies.addGenome(curGenome);
                            compGenome = null;
                            break;
                        }
                    }
                }

                if (compGenome != null)
                {
                    newSpecies = new Species(counter++);
                    species.add(newSpecies);
                    newSpecies.addGenome(curGenome);
                }
            }
        }

        lastSpecies = counter;
    }

    public void printToFileBySpecies(String fileName)
    {
        Path path = Paths.get(fileName);
        if (!Files.exists(path))
        {
            try
            {
                Files.createFile(path);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true)))
        {
            for (int i = 0, speciesSize = species.size(); i < speciesSize; i++)
            {
                Species value = species.get(i);
                value.printToFile(bw);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public int currentBestGenome()
    {
        if (currentBestGenome == null) return 0;
        return genomes.indexOf(currentBestGenome);
    }

    public int newInnovation()
    {
        return globalInnovationNum++;
    }

    public int currentInnovation()
    {
        return globalInnovationNum;
    }

    public int getCurNodeID()
    {
        return curNodeID;
    }

    public void setCurNodeID(int id)
    {
        this.curNodeID = id;
    }

    public double getMeanFitness()
    {
        return meanFitness;
    }

    public void setMeanFitness(double meanFitness)
    {
        this.meanFitness = meanFitness;
    }

    public double getVariance()
    {
        return variance;
    }

    public void setVariance(double variance)
    {
        this.variance = variance;
    }

    public double getStandardDeviation()
    {
        return standardDeviation;
    }

    public void setStandardDeviation(double standardDeviation)
    {
        this.standardDeviation = standardDeviation;
    }

    public int getWinnerGen()
    {
        return winnerGen;
    }

    public void setWinnerGen(int winnerGen)
    {
        this.winnerGen = winnerGen;
    }

    public int getGeneration()
    {
        return generation;
    }

    public void setGeneration(int generation)
    {
        this.generation = generation;
    }

    public double getMaxFitness()
    {
        return maxFitness;
    }

    public void setMaxFitness(double maxFitness)
    {
        this.maxFitness = maxFitness;
    }

    public int getHighestLastChanged()
    {
        return highestLastChanged;
    }

    public void setHighestLastChanged(int highestLastChanged)
    {
        this.highestLastChanged = highestLastChanged;
    }

    public double getMeanComplexity()
    {
        return meanComplexity;
    }

    public void setMeanComplexity(double meanComplexity)
    {
        this.meanComplexity = meanComplexity;
    }

    public ArrayList<Innovation> getInnovations()
    {
        return innovations;
    }

    public ArrayList<Species> getSpecies()
    {
        return species;
    }

    private Species newSpecies()
    {
        return new Species(lastSpecies++, true);
    }

    public ArrayList<Genome> getGenomes()
    {
        return genomes;
    }

    public Genome getGenome(int index)
    {
        return genomes.get(index);
    }

    public int size()
    {
        return popSize;
    }

    private static class SpecieStats
    {
        double aveFitness;
        double targetSizeReal;

        // Integer stats.
        int targetSizeInt;
        int eliteSizeInt;
        int offspringCount;
        int offspringAsexualCount;
        int offspringSexualCount;

        // Selection data.
        int selectionSizeInt;
    }
}
