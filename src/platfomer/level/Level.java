package platfomer.level;

import platfomer.Main;
import platfomer.entity.Entities;
import platfomer.entity.Entity;
import platfomer.entity.EntityFactory;
import platfomer.registry.Registry;
import platfomer.util.FastRandom;

public class Level
{
    private static final int DEFAULT_SEED = 1337;
    private FastRandom random;

    public Level()
    {
        random = new FastRandom(/*DEFAULT_SEED*/);
    }

    private double randsign()
    {
        double rand = random.nextInt(2);
        if (rand == 0)
        {
            rand = -1.0;
        }
        return rand;
    }

    private int randomBetween(int min, int max)
    {
        if (max < min)
        {
            int c = min;
            min = max;
            max = c;
        }
        return min + random.nextInt((max - min) + 1);
    }

    private int[] map;
    public int[] tiles;
    private int empty = 0;
    private int ground = 1;
    private int enemy1 = -1;
    private int enemy2 = -2;
    private int enemy1spawn = -101;
    private int enemy2spawn = -102;

    public int w, h;
    public int levelWidth;
    private static final int minY = 5;
    private static final int minX = 30;
    private static final int maxPlatformW = 6;
    private static final int minPlatformW = 1;
    public int maxPlatformStep = 3;
    public int minPlatformStep = 2;

    private int boundsX, boundsY;
    private int boundsW, boundsH;

    private static final int groundColor1 = 0xFF00FF00; //Top layer color
    private static final int groundColor2 = 0xFF329E32;
    private static final int skyColor = 0xFF70ABE8;//0xFF70ABD8;
    private static final int platformColor = 0xFFC1833C;
    private static final int platformColor2 = 0xFF563E23;

    private static final int goalStartX = 15;

    public void seed(long seed)
    {
        random = new FastRandom(seed);
    }

    public void seed(byte[] seed)
    {
        random = new FastRandom(seed);
    }

    public byte[] getSeed()
    {
        return random.getSeed();
    }

    public void generate()
    {
        boundsX = 0;
        boundsY = 0;
        w = minX + Main.WIDTH - 2 + Main.WIDTH * 4 + random.nextInt(Main.WIDTH * 6) + goalStartX;
//        w = minX + 1500 + goalStartX;
//        w = minX + 500 + goalStartX;
//        w = minX + Main.WIDTH - 2 + Main.WIDTH * 10 + random.nextInt(Main.WIDTH * 8) + goalStartX;
        h = Main.HEIGHT - 2 + random.nextInt(Main.HEIGHT * 3);
        levelWidth = w - goalStartX;
        map = new int[w * h];
        createLevel(1);
        load();
    }

    private void createLevel(int type)
    {
        if (type == 0)
        {
//            createPlatformLevel();
            createJumpLevel();
        }
        else
        {
            createTerrain();
            createGaps();
            createPlatforms();
            addMonsters();
        }
    }

    private void createTerrain()
    {
        int heightAdd = 0;
        if ((h - 1) - ((Main.HEIGHT - (minY + 3)) + minY) > 0)
        {
            heightAdd = random.nextInt((h - 1) - ((Main.HEIGHT - (minY + 3)) + minY));
        }
        int levelHeight = minY + (Main.HEIGHT - (minY + 3)) + heightAdd;
        for (int x = 0; x < w; x++)
        {
            for (int y = levelHeight; y < h; y++)
            {
                setTile(x, y, ground);
            }
        }
        int partition = 0;
        int partitionX = minX;
        int partitionY = levelHeight;
        while (partitionX + partition < levelWidth && partitionX < levelWidth)
        {
            //The number of tiles to raise or loewr
            int part = random.nextInt(5) + 2;
            //Which way to move the tiles
            int dir = random.nextInt(2);
            //The number of tiles to move up or down
            int raise = random.nextInt(3) + 1;
            if (dir == 0)
            {
                dir = -1;
            }
            if (partitionY <= minY && dir == -1)
            {
                dir = random.nextInt(2);
            }
            if (partitionY >= h - 2 && dir == 1)
            {
                dir = random.nextInt(2) - 1;
            }

            if (dir == 1 && partitionY + raise >= h)
            {
                raise = (h - 2) - partitionY;
            }
            if (dir == -1 && partitionY - raise < minY)
            {
                raise = partitionY - minY;
                if (raise == 0)
                {
                    continue;
                }
            }
            if (partitionX + part < w)
            {
                partition = part;
            }
            else
            {
                partition = (w - 1) - partitionX;
            }
            if (dir == -1)
            {
                for (int y = 0; y < raise; y++)
                {
                    for (int x = partitionX; x < w; x++)
                    {
                        setTile(x, partitionY - ((raise - 1) - y), ground);
                    }
                }
                partitionY += dir * raise;
            }
            else if (dir == 1)
            {
                for (int y = 0; y < raise; y++)
                {
                    for (int x = partitionX; x < w; x++)
                    {
                        setTile(x, partitionY + ((raise - 1) - y), empty);
                    }
                }
                partitionY += dir * raise;
            }

            partitionX += partition;
        }
    }

