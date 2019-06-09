package platfomer.entity;

import platfomer.entity.component.*;
import platfomer.networks.Network;
import platfomer.util.Vector2d;

public class EntityFactory
{
    public static Entity createPlayer(double x, double y)
    {
        Entity e = new Entity(x, y);
        e.addComponent(new RenderComponent(0xFFFFFFFF));
        e.addComponent(new CameraControlComponent());
        e.addComponent(new MovementComponent());
        e.addComponent(new RespawnComponent(3, (int) x, (int) y));
        e.addComponent(new PlayerComponent());
        e.addComponent(new KeyboardControlComponent());
        e.addComponent(new LevelLoadComponent(0, 0));
        return e;
    }

    public static Entity createNeuralPlayer(double x, double y, Network net)
    {
        Entity e = new Entity(x, y);
        e.addComponent(new RenderComponent(0xFFFFFFFF));
        e.addComponent(new CameraControlComponent());
        e.addComponent(new MovementComponent());
        e.addComponent(new NeuralNetComponent(net));
        e.addComponent(new PlayerComponent());
        e.addComponent(new LevelLoadComponent(0, 0));
        return e;
    }

    public static Entity createWalker(double x, double y)
    {
        Entity e = new Entity(x, y);
        e.addComponent(new RenderComponent(0xFFF44F44));
        MovementComponent movement = new MovementComponent().setMaxVelocity(new Vector2d(.1, 1));
        movement.stompable = true;
        e.addComponent(movement);
        e.addComponent(new WalkerAIComponent());
        return e;
    }

    public static Entity createJumper(double x, double y)
    {
        Entity e = new Entity(x, y);
        e.addComponent(new RenderComponent(0xFFB56C91));
        MovementComponent movement = new MovementComponent();
        movement.stompable = true;
        movement.jumpPower = .60;
        movement.drag.x = .25;
        movement.setMaxVelocity(new Vector2d(3, 3), true);
        e.addComponent(movement);
        e.addComponent(new JumperAIComponent());
        return e;
    }
}