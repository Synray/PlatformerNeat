package platfomer.gfx;

import platfomer.registry.Registry;

import java.util.Arrays;

public class GameScreen
{
	public int width, height;
	public int xOff, yOff;
	private int[] pixels;

	public GameScreen(int width, int height, int[] pixels)
	{
		this.width = width;
		this.height = height;
		this.pixels = pixels;
	}

	public void clear()
	{
		Arrays.fill(pixels, 0x4C96FF);
	}

	public void clear(int clear)
	{
		for (int i = 0; i < pixels.length; i++)
		{
			if ((clear >> 24 & 255) < 255)
			{
				pixels[i] = blend(pixels[i], clear);
			}
			else
			{
				pixels[i] = clear;
			}
		}
	}

	/**
	 * @return Color returned has an alpha of 0
	 */
	public static int blend(int backGround, int foreGround)
	{
		short alpha = (short) ((foreGround >> 24) & 255);
		double percent = 1.0 - (alpha / 255.0);

		short r1 = (short) ((backGround >> 16) & 255);
		short g1 = (short) ((backGround >> 8) & 255);
		short b1 = (short) ((backGround) & 255);

		short r2 = (short) ((foreGround >> 16) & 255);
		short g2 = (short) ((foreGround >> 8) & 255);
		short b2 = (short) ((foreGround) & 255);

		short r3 = (short) ((r1 * percent + r2 * (1.0 - percent)));
		short g3 = (short) ((g1 * percent + g2 * (1.0 - percent)));
		short b3 = (short) ((b1 * percent + b2 * (1.0 - percent)));

		return r3 << 16 | g3 << 8 | b3;
	}

	/**
	 * @return Color returned has an alpha of 0
	 */
	public static int blend(int backGround, int foreGround, double percent)
	{
		percent = (1.0 - percent);
		short r1 = (short) ((backGround >> 16) & 255);
		short g1 = (short) ((backGround >> 8) & 255);
		short b1 = (short) ((backGround) & 255);

		short r2 = (short) ((foreGround >> 16) & 255);
		short g2 = (short) ((foreGround >> 8) & 255);
		short b2 = (short) ((foreGround) & 255);

		short r3 = (short) ((r1 * percent + r2 * (1.0 - percent)));
		short g3 = (short) ((g1 * percent + g2 * (1.0 - percent)));
		short b3 = (short) ((b1 * percent + b2 * (1.0 - percent)));

		return r3 << 16 | g3 << 8 | b3;
	}

	public void setPixel(int x, int y, int color)
	{
		if (x >= 0 && x < width && y >= 0 && y < height)
		{
			int check = (color >> 24) & 255;
			if (check == 255)
			{
				if (color != 0xFFFF00FF)
				{
					pixels[x + y * width] = color;
				}
			}
			//handling transparent colors
			else
			{
				if (color != 0xFFFF00FF)
				{
					int pp = pixels[x + y * width];
					pixels[x + y * width] = blend(pp, color);
					
				}
			}
		}
	}

	public void setWorldPixel(int x, int y, int color)
	{
		x -= Registry.camera.x;
		y -= Registry.camera.y;
		setPixel(x, y, color);
	}
}