package platfomer;

import platfomer.sim.Simulation;
import platfomer.gfx.Camera;
import platfomer.gfx.GameScreen;
import platfomer.gfx.Text;
import platfomer.input.Input;
import platfomer.registry.Registry;
import platfomer.util.NEATUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Main extends Canvas implements Runnable
{
	private static final long serialVersionUID = 6258674237846496998L;
    public static volatile boolean paused = false;
    private static boolean running = true;
	public static Main instance;

	public static final int WIDTH = 32;
	public static final int HEIGHT = 22;
	private static final int SCALE = 20;

	private int tickCount = 0;

	private Simulation sim;

	private BufferedImage image;
	private int[] pixels;

	private JFrame frame;

	public Main()
	{
		super();
		image = new BufferedImage(WIDTH - 2, HEIGHT - 2, BufferedImage.TYPE_INT_RGB);
		pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		setFocusTraversalKeysEnabled(false);

		this.setMinimumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
		this.setMaximumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
		this.setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));

		frame = new JFrame("" + NEATUtil.weightMutPower);
		frame.setResizable(false);
		frame.setLayout(new BorderLayout());
		frame.add(this, BorderLayout.CENTER);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		this.requestFocus();
	}

	public synchronized void start()
	{
		running = true;
		new Thread(this).start();
	}

	public synchronized void stop()
	{
		running = false;
		WindowEvent wev = new WindowEvent(frame, WindowEvent.WINDOW_CLOSING);
		Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
	}

	private void init()
	{
		Registry.screen = new GameScreen(WIDTH - 2, HEIGHT - 2, pixels);
		Registry.camera = new Camera(0, 0, WIDTH - 2, HEIGHT - 2);
		Registry.input = new Input(this);
		EventQueue.invokeLater(() ->
		{
			try
			{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex)
			{
				ex.printStackTrace();
			}
		});

		sim = new Simulation();
		sim.init();
	}

	public void run()
	{
		init();

		long lastTime = System.nanoTime();
		int nsPerTick = 1000000000 / 60;
		int ticks = 0;
		int frames = 0;
		int lastNotify = (int) (lastTime / 1000000000);

		while (running)
		{
			long now = System.nanoTime();
			if (!paused)
			{
				if (now - lastTime > nsPerTick)
				{
					tick();
					ticks++;
					lastTime += nsPerTick;
				}

				if (now - lastTime > nsPerTick)
				{
					lastTime = now - nsPerTick;
				}

				render();
				frames++;

				int thisSecond = (int) (now / 1000000000);
				if (thisSecond > lastNotify)
				{
//					System.out.println(ticks + " ticks, " + frames + " frames");
					ticks = 0;
					frames = 0;
					lastNotify = thisSecond;
				}

				while (now - lastTime < nsPerTick)
				{
					Thread.yield();
					now = System.nanoTime();
				}
			}
			else
			{
				synchronized (this)
				{
					while (paused)
					{
						try
						{
							this.wait();
						}
						catch (InterruptedException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	public void wake()
	{
		synchronized (this)
		{
			this.notifyAll();
		}
	}

	public void tick()
	{
		tickCount++;
		Registry.input.tick();
		sim.tick();
	}

	public void render()
	{
		BufferStrategy bs = getBufferStrategy();
		if (bs == null)
		{
			createBufferStrategy(3);
			return;
		}
		sim.render();
		Graphics g = bs.getDrawGraphics();

		g.setColor(new Color(0x131631));
		g.fillRect(0, 0, getWidth(), getHeight());
		int ww = (WIDTH - 2) * SCALE;
		int hh = (HEIGHT - 2) * SCALE;
		int xo = (getWidth() - ww) / 2;
		int yo = (getHeight() - hh) / 2;
		g.drawImage(image, xo, yo, ww, hh, null);
		sim.drawGenome(g);
		Text.drawText((Graphics2D) g);
		g.dispose();
		bs.show();
	}

	public static void main(String[] args)
	{
		instance = new Main();
		instance.start();
	}
}