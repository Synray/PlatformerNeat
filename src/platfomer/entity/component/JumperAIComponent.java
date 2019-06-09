package platfomer.entity.component;

import platfomer.entity.Entity;

public class JumperAIComponent extends Component
{
	public boolean floating;
	public int fluttertick;
	public double flutterradius = 1.6;
	public double flutterdir = 0;
	public Entity target;
}