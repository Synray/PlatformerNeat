package platfomer.entity;

import platfomer.entity.component.Component;

import java.util.ArrayList;
import java.util.Iterator;


public class Entity implements Cloneable
{
	public ArrayList<Component> components;

	public double x, y;
	public boolean onscreen;
	public boolean removed = false;
	public boolean active = true;

	public Entity(double x, double y)
	{
		this.x = x;
		this.y = y;
		components = new ArrayList<>();
	}

	public Entity()
	{
		this(0, 0);
	}

	public <T extends Component> void addComponent(T component)
	{
		boolean add = false;
		int index = -1;
		for (Iterator i = components.iterator(); i.hasNext();)
		{
			T currentComponent = (T) i.next();
			if (currentComponent.getClass() == component.getClass())
			{
				index = components.indexOf(currentComponent);
				add = true;
				break;
			}
		}
		if (add)
		{
			if (index != -1)
			{
				components.remove(index);
				components.add(component);
			}
		}
		else
		{
			components.add(component);
		}
	}
	
	public void removeComponent(Component c)
	{
		components.remove(c);
	}

	public <T extends Component> T getComponent(Class<T> componentType)
	{
		for (Iterator i = components.iterator(); i.hasNext();)
		{
			T component = (T) i.next();
			if (component.getClass() == componentType)
			{
				return component;
			}
		}
		return null;
	}

	/**
	 * @return <code><b>true</b></code> if entity has enabled component of input type.
	 */
	public <T extends Component> boolean hasComponent(Class<T> cType)
	{
		for (Iterator i = components.iterator(); i.hasNext();)
		{
			T component = (T) i.next();
			if (component.getClass() == cType)
			{
				if (component.enabled)
				{
					return true;
				}
			}
		}
		return false;
	}
}