    private void createGaps()
    {
        int gap;
        int gapX = minX + 4 + random.nextInt(10);
        int maxGapSpace = (int) ((levelWidth - gapX) * .367);
        int gapSpace = random.nextInt(maxGapSpace);
        if (gapSpace == 0)
        {
            return;
        }
        while (gapSpace > 0)
        {
            int gapRoll = random.nextInt(10) + 1;
            if (gapRoll > 9)
            {
                gap = 1;
            }
            else if (gapRoll > 7)
            {
                gap = 2;
            }
            else if (gapRoll > 4)
            {
                gap = 3;
            }
            else
            {
                gap = 4;
            }
            if (gapX + gap >= levelWidth - 3)
            {
                gapSpace = 0;
                break;
            }
            if (gap > gapSpace)
            {
                gap = gapSpace;
            }
            for (int y = 0; y < h; y++)
            {
                for (int x = 0; x < gap; x++)
                {
                    setTile(gapX + x, y, empty);
                }
            }
            if ((gap == 4 && getTerrainHeight(gapX - 1) - getTerrainHeight(gapX + gap) >= 3) || getTerrainHeight(gapX - 1) - getTerrainHeight(gapX + gap) > 3)
            {
                //System.out.println("did this thing");
                for (; getTerrainHeight(gapX - 1) - getTerrainHeight(gapX + gap) >= 3; )
                {
//					System.out.println("this");
                    setTile(gapX + gap, getTerrainHeight(gapX + gap), empty);
                }
            }
            gapSpace -= gap;
            int maxGapX = (int) (levelWidth * .4);
            gapX += gap + (int) (maxGapX * 0.167) + random.nextInt(maxGapX);
        }
    }

