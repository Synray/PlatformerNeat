package platfomer.entity.system;

import platfomer.entity.Entity;
import platfomer.entity.component.CameraControlComponent;
import platfomer.level.LevelChanger;
import platfomer.registry.Registry;

public class CameraControlSystem extends ESystem
{
    public void tick(Entity e)
    {
        if (e.hasComponent(CameraControlComponent.class))
        {
            if (e.x != Registry.camera.xMid)
            {
                Registry.camera.x = (int) (e.x - Registry.camera.width / 2);
            }
            if (e.y != Registry.camera.yMid)
            {
                Registry.camera.y = (int) (e.y - Registry.camera.height / 2);
            }
        }
        Registry.camera.xMid = (Registry.camera.width / 2) + Registry.camera.x;
        Registry.camera.yMid = (Registry.camera.height / 2) + Registry.camera.y;
        adjustCameraBounds();
    }

    public static void adjustCameraBounds()
    {
        if (Registry.camera.x < 0)
        {
            Registry.camera.x = 0;
        }
        if (Registry.camera.x + Registry.camera.width > LevelChanger.getWidth())
        {
            Registry.camera.x = LevelChanger.getWidth() - Registry.camera.width;
        }
    }
}
