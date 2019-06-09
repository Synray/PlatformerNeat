package platfomer.genetics;

import platfomer.networks.Network;
import platfomer.genetics.Innovation.InnovType;
import platfomer.genetics.NodeGene.NPlace;
import platfomer.networks.Link;
import platfomer.networks.Node;
import platfomer.util.NEATUtil;
import platfomer.util.ProbabilityDistribution;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import static platfomer.genetics.NodeGene.NPlace.*;

public class Genome
{
    public double fitness;
    public int generation;

    public boolean winner;
    public Species species;

    private ArrayList<NodeGene> nodes;
    private ArrayList<LinkGene> links;

    public Genome(int gen, int nin, int nout)
    {
        this.generation = gen;
        nodes = new ArrayList<>();
        links = new ArrayList<>();

        int nodeID = 0;

        nodes.add(new NodeGene(NPlace.BIAS, nodeID++));

        for (int i = 0; i < nin; i++)
        {
            nodes.add(new NodeGene(NPlace.SENSOR, nodeID++));
        }

        for (int i = 0; i < nout; i++)
        {
            nodes.add(new NodeGene(OUTPUT, nodeID++));
        }
    }

    public static Genome fullyConnected(int nin, int nout)
    {
        ArrayList<NodeGene> nodes = new ArrayList<>();
        ArrayList<LinkGene> links = new ArrayList<>();

        int nodeID = 0;
        nodes.add(new NodeGene(NPlace.BIAS, nodeID++));

        for (int i = 0; i < nin; i++)
        {
            nodes.add(new NodeGene(NPlace.SENSOR, nodeID++));
        }

        int outStart = nodeID;
        for (int i = 0; i < nout; i++)
        {
            nodes.add(new NodeGene(OUTPUT, nodeID++));
        }

        for (int i = 0; i < nin + 1; ++i)
        {
            for (int o = 0; o < nout; o++)
            {
                links.add(new LinkGene(NEATUtil.randDouble(2), i, outStart + o, i + o * nout));
            }
        }

        return new Genome(0, nodes, links);
    }

    public Genome(Genome copy, int gen)
    {
        this.generation = gen;
        nodes = new ArrayList<>(copy.nodes.size());

        for (NodeGene node : copy.nodes)
        {
            nodes.add(new NodeGene(node));
        }

        links = new ArrayList<>(copy.links.size());
        for (LinkGene curGene : copy.links)
        {
            links.add(new LinkGene(curGene, curGene.inID, curGene.outID));
        }
    }

    public Genome(int gen, ArrayList<NodeGene> nodes, ArrayList<LinkGene> links)
    {
        this.generation = gen;
        this.nodes = nodes;
        this.links = links;
    }

