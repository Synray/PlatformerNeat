package platfomer.networks;

import platfomer.Main;
import platfomer.util.NEATUtil;

import java.awt.*;
import java.util.Arrays;

public class Network
{
    private NetworkGraph graph;

    public static class NetDims
    {
        public int nbias;
        public int nsensor;
        public int noutput;
        public int nhidden;

        public int nall;
        public int ninput;
        public int noninput;

        public int nlinks;
    }

    public NetDims dims;
    public Node[] nodes;
    public Link[] links;
    public double[] activations;
    private double[] activationsOther;

    public int[] depths;
    private int maxDepth;

    //Nodes must be sorted by type in this order: bias, sensor, output, hidden
    public Network(NetDims dims, Node[] nodes, Link[] links)
    {
        this.dims = dims;
        this.nodes = nodes;
        this.links = links;
        this.activations = new double[dims.nall];
        this.activationsOther = new double[dims.nall];

        for (int i = 0; i < dims.nbias; i++)
        {
            activations[i] = 1.0;
        }

        calculateDepths();
    }

    //clear the activations array, prevent recurrent links from interfering with separate input data
    public void flush()
    {
        for (int i = dims.ninput; i < dims.nall; ++i)
        {
            activations[i] = 0.0;
        }
    }

    public void loadInputs(double[] inputs)
    {
        int input = 0;
        for (int i = dims.nbias; i < dims.ninput; i++)
        {
            activations[i] = inputs[input++];
        }
    }

    //Activate one time
    public void activate()
    {
        activate(1);
    }

    //activate n times
    public void activate(int ncycles)
    {
        double[] actCurr = activations, actNew = activationsOther;

        //Only copy the inputs
        if (dims.ninput >= 0) System.arraycopy(activations, 0, activationsOther, 0, dims.ninput);

        for (int cycle = 0; cycle < ncycles; ++cycle)
        {
            for (int i = dims.ninput; i < dims.nall; i++)
            {
                Node node = nodes[i];

                double sum = 0.0;
                for (int j = node.incomingStart; j < node.incomingEnd; ++j)
                {
                    Link link = links[j];
                    sum += link.weight * actCurr[link.in];
                }

                actNew[i] = NEATUtil.sigmoid(sum);
            }

            //Swap the activation buffers
            double[] temp = actCurr;
            actCurr = actNew;
            actNew = temp;
        }

        //If cycles is odd, then the activation buffer needs to be copied back into activations
        if (actCurr != activations)
        {
            if (dims.nall - dims.ninput >= 0)
                System.arraycopy(activationsOther, dims.ninput, activations, dims.ninput, dims.nall - dims.ninput);
        }
    }

    /**
     * @return Output neurons activation values
     */
    public double[] getOutputs()
    {
        double[] out = new double[dims.noutput];
        System.arraycopy(activations, dims.ninput, out, 0, dims.noutput);
        return out;
    }

    //debug function
    public void printOutputs()
    {
        for (int i = dims.ninput; i < dims.ninput + dims.noutput; i++)
        {
            System.out.println("Output " + i + ": " + activations[i]);
        }
    }

    public int maxDepth()
    {
        return maxDepth;
    }

    private void calculateDepths()
    {
        depths = new int[dims.nall];
        boolean[] visited = new boolean[dims.nall];
        for (int i = dims.ninput; i < dims.ninput + dims.noutput; ++i)
        {
            assignDepth(i, visited, 0);
            Arrays.fill(visited, false);
        }

        maxDepth = 0;
        for (int i = 0; i < dims.nall; ++i)
        {
            if (depths[i] > maxDepth)
            {
                maxDepth = depths[i];
            }
        }

        for (int i = 0; i < dims.ninput; ++i)
        {
            depths[i] = maxDepth + 1;
        }

        maxDepth += 1;
    }

