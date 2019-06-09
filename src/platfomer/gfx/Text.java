package platfomer.gfx;

import java.awt.*;
import java.util.ArrayList;

public class Text
{
	private static ArrayList<TextSprite> messages = new ArrayList<>();
	private static Font font = new Font("Emulogic", 0, 8);

	public static void drawText(int x, int y, String string)
	{
		TextSprite msg = new TextSprite(x, y, string);
		messages.add(msg);
	}

	public static void drawText(Graphics2D g)
	{
		g.setFont(font);
		for (int i = 0; i < messages.size(); i++)
		{
			TextSprite txt = messages.get(i);
			g.setColor(txt.color);
			g.drawString(txt.message, txt.x, txt.y);
			messages.remove(messages.get(i));
			i--;
		}
	}

	private static class TextSprite
	{
		public Color color;
		public int x, y;
		public String message;

		public TextSprite(int x, int y, String message, Color color)
		{
			this.x = x;
			this.y = y;
			this.message = message;
			this.color = color;
		}

		public TextSprite(int x, int y, String message)
		{
			this(x, y, message, Color.white);
		}
	}
}