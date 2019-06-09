package util.gui;

import platfomer.genetics.Genome;
import platfomer.networks.Link;
import platfomer.networks.Network;
import platfomer.util.NEATUtil;

import javax.swing.*;
import java.awt.*;

public class NetworkViewer extends JPanel
{
	private Network network;

	/**
	 * A producer/consumer thread that generates a genome's network
	 * in the background to prevent the EDT from blocking
	 */
	private class GraphWorker implements Runnable
	{
		private Genome genome;

		private GraphWorker()
		{
		}

		// Receive a genome and wake
		public synchronized void receiveGenome(Genome genome)
		{
			this.genome = genome;
			this.notify();
		}

		@Override
		public void run()
		{
			while (true)
			{
				synchronized (GraphWorker.this)
				{
					try
					{
						this.wait();
					} catch (InterruptedException ignored)
					{

					}
				}
				if (genome != null)
				{
					Network network = genome.createNetwork();
//					network.graph(getWidth(), getHeight());
					produceNetwork(network);
				}
			}
		}

	}

	// Receive a network from the GraphWorker
	private synchronized void produceNetwork(Network net)
	{
		this.network = net;
		repaint();
	}

	private GraphWorker graphWorker;

	public NetworkViewer()
	{
		setBackground(new Color(0xC4F1F2));
		setDoubleBuffered(true);
		//setLayout(new GridBagLayout());

		setPreferredSize(new Dimension(1000, 800));
		setMinimumSize(new Dimension(500, 500));
		setSize(1000, 800);
		setOpaque(true);
	}

