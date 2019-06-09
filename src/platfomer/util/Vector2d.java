package platfomer.util;

public class Vector2d
{
	public double x;
	public double y;

	public Vector2d()
	{
		set(0, 0);
	}

	public Vector2d(Vector2d vector)
	{
		set(vector.x, vector.y);
	}

	public Vector2d(double x, double y)
	{
		set(x, y);
	}
	
	public Vector2d(double z)
	{
		set(z, z);
	}

	public Vector2d set(double x, double y)
	{
		this.x = x;
		this.y = y;
		return this;
	}

	public Vector2d setX(double x)
	{
		this.x = x;
		return this;
	}

	public Vector2d setY(double y)
	{
		this.y = y;
		return this;
	}

	public double getX()
	{
		return x;
	}

	public double getY()
	{
		return y;
	}
	
	public boolean equals(Object obj)
	{
		if(!(obj instanceof Vector2d)) return false;
		Vector2d vec = (Vector2d) obj;
        return vec.x == this.x && vec.y == this.y;
    }
}