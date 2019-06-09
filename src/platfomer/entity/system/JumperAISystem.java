package platfomer.entity.system;

import java.util.ArrayList;

import platfomer.entity.Entities;
import platfomer.entity.Entity;
import platfomer.entity.component.JumperAIComponent;
import platfomer.entity.component.MovementComponent;
import platfomer.entity.component.PlayerComponent;

public class JumperAISystem extends ESystem
{
    public void tick(Entity e)
    {
        if (e.hasComponent(JumperAIComponent.class) && e.hasComponent(MovementComponent.class))
        {
            JumperAIComponent jai = e.getComponent(JumperAIComponent.class);
            MovementComponent movement = e.getComponent(MovementComponent.class);
            ArrayList<Entity> players = Entities.getAllEntitiesPossessingComponent(PlayerComponent.class);
            if (movement.onGround)
            {
                if (jai.floating)
                {
                    jai.floating = false;
                    movement.maxVelocity.y = movement.maxVelocityD.y;
                }
                if (jai.fluttertick > 0)
                {
                    jai.fluttertick--;
                }
                if (jai.fluttertick <= 0)
                {
                    for (Entity player : players)
                    {
                        if ((int) player.x >= (int) e.x - 3 && (int) player.x <= (int) e.x + 3)
                        {
                            if ((int) player.y <= (int) e.y + 2)
                            {
                                movement.velocity.y = -movement.jumpPower;// - Math.random() * .15;
                                jai.target = player;
                                break;
                            }
                        }
                    }
                }
            }
            else
            {
                if (movement.velocity.y >= 0)
                {
                    if (!jai.floating)
                    {
                        jai.floating = true;
                        movement.maxVelocity.y = .07;
                        jai.flutterradius = .225;
                        //jai.flutterradius = Math.random() * 0.83 + 1.42;

                    }
                    if (jai.target != null)
                    {
                        jai.flutterdir = jai.target.x < e.x ? -1 : 1;
                        if (Math.abs(e.x - jai.target.x) < 0.125) jai.flutterdir = 0;
                    }
                    if (jai.flutterdir != 0)
                    {
                        movement.velocity.x = jai.flutterradius * Math.cos(jai.fluttertick / 6.0) + jai.flutterdir * .37;
                    }

                    jai.fluttertick++;
                }
            }
        }
    }
}