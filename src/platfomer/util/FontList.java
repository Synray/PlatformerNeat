package platfomer.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * An applet that displays the standard fonts and styles available in Java 1.1
 */
public class FontList extends JPanel
{
	// The available font families
	public String[] fonts;

	// The available font styles and names for each one

	private final int[] styles =
	{ Font.PLAIN, Font.ITALIC, Font.BOLD, Font.ITALIC + Font.BOLD };

	String[] stylenames =
	{ "Plain", "Italic", "Bold", "Bold Italic" };

	public FontList()
	{
		fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		for (int i = 0; i < fonts.length; i++)
		{
			System.out.println(fonts[i]);
		}
		setMinimumSize(new Dimension(300, 5 * 24 * fonts.length));
		setMaximumSize(new Dimension(300, 5 * 24 * fonts.length));
		setPreferredSize(new Dimension(300, 5 * 24 * fonts.length));
		setVisible(true);
	}

	// Draw the applet.
	public void paint(Graphics g)
	{
		super.paint(g);
		for (int f = 0; f < fonts.length; f++)
		{ // for each family
			for (int s = 0; s < styles.length; s++)
			{ // for each style
				Font font = new Font(fonts[f], styles[s], 18); // create font
				g.setFont(font); // set font
				String name = fonts[f] + ' ' + stylenames[s]; // create name
				g.drawString(name, 20, (f * 4 + s + 1) * 20); // display name
			}
		}
	}

	public static void main(String[] a)
	{
		JFrame f = new JFrame();
		f.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});
		JScrollPane pane = new JScrollPane(new FontList());
		pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		f.add(pane, BorderLayout.CENTER);
		f.setSize(300, 300);
		//f.setResizable(false);
		f.setVisible(true);
	}

}