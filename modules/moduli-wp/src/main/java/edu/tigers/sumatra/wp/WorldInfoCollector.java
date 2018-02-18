/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.wp;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.github.g3force.configurable.ConfigRegistration;
import com.github.g3force.configurable.Configurable;

import edu.tigers.sumatra.Referee;
import edu.tigers.sumatra.bot.BotState;
import edu.tigers.sumatra.bot.RobotInfo;
import edu.tigers.sumatra.botmanager.ABotManager;
import edu.tigers.sumatra.botmanager.BotProvider;
import edu.tigers.sumatra.botmanager.NoBotProvider;
import edu.tigers.sumatra.botparams.BotParamsManager;
import edu.tigers.sumatra.botparams.BotParamsProvider;
import edu.tigers.sumatra.botparams.NoBotParamsProvider;
import edu.tigers.sumatra.cam.ACam;
import edu.tigers.sumatra.cam.ICamFrameObserver;
import edu.tigers.sumatra.cam.data.CamBall;
import edu.tigers.sumatra.cam.data.CamDetectionFrame;
import edu.tigers.sumatra.drawable.ShapeMap;
import edu.tigers.sumatra.geometry.Geometry;
import edu.tigers.sumatra.ids.BotID;
import edu.tigers.sumatra.ids.BotIDMap;
import edu.tigers.sumatra.ids.EAiType;
import edu.tigers.sumatra.ids.ETeamColor;
import edu.tigers.sumatra.ids.IBotIDMap;
import edu.tigers.sumatra.math.SumatraMath;
import edu.tigers.sumatra.math.pose.Pose;
import edu.tigers.sumatra.math.vector.Vector2;
import edu.tigers.sumatra.math.vector.Vector3;
import edu.tigers.sumatra.model.SumatraModel;
import edu.tigers.sumatra.persistence.RecordManager;
import edu.tigers.sumatra.referee.AReferee;
import edu.tigers.sumatra.referee.IRefereeObserver;
import edu.tigers.sumatra.referee.data.GameState;
import edu.tigers.sumatra.referee.data.RefereeMsg;
import edu.tigers.sumatra.vision.AVisionFilter;
import edu.tigers.sumatra.vision.IVisionFilterObserver;
import edu.tigers.sumatra.vision.data.FilteredVisionBall;
import edu.tigers.sumatra.vision.data.FilteredVisionBot;
import edu.tigers.sumatra.vision.data.FilteredVisionFrame;
import edu.tigers.sumatra.vision.data.IKickEvent;
import edu.tigers.sumatra.wp.ball.trajectory.BallFactory;
import edu.tigers.sumatra.wp.data.BallKickFitState;
import edu.tigers.sumatra.wp.data.ExtendedCamDetectionFrame;
import edu.tigers.sumatra.wp.data.ITrackedBall;
import edu.tigers.sumatra.wp.data.ITrackedBot;
import edu.tigers.sumatra.wp.data.SimpleWorldFrame;
import edu.tigers.sumatra.wp.data.TrackedBall;
import edu.tigers.sumatra.wp.data.TrackedBot;
import edu.tigers.sumatra.wp.data.WorldFrameWrapper;
import edu.tigers.sumatra.wp.util.BallContactCalculator;
import edu.tigers.sumatra.wp.util.BotInterchangeCalculator;
import edu.tigers.sumatra.wp.util.BotStateFromTrajectoryCalculator;
import edu.tigers.sumatra.wp.util.CurrentBallDetector;
import edu.tigers.sumatra.wp.util.GameStateCalculator;
import edu.tigers.sumatra.wp.util.RobotInfoProvider;


/**
 * This module collects some AI-independent world information, like filtered camera data and vision data
 *
 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
 */