    private void createPlatforms()
    {
        int startX = minX + random.nextInt(6) + 2;
        int maxPlatformChain = 10;

        int platformX, platformY;
        int platformW;
        /*
          end of the platform
         */
        int lastPlatformX;
        int lastPlatformY;
        int platformChain = 0;

        platformX = startX;
        platformY = getTerrainHeight(startX) - (2 + random.nextInt(3));
        while (platformX < levelWidth)
        {
            //width = 1 to 6
            platformW = randomBetween(minPlatformW, maxPlatformW);
            //set the chain length
            if (platformChain <= 0)
            {
                platformChain = random.nextInt(maxPlatformChain) + 1;
            }
            //Check every block for intersection with the terrain.
            //raise the platform so that it is at least one tile above the ground
            for (int x = -1; x <= platformW; x++)
            {
                if (platformY >= getTerrainHeight(platformX + x) - 1)
                {
                    platformY = getTerrainHeight(platformX + x) - (2 + random.nextInt(3));
                }
            }

            //Set the platform tiles to ground
            for (int x = 0; x < platformW; x++)
            {
                if (platformX + x > levelWidth - 1)
                {
                    break;
                }
                setTile(platformX + x, platformY, ground);
            }

            lastPlatformX = platformX + platformW - 1;
            lastPlatformY = platformY;

            if (platformChain > 1)
            {
                //Set the y position relative to the last platform
                //from 3 below to 3 above
                platformY = lastPlatformY - (random.nextInt(7) - 3);
                //
                platformX = (lastPlatformX) + 3 + random.nextInt(3);
                if (lastPlatformY - platformY == 4 && (platformX) - lastPlatformX == 5)
                {
                    System.out.println("changed platform distance");
                    if (random.nextInt(2) == 1)
                    {
                        platformX--;
                    }
                    else
                    {
                        platformY++;
                    }
                }
            }
            else //new chain
            {
                platformX = (platformX + platformW) + 2 + random.nextInt(Math.max((int) (levelWidth * .26667), 50));
                if (getTerrainHeight(platformX) != h)
                {
                    platformY = getTerrainHeight(platformX) - (3 + random.nextInt(2));
                }
                else
                {
                    int lHeight = 0, rHeight = 0;
                    for (int x = platformX; getTerrainHeight(x) == h; x++)
                    {
                        if (x > levelWidth - 1)
                        {
                            break;
                        }
                        rHeight = getTerrainHeight(x + 1);
                    }
                    for (int x = platformX; getTerrainHeight(x) == h; x--)
                    {
                        if (x <= 0)
                        {
                            break;
                        }
                        lHeight = getTerrainHeight(x - 1);
                    }
//                    System.out.println(Math.min(lHeight, rHeight));
                    platformY = Math.min(lHeight, rHeight) - (3 + random.nextInt(2));
                }
            }
            if (platformY > getTerrainHeight(platformX))
            {
                platformY = getTerrainHeight(platformX) - (3 + random.nextInt(2));
            }
            if (platformY < minY)
            {
                platformY = minY;
            }
            platformChain--;
        }
    }

    private void addMonsters()
    {
        int firstEnemyX = -1;
        int firstEnemyY = -1;
        int maxMonsters = (int) (levelWidth * 0.06667);
        int monsters = random.nextInt(maxMonsters);
        int monsterX = minX + random.nextInt(15);
        int monsterGroup = 0;
        if (monsters == 0)
        {
            if (random.nextDouble() < .15)
            {
                monsters += maxMonsters;
            }
            else
            {
                monsters = (int) (maxMonsters * random.nextUniform());
            }
        }

        {
            while (monsters > 0 && monsterX < levelWidth)
            {
                if (monsterGroup == 0)
                {
                    monsterGroup = randomBetween(1, 5);
                }
                else
                {
                    if (getTerrainHeight(monsterX) != h
                            && getTerrainHeight(monsterX - 1) - getTerrainHeight(monsterX) < 3)
                    {
                        setTile(monsterX, getTerrainHeight(monsterX) - 1, enemy1);
                        if (firstEnemyX == firstEnemyY && firstEnemyX == -1)
                        {
                            firstEnemyX = monsterX;
                            firstEnemyY = getTerrainHeight(monsterX) - 1;
                        }
                        if (random.nextDouble() < .20)
                        {
                            setTile(monsterX, getTerrainHeight(monsterX) - 1, enemy2);
                        }
                    }
                    monsterGroup--;
                    if (monsterGroup == 0)
                    {
                        monsterX += randomBetween(15, 25);
                    }
                    else
                    {
                        monsterX += randomBetween(4, 7);
                    }
                }
            }
        }
    }

