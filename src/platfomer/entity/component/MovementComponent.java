package platfomer.entity.component;

import platfomer.util.Vector2d;

public class MovementComponent extends Component
{
	public static Vector2d uVelocity = new Vector2d(0, 0);
	public static Vector2d uAcceleration = new Vector2d();
	public static Vector2d uDrag = new Vector2d(1, 0);
	//public static Vector2d uWalkSpeed = new Vector2d(.8, 0);//new Vector2d(.6, 3, .15);
	public static Vector2d uWalkSpeed = new Vector2d(.8, 0);//new Vector2d(.6, 3, .15);
	//public static double uSpeed = .15;
	public static double uSpeed = .01;
	public boolean bouncing = false;
	public boolean colliding = true;

	public boolean gravity = true;
	public double gforce = 0.0384225;

	/**
	 * Entity's default velocity.
	 */
	public Vector2d velocityD;
	/**
	 * Entity's default acceleration.
	 */
	public Vector2d accelerationD;
	/**
	 * Entity's default drag.
	 */
	public Vector2d dragD;
	/**
	 * Entity's default max walking speed.
	 */
	public Vector2d walkSpeedD;
	/**
	 * Entity's default speed.
	 */
	public double speedD;
	
	public Vector2d velocity;
	public Vector2d acceleration;
	public Vector2d drag;
	public Vector2d maxVelocity;
	public Vector2d walkSpeed;
	
	public Vector2d maxVelocityD;
	
	public double speed = 1;
	public double jumpPower = .5;
	public static Vector2d MAXVELOCITY = new Vector2d(.20, 1);
	public boolean onGround;

	public boolean stompable;

	public MovementComponent()
	{
		super();
		this.velocity = new Vector2d(uVelocity);
		this.acceleration = new Vector2d(uVelocity);
		this.drag = new Vector2d(uDrag);
		this.maxVelocity = new Vector2d(MAXVELOCITY);
		this.walkSpeed = new Vector2d(uWalkSpeed);
		this.maxVelocityD = new Vector2d(MAXVELOCITY);
	}

	public MovementComponent(Vector2d velocity, Vector2d acceleration, Vector2d drag)
	{
		super();
		this.velocity = new Vector2d(velocity);
		this.acceleration = new Vector2d(acceleration);
		this.drag = new Vector2d(drag);
		this.maxVelocity = new Vector2d(MAXVELOCITY);
		this.maxVelocityD = new Vector2d(MAXVELOCITY);
		this.walkSpeed = new Vector2d(uWalkSpeed);
	}

	public MovementComponent(Vector2d velocity, Vector2d acceleration, Vector2d drag, Vector2d maxVelocity, Vector2d walkSpeed)
	{
		super();
		this.velocity = new Vector2d(velocity);
		this.acceleration = new Vector2d(acceleration);
		this.drag = new Vector2d(drag);
		this.maxVelocity = new Vector2d(maxVelocity);
		this.walkSpeed = new Vector2d(walkSpeed);
		
		this.velocityD = new Vector2d(velocity);
		this.accelerationD = new Vector2d(acceleration);
		this.dragD = new Vector2d(drag);
		this.walkSpeedD = new Vector2d(walkSpeed);
		this.maxVelocityD = new Vector2d(maxVelocity);
	}
	
	public MovementComponent setMaxVelocity(Vector2d max, boolean setDefault)
	{
		this.maxVelocity = new Vector2d(max);
		if(setDefault)
		{
			this.maxVelocityD = new Vector2d(max);
		}
		return this;
	}
	public MovementComponent setMaxVelocity(Vector2d max)
	{
		this.maxVelocity = new Vector2d(max);
		return this;
	}

}