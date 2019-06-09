package platfomer.entity.component;

import platfomer.networks.Network;

public class NeuralNetComponent extends Component
{
	public Network net;
	public boolean justJumped;

	public NeuralNetComponent(Network net)
	{
		this.net = net;
	}
}