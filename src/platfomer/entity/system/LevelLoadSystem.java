package platfomer.entity.system;

import platfomer.entity.Entity;
import platfomer.entity.component.LevelLoadComponent;
import platfomer.level.LevelChanger;

public class LevelLoadSystem extends ESystem
{
	public void tick(Entity e)
	{
		LevelLoadComponent component = e.getComponent(LevelLoadComponent.class);
		if (component != null)
		{
			if (component.tileX != (int) ((e.x + component.xOff)))
			{
				component.tileX = (int) ((e.x + component.xOff));
				component.changedTile = true;
			}
			if (component.tileY != (int) ((e.y + component.yOff)))
			{
				component.tileY = (int) ((e.y + component.yOff));
				component.changedTile = true;
			}
			if (component.changedTile)
			{
				LevelChanger.changeBounds();
				component.changedTile = false;
			}
		}
	}
}
