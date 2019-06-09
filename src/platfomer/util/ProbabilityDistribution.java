package platfomer.util;

public class ProbabilityDistribution
{
    private static final double MAX_FLOAT_ERROR = 0.000001;
    private double[] probArr;
    private int[] labelArr;

    // Construct with the provided distribution probabilities.
    // The provided probabilities do not have to sum 1.0 as they will be normalised during construction.
    public ProbabilityDistribution(double[] probArr)
    {
        this.probArr = probArr;
        normaliseProbabilities();

        // Assign labels.
        labelArr = new int[probArr.length];
        for (int i = 0; i < this.probArr.length; i++)
        {
            labelArr[i] = i;
        }
    }

    // Construct with the provided distribution probabilities and associated labels.
    // The provided probabilities do not have to sum 1.0 as they will be normalised during construction.
    public ProbabilityDistribution(double[] probArr, int[] labelArr)
    {
        if (probArr.length != labelArr.length) throw new IllegalArgumentException("Array lengths are not equal.");

        this.probArr = probArr;
        normaliseProbabilities();
        this.labelArr = labelArr;
    }

    public ProbabilityDistribution(ProbabilityDistribution copy)
    {
        this(copy.probArr, copy.labelArr);
    }

    public double[] getProbabilities()
    {
        return probArr;
    }

    public int[] getLabels()
    {
        return labelArr;
    }

    // Remove the specified outcome from the set of probabilities and return as a new DiscreteDistribution object.
    public ProbabilityDistribution removeOutcome(int labelId)
    {
        // Find the item with specified label.
        int idx = 0;
        for (; idx < labelArr.length && labelArr[idx] != labelId; idx++) ;

        if (idx >= probArr.length)
        {
            throw new IllegalArgumentException("Invalid labelId");
        }

        double[] probArr = new double[this.probArr.length - 1];
        int[] labels = new int[this.probArr.length - 1];
        for (int i = 0; i < idx; i++)
        {
            probArr[i] = this.probArr[i];
            labels[i] = labelArr[i];
        }

        for (int i = idx + 1, j = idx; i < this.probArr.length; i++, j++)
        {
            probArr[j] = this.probArr[i];
            labels[j] = labelArr[i];
        }

        return new ProbabilityDistribution(probArr, labels);
    }

    public int sample()
    {
        // Obtain a random threshold value by sampling uniformly from interval [0,1).
        double thresh = NEATUtil.randDouble();

        // Loop through the discrete probabilities, accumulating as we go and stopping once
        // the accumulator is greater than the random sample.
        double acc = 0.0;
        for (int i = 0; i < probArr.length; i++)
        {
            acc += probArr[i];
            if (acc > thresh)
            {
                return labelArr[i];
            }
        }

        // We might get here through floating point arithmetic rounding issues.
        // e.g. accumulator == throwValue.

        // Find a nearby non-zero probability to select.
        // Wrap around to start of array.
        for (int i = 0; i < probArr.length; i++)
        {
            if (0.0 != probArr[i])
            {
                return labelArr[i];
            }
        }

        // If we get here then we have an array of zero probabilities.
        throw new IllegalStateException("Invalid distribution. No non-zero probabilities to select.");
    }

    private void normaliseProbabilities()
    {
        if (probArr.length == 0)
        {
            throw new IllegalArgumentException("Invalid probabilities array of zero length.");
        }

        // Calc sum(probArr).
        double total = 0.0;
        for (int i = 0; i < probArr.length; i++)
        {
            total += probArr[i];
        }

        // Handle special case where all provided probabilities are at or near zero;
        // in this case we evenly assign probabilities across all choices.
        if (total <= MAX_FLOAT_ERROR)
        {
            double p = 1.0 / probArr.length;
            for (int i = 0; i < probArr.length; i++)
            {
                probArr[i] = p;
            }
            return;
        }

        // Test if probabilities are already normalised (within reasonable limits of precision for floating point variables).
        if (Math.abs(1.0 - total) < MAX_FLOAT_ERROR)
        {   // Close enough!!
            return;
        }

        // Normalise the probabilities.
        double factor = 1.0 / total;
        for (int i = 0; i < probArr.length; i++)
        {
            probArr[i] *= factor;
        }
    }
}
