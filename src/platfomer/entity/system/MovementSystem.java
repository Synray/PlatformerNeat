package platfomer.entity.system;

import platfomer.entity.Entities;
import platfomer.entity.Entity;
import platfomer.entity.component.JumperAIComponent;
import platfomer.entity.component.MovementComponent;
import platfomer.entity.component.PlayerComponent;
import platfomer.entity.component.RespawnComponent;
import platfomer.level.LevelChanger;

import java.util.ArrayList;

public class MovementSystem extends ESystem
{
    public MovementSystem()
    {
    }

    public void tick(Entity e)
    {
        if (e.removed)
        {
            return;
        }
        MovementComponent movement = e.getComponent(MovementComponent.class);
        if (movement != null)
        {
            if (movement.enabled)
            {
                computeVelocity(movement);
                move(movement.velocity.x, movement.velocity.y, e, movement);
                stayInBounds(e, movement);
            }
        }
    }

    private static void computeVelocity(MovementComponent movement)
    {
        if (movement.acceleration.x != 0)
        {
            movement.velocity.x += movement.acceleration.x;
        }
        else if (movement.drag.x != 0)
        {
            if (movement.velocity.x - movement.drag.x > 0)
            {
                movement.velocity.x -= movement.drag.x;
            }
            else if (movement.velocity.x + movement.drag.x < 0)
            {
                movement.velocity.x += movement.drag.x;
            }
            else
            {
                movement.velocity.x = 0;
            }
        }

        if (movement.velocity.x != 0 && movement.maxVelocity.x <= 10000.0)
        {
            if (movement.velocity.x > movement.maxVelocity.x) movement.velocity.x = movement.maxVelocity.x;
            else if (movement.velocity.x < -movement.maxVelocity.x) movement.velocity.x = -movement.maxVelocity.x;
        }

        if (movement.gravity)
        {
            if (movement.gforce != 0.0)
            {
                movement.acceleration.y = movement.gforce;
            }
            else
            {
                movement.acceleration.y = 0.0;
            }
        }
        else
        {
            movement.acceleration.y = 0;
        }

        if (movement.acceleration.y != 0)
        {
            movement.velocity.y += movement.acceleration.y;
        }
        else if (movement.drag.y != 0)
        {
            if (movement.velocity.y - movement.drag.y > 0)
            {
                movement.velocity.y -= movement.drag.y;
            }
            else if (movement.velocity.y + movement.drag.y < 0)
            {
                movement.velocity.y += movement.drag.y;
            }
            else
            {
                movement.velocity.y = 0;
            }
        }
        if (movement.velocity.y != 0 && movement.maxVelocity.y <= 10000)
        {
            if (movement.velocity.y > movement.maxVelocity.y) movement.velocity.y = movement.maxVelocity.y;
            else if (movement.velocity.y < -movement.maxVelocity.y) movement.velocity.y = -movement.maxVelocity.y;
        }
    }

    private static void move(double xv, double yv, Entity e, MovementComponent movement)
    {
        if (xv != 0 && yv != 0)
        {
            move(xv, 0, e, movement);
            move(0, yv, e, movement);
            return;
        }
        if (xv != 0)
        {
            while (xv != 0)
            {
                if (Math.abs(xv) > 1)
                {
                    if (!isColliding(Math.signum(xv), yv, e, movement))
                    {
                        e.x += Math.signum(xv);
                        xv -= Math.signum(xv);
                    }
                    else
                    {
                        xv = 0;
                        movement.velocity.x = 0;
                        movement.acceleration.x = 0;
                        e.x = (int) e.x;
                    }
                }
                else
                {
                    if (!isColliding(xv, yv, e, movement))
                    {
                        e.x += xv;
                        xv = 0;
                    }
                    else
                    {
                        xv = 0;
                        movement.velocity.x = 0;
                        movement.acceleration.x = 0;
                        e.x = (int) e.x;
                    }
                }
                if (!isColliding(0, 1, e, movement))
                {
                    movement.onGround = false;
                }
            }
        }
        if (yv != 0)
        {
            movement.onGround = false;
            while (yv != 0)
            {
                if (Math.abs(yv) > 1)
                {
                    if (!isColliding(xv, Math.signum(yv), e, movement))
                    {
                        e.y += Math.signum(yv);
                        yv -= Math.signum(yv);
                    }
                    else
                    {
                        if (yv > 0)
                        {
                            movement.onGround = true;
                        }
                        yv = 0;
                        movement.velocity.y = 0;
                        e.y = (int) e.y;
                    }
                }
                else
                {
                    if (!isColliding(xv, Math.signum(yv), e, movement))
                    {
                        e.y += yv;
                        yv = 0;
                    }
                    else
                    {
                        if (yv > 0)
                        {
                            movement.onGround = true;
                        }
                        yv = 0;
                        movement.velocity.y = 0;
                        e.y = (int) e.y;
                    }
                }

            }
        }
        if (movement.colliding)
        {
            if (!isSolidTile(e, 0, 1) && !touchingEntity(e, 0, 1, movement, false))
            {
                movement.onGround = false;
            }
        }
    }

    private static boolean isColliding(double xa, double ya, Entity e, MovementComponent movement)
    {
        if (movement.colliding)
        {
            if (isSolidTile(e, xa, ya))
            {
                return true;
            }
            return touchingEntity(e, xa, ya, movement, true);
        }
        return false;
    }