public class WorldInfoCollector extends AWorldPredictor
		implements IRefereeObserver, IVisionFilterObserver, ICamFrameObserver
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(WorldInfoCollector.class.getName());
	
	private final GameStateCalculator gameStateCalculator = new GameStateCalculator();
	private final WorldFrameVisualization worldFrameVisualization = new WorldFrameVisualization();
	private final BallContactCalculator ballContactCalculator = new BallContactCalculator();
	private final CurrentBallDetector currentBallDetector = new CurrentBallDetector();
	private final BotInterchangeCalculator botInterchangeCalculator = new BotInterchangeCalculator();
	@Configurable(comment = "Use robot feedback for position and velocity.", defValue = "true")
	private static boolean preferRobotFeedback = true;
	private final BotStateFromTrajectoryCalculator botStateFromTrajectoryCalculator = new BotStateFromTrajectoryCalculator();
	
	private AVisionFilter visionFilter;
	private RobotInfoProvider robotInfoProvider;
	
	private long lastWFTimestamp = 0;
	private RefereeMsg latestRefereeMsg = new RefereeMsg();
	private IKickEvent lastKickEvent = null;
	@Configurable(comment = "Prefer the state of the current trajectory that the bot executes", defValue = "true")
	private static boolean preferTrajState = true;
	
	@Configurable(defValue = "100.0")
	private static double maxPositionDiff = 100.0;
	
	@Configurable(comment = "Add a faked ball. Set pos,vel,acc in code.", defValue = "false")
	private static boolean fakeBall = false;
	private final BerkeleyAutoPauseHook berkeleyAutoPauseHook = new BerkeleyAutoPauseHook();
	
	static
	{
		ConfigRegistration.registerClass("wp", WorldInfoCollector.class);
	}
	
	
	private Map<BotID, BotState> getFilteredBotStates(final Collection<FilteredVisionBot> visionBots)
	{
		return visionBots.stream()
				.collect(Collectors.toMap(
						FilteredVisionBot::getBotID,
						FilteredVisionBot::toBotState));
	}
	
	
	private Map<BotID, BotState> getInternalBotStates(final Collection<RobotInfo> robotInfo)
	{
		return robotInfo.stream()
				.map(RobotInfo::getInternalState)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toMap(
						BotState::getBotID,
						Function.identity()));
	}
	
	
	private BotState select(BotState filterState, BotState internalState)
	{
		if (filterState == null)
		{
			return internalState;
		} else if (internalState == null)
		{
			return filterState;
		} else if (preferRobotFeedback)
		{
			return internalState;
		}
		return filterState;
	}
	
	
	private boolean hasBallContact(final RobotInfo robotInfo, final Pose pose)
	{
		return ballContactCalculator.ballContact(robotInfo, pose, robotInfo.getCenter2DribblerDist());
	}
	
	
	private TrackedBot createTrackedBot(RobotInfo robotInfo, BotState filterState, BotState internalState)
	{
		BotState currentBotState = select(filterState, internalState);
		Optional<BotState> trajState = botStateFromTrajectoryCalculator.getState(robotInfo);
		boolean similar = trajState.map(s -> isSimilar(s, currentBotState)).orElse(false);
		BotState botState = similar && preferTrajState
				? botStateFromTrajectoryCalculator.getLatestState(robotInfo.getBotId()).orElse(currentBotState)
				: currentBotState;
		if (trajState.isPresent() && !similar)
		{
			botStateFromTrajectoryCalculator.reset(robotInfo.getBotId());
		}
		return TrackedBot.newBuilder()
				.withBotId(botState.getBotID())
				.withTimestamp(lastWFTimestamp)
				.withState(botState)
				.withFilteredState(filterState)
				.withBufferedTrajState(trajState.orElse(null))
				.withBotInfo(robotInfo)
				.withBallContact(hasBallContact(robotInfo, botState.getPose()))
				.build();
	}
	
	
	private boolean isSimilar(final BotState trajState, final BotState currentState)
	{
		return trajState.getPos().distanceToSqr(currentState.getPos()) < SumatraMath.square(maxPositionDiff);
	}
	
	
	private IBotIDMap<ITrackedBot> collectTrackedBots(
			final List<FilteredVisionBot> filteredVisionBots,
			final Collection<RobotInfo> robotInfo)
	{
		Map<BotID, BotState> filteredBotStates = getFilteredBotStates(filteredVisionBots);
		Map<BotID, BotState> internalBotStates = getInternalBotStates(robotInfo);
		
		Map<BotID, ITrackedBot> trackedBots = robotInfo.stream()
				.map(r -> createTrackedBot(r, filteredBotStates.get(r.getBotId()), internalBotStates.get(r.getBotId())))
				.collect(Collectors.toMap(ITrackedBot::getBotId, Function.identity()));
		return new BotIDMap<>(trackedBots);
	}
	
	
	private Map<BotID, RobotInfo> collectRobotInfo(final List<FilteredVisionBot> filteredVisionBots)
	{
		Set<BotID> allRelevantBots = new HashSet<>(robotInfoProvider.getConnectedBotIds());
		filteredVisionBots.stream().map(FilteredVisionBot::getBotID).forEach(allRelevantBots::add);
		
		return allRelevantBots.stream()
				.map(robotInfoProvider::getRobotInfo)
				.collect(Collectors.toMap(
						RobotInfo::getBotId,
						Function.identity()));
	}
	
	
	private IKickEvent getKickEvent(final FilteredVisionFrame filteredVisionFrame)
	{
		if (filteredVisionFrame.getKickEvent().isPresent())
		{
			lastKickEvent = filteredVisionFrame.getKickEvent().get();
		} else if (filteredVisionFrame.getBall().getVel().getLength2() < 0.1)
		{
			lastKickEvent = null;
		}
		return lastKickEvent;
	}
	
	
	private void visualize(final WorldFrameWrapper wfw)
	{
		ShapeMap shapeMap = new ShapeMap();
		worldFrameVisualization.process(wfw, shapeMap);
		notifyNewShapeMap(lastWFTimestamp, shapeMap, "WP");
	}
	
	
	private BallKickFitState getKickFitState(final FilteredVisionFrame filteredVisionFrame)
	{
		if (filteredVisionFrame.getKickFitState().isPresent())
		{
			return new BallKickFitState(filteredVisionFrame.getKickFitState().get(), filteredVisionFrame.getTimestamp());
		}
		
		return null;
	}
	
	
	private void processFilteredVisionFrame(final FilteredVisionFrame filteredVisionFrame)
	{
		lastWFTimestamp = filteredVisionFrame.getTimestamp();
		robotInfoProvider.setLastWFTimestamp(lastWFTimestamp);
		
		ITrackedBall ball = getTrackedBall(filteredVisionFrame);
		ballContactCalculator.setTrackedBall(ball);
		
		Map<BotID, RobotInfo> robotInfo = collectRobotInfo(filteredVisionFrame.getBots());
		visionFilter.setRobotInfoMap(robotInfo);
		
		IBotIDMap<ITrackedBot> bots = collectTrackedBots(filteredVisionFrame.getBots(), robotInfo.values());
		
		IKickEvent kickEvent = getKickEvent(filteredVisionFrame);
		BallKickFitState kickFitState = getKickFitState(filteredVisionFrame);
		Set<BotID> toInterchange = botInterchangeCalculator.computeBotsToInterchange(bots, latestRefereeMsg);
		
		long frameNumber = filteredVisionFrame.getId();
		SimpleWorldFrame swf = new SimpleWorldFrame(bots, ball, kickEvent, kickFitState, frameNumber, lastWFTimestamp);
		
		GameState gameState = gameStateCalculator.getNextGameState(latestRefereeMsg, ball.getPos());
		
		WorldFrameWrapper wfw = new WorldFrameWrapper(swf, latestRefereeMsg, gameState, toInterchange);
		consumers.forEach(c -> c.onNewWorldFrame(wfw));
		observers.forEach(c -> c.onNewWorldFrame(wfw));
		
		visualize(wfw);
	}
	
	
	private ITrackedBall getTrackedBall(final FilteredVisionFrame filteredVisionFrame)
	{
		FilteredVisionBall filteredVisionBall = fakeBall ? fakeBall() : filteredVisionFrame.getBall();
		return TrackedBall.fromFilteredVisionBall(lastWFTimestamp, filteredVisionBall);
	}
	
	
	private FilteredVisionBall fakeBall()
	{
		return FilteredVisionBall.Builder.create()
				.withPos(Vector3.fromXYZ(1500, 500, 0))
				.withVel(Vector3.from2d(Vector2.fromXY(-1500, -900).scaleToNew(2000), 0))
				.withAcc(Vector3.zero())
				.withIsChipped(false)
				.withvSwitch(0)
				.withLastVisibleTimestamp(lastWFTimestamp)
				.build();
	}
	
	
	@Override
	public final void initModule()
	{
		// nothing to do
	}
	
	
	@Override
	public final void deinitModule()
	{
		// nothing to do
	}
	
	
	@Override
	public final void startModule()
	{
		Geometry.refresh();
		clearObservers();
		
		registerToVisionFilterModule();
		registerToRefereeModule();
		registerToCamModule();
		registerToRecordManagerModule();
		
		initRobotInfoProvider();
		
		BallFactory.updateConfigs();
		ShapeMap.setPersistDebugShapes(!SumatraModel.getInstance().isProductive());
	}
	
	
	@Override
	public final void stopModule()
	{
		unregisterFromVisionFilterModule();
		unregisterFromRefereeModule();
		unregisterFromCamModule();
		unregisterToRecordManagerModule();
	}
	
	
	private void registerToRecordManagerModule()
	{
		if (SumatraModel.getInstance().isModuleLoaded(RecordManager.class))
		{
			RecordManager recordManager = SumatraModel.getInstance().getModule(RecordManager.class);
			recordManager.addHook(berkeleyAutoPauseHook);
		}
	}
	
	
	private void unregisterToRecordManagerModule()
	{
		if (SumatraModel.getInstance().isModuleLoaded(RecordManager.class))
		{
			RecordManager recordManager = SumatraModel.getInstance().getModule(RecordManager.class);
			recordManager.removeHook(berkeleyAutoPauseHook);
		}
	}
	
	
	private void registerToVisionFilterModule()
	{
		visionFilter = SumatraModel.getInstance().getModule(AVisionFilter.class);
		visionFilter.addObserver(this);
	}
	
	
	private void unregisterFromVisionFilterModule()
	{
		if (visionFilter != null)
		{
			visionFilter.removeObserver(this);
			visionFilter = null;
		}
	}
	
	
	private void registerToRefereeModule()
	{
		AReferee referee = SumatraModel.getInstance().getModule(AReferee.class);
		referee.addObserver(this);
	}
	
	
	private void unregisterFromRefereeModule()
	{
		AReferee referee = SumatraModel.getInstance().getModule(AReferee.class);
		referee.removeObserver(this);
	}
	
	
	private void registerToCamModule()
	{
		if (!"SUMATRA".equals(SumatraModel.getInstance().getEnvironment()))
		{
			ACam cam = SumatraModel.getInstance().getModule(ACam.class);
			cam.addObserver(this);
		}
	}
	
	
	private void unregisterFromCamModule()
	{
		if (!"SUMATRA".equals(SumatraModel.getInstance().getEnvironment()))
		{
			ACam cam = SumatraModel.getInstance().getModule(ACam.class);
			cam.removeObserver(this);
		}
	}
	
	
	private BotProvider getBotProvider()
	{
		if (SumatraModel.getInstance().isModuleLoaded(ABotManager.class))
		{
			return SumatraModel.getInstance().getModule(ABotManager.class);
		}
		return new NoBotProvider();
	}
	
	
	private BotParamsProvider getBotParamsProvider()
	{
		if (SumatraModel.getInstance().isModuleLoaded(BotParamsManager.class))
		{
			return SumatraModel.getInstance().getModule(BotParamsManager.class);
		}
		return new NoBotParamsProvider();
	}
	
	
	private void initRobotInfoProvider()
	{
		robotInfoProvider = new RobotInfoProvider(getBotProvider(), getBotParamsProvider());
	}
	
	
	private void clearObservers()
	{
		if (!observers.isEmpty())
		{
			log.warn("There were observers left: " + observers);
			observers.clear();
		}
		if (!consumers.isEmpty())
		{
			log.warn("There were consumers left: " + consumers);
			consumers.clear();
		}
	}
	
	
	@Override
	public void onNewFilteredVisionFrame(final FilteredVisionFrame filteredVisionFrame)
	{
		processFilteredVisionFrame(filteredVisionFrame);
		notifyNewShapeMap(lastWFTimestamp, filteredVisionFrame.getShapeMap(), "VISION_FILTER");
	}
	
	
	@Override
	public void onNewRefereeMsg(final Referee.SSL_Referee refMsg)
	{
		long ts = lastWFTimestamp;
		if (refMsg.getCommandCounter() == latestRefereeMsg.getCommandCounter())
		{
			ts = latestRefereeMsg.getFrameTimestamp();
		}
		updateTeamOnPositiveHalf(refMsg);
		latestRefereeMsg = new RefereeMsg(ts, refMsg);
	}
	
	
	private void updateTeamOnPositiveHalf(final Referee.SSL_Referee refMsg)
	{
		if (refMsg.hasBlueTeamOnPositiveHalf())
		{
			Geometry.setNegativeHalfTeam(refMsg.getBlueTeamOnPositiveHalf() ? ETeamColor.YELLOW : ETeamColor.BLUE);
		}
	}
	
	
	@Override
	public void onNewCamDetectionFrame(final CamDetectionFrame camDetectionFrame)
	{
		CamBall ball = currentBallDetector.findCurrentBall(camDetectionFrame.getBalls());
		ExtendedCamDetectionFrame eFrame = new ExtendedCamDetectionFrame(camDetectionFrame, ball);
		observers.forEach(o -> o.onNewCamDetectionFrame(eFrame));
	}
	
	
	@Override
	public void onClearCamFrame()
	{
		observers.forEach(IWorldFrameObserver::onClearCamDetectionFrame);
		lastWFTimestamp = 0;
		currentBallDetector.reset();
		gameStateCalculator.reset();
		worldFrameVisualization.reset();
		latestRefereeMsg = new RefereeMsg();
	}
	
	
	@Override
	public void updateBot2AiAssignment(final BotID botID, final EAiType aiTeam)
	{
		robotInfoProvider.updateBot2AiAssignment(botID, aiTeam);
	}
	
	
	@Override
	public Map<BotID, EAiType> getBotToAiMap()
	{
		return robotInfoProvider.getBotToAiMap();
	}
}