	public void loadGenome(Genome g)
	{
		// Start the graph worker
		if (graphWorker == null)
		{
			graphWorker = new GraphWorker();
			new Thread(graphWorker).start();
		}
		graphWorker.receiveGenome(g);
	}

	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		graphNetwork((Graphics2D) g);
	}

	public void graphNetwork(Graphics2D g)
	{
		if (network == null)
		{
			return;
		}

		int xpixels = getWidth() - 2 * NEATUtil.nodeSize;
        int ypixels = getHeight() - 2 * NEATUtil.nodeSize;

		Network.NetDims dims = network.dims;

        int[] yposRow = new int[network.maxDepth() + 1];
        int[] nodesInRow = new int[network.maxDepth() + 1];
		int xstep = xpixels / (network.maxDepth() + 1);
		Rectangle[] graphNodes = new Rectangle[dims.nall];

        for(int i = 0; i < dims.nall; ++i)
		{
			nodesInRow[network.depths[i]]++;
		}

        for (int i = 0; i < dims.nall; ++i)
		{
			int ystep = (ypixels) / (nodesInRow[network.depths[i]] + 1);
			int ypos = 20 + yposRow[network.depths[i]]++ * ystep;
			int xpos = xpixels - network.depths[i] * xstep;
			graphNodes[i] = new Rectangle(xpos, ypos);
			graphNodes[i].y = ypos;
			graphNodes[i].x = xpos;
			g.fillRect(xpos - NEATUtil.nodeSize / 2, ypos - NEATUtil.nodeSize / 2, NEATUtil.nodeSize,
			// draw border with inverse of node color
					NEATUtil.nodeSize);
			int opacity = 0x7F;
			int color = 0xFF;
			g.setColor(new Color(opacity << 24 | (((color & 255) < 0x7f) ? 0xFFFFFF : 0x000000), true));
			g.setStroke(new BasicStroke(4));
			g.drawRect(xpos - (NEATUtil.nodeSize / 2) + 2, ypos - (NEATUtil.nodeSize / 2) + 2, NEATUtil.nodeSize - 4,
					NEATUtil.nodeSize - 4);
		}

        for (int i = 0; i < dims.nlinks; ++i)
		{
			Link link = network.links[i];
			int thickness = (int) Math.min(1 + 2 * Math.abs(link.weight), 14);
			int opacity = 0xA0000000;
			// unactivated links are more transparent
//			if (link.in.activation == 0)
//			{
//				opacity = 0x40000000;
//			}
			// activation * color
			int color = (int) (0x80 - Math.floor(Math.abs(NEATUtil.tanh(link.weight)) * 0x80));
			// positive weights are green
			if (link.weight > 0)
			{
				color = opacity + 0x8000 + 0x10000 * color;
			}
			// negative weights are red
			if (link.weight < 0)
			{
				color = opacity + 0x800000 + 0x100 * color;
			}

			g.setStroke(new BasicStroke(thickness));
			g.setColor(new Color(color, true));

			int x1, y1, x2, y2;
			x1 = graphNodes[link.out].x - NEATUtil.nodeSize / 2;
			y1 = graphNodes[link.out].y;
			x2 = graphNodes[link.in].x + NEATUtil.nodeSize / 2;
			y2 = graphNodes[link.in].y;
			g.drawLine(x1, y1, x2, y2);
		}

//        if (inputRect)
//        {
//            xstep = -(width - ((NEATUtil.inputWidth + 1) * NEATUtil.nodeSize) - 20) / (maxrow);
//        }
//        int ystep;
        int ypos = NEATUtil.nodeSize;
        int xpos = getWidth() - NEATUtil.nodeSize - 20;

		// graph the nodes
//		for (int i = 0; i < network.getAllNodes().size(); i++)
//		{
//			NodeGene node = network.getAllNodes().get(i);
//			int xpos = node.xpos;
//			int ypos = node.ypos;
//			int color;
//			int opacity = 0xFF;
//
//			// skip unassigned nodes
//			if (node.row < 0)
//			{
//				continue;
//			}
//
//			// unactivated nodes are more transparent
//			if (node.activation == 0)
//			{
//				int col = 127 + (int) (node.activation * 128);
//				color = col << 16 | col << 8 | col;
//				opacity = 0x7f;
//				g.setColor(new Color(opacity << 24 | color, true));
//			}
//			// white = positive activation
//			else if (node.activation > 0)
//			{
//				int col = 127 + (int) (node.activation * 128);
//				color = col << 16 | col << 8 | col;
//				g.setColor(new Color(opacity << 24 | color, true));
//			}
//			// black = negative activation
//			else
//			{
//				int col = (int) (-node.activation * 127);
//				color = col << 16 | col << 8 | col;
//				g.setColor(new Color(opacity << 24 | color, true));
//			}
//			// shade disconnected nodes red
//			if (node.disconnect)
//			{
//				color = g.getColor().getRGB() | 0x8F << 16;
//				g.setColor(new Color(color));
//			}
//			// draw node rectangle
//			g.fillRect(xpos - NEATUtil.nodeSize / 2, ypos - NEATUtil.nodeSize / 2, NEATUtil.nodeSize,
//					NEATUtil.nodeSize);
//			// draw border with inverse of node color
//			g.setColor(new Color(opacity << 24 | (((color & 255) < 0x7f) ? 0xFFFFFF : 0x000000), true));
//			g.setStroke(new BasicStroke(4));
//			g.drawRect(xpos - (NEATUtil.nodeSize / 2) + 2, ypos - (NEATUtil.nodeSize / 2) + 2, NEATUtil.nodeSize - 4,
//					NEATUtil.nodeSize - 4);
//		}

		// DRAW CONNECTIONS
//		for (int i = 0; i < network.getAllNodes().size(); i++)
//		{
//			NodeGene node = network.getAllNodes().get(i);
//			// skip unassigned nodes
//			if (node.row < 0)
//			{
//				continue;
//			}
//			for (int j = 0; j < node.incoming.size(); j++)
//			{
//				Link link = node.incoming.get(j);
//				int opacity = 0xA0000000;
//				// unactivated links are more transparent
//				if (link.in.activation == 0)
//				{
//					opacity = 0x40000000;
//				}
//				// activation * color
//				int color = (int) (0x80 - Math.floor(Math.abs(NEATUtil.tanh(link.weight)) * 0x80));
//				// positive weights are green
//				if (link.weight > 0)
//				{
//					color = opacity + 0x8000 + 0x10000 * color;
//				}
//				// negative weights are red
//				if (link.weight < 0)
//				{
//					color = opacity + 0x800000 + 0x100 * color;
//				}
//				g.setColor(new Color(color, true));
//
//				// draw normal link
//				if (!link.recurrent)
//				{
//					int thickness = (int) Math.min(1 + 2 * Math.abs(link.weight), 14);
//					g.setStroke(new BasicStroke(thickness));
//					int x1, y1, x2, y2;
//					x1 = node.xpos - NEATUtil.nodeSize / 2;
//					y1 = node.ypos;
//					x2 = link.in.xpos + NEATUtil.nodeSize / 2;
//					y2 = link.in.ypos;
//					g.drawLine(x1, y1, x2, y2);
//				}
//				// draw recurrent link, a loop
//				else
//				{
//					int thickness = (int) Math.min(1 + 2 * Math.abs(link.weight), 14);
//
//					g.setStroke(new BasicStroke(thickness));
//					g.drawLine(link.in.xpos + NEATUtil.nodeSize / 2, link.in.ypos,
//							link.in.xpos + NEATUtil.nodeSize / 2 + 20, link.in.ypos - 20);
//					g.drawLine(link.in.xpos + NEATUtil.nodeSize / 2 + 20, link.in.ypos - 20,
//							link.in.xpos + NEATUtil.nodeSize / 2 + 20, link.in.ypos - 25);
//					g.drawLine(link.in.xpos + NEATUtil.nodeSize / 2 + 20, link.in.ypos - 25,
//							node.xpos - NEATUtil.nodeSize / 2 - 20, link.in.ypos - 25);
//					g.drawLine(node.xpos - NEATUtil.nodeSize / 2 - 20, link.in.ypos - 25,
//							node.xpos - NEATUtil.nodeSize / 2 - 20, node.ypos);
//					g.drawLine(node.xpos - NEATUtil.nodeSize / 2 - 20, node.ypos, node.xpos - NEATUtil.nodeSize / 2,
//							node.ypos);
//				}
//			}
//		}
	}

}