    private void assignDepth(int id, boolean[] visited, int d)
    {
        if (visited[id]) return;

        visited[id] = true;

        if (!isOutput(id) && depths[id] >= d) return;

        depths[id] = d;

        if (id >= dims.nbias && id < dims.ninput)
        {
            return;
        }

        Node node = nodes[id];
        for (int j = node.incomingStart; j < node.incomingEnd; ++j)
        {
            Link link = links[j];

            //Skip output nodes
            if (link.in >= dims.ninput && link.in < dims.ninput + dims.noutput) continue;
            assignDepth(link.in, visited, d + 1);
        }
    }

    //Procedurally assign an x and y position for each node, in order to generate a graphical representation of the network
    private boolean isOutput(int id)
    {
        return id >= dims.ninput && id < dims.ninput + dims.noutput;
    }

    public NetworkGraph createGraph()
    {
        if (this.graph == null)
            this.graph = new NetworkGraph();
        return this.graph;
    }

    //TODO: Make positions not hard-coded
    public class NetworkGraph
    {
        public int[] _nodes; // list of nodes connect to inputs and outputs
        public Point[] nodePoints;

        public NetworkGraph()
        {
            int[] nodesInRow = new int[maxDepth];
            int[] yposRow = new int[maxDepth];

            nodesInRow[0] = dims.noutput;

            int connectedNodes = dims.ninput; // start with all the inputs connected
            for (int i = dims.ninput; i < depths.length; ++i)
            {
                if (depths[i] != 0 || isOutput(i))
                {
                    connectedNodes++;
                    nodesInRow[depths[i]]++;
                }
            }

            _nodes = new int[connectedNodes];
            nodePoints = new Point[connectedNodes];

            int nodeIdx = 0;

            int xOff = (Main.WIDTH - 2) / 2 - NEATUtil.inputWidth / 2;
            int yOff = (Main.HEIGHT - 2) / 2 - NEATUtil.inputHeight / 2;

            for (int i = 0; i < dims.nbias; i++)
            {
                _nodes[nodeIdx] = i;
                int biasX = 23 + (xOff * NEATUtil.nodeSize) + (NEATUtil.inputWidth) * NEATUtil.nodeSize;
                int biasY = 23 + (yOff * NEATUtil.nodeSize) + NEATUtil.inputHeight * NEATUtil.nodeSize + NEATUtil.nodeSize;
                nodePoints[nodeIdx] = new Point(biasX, biasY);

                nodeIdx++;
            }

            for (int i = 0; i < dims.ninput - dims.nbias; i++)
            {
                _nodes[nodeIdx] = i + dims.nbias;
                int x = 23 + (xOff * NEATUtil.nodeSize) + i % (NEATUtil.inputWidth) * NEATUtil.nodeSize;
                int y = 23 + (yOff * NEATUtil.nodeSize) + i / (NEATUtil.inputWidth) * NEATUtil.nodeSize;
                nodePoints[nodeIdx] = new Point(x, y);

                nodeIdx++;
            }

            int ox = 542;
            for (int i = dims.ninput; isOutput(i); i++)
            {
                int oy = 48 + 16 * (i - dims.ninput);
                _nodes[nodeIdx] = i;
                nodePoints[nodeIdx] = new Point(ox, oy);
                nodeIdx++;
            }


            int xpos, ypos;
            int xmin = 208, xmax = 536;
            int ymin = 23 + NEATUtil.nodeSize, ymax = ymin + (Main.HEIGHT - 2) * NEATUtil.nodeSize - NEATUtil.nodeSize*2;

            int hiddenWidth = xmax - xmin;
            int hiddenHeight = ymax - ymin;

            int xstep;
            if (maxDepth < 2)
            {
                xstep = hiddenWidth / 2;
            }
            else
            {
                xstep = hiddenWidth / (maxDepth - 1);
            }
            for (int i = dims.ninput + dims.noutput; i < dims.nall; i++)
            {
                if (depths[i] != 0)
                {
                    xpos = xmax - depths[i] * xstep;
                    int ystep = hiddenHeight / (Math.max(nodesInRow[depths[i]], 1));

                    ypos = ymin + ystep / 2 + yposRow[depths[i]]++ * ystep;
                    _nodes[nodeIdx] = i;
                    nodePoints[nodeIdx] = new Point(xpos, ypos);
                    nodeIdx++;
                }
            }
        }
    }
}
