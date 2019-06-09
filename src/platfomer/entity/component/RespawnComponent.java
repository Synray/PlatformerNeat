package platfomer.entity.component;

public class RespawnComponent extends Component
{
	public int lives;
	public boolean dying = false;
	public boolean respawn = false;
	public int respawntick = 0;
	public int spawnX, spawnY;

	public RespawnComponent(int lives)
	{
		this(lives, 0, 0);
	}

	public RespawnComponent(int lives, int spawnX, int spawnY)
	{
		this.lives = lives;
		this.spawnX = spawnX;
		this.spawnY = spawnY;
	}

}