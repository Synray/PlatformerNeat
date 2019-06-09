package platfomer.entity.system;

import platfomer.entity.Entity;
import platfomer.entity.component.KeyboardControlComponent;
import platfomer.entity.component.MovementComponent;
import platfomer.registry.Registry;

import java.awt.event.KeyEvent;

public class KeyboardControlSystem extends ESystem
{
    public void tick(Entity e)
    {
        if (e.hasComponent(KeyboardControlComponent.class))
        {
            if (e.hasComponent(MovementComponent.class))
            {
                MovementComponent movement = e.getComponent(MovementComponent.class);
                movement.acceleration.x = 0;
                if (Registry.input.key(KeyEvent.VK_RIGHT).held && !Registry.input.key(KeyEvent.VK_LEFT).held)
                {
                    movement.acceleration.x = movement.speed;

                }
                if (Registry.input.key(KeyEvent.VK_LEFT).held && !Registry.input.key(KeyEvent.VK_RIGHT).held)
                {

                    movement.acceleration.x = -movement.speed;

                }
                if (Registry.input.key(KeyEvent.VK_UP).pressed && movement.onGround)
                {
                    movement.velocity.y = -movement.jumpPower;
                }
                if (movement.bouncing)
                {
                    movement.velocity.y = -movement.jumpPower / 2;
                    if (Registry.input.key(KeyEvent.VK_UP).held)
                    {
                        movement.velocity.y = -movement.jumpPower * 1.15;
                    }
                    movement.bouncing = false;
                }

                if (Registry.input.key(KeyEvent.VK_SPACE).pressed)
                {
                    movement.gravity ^= true;

                }
                if (!movement.gravity)
                {
                    movement.velocity.y = 0;
                    if (Registry.input.key(KeyEvent.VK_UP).held)
                    {
                        movement.velocity.y = -movement.jumpPower;
                    }
                    else if(Registry.input.key(KeyEvent.VK_DOWN).held)
                    {
                        movement.velocity.y = movement.jumpPower;
                    }
                }
            }
        }
    }
}