    //load a genome from a file
    public Genome(String file)
    {
        try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr))
        {
            loadFromFile(br);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public Genome(BufferedReader br)
    {
        loadFromFile(br);
    }

    private void loadFromFile(BufferedReader br)
    {
        nodes = new ArrayList<>();
        links = new ArrayList<>();
        String line;
        int nin = 0;
        int nout;
        boolean done = false;
        try
        {
            label:
            while ((line = br.readLine()) != null && !done)
            {
                if (line.startsWith("/*"))
                {
                    continue;
                }
                String[] data = line.split(" ");
                switch (data[0])
                {
                    case "genomestart":
                        break;
                    case "winner":
                        winner = true;
                        break;
                    case "generation":
                        //TODO: Fix the way generations are handled, maybe write the generation of the population at the top of the file
//                        generation = Integer.parseInt(data[1]);
                        generation = 0;
                        break;
                    case "genomeend":
                        break label;
                    case "numin":
                        nin = Integer.parseInt(data[1]);
                        break;
                    case "numout":
                        nout = Integer.parseInt(data[1]);
                        if (nin != 0 && nout != 0)
                        {
                            int nodeID = 0;

                            nodes.add(new NodeGene(NPlace.BIAS, nodeID++));

                            for (int i = 0; i < nin; i++)
                            {
                                nodes.add(new NodeGene(NPlace.SENSOR, nodeID++));
                            }

                            for (int i = 0; i < nout; i++)
                            {
                                nodes.add(new NodeGene(OUTPUT, nodeID++));
                            }

                            done = true;
                        }
                        break;
                    case "node":
                        NodeGene newNode = new NodeGene(data);
                        nodes.add(newNode);
                        break;
                    case "gene":
                        LinkGene gene = new LinkGene(data);
                        links.add(gene);
                        break;
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    //save to file
    public void printToFile(BufferedWriter bw) throws IOException
    {

        bw.write("genomestart\n");
        if (winner)
        {
            bw.write("winner\n");
        }
        bw.write("generation " + generation + '\n');

        for (NodeGene node : nodes)
        {
            node.printToFile(bw);
        }
        for (LinkGene gene : links)
        {
            gene.printToFile(bw);
        }
        bw.write("genomeend\n");
    }

    //generate the genome's network -- aka, the phenotype
    public Network createNetwork()
    {
        int nnodes = nodes.size();

        Network.NetDims dims = new Network.NetDims();

        for (NodeGene node : nodes)
        {
            switch (node.placement)
            {
                case BIAS:
                    dims.nbias++;
                    break;
                case SENSOR:
                    dims.nsensor++;
                    break;
                case OUTPUT:
                    dims.noutput++;
                    break;
                case HIDDEN:
                    dims.nhidden++;
                    break;
            }
        }
        dims.nall = nnodes;
        dims.ninput = dims.nbias + dims.nsensor;
        dims.noninput = dims.noutput + dims.nhidden;

        Link[] netlinks = new Link[links.size()];
        int nlinks = 0;
        int[] node_nlinks = new int[nnodes];

        for (LinkGene l : links)
        {
            Link netlink = new Link();

            netlink.weight = l.weight;
            netlink.in = getNodeIndex(l.inID);
            netlink.out = getNodeIndex(l.outID);

            node_nlinks[netlink.out]++;
            netlinks[nlinks++] = netlink;
        }
        dims.nlinks = nlinks;

        Node[] netnodes = new Node[nnodes];
        for (int i = 0; i < nnodes; ++i) netnodes[i] = new Node();

        netnodes[0].incomingStart = 0;
        netnodes[0].incomingEnd = node_nlinks[0];
        for (int i = 1; i < nnodes; i++)
        {
            Node prev = netnodes[i - 1];
            Node curr = netnodes[i];

            curr.incomingStart = prev.incomingEnd;
            curr.incomingEnd = curr.incomingStart + node_nlinks[i];
        }

        //Sort the links
        Arrays.fill(node_nlinks, 0);
        Link[] netlinks_sorted = new Link[nlinks];

        for (int i = 0; i < nlinks; i++)
        {
            Link link = netlinks[i];
            int inode = link.out;
            int isorted = netnodes[inode].incomingStart + node_nlinks[inode]++;
            netlinks_sorted[isorted] = link;
        }

        return new Network(dims, netnodes, netlinks_sorted);
    }

    private void addLink(LinkGene link)
    {
        int inum = link.innovationNum;

        int i = 0;
        for (; i <= links.size(); ++i)
        {
            if (i < links.size())
            {
                LinkGene curGene = links.get(i);
                if (curGene.innovationNum >= inum)
                {
                    links.add(i, link);
                    break;
                }
            }
            else
            {
                links.add(link);
                break;
            }
        }
    }

    private static void insertNode(ArrayList<NodeGene> nList, NodeGene node)
    {
        int id = node.nodeID;
        for (int i = 0; i <= nList.size(); i++)
        {
            if (i < nList.size())
            {
                NodeGene curNode = nList.get(i);
                if (curNode.nodeID >= id)
                {
                    nList.add(i, node);
                    break;
                }
            }
            else
            {
                nList.add(node);
                break;
            }
        }
    }

    public void normalizeWeights(double scale)
    {
        double total = 0.0;
        for (int i = 0; i < links.size(); i++)
        {
            double w = links.get(i).weight;
            total += w * w;
        }
        if (total == 0.0) return;

        double scalar = scale / Math.sqrt(total);

        for (int i = 0; i < links.size(); i++)
        {
            links.get(i).weight *= scalar;
        }
    }

    /**
     * Apply gaussian noise to the connections in the network.
     * There's also a slight chance to reset a weight to a new gaussian value.
     *
     * @param power
     */
    boolean mutateLinkWeights(double power)
    {
        if (links.size() == 0)
        {
            return false;
        }

        double num;
        double geneTotal;
        double powerMod;

        double randWeight;
        double randChoice;
        double endPart;
        double gaussPoint;
        double coldGaussPoint;

        boolean severe = false;

        if (NEATUtil.randDouble() > 0.5)
        {
            severe = true;
        }

        num = 0.0;
        geneTotal = links.size();
        endPart = geneTotal * 0.8;
        powerMod = 1.0;

        for (LinkGene curGene : links)
        {
            if (severe)
            {
                gaussPoint = 0.3;
                coldGaussPoint = 0.1;
            }
            else
            {
                if (geneTotal >= 10 && num > endPart)
                {
                    gaussPoint = 0.5;
                    coldGaussPoint = 0.3;
                }
                else
                {
                    if (NEATUtil.randDouble() > 0.5)
                    {
                        gaussPoint = 1.0 - 1.0;
                        coldGaussPoint = 1.0 - 1.0 - 0.1;
                    }
                    else
                    {
                        gaussPoint = 1.0 - 1.0;
                        coldGaussPoint = 1.0 - 1.0;
                    }
                }
            }

            randWeight = NEATUtil.randDouble(power * powerMod);
            randChoice = NEATUtil.randDouble();
            if (randChoice > gaussPoint)
            {
                curGene.weight += randWeight;
            }
            else if (randChoice > coldGaussPoint)
            {
                curGene.weight = randWeight;
            }

            if (NEATUtil.capWeights)
            {
                if (curGene.weight > NEATUtil.weightCap)
                {
                    curGene.weight = NEATUtil.weightCap;
                }
                if (curGene.weight < -NEATUtil.weightCap)
                {
                    curGene.weight = -NEATUtil.weightCap;
                }
            }

            num += 1.0;
        }

        return true;
    }

    //Add a new node by splitting a link in two
    //The new link going into the node gets a weight of 1.0,
    //while the link exiting the new node keeps the old weight. This reduces
    //negative fitness effects of adding new structures.
    public boolean mutateAddNode(Population population)
    {
        if (links.size() == 0) return false;

        LinkGene splitLink = null;
        int linkIndex = 0;
        for (int i = 0; i < 20 && splitLink == null; i++)
        {
            linkIndex = NEATUtil.randInt(0, links.size() - 1);
            splitLink = links.get(linkIndex);
        }

        if (splitLink == null)
        {
            return false;
        }

        links.remove(linkIndex);

        Innovation innov = null;
        ArrayList<Innovation> innovs = population.getInnovations();
        for (Innovation existing : innovs)
        {
            if (existing.innovType == InnovType.NODE
                    && existing.nodeInID == splitLink.inID
                    && existing.nodeOutID == splitLink.outID
                    && existing.oldInnovNum == splitLink.innovationNum)
            {
                innov = existing;
                break;
            }
        }

        if (innov == null)
        {
            innov = new Innovation(splitLink.inID, splitLink.outID, population.newInnovation(), population.newInnovation(), population.getCurNodeID() + 1, splitLink.innovationNum);
            population.setCurNodeID(innov.newNodeID);
        }

        NodeGene newNode = new NodeGene(NPlace.HIDDEN, innov.newNodeID, splitLink.innovationNum);
        LinkGene inputLink = new LinkGene(1.0, innov.nodeInID, innov.newNodeID, innov.innovNum);
        LinkGene outputLink = new LinkGene(splitLink.weight, innov.newNodeID, innov.nodeOutID, innov.innovNum2);

        addLink(inputLink);
        addLink(outputLink);
        insertNode(nodes, newNode);

        return true;
    }

    private boolean mutateAddLink(Population population, int tries)
    {
//        RecurrencyChecker recurChecker = new RecurrencyChecker(nodes.size(), links);
        NodeGene inNode = null;
        NodeGene outNode = null;

        boolean doRecur = NEATUtil.randDouble() < NEATUtil.doRecurOnlyProb;
        boolean found = false;

        int firstNonsensor = 0;
        while (firstNonsensor < nodes.size() && nodes.get(firstNonsensor).isInput())
        {
            ++firstNonsensor;
        }

        if (firstNonsensor >= nodes.size()) return false;

        for (int tryCount = 0; tryCount < tries && !found; tryCount++)
        {

            if (doRecur)
            {
                inNode = nodes.get(NEATUtil.randInt(firstNonsensor, nodes.size() - 1));
                outNode = inNode;
            }
            else
            {
                if (NEATUtil.randDouble() < NEATUtil.mutateAddBiasProb)
                {
                    inNode = nodes.get(0);
                }
                else
                {
                    inNode = nodes.get(NEATUtil.randInt(0, nodes.size() - 1));
                }
                outNode = nodes.get(NEATUtil.randInt(firstNonsensor, nodes.size() - 1));
            }

            found = !linkExists(inNode.nodeID, outNode.nodeID);// && (doRecur == recurChecker.isRecur(inNode.nodeID, outNode.nodeID));
        }

        if (!found)
        {
            return false;
        }

        LinkGene newGene = null;
        for (Innovation innov : population.getInnovations())
        {

            if (innov.innovType == InnovType.LINK
                    && innov.nodeInID == inNode.nodeID
                    && innov.nodeOutID == outNode.nodeID)
//                    && innov.recurrent == doRecur)
            {
                double newWeight = innov.newWeight;
                if (NEATUtil.randDouble() < NEATUtil.mutateLinkWeightsProb)
                {
                    newWeight = NEATUtil.randDouble(innov.newWeight);
                }
                newGene = new LinkGene(newWeight, inNode.nodeID, outNode.nodeID, /*doRecur,*/ innov.innovNum);
            }
        }

        if (newGene == null)
        {
            //Local weight scaling
            double newWeight = NEATUtil.randDouble(NEATUtil.weightMutPower);
            if (NEATUtil.linkRelativeWeightPower)
            {
                double totalWeight = 0.0;
                for (LinkGene l : links)
                {
                    if (l.outID == outNode.nodeID)
                    {
                        totalWeight += Math.abs(l.weight);
                    }
                }
                newWeight = NEATUtil.randDouble(totalWeight + NEATUtil.weightMutPower);
            }

            newGene = new LinkGene(newWeight, inNode.nodeID, outNode.nodeID, /*doRecur,*/ population.newInnovation());

            population.getInnovations().add(new Innovation(inNode.nodeID, outNode.nodeID, population.currentInnovation() - 1, newWeight/*, doRecur*/));
        }

        addLink(newGene);
        return true;
    }

    private boolean mutateDeleteLink()
    {
        if (links.size() <= 1) return false;

        if (!NEATUtil.linkDeleteNonDestructive)
        {
            int linkIndex = NEATUtil.randInt(links.size());
            LinkGene link = links.get(linkIndex);
            links.remove(linkIndex);
            deleteNodeIfOrphaned(link.inID);
            deleteNodeIfOrphaned(link.outID);
            return true;
        }
        else
        {
            for (int tryCount = 0; tryCount < NEATUtil.newlinkTries + links.size(); tryCount++)
            {
                int linkIndex = NEATUtil.randInt(links.size());
                LinkGene link = links.get(linkIndex);

                boolean found = false;

                NodeGene outNode = getNode(link.outID);
                if (outNode == null) found = true;
                else
                {
                    if (outNode.placement == OUTPUT)
                    {
                        NodeGene inNode = getNode(link.inID);
                        if (inNode == null || inNode.isInput()) found = true;
                        else
                        {
                            for (LinkGene check : links)
                            {
                                if (check != link && check.inID == link.inID
                                        && check.inID != check.outID)
                                {
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                    else
                    {
                        for (LinkGene check : links)
                        {
                            if (check != link && check.outID == link.outID
                                    && check.inID != check.outID)
                            {
                                found = true;
                                break;
                            }
                        }
                    }
                }

                if (found)
                {
                    links.remove(linkIndex);
                    deleteNodeIfOrphaned(link.inID);
                    deleteNodeIfOrphaned(link.outID);
                    return true;
                }
            }
        }

        return false;
    }

    private void deleteNodeIfOrphaned(int nodeID)
    {
        NodeGene node = getNode(nodeID);
        if (node == null || node.placement != NPlace.HIDDEN) return;

        LinkGene recurLink = null;

        for (LinkGene link : links)
        {
            if (link.inID == nodeID || link.outID == nodeID)
            {
                if (link.inID == link.outID)
                {
                    if (recurLink == null)
                    {
                        recurLink = link;
                    }
                }
                else return;
            }
        }

        if (recurLink != null)
        {
            links.remove(recurLink);
        }
        nodes.remove(node);
    }

    //Looks for a node with only one input and only one output, and attempts to
    //replace the node with a link whose weight has the same effect as the
    //two genes
    private boolean mutateDeleteNode()
    {
        int firstHidden = 0;
        while (firstHidden < nodes.size() && nodes.get(firstHidden).placement != HIDDEN)
        {
            ++firstHidden;
        }

        if (firstHidden == nodes.size()) return false;

        NodeGene remove = null;
        LinkGene outLink = null;
        LinkGene inLink = null;

        boolean found = false;

        for (int tryCount = 0; tryCount < nodes.size() - firstHidden && !found; tryCount++)
        {
            remove = nodes.get(NEATUtil.randInt(firstHidden, nodes.size() - 1));
            assert remove.placement == HIDDEN;
            if (remove.splitLinkID == -1) continue;

            outLink = inLink = null;

            //Check each link
            found = true;
            for (LinkGene link : links)
            {
                if (link.inID == remove.nodeID)
                {
                    if (outLink == null)
                    {
                        outLink = link;
                    }
                    else
                    {
                        found = false;
                        break;
                    }
                }
                if (link.outID == remove.nodeID)
                {
                    if (inLink == null)
                    {
                        inLink = link;
                    }
                    else
                    {
                        found = false;
                        break;
                    }
                }
            }
        }

        if (!found) return false;


        //case 0: no input link, no output link. This theoretically shouldn't happen because
        //orphaned nodes are deleted by mutateDeleteLink(), but to be safe: remove it
        //case 1: no input link, one output link
        //case 2: one input link, no output link
        if (outLink == null || inLink == null)
        {
            links.remove(inLink);
            links.remove(outLink);
            nodes.remove(remove);
            return true;
        }

        //case 3: one input, one output
        //If a link has already been created between the two nodes, just delete the node
        //NOTE: it's possible for a genome to have a links going both ways:
        //A<------B
        //as well as
        //A-->C-->B
        //it's fine for A-C-B to be replaced with a link, because B-A travels backwards
        if (linkExists(inLink.inID, outLink.outID))
        {
            links.remove(inLink);
            links.remove(outLink);
            nodes.remove(remove);
            return true;
        }

        //If
        //It's possible, but less likely that removing the node will be beneficial
        //Cycles have a time delay of 2 activations, while recurrent links have a delay of one.
        //At

        //Add the link
        //Most of the time, assume that the input link usually sees solid tiles, which
        //have a value of 1
        double splitWeight = NEATUtil.sigmoid(inLink.weight) * outLink.weight;

        //Sometimes, multiply the weight by negative one. If the weight is used for
        //detecting enemies, than this increases the chance that the mutation will be
        //beneficial
        //TODO: replace with ProbDist
        //TODO: Do weights ever actually need to be negative?
        if (NEATUtil.randDouble() < 0.1)
        {
            splitWeight = NEATUtil.sigmoid(-inLink.weight) * outLink.weight;
        }
        else if (NEATUtil.randDouble() < 0.1)
        {
            splitWeight = NEATUtil.randDouble(NEATUtil.weightMutPower);
        }
        LinkGene splitLink = new LinkGene(splitWeight, inLink.inID, outLink.outID, remove.splitLinkID);
        addLink(splitLink);
        return true;
    }

    private boolean linkExists(int inID, int outID)
    {
        for (LinkGene g : links)
        {
            if (g.inID == inID && g.outID == outID /*&& g.recurrent == isRecurrent*/)
            {
                return true;
            }
        }

        return false;
    }

//Offspring methods

    //Line up two genomes, then choose random similar links from each parent.
    //For non matching links, use those belonging to the most fit parent
    //TODO: Add a small amount of the less fit parent's genes to avoid duplicates
    public Genome createOffspring(Genome dad, Population population, int generation)
    {
        ArrayList<NodeGene> newNodes = new ArrayList<>();
        ArrayList<LinkGene> newGenes = new ArrayList<>();

        LinkGene momGene;
        LinkGene dadGene;
        int momGeneIdx;
        int dadGeneIdx;
        int momInnov;
        int dadInnov;

        LinkGene chosenGene;

        LinkGene newGene;
        boolean momBetter;
        boolean skip;

        if (fitness > dad.fitness)
        {
            momBetter = true;
        }
        else if (fitness < dad.fitness)
        {
            momBetter = false;
        }
        else
        {
            momBetter = links.size() < dad.links.size();
        }

        for (int n = 0; n < dad.nodes.size(); n++)
        {
            NodeGene node = new NodeGene(dad.nodes.get(n));
            if (node.isInput() || node.placement == OUTPUT)
            {
                insertNode(newNodes, node);
            }
        }

        Genome mom = this;
        momGeneIdx = 0;
        dadGeneIdx = 0;
        while (momGeneIdx < links.size() || dadGeneIdx < dad.links.size())
        {
            Genome chosenGenome;
            skip = false;

            //If reached the end of mom's links, but dad is the fittest parent, then continue to add dad's links to the baby
            if (momGeneIdx == links.size())
            {
                dadGene = dad.links.get(dadGeneIdx);
                chosenGene = dadGene;
                chosenGenome = dad;
                dadGeneIdx++;
                if (momBetter && NEATUtil.randDouble() >= NEATUtil.excessInheritProb)
                    continue;
            }
            //Vice Versa
            else if (dadGeneIdx == dad.links.size())
            {
                momGene = links.get(momGeneIdx);
                chosenGene = momGene;
                chosenGenome = mom;
                momGeneIdx++;
                if (!momBetter && NEATUtil.randDouble() >= NEATUtil.excessInheritProb)
                    continue;
            }
            else
            {
                momGene = links.get(momGeneIdx);
                dadGene = dad.links.get(dadGeneIdx);
                momInnov = momGene.innovationNum;
                dadInnov = dadGene.innovationNum;

                //If both parents share the gene, pick a random version of it
                if (momInnov == dadInnov)
                {
                    if (NEATUtil.randDouble() < 0.5)
                    {
                        chosenGene = momGene;
                        chosenGenome = mom;
                    }
                    else
                    {
                        chosenGene = dadGene;
                        chosenGenome = dad;
                    }

                    momGeneIdx++;
                    dadGeneIdx++;
                }
                //Otherwise, only add links from the fittest parent
                else if (momInnov < dadInnov)
                {
                    chosenGene = momGene;
                    chosenGenome = mom;
                    momGeneIdx++;
                    if (!momBetter && NEATUtil.randDouble() >= NEATUtil.excessInheritProb)
                        continue;
                }
                else
                {
                    chosenGene = dadGene;
                    chosenGenome = dad;
                    dadGeneIdx++;
                    if (momBetter && NEATUtil.randDouble() >= NEATUtil.excessInheritProb)
                        continue;
                }
            }
            //TODO: Runs: .7-.6-.5-.4-.3-.2-.1-.0
            //Prevent adding a linkgene that connects the same nodes as a previously evolved linkgene
            for (LinkGene check : newGenes)
            {
                if (check.inID == chosenGene.inID && check.outID == chosenGene.outID)// && check.recurrent == chosenGene.recurrent)
                {
                    skip = true;
                    break;
                }
            }

            assert chosenGene != null;

            if (!skip)
            {
                NodeGene newInNode;
                NodeGene newOutNode;

                NodeGene inNode = chosenGenome.getNode(chosenGene.inID);
                NodeGene outNode = chosenGenome.getNode(chosenGene.outID);

                boolean found;

                if (inNode.nodeID < outNode.nodeID)
                {
                    found = false;
                    NodeGene curNode = null;
                    for (NodeGene node : newNodes)
                    {
                        curNode = node;
                        if (curNode.nodeID == inNode.nodeID)
                        {
                            found = true;
                            break;
                        }
                    }

                    if (found)
                    {
                        newInNode = curNode;
                    }
                    else
                    {
                        newInNode = new NodeGene(inNode);
                        insertNode(newNodes, newInNode);
                    }

                    found = false;
                    curNode = null;
                    for (NodeGene newNode : newNodes)
                    {
                        curNode = newNode;
                        if (curNode.nodeID == outNode.nodeID)
                        {
                            found = true;
                            break;
                        }
                    }

                    if (found)
                    {
                        newOutNode = curNode;
                    }
                    else
                    {
                        newOutNode = new NodeGene(outNode);
                        insertNode(newNodes, newOutNode);
                    }
                }
                else
                {
                    found = false;
                    NodeGene curNode = null;
                    for (NodeGene node : newNodes)
                    {
                        curNode = node;
                        if (curNode.nodeID == outNode.nodeID)
                        {
                            found = true;
                            break;
                        }
                    }

                    if (found)
                    {
                        newOutNode = curNode;
                    }
                    else
                    {
                        newOutNode = new NodeGene(outNode);
                        insertNode(newNodes, newOutNode);
                    }

                    found = false;
                    curNode = null;
                    for (NodeGene newNode : newNodes)
                    {
                        curNode = newNode;
                        if (curNode.nodeID == inNode.nodeID)
                        {
                            found = true;
                            break;
                        }
                    }

                    if (found)
                    {
                        newInNode = curNode;
                    }
                    else
                    {
                        newInNode = new NodeGene(inNode);
                        insertNode(newNodes, newInNode);
                    }
                }

                newGene = new LinkGene(chosenGene, newInNode.nodeID, newOutNode.nodeID);
                newGenes.add(newGene);
            }
        }

        Genome g = new Genome(generation, newNodes, newGenes);
        g.mutate(population);
        return g;
    }

    public void mutate(Population population)
    {
        ProbabilityDistribution distCurrent = new ProbabilityDistribution(NEATUtil.currentMutationDistribution);
        boolean success;
        for (; ; )
        {
            int outcome = distCurrent.sample();
            switch (outcome)
            {
                case 0:
                    success = mutateLinkWeights(NEATUtil.weightMutPower);
                    break;
                case 1:
                    success = mutateAddNode(population);
                    break;
                case 2:
                {
                    int numLinks = NEATUtil.randInt(1, NEATUtil.mutateAddExtraLinks);
                    success = false;
                    for (int i = 0; i < numLinks; ++i)
                    {
                        success |= mutateAddLink(population, nodes.size() + links.size() + NEATUtil.newlinkTries);
                    }
                    break;
                }
                case 3:
                {
                    int numLinks = NEATUtil.randInt(1, NEATUtil.mutateRemoveExtraLinks);
                    success = false;
                    for (int i = 0; i < numLinks; ++i)
                    {
                        success |= mutateDeleteLink();
                    }
                    break;
                }
                case 4:
                    success = mutateDeleteNode();
                    break;
                default:
                    System.err.println("Unhandled mutation");
                    return;
            }

            // Success. Break out of loop.
            if (success)
            {
                return;
            }

            // Mutation did not succeed. Remove attempted type of mutation from set of possible outcomes.
            distCurrent = distCurrent.removeOutcome(outcome);
            if (0 == distCurrent.getProbabilities().length)
            {   // Nothing left to try. Do nothing.
                return;
            }
        }
    }

    //Create an offspring asexually: mutate a clone of this genome
    public Genome createOffspring(Population population, int generation)
    {
        Genome g = new Genome(this, generation);
        g.mutate(population);
        return g;
    }

    //Sum the differences between genomes too determine whether they belong to the same species
    public double compatibility(Genome g)
    {
        int myInnov;
        int otherInnov;
        int myGeneIdx = 0;
        int otherGeneIdx = 0;

        double mutDiff;

        double disjoint = 0;
        double excess = 0;
        double matching = 0;
        double mutDiffTotal = 0;

        while (!(myGeneIdx == links.size() && otherGeneIdx == g.links.size()))
        {
            if (myGeneIdx == links.size())
            {
                ++otherGeneIdx;
                excess += 1.0;
            }
            else if (otherGeneIdx == g.links.size())
            {
                ++myGeneIdx;
                excess += 1.0;
            }
            else
            {
                myInnov = links.get(myGeneIdx).innovationNum;
                otherInnov = g.links.get(otherGeneIdx).innovationNum;

                if (myInnov == otherInnov)
                {
                    ++matching;
                    mutDiff = links.get(myGeneIdx).weight - g.links.get(otherGeneIdx).weight;
                    if (mutDiff < 0.0)
                    {
                        mutDiff = 0.0 - mutDiff;
                    }
                    mutDiffTotal += mutDiff;

                    ++myGeneIdx;
                    ++otherGeneIdx;
                }
                else if (myInnov < otherInnov)
                {
                    ++myGeneIdx;
                    disjoint += 1.0;
                }
                else if (otherInnov < myInnov)
                {
                    ++otherGeneIdx;
                    disjoint += 1.0;
                }
            }
        }
        if (matching == 0.0) matching = 1.0;
        return (NEATUtil.disjointCoeff * disjoint) + (NEATUtil.excessCoeff * excess) + (NEATUtil.mutDiffCoeff * (mutDiffTotal / matching));
    }

    public int getLastNodeID()
    {
        return nodes.get(nodes.size() - 1).nodeID + 1;
    }

    public int getLastGeneInnovnum()
    {
        if (links.size() == 0)
        {
            return 0;
        }
        return links.get(links.size() - 1).innovationNum + 1;
    }

    public int getComplexity()
    {
        return links.size();
    }

    public int getNumLinks()
    {
        return links.size();
    }

    public int getNumNodes()
    {
        return nodes.size();
    }

    private static class RecurrencyChecker
    {
        private int numNodes;
        private LinkGene[] genes;
        private boolean[] traversed;
        private int numGenes;

        public RecurrencyChecker(int numNodes, ArrayList<LinkGene> genomeGenes)
        {
            this.numNodes = numNodes;

            genes = new LinkGene[genomeGenes.size()];
            numGenes = genes.length;
            genomeGenes.toArray(genes);
            Arrays.sort(genes, Comparator.comparingInt(a -> a.outID));
        }

        private int find(int nodeID, int index)
        {
            if (index < 0)
            {
                int first = 0;
                int last = genes.length - 1;
                index = 0;
                int step;
                int count = last - first;
                while (count > 0)
                {
                    index = first;
                    step = count / 2;
                    index += step;
                    if (genes[index].outID < nodeID)
                    {
                        first = ++index;
                        count -= step + 1;
                    }
                    else count = step;
                }
            }
            else
            {
                index++;
            }
            if (index >= genes.length) return -1;
            if (genes[index].outID != nodeID) return -1;
            return index;
        }

        private boolean isRecur(int inID, int outID, int count, int thresh)
        {
            ++count;
            if (count > thresh) return false;

            if (inID == outID)
            {
                return true;
            }
            else
            {
                int index = -1;
                while ((index = find(inID, index)) > 0)
                {
                    if (!traversed[index])
                    {
                        if (isRecur(genes[index].inID, outID, count, thresh))
                        {
                            return true;
                        }
                        traversed[index] = true;
                    }
                }

                return false;
            }
        }

        public boolean isRecur(int in, int out)
        {
            int thresh = numNodes * numNodes;

            traversed = new boolean[numGenes];

            return isRecur(in, out, 0, thresh);
        }
    }

    private int getNodeIndex(int nodeID)
    {
        int first = 0;
        int last = nodes.size() - 1;
        int index = 0;
        int step;
        int count = last - first;
        while (count > 0)
        {
            index = first;
            step = count / 2;
            index += step;
            if (nodes.get(index).nodeID < nodeID)
            {
                first = ++index;
                count -= step + 1;
            }
            else count = step;
        }

        if (index >= nodes.size()) return -1;
        if (nodes.get(index).nodeID != nodeID) return -1;
        return index;
    }

    private NodeGene getNode(int nodeID)
    {
        int first = 0;
        int last = nodes.size() - 1;
        int index = 0;
        int step;
        int count = last - first;
        while (count > 0)
        {
            index = first;
            step = count / 2;
            index += step;
            if (nodes.get(index).nodeID < nodeID)
            {
                first = ++index;
                count -= step + 1;
            }
            else count = step;
        }

        if (index >= nodes.size()) return null;
        if (nodes.get(index).nodeID != nodeID) return null;
        return nodes.get(index);
    }
}
