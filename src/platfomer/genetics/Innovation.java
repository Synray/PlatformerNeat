package platfomer.genetics;

//Global ID that represents a mutation
public class Innovation
{
    public enum InnovType
    {
        NODE, LINK
    }

    public final InnovType innovType;

    public final int nodeInID;
    public final int nodeOutID;

    public final int innovNum;
    public final int innovNum2;

    public final double newWeight;

    public final int newNodeID;

    //Only used in a NodeGene mutation
    //The innovation number of the link gene that was split when the node was created
    public final int oldInnovNum;

    public Innovation(int inNode, int outNode, int num1, int num2, int newid, int oldinnov)
    {
        innovType = InnovType.NODE;
        nodeInID = inNode;
        nodeOutID = outNode;
        innovNum = num1;
        innovNum2 = num2;
        newNodeID = newid;
        oldInnovNum = oldinnov;

        //unused params
        newWeight = 0;
    }

    public Innovation(int nin, int nout, int num1, double w)
    {
        innovType = InnovType.LINK;
        nodeInID = nin;
        nodeOutID = nout;
        innovNum = num1;
        newWeight = w;

        //unused params
        innovNum2 = 0;
        newNodeID = 0;
        oldInnovNum = -1;
    }
}
