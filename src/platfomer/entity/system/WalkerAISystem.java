package platfomer.entity.system;

import platfomer.entity.Entity;
import platfomer.entity.component.MovementComponent;
import platfomer.entity.component.WalkerAIComponent;
import platfomer.level.LevelChanger;

public class WalkerAISystem extends ESystem
{
	public void tick(Entity e)
	{
		if (e.hasComponent(WalkerAIComponent.class) && e.hasComponent(MovementComponent.class))
		{
			WalkerAIComponent ai = e.getComponent(WalkerAIComponent.class);
			MovementComponent movement = e.getComponent(MovementComponent.class);
			if (ai.timer <= 0)
			{
				if(ai.dir != 1)
				{
					ai.dir = 1;
				}
				else
				{
					ai.dir = -1;
				}
				
				if (LevelChanger.curLevel.getTerrainHeight((int) e.x + ai.dir) == LevelChanger.getHeight())
				{
					ai.dir = -ai.dir;
				}
//				if (ai.dir != 0)
//				{
//					ai.dir = 0;
//				}
//				else
//				{
//					//ai.dir = GameUtil.randomBetween(-1, 1);
//				}
//				if (ai.dir == 0)
//				{
//					ai.timer = 60;
//					//ai.timer = GameUtil.randomBetween(30, 60);
//				}
//				else
				{
					ai.timer = 60;
					//ai.timer = GameUtil.randomBetween(30, 150);
				}
			}
			movement.acceleration.x = 0;
			if (ai.dir == 1)
			{
				movement.acceleration.x = movement.speed;
			}
			else if (ai.dir == -1)
			{
				movement.acceleration.x = -movement.speed;
			}
			if (ai.x != e.x)
			{
				ai.x = (int) e.x;
				if (LevelChanger.curLevel.getTerrainHeight((int) e.x + ai.dir) == LevelChanger.getHeight())
				{
					ai.dir = 0;
					movement.acceleration.x = 0;
				}
			}
			/*ArrayList<Entity> players = Entities.getAllEntitiesPossessingComponent(KeyboardControlComponent.class);
			for (Entity player : players)
			{
				MovementComponent playermovement = player.getComponent(MovementComponent.class);
				int px = (int) player.x;
				int py = (int) player.y;
				int ex = (int) e.x;
				int ey = (int) e.y;
				if (playermovement.velocity.y <= 0)
				{
					if (px == ex && py == ey)
					{
						player.removed = true;
					}
				}
				else
				{
					if (px == ex && py + 1 == ey)
					{
						e.removed = true;
						playermovement.bouncing = true;
					}
				}
			}*/
			ai.timer--;
		}
	}
}