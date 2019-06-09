package platfomer.gfx;


public class Camera
{
	public int x, y;
	public int width, height;
	public int xMid, yMid;

	public Camera(int x, int y, int width, int height)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public void tick()
	{
		xMid = (width / 2) + x;
		yMid = (height / 2) + y;
	}
}