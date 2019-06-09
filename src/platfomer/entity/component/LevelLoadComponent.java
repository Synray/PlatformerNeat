package platfomer.entity.component;

public class LevelLoadComponent extends Component
{
	public int tileX, tileY;
	
	//offsets to change the point of checking tiles
	public int xOff, yOff;
	public boolean changedTile = true;
	public LevelLoadComponent(int xOff, int yOff)
	{
		this.xOff = xOff;
		this.yOff = yOff;
	}
}