    private void createPlatformLevel()
    {
        int heightAdd = 0;
        if ((h - 1) - ((Main.HEIGHT - (minY + 3)) + minY) > 0)
        {
            heightAdd = random.nextInt((h - 1) - ((Main.HEIGHT - (minY + 3)) + minY));
        }
        int levelHeight = minY + (Main.HEIGHT - (minY + 3)) + heightAdd;
        for (int x = 0; x < w; x++)
        {
            for (int y = levelHeight; y < h; y++)
            {
                setTile(x, y, ground);
            }
        }
        int gap;
        int gapX = minX + 4 + random.nextInt(10);
        //int maxGapSpace = (int) ((levelWidth - gapX) * .367);
        //int gapSpace = random.nextInt(maxGapSpace);
        while (gapX < levelWidth - 3)
        {
            int gapRoll = random.nextInt(10) + 1;
            if (gapRoll > 9)
            {
                gap = 1;
            }
            else if (gapRoll > 7)
            {
                gap = 2;
            }
            else if (gapRoll > 4)
            {
                gap = 3;
            }
            else
            {
                gap = 4;
            }
            for (int y = levelHeight; y < h; y++)
            {
                for (int x = 0; x < gap; x++)
                {
                    setTile(gapX + x, y, empty);
                }
            }
			/*if ((gap == 4 && getTerrainHeight(gapX - 1) - getTerrainHeight(gapX + gap) == 4) || getTerrainHeight(gapX - 1) - getTerrainHeight(gapX + gap) > 4)
			{
				//System.out.println("did this thing");
				for (; getTerrainHeight(gapX - 1) - getTerrainHeight(gapX + gap) > 3;)
				{
					setTile(gapX + gap, getTerrainHeight(gapX + gap), empty);
				}
			}*/
            gapX += gap + 3; //+ random.nextInt(10);
        }
		/*int platformX = minX;
		int platformY = levelHeight;
		while (platformX < levelWidth - 5)
		{
			setTile(platformX, platformY, ground);

			platformX += randomBetween(1, 4);
		}
		for (int x = platformX; x < w; x++)
		{
			for (int y = levelHeight; y < h; y++)
			{
				setTile(x, y, ground);
			}
		}*/
    }

    private void createJumpLevel()
    {
        int heightAdd = 0;
        if ((h - 1) - ((Main.HEIGHT - (minY + 3)) + minY) > 0)
        {
            heightAdd = random.nextInt((h - 1) - ((Main.HEIGHT - (minY + 3)) + minY));
        }
        int levelHeight = minY + (Main.HEIGHT - (minY + 3)) + heightAdd;
        for (int x = 0; x < minX; x++)
        {
            for (int y = levelHeight; y < h; y++)
            {
                setTile(x, y, ground);
            }
        }
        for (int x = levelWidth; x < w; x++)
        {
            for (int y = levelHeight; y < h; y++)
            {
                setTile(x, y, ground);
            }
        }
        int jumpX = minX;
        int jumpY = levelHeight;
        int jumpDistance = 0;
        int maxHeight;
        int deltaY = 0;
        while (jumpX < levelWidth)
        {
            //Choose the the vertical direction of the next jump
            //up = -1, down = 1, same height = 0
            int dir = random.nextInt(3) - 1;

            if (jumpY <= minY && dir == -1)
            {
                dir = random.nextInt(2);
            }
            if (jumpY >= h - 2 && dir == 1)
            {
                dir = random.nextInt(2) - 1;
            }
            //if going up, the longest jumps we want are 2 high by 4 wide, 3 high by 3 wide, and 4 high by 1-2 wide
            if (dir == -1)
            {
                jumpDistance = randomBetween(1, 5);
                maxHeight = 7 - jumpDistance;
                if (maxHeight > 4) maxHeight = 4;
                deltaY = -randomBetween(0, maxHeight);

                if (jumpY + deltaY < minY)
                {
                    deltaY = jumpY - minY;
                }
            }
            //Going straight across
            else if (dir == 0)
            {
                jumpDistance = randomBetween(2, 5);
                deltaY = 0;
            }
            //Going down
            else
            {
                jumpDistance = randomBetween(2, 5);
                deltaY = randomBetween(0, 4);
                if (jumpY + deltaY >= h - 1)
                {
                    deltaY = (h - 2) - jumpY;
                }
            }

            jumpX += jumpDistance;
            jumpY += deltaY;
            for (int y = jumpY; y < h; y++)
            {
                setTile(jumpX, y, ground);
            }
        }
    }

    public void resetEnemies()
    {
        for (int i = 0; i < map.length; i++)
        {
            if (map[i] == enemy1spawn)
            {
                map[i] = enemy1;
            }
            if (map[i] == enemy2spawn)
            {
                map[i] = enemy2;
            }
        }
        load();
    }

    public int getTerrainHeight(int x)
    {
        if (x < 0) x = 0;
        if (x >= levelWidth) x = levelWidth - 1;
        for (int y = 0; y < h; y++)
        {
            if (getTile(x, y) == ground)
            {
                if (y < h - 1)
                {
                    if (getTile(x, y + 1) == ground)
                    {
                        return y;
                    }
                }
                else
                {
                    return y;
                }
            }
        }
        return h;
    }