    private static boolean touchingEntity(Entity e, double xa, double ya, MovementComponent movement, boolean hurt)
    {
        ArrayList<Entity> entities = Entities.getAllEntitiesPossessingComponent(MovementComponent.class);
        boolean inEntity = false;
        boolean nextEntity = false;
        for (int i = 0; i < entities.size(); i++)
        {
            Entity e2 = entities.get(i);
            if (e2 == e)
            {
                continue;
            }
            MovementComponent e2m = e2.getComponent(MovementComponent.class);
            if (e2.removed || e.removed)
            {
                continue;
            }
            if (!e2m.colliding)
            {
                continue;
            }
            if (!movement.colliding)
            {
                continue;
            }

            double e1y = e.y;
            if ((int) (e.x) >= (int) e2.x && (int) (e.x) < (int) Math.ceil(e2.x))
            {
                if ((int) (e1y) >= (int) e2.y && (e1y) < (int) Math.ceil(e2.y))
                {
                    inEntity = true;
                }
            }
            if ((int) (e.x + xa) >= (int) e2.x && (int) (e.x + xa) <= (int) (e2.x))
            {
                if ((int) (e1y + ya) >= (int) e2.y && (int) (e1y + ya) <= (int) (e2.y))
                {
                    nextEntity = true;
                    if (hurt)
                    {
                        if (e2m.stompable && movement.stompable) continue;
                        if (movement.stompable)
                        {
                            if (e2.y >= e.y || e2m.velocity.y <= 0)
                            {
                                e2.removed = true;
                            }
                            else
                            {
                                e.removed = true;
                                e2m.bouncing = true;
                                PlayerComponent pc;
                                if ((pc = e2.getComponent(PlayerComponent.class)) != null)
                                {
                                    if (e.hasComponent(JumperAIComponent.class))
                                    {
                                        pc.stomps += 5;
                                    }
                                    else
                                    {
                                        pc.stomps++;
                                    }
                                }
                            }
                        }
                        else if (e2m.stompable)
                        {
                            if (e.y >= e2.y || ya <= 0 || movement.velocity.y < 0)
                            {
                                e.removed = true;
                            }
                            else
                            {
                                e2.removed = true;
                                movement.bouncing = true;
                                PlayerComponent pc;
                                if ((pc = e.getComponent(PlayerComponent.class)) != null)
                                {
                                    if (e2.hasComponent(JumperAIComponent.class))
                                    {
                                        pc.stomps += 5;
                                    }
                                    else
                                    {
                                        pc.stomps++;
                                    }
                                }
                            }
                        }
                        if (e.hasComponent(PlayerComponent.class))
                        {
                            PlayerComponent p = e.getComponent(PlayerComponent.class);
                            if (p.invincible)
                            {
                                e.removed = false;
                            }
                        }
                        if (e2.hasComponent(PlayerComponent.class))
                        {
                            PlayerComponent p = e2.getComponent(PlayerComponent.class);
                            if (p.invincible)
                            {
                                e2.removed = false;
                            }
                        }
                    }
                }
            }
        }
        return !inEntity && nextEntity;
    }

    private static boolean isSolidTile(Entity e, double xa, double ya)
    {
        double ex = e.x;
        double ey = e.y;

        int curTile = LevelChanger.curLevel.getTile((int) (ex), (int) (ey));
        int nextTile = LevelChanger.curLevel.getTile((int) (ex + xa), (int) (ey + ya));
        if (ya < 0)
        {
            if (nextTile == 1)
            {
                return LevelChanger.curLevel.getTile((int) (e.x + xa), (int) (e.y - 2)) != 0;
            }
        }
        if (curTile == 1)
        {
            if (LevelChanger.curLevel.getTile((int) (e.x), (int) (e.y - 1)) == 0 && LevelChanger.curLevel.getTile((int) (e.x), (int) (e.y + 1)) == 0)
            {
                return false;
            }
            else
            {
                while (curTile == 1)
                {
                    e.y--;
                    curTile = LevelChanger.curLevel.getTile((int) (e.x), (int) (e.y));
                }
            }
            return true;
        }
        if (nextTile == 1)
        {
            return LevelChanger.curLevel.getTile((int) (e.x + xa), (int) (e.y - 1)) != 0 || LevelChanger.curLevel.getTile((int) (e.x + xa), (int) (e.y + 1)) != 0;
        }
        return false;
    }

    private static void stayInBounds(Entity e, MovementComponent movement)
    {
        if (e.x < 0)
        {
            e.x = 0;
            movement.velocity.x = 0;
        }
		/*if (e.y < 0)
		{
			e.y = 0;
		}*/
        if (e.x + 1 > LevelChanger.getWidth())
        {
            e.x = LevelChanger.getWidth() - 1;
            movement.velocity.x = 0;
        }
        if (e.y + 1 > LevelChanger.getHeight())
        {
            if (e.hasComponent(RespawnComponent.class))
            {
                RespawnComponent respawn = e.getComponent(RespawnComponent.class);
                if (!e.removed && !respawn.dying)
                {
                    e.removed = true;
                }
            }
            else
            {
                e.removed = true;
            }
        }

    }
}
