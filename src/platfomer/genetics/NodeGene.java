package platfomer.genetics;

import java.io.BufferedWriter;
import java.io.IOException;

//Represents a computing unit in the network
public class NodeGene
{
    public enum NPlace
    {
        SENSOR, BIAS, HIDDEN, OUTPUT
    }

    public int nodeID;
    public final NPlace placement;
    public final int splitLinkID;

    public NodeGene(NPlace place, int nodeid)
    {
        placement = place;
        this.nodeID = nodeid;
        this.splitLinkID = -1;
    }

    public NodeGene(NPlace place, int nodeid, int splitLinkID)
    {
        placement = place;
        this.nodeID = nodeid;
        this.splitLinkID = splitLinkID;
    }

    public boolean isInput()
    {
        return placement == NPlace.SENSOR || placement == NPlace.BIAS;
    }

    public NodeGene(NodeGene n)
    {
        this.nodeID = n.nodeID;
        this.placement = n.placement;
        this.splitLinkID = n.splitLinkID;
    }

    public NodeGene(String... data)
    {
        // skip the word "node" in data[0]
        nodeID = Integer.parseInt(data[1]);
        placement = NPlace.values()[Integer.parseInt(data[2])];
        this.splitLinkID = Integer.parseInt(data[3]);
    }

    public void printToFile(BufferedWriter bw) throws IOException
    {
        bw.write("node " + nodeID + ' ' + placement.ordinal() + ' ' + splitLinkID + '\n');
    }
}
