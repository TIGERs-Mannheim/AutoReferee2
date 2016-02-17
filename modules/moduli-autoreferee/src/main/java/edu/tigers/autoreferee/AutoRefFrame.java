/*
 * *********************************************************
 * Copyright (c) 2009 - 2015, DHBW Mannheim - Tigers Mannheim
 * Project: TIGERS - Sumatra
 * Date: Nov 12, 2015
 * Author(s): "Lukas Magel"
 * *********************************************************
 */
package edu.tigers.autoreferee;

import java.util.List;
import java.util.Optional;

import edu.tigers.autoreferee.engine.calc.BotPosition;
import edu.tigers.sumatra.math.IVector2;
import edu.tigers.sumatra.referee.RefereeMsg;
import edu.tigers.sumatra.wp.data.EGameStateNeutral;
import edu.tigers.sumatra.wp.data.SimpleWorldFrame;
import edu.tigers.sumatra.wp.data.WorldFrameWrapper;


/**
 * @author "Lukas Magel"
 */
public class AutoRefFrame implements IAutoRefFrame
{
	
	private WorldFrameWrapper			worldFrameWrapper;
	
	private IAutoRefFrame				previousFrame;
	
	private BotPosition					botLastTouchedBall;
	private BotPosition					botTouchedBall;
	
	private IVector2						ballLeftFieldPos;
	
	private List<EGameStateNeutral>	stateHistory;
	
	
	/**
	 * @param previous
	 * @param worldFrameWrapper
	 */
	public AutoRefFrame(final IAutoRefFrame previous,
			final WorldFrameWrapper worldFrameWrapper)
	{
		botLastTouchedBall = new BotPosition();
		botTouchedBall = null;
		previousFrame = previous;
		this.worldFrameWrapper = worldFrameWrapper;
	}
	
	
	@Override
	public IAutoRefFrame getPreviousFrame()
	{
		return previousFrame;
	}
	
	
	@Override
	public SimpleWorldFrame getWorldFrame()
	{
		return worldFrameWrapper.getSimpleWorldFrame();
	}
	
	
	@Override
	public BotPosition getBotLastTouchedBall()
	{
		return botLastTouchedBall;
	}
	
	
	/**
	 * @param botLastTouchedBall
	 */
	public void setBotLastTouchedBall(final BotPosition botLastTouchedBall)
	{
		this.botLastTouchedBall = botLastTouchedBall;
	}
	
	
	/**
	 * @return the botTouchedBall
	 */
	@Override
	public Optional<BotPosition> getBotTouchedBall()
	{
		return Optional.ofNullable(botTouchedBall);
	}
	
	
	/**
	 * @param botTouchedBall the botTouchedBall to set
	 */
	public void setBotTouchedBall(final BotPosition botTouchedBall)
	{
		this.botTouchedBall = botTouchedBall;
	}
	
	
	@Override
	public IVector2 getBallLeftFieldPos()
	{
		return ballLeftFieldPos;
	}
	
	
	/**
	 * @param getBallLeftFieldPos
	 */
	public void setBallLeftFieldPos(final IVector2 getBallLeftFieldPos)
	{
		ballLeftFieldPos = getBallLeftFieldPos;
	}
	
	
	@Override
	public EGameStateNeutral getGameState()
	{
		return worldFrameWrapper.getGameState();
	}
	
	
	@Override
	public void cleanUp()
	{
		previousFrame = null;
	}
	
	
	@Override
	public List<EGameStateNeutral> getStateHistory()
	{
		return stateHistory;
	}
	
	
	/**
	 * @param stateHistory the stateHistory to set
	 */
	public void setStateHistory(final List<EGameStateNeutral> stateHistory)
	{
		this.stateHistory = stateHistory;
	}
	
	
	@Override
	public long getTimestamp()
	{
		return worldFrameWrapper.getSimpleWorldFrame().getTimestamp();
	}
	
	
	@Override
	public RefereeMsg getRefereeMsg()
	{
		return worldFrameWrapper.getRefereeMsg();
	}
}
