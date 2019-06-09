package platfomer.entity.system;

import platfomer.Main;
import platfomer.entity.component.*;
import platfomer.entity.Entities;
import platfomer.entity.Entity;
import platfomer.level.LevelChanger;
import platfomer.registry.Registry;
import platfomer.util.NEATUtil;

public class NeuralNetSystem extends ESystem
{
    public void tick(Entity e)
    {
        if (e.hasComponent(NeuralNetComponent.class) && e.hasComponent(MovementComponent.class))
        {
            if (LevelChanger.curLevel.tiles.length < NEATUtil.inputWidth * NEATUtil.inputHeight)
            {
                return;
            }

            MovementComponent movement = e.getComponent(MovementComponent.class);
            NeuralNetComponent net = e.getComponent(NeuralNetComponent.class);
            getInputs();

            net.net.loadInputs(input);
            net.net.activate();

            double[] netOutputs = net.net.getOutputs();

            if (output == null || output.length != netOutputs.length)
            {
                output = new boolean[netOutputs.length];
            }

            for (int i = 0; i < netOutputs.length; ++i)
            {
                output[i] = (netOutputs[i] > 0.0);
            }

            movement.acceleration.x = 0;
            if (output[0] && !output[2])
            {
                movement.acceleration.x = -movement.speed;
            }
            if (output[2] && !output[0])
            {
                movement.acceleration.x = movement.speed;
            }
            if (output[1])
            {
                if (movement.onGround && !net.justJumped)
                {
                    movement.velocity.y = -movement.jumpPower;
                }
                net.justJumped = true;
            }
            if (!output[1])
            {
                net.justJumped = false;
            }
            if (movement.bouncing)
            {
                movement.velocity.y = -movement.jumpPower / 2;
                if (output[1])
                {
                    movement.velocity.y = -movement.jumpPower * 1.15;
                }
                movement.bouncing = false;
            }
        }
    }

    private static double[] input;
    private static boolean[] output;

    public static double[] getInputs()
    {
        copyFromIntArray(LevelChanger.curLevel.tiles);
        for (int i = 0; i < input.length - 1; i++)
        {
            if (input[i] < 0)
            {
                input[i] = 0;
            }
        }

        int yOff = (Main.HEIGHT - 2) / 2 - NEATUtil.inputHeight / 2;
        int xOff = (Main.WIDTH - 2) / 2 - NEATUtil.inputWidth / 2;
        int yMax = yOff + NEATUtil.inputHeight;
        int xMax = xOff + NEATUtil.inputWidth;

        for (int i = 0; i < Entities.entities.size(); i++)
        {
            Entity e = Entities.entities.get(i);

            if (e.x - Registry.camera.x < xOff || e.y - Registry.camera.y < yOff) continue;
            if (e.x - Registry.camera.x > xMax || e.y - Registry.camera.y > yMax) continue;

            int mobX = (int) (e.x - Registry.camera.x);
            int mobY = (int) (e.y - Registry.camera.y);
            if (mobX >= xOff && mobX < xMax && mobY >= yOff && mobY < yMax)
            {
                if (e.hasComponent(JumperAIComponent.class))
                {
                    input[(mobX - xOff) + (mobY - yOff) * NEATUtil.inputWidth] = -2;
                }
                if (e.hasComponent(WalkerAIComponent.class))
                {
                    input[(mobX - xOff) + (mobY - yOff) * NEATUtil.inputWidth] = -1;
                }
                if (e.hasComponent((PlayerComponent.class)))
                {
                    input[(mobX - xOff) + (mobY - yOff) * NEATUtil.inputWidth] = 2;
                }
            }
        }
        return input;
    }

    private static void copyFromIntArray(int[] source)
    {
        if (input == null)// || input.length != source.length)
        {
//			input = new double[source.length];
            input = new double[NEATUtil.inputWidth * NEATUtil.inputHeight];
        }
        int yOff = (Main.HEIGHT - 2) / 2 - NEATUtil.inputHeight / 2;
        int xOff = (Main.WIDTH - 2) / 2 - NEATUtil.inputWidth / 2;
        for (int y = 0; y < NEATUtil.inputHeight; ++y)
        {
            int yp = yOff + y;
            for (int x = 0; x < NEATUtil.inputWidth; ++x)
            {
                int xp = xOff + x;
                input[x + y * NEATUtil.inputWidth] = source[xp + yp * 30];
            }
        }
//        for (int i = 0; i < source.length; i++)
//        {
//            input[i] = source[i];
//        }
    }
}