    private void setTile(int x, int y, int tile)
    {

        map[x + y * w] = tile;
    }

    public int getTile(int x, int y)
    {
        if (x < 0 || y < 0 || x >= w)
        {
            return empty;
        }
        if (y >= h)
        {
            return map[x + (h - 1) * w];
        }
        return map[x + y * w];
    }

    public void load()
    {
        //determining how many tiles can be loaded at the current location
        getLoadingBounds();
        tiles = new int[boundsW * boundsH];
        for (int y = 0; y < boundsH; y++)
        {
            for (int x = 0; x < boundsW; x++)
            {
                if (getTile(boundsX + x, boundsY + y) == enemy1)
                {
                    Entity walker = EntityFactory.createWalker(boundsX + x, boundsY + y);
                    Entities.add(walker);
                    setTile(boundsX + x, boundsY + y, enemy1spawn);
                }
                else if (getTile(boundsX + x, boundsY + y) == enemy2)
                {
                    Entity jumper = EntityFactory.createJumper(boundsX + x, boundsY + y);
                    Entities.add(jumper);
                    setTile(boundsX + x, boundsY + y, enemy2spawn);
                }
                tiles[x + y * boundsW] = getTile((boundsX + x), (boundsY + y));
            }
        }
    }

    private void getLoadingBounds()
    {
        int minX = Registry.camera.x;
        int minY = Registry.camera.y;
        int maxX = minX + Registry.camera.width;
        int maxY = minY + Registry.camera.height;

        if (minX > w)
        {
            minX = (w - 1) - Registry.camera.width;
        }
//		if (minY > h)
//		{
//			minY = (h-1)-Registry.camera.height;
//		}
        if (minX < 0)
        {
            minX = 0;
        }
//		if (minY < 0)
//		{
//			minY = 0;
//		}
        if (maxX > w)
        {
            maxX = w;
        }
//		if (maxY > h)
//		{
//			maxY = h;
//		}
        boundsX = minX;
        boundsY = minY;
        boundsW = maxX - minX;
        boundsH = maxY - minY;
    }

    public void render()
    {
        for (int i = 0; i < boundsW * boundsH; i++)
        {
            int x = i % boundsW + boundsX;
            int y = i / boundsW + boundsY;
            if (getTile(x, y) == ground)
            {

                if (getTile(x, y - 1) <= empty)
                {
                    if (getTile(x, y + 1) == ground || y == h - 1)
                    {
                        Registry.screen.setWorldPixel(x, y, groundColor1);
                    }
                }
                if (getTile(x, y - 1) == ground)
                {
                    //0xFF1D871D
                    Registry.screen.setWorldPixel(x, y, groundColor2);
                }
                if (x == levelWidth)
                {
                    if ((y & 1) == 1)
                    {
                        Registry.screen.setWorldPixel(x, y, 0xFFFFFFFF);
                    }
                    else
                    {
                        Registry.screen.setWorldPixel(x, y, 0xFF000000);
                    }
                }
                if (y < h - 1 && getTile(x, y + 1) <= empty)
                {
                    //0xFFC1833C
                    Registry.screen.setWorldPixel(x, y, platformColor);
                }
            }
            else
            {
                //0xFF70ABD8
                Registry.screen.setWorldPixel(x, y, skyColor);
                for (int yy = y; yy > 0; yy--)
                {
                    if (getTile(x, yy) == ground)
                    {
                        if (getTile(x - 1, yy) <= empty)
                        {
                            if (getTile(x + 2, yy) <= empty)
                            {
                                //0xFF563E23
                                Registry.screen.setWorldPixel(x, y, platformColor2);
                            }
                        }
                        else if (getTile(x + 1, yy) <= empty)
                        {
                            if (getTile(x - 2, yy) <= empty)
                            {
                                Registry.screen.setWorldPixel(x, y, platformColor2);
                            }
                        }
                        else if (getTile(x - 1, yy) == ground && getTile(x + 1, yy) == ground)
                        {
                            Registry.screen.setWorldPixel(x, y, platformColor2);
                        }
                        break;
                    }
                }
            }
        }
    }
}