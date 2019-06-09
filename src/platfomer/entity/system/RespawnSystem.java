package platfomer.entity.system;

import platfomer.entity.Entity;
import platfomer.entity.component.*;
import platfomer.gfx.Text;

public class RespawnSystem extends ESystem
{
	public void tick(Entity e)
	{
		if (e.hasComponent(RespawnComponent.class))
		{
			RespawnComponent respawn = e.getComponent(RespawnComponent.class);
			if (e.removed && respawn.lives > 0 && !respawn.dying)
			{
				e.removed = false;
				if (respawn.respawntick <= 0)
				{
					e.x = respawn.spawnX;
					e.y = respawn.spawnY;
					respawn.lives--;
					respawn.respawntick = 70;
				}
			}
			else if (e.removed && respawn.lives == 0 && !respawn.dying)
			{
				if (e.hasComponent(MovementComponent.class))
				{
					MovementComponent m = e.getComponent(MovementComponent.class);
					m.gravity = false;
				}
				if (e.hasComponent(KeyboardControlComponent.class))
				{
					KeyboardControlComponent k = e.getComponent(KeyboardControlComponent.class);
					k.disable();
				}
				if(e.hasComponent(NeuralNetComponent.class))
				{
					NeuralNetComponent ne = e.getComponent(NeuralNetComponent.class);
					ne.disable();
				}
				e.removed = false;
				respawn.dying = true;
				respawn.respawntick = 183;
			}
			if (respawn.dying)
			{
				if (e.hasComponent(MovementComponent.class))
				{
					MovementComponent movement = e.getComponent(MovementComponent.class);
					movement.colliding = false;
					if (respawn.respawntick > 176)
					{
						movement.acceleration.y = 0;
						movement.acceleration.x = 0;
						movement.velocity.x = 0;
						movement.velocity.y = -.7;

					}
					if (respawn.respawntick == 176)
					{
						movement.velocity.y = 0;
					}
					if (respawn.respawntick > 137)
					{
						if (respawn.respawntick % 10 == 0)
						{
							RenderComponent render = e.getComponent(RenderComponent.class);
							render.enabled ^= true;
						}
					}
					if (respawn.respawntick == 137)
					{
						RenderComponent render = e.getComponent(RenderComponent.class);
						render.enabled = true;
					}

					if (respawn.respawntick == 80)
					{

						if (e.hasComponent(MovementComponent.class))
						{
							MovementComponent m = e.getComponent(MovementComponent.class);
							m.gravity = true;
						}
					}
					if (respawn.respawntick == 0)
					{
						e.removed = true;
					}
					respawn.respawntick--;
				}
				else
				{
					e.removed = true;
				}
			}
			else if (respawn.respawntick > 0)
			{
				if (respawn.respawntick % 10 == 0)
				{
					RenderComponent render = e.getComponent(RenderComponent.class);
					render.enabled ^= true;
				}
				respawn.respawntick--;
				if (respawn.respawntick == 0)
				{
					RenderComponent render = e.getComponent(RenderComponent.class);
					render.enabled = true;
				}
			}
		}
	}

	public void render(Entity e)
	{
		if (e.hasComponent(RespawnComponent.class) && e.hasComponent(KeyboardControlComponent.class))
		{
			RespawnComponent respawn = e.getComponent(RespawnComponent.class);
			Text.drawText(2, 9, "Lives: " + respawn.lives);
		}
	}
}