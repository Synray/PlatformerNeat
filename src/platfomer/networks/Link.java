package platfomer.networks;

//Represents connections between nodes
public class Link
{
    public double weight;
    public int in;
    public int out;

    public Link()
    {

    }

    public Link(double w, int in, int out, boolean recur)
    {
        this.weight = w;
        this.in = in;
        this.out = out;
    }
}
