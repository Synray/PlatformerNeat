package platfomer.entity.system;

import platfomer.entity.Entity;

public abstract class ESystem
{
	protected static int tick;

	/**
	 * called once on the creation of the entity
	 */
	public void add(Entity e)
	{

	}

	/**
	 * called once on removal of the entity
	 */
	public void remove(Entity e)
	{

	}

	/**
	 * called every frame in tick()
	 */
	public void tick(Entity e)
	{

	}

	/**
	 * called every frame in render()
	 */
	public void render(Entity e)
	{

	}
}
