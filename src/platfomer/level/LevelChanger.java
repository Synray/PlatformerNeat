package platfomer.level;

//changes the level to a premade level or maze
public class LevelChanger
{
    public static Level curLevel;

    public static void newLevel()
    {
        if (curLevel == null)
        {
            curLevel = new Level();
        }
        curLevel.generate();
    }

    public static void changeBounds()
    {
        curLevel.load();
    }

    public static void tick()
    {
    }

    public static void render()
    {
        curLevel.render();
    }

    public static int getWidth()
    {
        return curLevel.w;
    }

    public static int getHeight()
    {
        return curLevel.h;
    }
}