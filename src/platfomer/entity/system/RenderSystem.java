package platfomer.entity.system;

import platfomer.entity.Entity;
import platfomer.entity.component.RenderComponent;
import platfomer.registry.Registry;

public class RenderSystem extends ESystem
{
	public void render(Entity e)
	{
		if (e.hasComponent(RenderComponent.class))
		{
			RenderComponent render = e.getComponent(RenderComponent.class);
			if (render.pixelColor != 0xFF00FF)
			{
				Registry.screen.setWorldPixel((int) e.x, (int) e.y, render.pixelColor);
			}
		}
	}
}