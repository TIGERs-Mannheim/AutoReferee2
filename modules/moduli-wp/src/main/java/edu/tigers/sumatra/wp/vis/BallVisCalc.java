/*
 * *********************************************************
 * Copyright (c) 2009 - 2015, DHBW Mannheim - Tigers Mannheim
 * Project: TIGERS - Sumatra
 * Date: Jul 24, 2015
 * Author(s): Nicolai Ommer <nicolai.ommer@gmail.com>
 * *********************************************************
 */
package edu.tigers.sumatra.wp.vis;

import java.awt.Color;
import java.util.List;

import edu.tigers.sumatra.drawable.DrawableCircle;
import edu.tigers.sumatra.drawable.IDrawableShape;
import edu.tigers.sumatra.math.IVector2;
import edu.tigers.sumatra.shapes.circle.Circle;
import edu.tigers.sumatra.wp.data.Geometry;
import edu.tigers.sumatra.wp.data.TrackedBall;
import edu.tigers.sumatra.wp.data.WorldFrameWrapper;


/**
 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
 */
public class BallVisCalc implements IVisCalc
{
	private static final double BALL_FLY_TOL = 15;
	
	
	@Override
	public void process(final WorldFrameWrapper wfw)
	{
		List<IDrawableShape> shapes = wfw.getShapeMap().get(EWpShapesLayer.BALL);
		
		TrackedBall ball = wfw.getSimpleWorldFrame().getBall();
		
		Color color = ball.getPos3().z() > BALL_FLY_TOL ? Color.red : Color.ORANGE;
		DrawableCircle point = new DrawableCircle(ball.getPos(), Geometry.getBallRadius(), color);
		point.setFill(true);
		shapes.add(point);
		
		DrawableCircle circle1 = new DrawableCircle(new Circle(ball.getPos(), 120), Color.YELLOW);
		shapes.add(circle1);
		DrawableCircle circle2 = new DrawableCircle(new Circle(ball.getPos(), 105), Color.YELLOW);
		shapes.add(circle2);
		
		DrawableCircle dCircle = new DrawableCircle(ball.getPosByVel(0), Geometry.getBallRadius() + 5,
				Color.red);
		shapes.add(dCircle);
		
		IVector2 ballPos = ball.getPosByVel(0);
		DrawableCircle dCircleStop = new DrawableCircle(ballPos, Geometry.getBallRadius() + 5, Color.red);
		shapes.add(dCircleStop);
	}
	
}
