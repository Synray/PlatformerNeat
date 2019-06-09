package platfomer.genetics;

import java.io.BufferedWriter;
import java.io.IOException;

public class LinkGene
{
	public int innovationNum;

	public double weight;
	public int inID;
	public int outID;

	public LinkGene(double w, int inID, int outID, int innov)
	{
		this.weight = w;
		this.inID = inID;
		this.outID = outID;
		this.innovationNum = innov;
	}

	//construct a copy of a gene
	public LinkGene(LinkGene g, int inID, int outID)
	{
		this(g.weight, inID, outID, g.innovationNum);
	}
	
	//load a gene from a file
	public LinkGene(String... data)
	{
		inID = Integer.parseInt(data[1]);
		outID = Integer.parseInt(data[2]);
		weight = Double.parseDouble(data[3]);
		innovationNum = Integer.parseInt(data[4]);
	}

	//save gene to a file
	public void printToFile(BufferedWriter bw) throws IOException
	{
		bw.write("gene " + inID + ' ' + outID + ' ' + weight + ' ' + innovationNum  + '\n');
	}
}
