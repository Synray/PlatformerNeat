package platfomer.entity;

import java.util.ArrayList;

import platfomer.entity.system.LevelLoadSystem;
import platfomer.entity.system.NeuralNetSystem;
import platfomer.entity.system.RespawnSystem;
import platfomer.entity.component.Component;
import platfomer.entity.system.CameraControlSystem;
import platfomer.entity.system.JumperAISystem;
import platfomer.entity.system.KeyboardControlSystem;
import platfomer.entity.system.MovementSystem;
import platfomer.entity.system.RenderSystem;
import platfomer.entity.system.WalkerAISystem;

public class Entities
{
    public static ArrayList<Entity> entities = new ArrayList<>();

    //systems
    public static MovementSystem movementSystem = new MovementSystem();
    public static CameraControlSystem cameraControlSystem = new CameraControlSystem();
    public static LevelLoadSystem levelLoadSystem = new LevelLoadSystem();
    public static KeyboardControlSystem keyboardControlSystem = new KeyboardControlSystem();
    public static NeuralNetSystem neuralNetSystem = new NeuralNetSystem();
    public static WalkerAISystem walkerAISystem = new WalkerAISystem();
    public static JumperAISystem jumperAISystem = new JumperAISystem();
    public static RenderSystem renderSystem = new RenderSystem();
    public static RespawnSystem respawnSystem = new RespawnSystem();

    public static void tick()
    {
        for (int i = 0; i < entities.size(); i++)
        {
            Entity e = entities.get(i);
            if (e.active)
            {
                keyboardControlSystem.tick(e);
                neuralNetSystem.tick(e);
                walkerAISystem.tick(e);
                jumperAISystem.tick(e);
                movementSystem.tick(e);
                cameraControlSystem.tick(e);
                levelLoadSystem.tick(e);
            }
        }
        for (int i = 0; i < entities.size(); i++)
        {
            Entity e = entities.get(i);
            respawnSystem.tick(e);
            if (entities.get(i).removed)
            {
                entities.remove(entities.get(i));
                i--;
            }
        }

    }

    public static <T extends Component> ArrayList<Entity> getAllEntitiesPossessingComponent(Class<T> component)
    {
        ArrayList<Entity> possessors = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++)
        {
            if (entities.get(i).hasComponent(component) && entities.get(i).active && !entities.get(i).removed)
            {
                possessors.add(entities.get(i));
            }
        }
        return possessors;
    }

    public static void render()
    {
        for (int i = 0; i < entities.size(); i++)
        {
            Entity e = entities.get(i);
            renderSystem.render(e);
            respawnSystem.render(e);
        }
    }

    public static void add(Entity e)
    {
        entities.add(e);
    }

    public static void remove(Entity e)
    {
        entities.remove(e);
    }

    public static void clear()
    {
        entities.clear();
    }
}