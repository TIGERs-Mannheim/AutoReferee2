/*
 * Copyright (c) 2009 - 2023, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.wp.data;

import com.sleepycat.persist.model.Persistent;
import edu.tigers.sumatra.ball.trajectory.IBallTrajectory;
import edu.tigers.sumatra.ids.BotID;
import edu.tigers.sumatra.math.IMirrorable;
import edu.tigers.sumatra.math.pose.Pose;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.IVector3;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;


@Value
@Persistent
@Builder
@AllArgsConstructor
public class KickedBall implements IMirrorable<KickedBall>
{
	long kickTimestamp;
	long trajectoryStartTime;

	BotID kickingBot;
	Pose kickingBotPose;

	IBallTrajectory ballTrajectory;


	@SuppressWarnings("unused") // berkeley
	private KickedBall()
	{
		kickTimestamp = 0;
		trajectoryStartTime = 0;
		kickingBot = BotID.noBot();
		kickingBotPose = Pose.zero();
		ballTrajectory = null;
	}


	public IVector3 getKickVel()
	{
		return ballTrajectory.getInitialVel();
	}


	public IVector2 getKickPos()
	{
		return ballTrajectory.getPosByTime(0).getXYVector();
	}


	/**
	 * @return the absolute kick speed
	 */
	public double getAbsoluteKickSpeed()
	{
		return getKickVel().getLength();
	}


	public long getTimestamp()
	{
		return kickTimestamp;
	}


	public IVector2 getPosition()
	{
		return getKickPos();
	}


	@Override
	public KickedBall mirrored()
	{
		return new KickedBall(
				kickTimestamp,
				trajectoryStartTime,
				kickingBot,
				kickingBotPose.mirrored(),
				ballTrajectory.mirrored()
		);
	}
}
