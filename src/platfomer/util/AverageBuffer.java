package platfomer.util;

public class AverageBuffer
{
    private double[] buffer;
    private int head, tail;
    private double total;

    public AverageBuffer(int capacity)
    {
        buffer = new double[capacity];
        head = tail = -1;
    }

    public int size()
    {
        if(head == -1)
        {
            return 0;
        }


        if(head > tail)
        {
            return (head - tail) + 1;
        }

        if(head < tail)
        {
            return (buffer.length - tail) + head + 1;
        }

        //head == tail, only one element
        return 1;
    }

    public double getTotal()
    {
        return total;
    }

    public double getMean()
    {
        if (head == -1)
        {
            return 0.0;
        }

        return total / size();
    }

    public double trend()
    {
        if (head == -1)
        {
            return 0.0;
        }

        if(buffer.length < 2) return buffer[head];

        double delta = 0.0;
        double prev = buffer[tail];
        for(int i = 1, index = (tail + 1) % buffer.length; i < buffer.length; ++i, index = ++index % buffer.length)
        {
            delta += buffer[index] - prev;
            prev = buffer[index];
        }

        return delta / buffer.length;
    }

    public void clear()
    {
        head = tail = -1;
        total = 0.0;
    }

    public void enqueue(double item)
    {
        if (head == -1)
        {
            head = tail = 0;
            buffer[0] = item;
            total += item;
            return;
        }

        if (++head == buffer.length)
        {
            head = 0;
        }

        if (head == tail)
        {
            total -= buffer[head];
            if(++tail == buffer.length)
            {
                tail = 0;
            }
        }

        buffer[head] = item;
        total += item;
    }

    public double dequeue()
    {
        if(tail == -1)
        {   // buffer is currently empty.
            throw new IllegalStateException("buffer is empty.");
        }

        double d = buffer[tail];
        total -= d;

        if(tail == head)
        {   // The buffer is now empty.
            head = tail = -1;
            return d;
        }

        if(++tail == buffer.length)
        {   // Wrap around.
            tail = 0;
        }

        return d;
    }

    public double pop()
    {
        if(tail == -1)
        {   // buffer is currently empty.
            throw new IllegalStateException("buffer is empty.");
        }

        double d = buffer[head];
        total -= d;

        if(tail == head)
        {   // The buffer is now empty.
            head = tail = -1;
            return d;
        }

        if(--head == -1)
        {   // Wrap around.
            head = buffer.length - 1;
        }

        return d;
    }
}
