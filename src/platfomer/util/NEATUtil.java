package platfomer.util;

/**
 * Constants used by the NEAT algorithm
 */
public class NEATUtil
{
    //it's easier to ensure deterministic behavior with a global random
    private static final int DEFAULT_SEED = 1337;
    private static FastRandom random = new FastRandom();

    //dropoff
    public static final boolean dropoff = false;
    //Age when species start to be penalized
    public static final int dropoffAge = 15;
    public static final int novelAge = 10;                  //number of generations to protect new species
    public static final double ageSignificance = 1.0;        // benefit multiplier given to new species

    public static double survivalThresh = 0.1;        //The number of organisms that survive each epoch

    //XXX:
    //Run 1: Old mate multipoint
    //Runs 2-8: Mate multipoint adds weaker parent's genes
    //
    //Runs 1-4: no weight cap, relativeWeightPower=true
    //Rusn 5-8: normalized population on load, weight cap 15, relativeWeightPower=false

    //Complexifying Mutation probabilities
//    public static final double mutateLinkWeightsProb = 0.80;
    public static final double mutateLinkWeightsProb = 0.66;
//        public static final double mutateAddLinkProb = 0.17;
    public static final double mutateAddLinkProb = 0.32;
    public static final double mutateAddNodeProb = 0.01;
    public static final double mutateDeleteLinkProb = 0.01;
    public static final double mutateDeleteNodeProb = 0.00;

    //Simplifying mutation probabilities
    public static final double simplifyLinkWeightsProb = 0.59;
    public static final double simplifyAddLinkProb = 0.0;
    public static final double simplifyAddNodeProb = 0.0;
    public static final double simplifyDeleteLinkProb = 0.405;
    //Delete node is 0.0 during simplify to allow networks to slowly gain complexity.
    public static final double simplifyDeleteNodeProb = 0.005;

    public static final ProbabilityDistribution complexifyingMutationDistribution = new ProbabilityDistribution(new double[]{mutateLinkWeightsProb, mutateAddNodeProb, mutateAddLinkProb, mutateDeleteLinkProb, mutateDeleteNodeProb});
    public static final ProbabilityDistribution simplifyingMutationDistribution = new ProbabilityDistribution(new double[]{simplifyLinkWeightsProb, simplifyAddLinkProb, simplifyAddNodeProb, simplifyDeleteLinkProb, simplifyDeleteNodeProb});
//    public static final ProbabilityDistribution simplifyingMutationDistribution = new ProbabilityDistribution(new double[]{0.2, 0.0, 0.0, 0.7, 0.1});

    public static ProbabilityDistribution currentMutationDistribution = complexifyingMutationDistribution;

    public static final double complexifyingAsexualProportion = 0.8; //.2, .5, .7
    public static final double simplifyingAsexualProportion = 1.0;
    public static double currentAsexualProportion = complexifyingAsexualProportion;

    //////////////////////Mutation modifiers/////////////////////

    //Link modifiers
    //	public static final int mutateAddMinLinks = 1;
    public static final int mutateAddExtraLinks = 1;
    //	public static final int mutateRemoveMinLinks = 1;
    public static final int mutateRemoveExtraLinks = 1;

    public static final double doRecurOnlyProb = 0.0;
    //TODO: public static final double nonSensorOnlyProb = 0.1;
    public static final double mutateAddBiasProb = 0.05;
    public static final int newlinkTries = 20;

    //non-destructive link delete
    public static final boolean linkDeleteNonDestructive = false;
    public static final boolean linkRelativeWeightPower = false;

    public static final double weightMutPower = 2.0;
    //Keep link weights in the range of -weightCap to +weightCap
    //When enabled, it seems networks just evolve more nodes so that the sum of the weights is higher than the cap
    public static final boolean capWeights = false;
    public static final double weightCap = 5.0;

    /////////////////Mate Modifiers//////////////////////////////
    //The chance to choose one mating method or the other
    public static final double interspeciesMate = 0.1;
    //How many disjoint/excess genes to pass on from the less-fit parent
    public static final double excessInheritProb = 0.1;

    //The target number of species to have in the population
    public static final boolean keepConstantNumSpecies = false;
    public static int targetNumSpecies = 5;
    public static final double speciesThresholdDelta = 0.3;
    // The maximum distance for organisms to be considered the same species
    public static double speciesThreshold = 10.0;
    //Constants for controlling species' distance from each other
    //disjoint and excess measures the number of different nodes
    //mutDiff measures the difference in weight between structurally similar links
    public static final double disjointCoeff = 1.0;
    public static final double excessCoeff = 1.0;
    public static final double mutDiffCoeff = 0.4;

    //////////////////Search Phase//////////////////////////
    public enum SearchPhase
    {
        COMPLEXIFY,
        SIMPLIFY
    }

    public static SearchPhase currentSearchPhase = SearchPhase.COMPLEXIFY;

    //How complex the population should get before switching to simplify
    public static final int COMPLEXITY_CEILING = 30;
    //Minimum number of generations to stay in simplify
    public static final int MIN_SIMPLIFY_GENERATIONS = 25;
    //How many generations to track the mean complexity of the population
    //This indirectly affects parts of the search like the length of the simplify phase.
    public static final int COMPLEXITY_MOVING_AVERAGE_LENGTH = 30;

    //////////////////GRAPH VARS/////////////////////////////
    public static int nodeSize = 6;
    public static int inputWidth = 13;
    public static int inputHeight = 13;

    public static void reseedRandomToDefault()
    {
        random = new FastRandom(DEFAULT_SEED);
    }

    public static void reseedRandom()
    {
        random = new FastRandom();
    }

    public static double sigmoid(double x)
    {
        return 2.0 / (1 + Math.exp(-4.924273 * x)) - 1.0;
    }

    public static double tanh(double x)
    {
        double exp = Math.exp(2 * x);
        return (exp - 1) / (exp + 1);
    }

    public static double relu(double x)
    {
        return Math.max(0, x);
    }

    public static double linear(double x)
    {
        return x;
    }

    //Utility functions
    public static double randDouble()
    {
        return random.nextDouble();
    }

    public static double randDouble(double d)
    {
        double r = random.nextDouble() * d;
        double sign = random.nextInt(2) == 1 ? -1.0 : 1.0;
        return sign * r;
    }

    public static double randGaussian()
    {
        return random.nextGaussian();
    }

    public static int randInt(int max)
    {
        return random.nextInt(max);
    }

    public static int randInt(int min, int max)
    {
        if ((max - min) + 1 <= 0)
        {
            return 0;
        }

        return random.nextInt((max - min) + 1) + min;
    }
}
