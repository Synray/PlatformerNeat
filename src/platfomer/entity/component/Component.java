package platfomer.entity.component;

/**
 * Contains data that distinguishes different types of Entities
 */
public abstract class Component
{
	public boolean enabled = true;
	public boolean removed = false;
	
	public void enable()
	{
		this.enabled = true;
	}
	
	public void disable()
	{
		this.enabled = false;
	}
	
	public void toggle()
	{
		this.enabled = !this.enabled;
	}
}
