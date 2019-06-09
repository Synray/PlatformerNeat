package platfomer.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import platfomer.Main;

public class Input implements KeyListener
{
    private boolean focused = true;
    private Main game;

    public static class Key
    {
        public boolean pressed;
        public boolean held;
        public boolean released;
    }

    private boolean[] newState = new boolean[KeyEvent.KEY_LAST];
    private boolean[] oldState = new boolean[KeyEvent.KEY_LAST];
    private Key[] keys = new Key[KeyEvent.KEY_LAST];

    public Input(Main game)
    {
        this.game = game;
        this.game.addKeyListener(this);
        for (int i = 0; i < keys.length; i++)
        {
            keys[i] = new Key();
        }
    }

    public void tick()
    {
        for (int i = 0; i < KeyEvent.KEY_LAST; ++i)
        {
            keys[i].pressed = keys[i].released = false;

            if (newState[i] && !oldState[i])
            {
                keys[i].pressed = true;
                keys[i].held = true;
            }
            if (!newState[i] && oldState[i])
            {
                keys[i].released = true;
                keys[i].held = false;
            }
            oldState[i] = newState[i];
        }

        if (!Main.instance.hasFocus())
        {
            if (focused)
            {
                for (int i = 0; i < KeyEvent.KEY_LAST; i++)
                {
                    keys[i].pressed = keys[i].held = keys[i].released = false;
                    newState[i] = oldState[i] = false;
                }
                focused = false;
            }
        }
        else
        {
            focused = true;
        }
    }

    public void keyPressed(KeyEvent e)
    {
        if (e.getKeyCode() < KeyEvent.KEY_LAST)
        {
            newState[e.getKeyCode()] = true;
        }
    }

    public void keyReleased(KeyEvent e)
    {
        if (e.getKeyCode() < KeyEvent.KEY_LAST)
        {
            newState[e.getKeyCode()] = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
        {
            Main.paused ^= true;
            if (!Main.paused)
            {
                game.wake();
            }
        }
    }

    public void keyTyped(KeyEvent e)
    {

    }

    public Key key(int keyCode)
    {
        if (keyCode < KeyEvent.KEY_LAST)
            return keys[keyCode];

        return keys[KeyEvent.VK_UNDEFINED];
    }

    public boolean control()
    {
        return keys[KeyEvent.VK_CONTROL].held;
    }

    public boolean shift()
    {
        return keys[KeyEvent.VK_SHIFT].held;
    }

    public boolean alt()
    {
        return keys[KeyEvent.VK_ALT].held;
    }
}