/*
 * *********************************************************
 * Copyright (c) 2009 - 2015, DHBW Mannheim - Tigers Mannheim
 * Project: TIGERS - Sumatra
 * Date: May 22, 2015
 * Author(s): Nicolai Ommer <nicolai.ommer@gmail.com>
 * *********************************************************
 */
package edu.tigers.sumatra.wp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import Jama.Matrix;
import edu.tigers.sumatra.cam.data.CamBall;
import edu.tigers.sumatra.cam.data.CamRobot;
import edu.tigers.sumatra.export.IJsonString;
import edu.tigers.sumatra.export.INumberListable;
import edu.tigers.sumatra.ids.BotID;
import edu.tigers.sumatra.ids.ETeamColor;
import edu.tigers.sumatra.math.AVector;
import edu.tigers.sumatra.math.IVector2;
import edu.tigers.sumatra.math.IVector3;
import edu.tigers.sumatra.math.Vector2;
import edu.tigers.sumatra.math.Vector3;
import edu.tigers.sumatra.wp.data.ITrackedBot;
import edu.tigers.sumatra.wp.data.TrackedBall;
import edu.tigers.sumatra.wp.data.TrackedBot;


/**
 * Data container for all relevant data for export
 * 
 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
 */
public class ExportDataContainer
{
	@SuppressWarnings("unused")
	private static final Logger						log						= Logger.getLogger(ExportDataContainer.class
																									.getName());
																									
	private long											timestampRecorded		= 0;
	private final List<CamBall>						balls						= new ArrayList<>();
	private final List<CamRobot>						rawBots					= new ArrayList<>();
	private final List<WpBot>							wpBots					= new ArrayList<>();
	private final List<SkillBot>						skillBots				= new ArrayList<>();
	private CamBall										curBall					= new CamBall();
	private WpBall											wpBall					= new WpBall();
	private final Map<String, INumberListable>	customNumberListable	= new HashMap<>();
																							
																							
	/**
	 * @param tBot
	 * @param frameId
	 * @param timestamp
	 * @return
	 */
	public static WpBot trackedBot2WpBot(final ITrackedBot tBot, final long frameId, final long timestamp)
	{
		IVector3 pos = new Vector3(tBot.getPos(), tBot.getAngle());
		IVector3 vel = new Vector3(tBot.getVel(), tBot.getaVel());
		return new WpBot(pos, vel, tBot.getBotId().getNumber(), tBot.getBotId().getTeamColor(), frameId, timestamp);
	}
	
	
	/**
	 * @param m
	 * @return
	 */
	public static List<Number> toNumberList(final Matrix m)
	{
		double[] arr = m.getRowPackedCopy();
		List<Number> list = new ArrayList<>(arr.length);
		for (double d : arr)
		{
			list.add(d);
		}
		return list;
	}
	
	
	/**
	 * @param folder
	 * @param filename
	 * @return
	 */
	public static List<CamBall> readRawBall(final String folder, final String filename)
	{
		try
		{
			return Files
					.lines(Paths.get(folder + "/" + filename + ".csv"))
					.filter(line -> !line.startsWith("#"))
					.map(line -> line.split(","))
					.map(arr -> Arrays.asList(arr).stream()
							.map(s -> Double.valueOf(s))
							.collect(Collectors.toList()))
					.map(l -> CamBall.fromNumberList(l))
					.collect(Collectors.toList());
		} catch (IOException err)
		{
			throw new IllegalStateException();
		}
	}
	
	
	/**
	 * @param folder
	 * @param filename
	 * @param color
	 * @return
	 */
	public static List<CamRobot> readRawBots(final String folder, final String filename, final ETeamColor color)
	{
		int colorNumber = color.getNumberList().get(0).intValue();
		try
		{
			return Files
					.lines(Paths.get(folder + "/" + filename + ".csv"))
					.filter(line -> !line.startsWith("#"))
					.map(line -> line.split(","))
					.filter(arr -> Integer.valueOf(arr[3]) == colorNumber)
					.map(arr -> Arrays.asList(arr).stream()
							.map(s -> Double.valueOf(s))
							.collect(Collectors.toList()))
					.map(l -> CamRobot.fromNumberList(l))
					.collect(Collectors.toList());
		} catch (IOException err)
		{
			throw new IllegalStateException();
		}
	}
	
	
	/**
	 * @return the timestampRecorded
	 */
	public final long getTimestampRecorded()
	{
		return timestampRecorded;
	}
	
	
	/**
	 * @param timestampRecorded the timestampRecorded to set
	 */
	public final void setTimestampRecorded(final long timestampRecorded)
	{
		this.timestampRecorded = timestampRecorded;
	}
	
	
	/**
	 * @return the balls
	 */
	public final List<CamBall> getBalls()
	{
		return balls;
	}
	
	
	/**
	 * @return the curBall
	 */
	public final CamBall getCurBall()
	{
		return curBall;
	}
	
	
	/**
	 * @param curBall the curBall to set
	 */
	public final void setCurBall(final CamBall curBall)
	{
		this.curBall = curBall;
	}
	
	
	/**
	 * @return the wpBall
	 */
	public final WpBall getWpBall()
	{
		return wpBall;
	}
	
	
	/**
	 * @param wpBall the wpBall to set
	 */
	public final void setWpBall(final WpBall wpBall)
	{
		this.wpBall = wpBall;
	}
	
	
	/**
	 * @return the rawBots
	 */
	public final List<CamRobot> getRawBots()
	{
		return rawBots;
	}
	
	
	/**
	 * @return the wpBots
	 */
	public final List<WpBot> getWpBots()
	{
		return wpBots;
	}
	
	
	/**
	 * @return the skillBots
	 */
	public final List<SkillBot> getSkillBots()
	{
		return skillBots;
	}
	
	
	/**
	 */
	public static class WpBall implements IJsonString, INumberListable
	{
		private IVector3	pos			= Vector3.ZERO_VECTOR;
		private IVector3	vel			= Vector3.ZERO_VECTOR;
		private IVector3	acc			= Vector3.ZERO_VECTOR;
		private long		frameId		= -1;
		private long		timestamp	= 0;
												
												
		/**
		 * 
		 */
		public WpBall()
		{
		}
		
		
		/**
		 * @param pos
		 * @param vel
		 * @param acc
		 * @param frameId
		 * @param timestamp
		 */
		public WpBall(final IVector3 pos, final IVector3 vel, final IVector3 acc, final long frameId,
				final long timestamp)
		{
			super();
			this.pos = pos;
			this.vel = vel;
			this.acc = acc;
			this.frameId = frameId;
			this.timestamp = timestamp;
		}
		
		
		/**
		 * @param list
		 * @return
		 */
		public static WpBall fromNumberList(final List<? extends Number> list)
		{
			return new WpBall(AVector.fromNumberList(list.subList(0, 3)).getXYZVector(),
					AVector.fromNumberList(list.subList(3, 6)).getXYZVector(),
					AVector.fromNumberList(list.subList(6, 9)).getXYZVector(),
					list.size() > 10 ? list.get(10).longValue() : -1,
					list.size() > 11 ? list.get(11).longValue() : -1);
		}
		
		
		/**
		 * @return
		 */
		public TrackedBall toTrackedBall()
		{
			return new TrackedBall(pos, vel, acc);
		}
		
		
		@Override
		public JSONObject toJSON()
		{
			Map<String, Object> jsonMapping = new LinkedHashMap<String, Object>();
			jsonMapping.put("pos", pos.toJSON());
			jsonMapping.put("vel", vel.toJSON());
			jsonMapping.put("frameId", frameId);
			jsonMapping.put("timestamp", timestamp);
			return new JSONObject(jsonMapping);
		}
		
		
		@Override
		public List<Number> getNumberList()
		{
			List<Number> numbers = new ArrayList<>();
			numbers.addAll(pos.getNumberList());
			numbers.addAll(vel.getNumberList());
			numbers.addAll(acc.getNumberList());
			numbers.add(frameId);
			numbers.add(timestamp);
			return numbers;
		}
		
		
		/**
		 * @return the pos
		 */
		public final IVector3 getPos()
		{
			return pos;
		}
		
		
		/**
		 * @param pos the pos to set
		 */
		public final void setPos(final IVector3 pos)
		{
			this.pos = pos;
		}
		
		
		/**
		 * @return the vel
		 */
		public final IVector3 getVel()
		{
			return vel;
		}
		
		
		/**
		 * @param vel the vel to set
		 */
		public final void setVel(final IVector3 vel)
		{
			this.vel = vel;
		}
		
		
		/**
		 * @return the acc
		 */
		public final IVector3 getAcc()
		{
			return acc;
		}
		
		
		/**
		 * @param acc the acc to set
		 */
		public final void setAcc(final IVector3 acc)
		{
			this.acc = acc;
		}
		
		
		/**
		 * @return the frameId
		 */
		public final long getFrameId()
		{
			return frameId;
		}
		
		
		/**
		 * @param frameId the frameId to set
		 */
		public final void setFrameId(final long frameId)
		{
			this.frameId = frameId;
		}
		
		
		/**
		 * @return the timestamp
		 */
		public final long getTimestamp()
		{
			return timestamp;
		}
		
		
		/**
		 * @param timestamp the timestamp to set
		 */
		public final void setTimestamp(final long timestamp)
		{
			this.timestamp = timestamp;
		}
		
		
		@Override
		public String toString()
		{
			StringBuilder builder = new StringBuilder();
			builder.append("WpBall [pos=");
			builder.append(pos);
			builder.append(", vel=");
			builder.append(vel);
			builder.append(", acc=");
			builder.append(acc);
			builder.append(", frameId=");
			builder.append(frameId);
			builder.append(", timestamp=");
			builder.append(timestamp);
			builder.append("]");
			return builder.toString();
		}
	}
	
	
	/**
	 */
	public static class WpBot implements IJsonString, INumberListable
	{
		private int					id				= -1;
		private ETeamColor		color			= ETeamColor.UNINITIALIZED;
		private IVector3			pos			= Vector3.ZERO_VECTOR;
		private IVector3			vel			= Vector3.ZERO_VECTOR;
		private final IVector3	acc			= Vector3.ZERO_VECTOR;
		private long				frameId		= -1;
		private long				timestamp	= 0;
														
														
		/**
		 * 
		 */
		public WpBot()
		{
		}
		
		
		/**
		 * @param pos
		 * @param vel
		 * @param id
		 * @param color
		 * @param frameId
		 * @param timestamp
		 */
		public WpBot(final IVector3 pos, final IVector3 vel, final int id, final ETeamColor color,
				final long frameId, final long timestamp)
		{
			super();
			this.pos = pos;
			this.vel = vel;
			this.id = id;
			this.color = color;
			this.frameId = frameId;
			this.timestamp = timestamp;
		}
		
		
		/**
		 * @param list
		 * @return
		 */
		public static WpBot fromNumberList(final List<? extends Number> list)
		{
			return new WpBot(AVector.fromNumberList(list.subList(0, 3)).getXYZVector(),
					AVector.fromNumberList(list.subList(3, 6)).getXYZVector(),
					list.get(9).intValue(),
					ETeamColor.fromNumberList(list.get(10)),
					list.size() > 11 ? list.get(11).longValue() : -1,
					list.size() > 12 ? list.get(12).longValue() : -1);
		}
		
		
		/**
		 * @return
		 */
		public ITrackedBot toTrackedBot()
		{
			BotID botId = BotID.createBotId(id, color);
			TrackedBot tBot = new TrackedBot(botId);
			tBot.setPos(pos.getXYVector());
			tBot.setVel(vel.getXYVector());
			tBot.setAngle(pos.z());
			tBot.setaVel(vel.z());
			return tBot;
		}
		
		
		@Override
		public JSONObject toJSON()
		{
			Map<String, Object> jsonMapping = new LinkedHashMap<String, Object>();
			jsonMapping.put("pos", pos.toJSON());
			jsonMapping.put("vel", vel.toJSON());
			jsonMapping.put("acc", acc.toJSON());
			jsonMapping.put("id", id);
			jsonMapping.put("color", color.name());
			jsonMapping.put("frameId", frameId);
			jsonMapping.put("timestamp", timestamp);
			return new JSONObject(jsonMapping);
		}
		
		
		@Override
		public List<Number> getNumberList()
		{
			List<Number> numbers = new ArrayList<>();
			numbers.add(id);
			numbers.addAll(color.getNumberList());
			numbers.addAll(pos.getNumberList());
			numbers.addAll(vel.getNumberList());
			numbers.addAll(acc.getNumberList());
			numbers.add(frameId);
			numbers.add(timestamp);
			return numbers;
		}
		
		
		/**
		 * @return the id
		 */
		public final int getId()
		{
			return id;
		}
		
		
		/**
		 * @param id the id to set
		 */
		public final void setId(final int id)
		{
			this.id = id;
		}
		
		
		/**
		 * @return the color
		 */
		public final ETeamColor getColor()
		{
			return color;
		}
		
		
		/**
		 * @param color the color to set
		 */
		public final void setColor(final ETeamColor color)
		{
			this.color = color;
		}
		
		
		/**
		 * @return the pos
		 */
		public final IVector3 getPos()
		{
			return pos;
		}
		
		
		/**
		 * @param pos the pos to set
		 */
		public final void setPos(final IVector3 pos)
		{
			this.pos = pos;
		}
		
		
		/**
		 * @return the vel
		 */
		public final IVector3 getVel()
		{
			return vel;
		}
		
		
		/**
		 * @param vel the vel to set
		 */
		public final void setVel(final IVector3 vel)
		{
			this.vel = vel;
		}
		
		
		/**
		 * @return the frameId
		 */
		public final long getFrameId()
		{
			return frameId;
		}
		
		
		/**
		 * @param frameId the frameId to set
		 */
		public final void setFrameId(final long frameId)
		{
			this.frameId = frameId;
		}
		
		
		/**
		 * @return the timestamp
		 */
		public final long getTimestamp()
		{
			return timestamp;
		}
		
		
		/**
		 * @param timestamp the timestamp to set
		 */
		public final void setTimestamp(final long timestamp)
		{
			this.timestamp = timestamp;
		}
		
		
		@Override
		public String toString()
		{
			StringBuilder builder = new StringBuilder();
			builder.append("WpBot [id=");
			builder.append(id);
			builder.append(", color=");
			builder.append(color);
			builder.append(", pos=");
			builder.append(pos);
			builder.append(", vel=");
			builder.append(vel);
			builder.append(", frameId=");
			builder.append(frameId);
			builder.append(", timestamp=");
			builder.append(timestamp);
			builder.append("]");
			return builder.toString();
		}
	}
	
	/**
	 */
	public static class SkillBot implements IJsonString, INumberListable
	{
		private int				id				= -1;
		private ETeamColor	color			= ETeamColor.UNINITIALIZED;
		private long			timestamp	= 0;
		private IVector2		trajVel		= Vector2.ZERO_VECTOR;
		private IVector2		trajPos		= Vector2.ZERO_VECTOR;
		private IVector3		setVel		= Vector3.ZERO_VECTOR;
		private IVector3		setPos		= Vector3.ZERO_VECTOR;
		private IVector3		localVel		= Vector3.ZERO_VECTOR;
													
													
		/**
		 * 
		 */
		public SkillBot()
		{
		}
		
		
		/**
		 * @param id
		 * @param color
		 * @param timestamp
		 */
		public SkillBot(final int id, final ETeamColor color, final long timestamp)
		{
			super();
			this.id = id;
			this.color = color;
			this.timestamp = timestamp;
		}
		
		
		/**
		 * @param list
		 * @return
		 */
		public static SkillBot fromNumberList(final List<? extends Number> list)
		{
			// SkillBot sb = new SkillBot(list.get(0).intValue(),
			// ETeamColor.fromNumberList(list.get(1)),
			// list.get(2).longValue());
			throw new NotImplementedException();
		}
		
		
		@Override
		public JSONObject toJSON()
		{
			Map<String, Object> jsonMapping = new LinkedHashMap<String, Object>();
			jsonMapping.put("trajVel", trajVel.toJSON());
			jsonMapping.put("trajPos", trajPos.toJSON());
			jsonMapping.put("setVel", setVel.toJSON());
			jsonMapping.put("setPos", setPos.toJSON());
			jsonMapping.put("localVel", localVel.toJSON());
			jsonMapping.put("id", id);
			jsonMapping.put("color", color.name());
			jsonMapping.put("timestamp", timestamp);
			return new JSONObject(jsonMapping);
		}
		
		
		@Override
		public List<Number> getNumberList()
		{
			List<Number> numbers = new ArrayList<>();
			numbers.add(id);
			numbers.addAll(color.getNumberList());
			numbers.add(timestamp);
			numbers.addAll(trajVel.getNumberList());
			numbers.addAll(trajPos.getNumberList());
			numbers.addAll(setVel.getNumberList());
			numbers.addAll(setPos.getNumberList());
			numbers.addAll(localVel.getNumberList());
			return numbers;
		}
		
		
		/**
		 * @return the id
		 */
		public int getId()
		{
			return id;
		}
		
		
		/**
		 * @param id the id to set
		 */
		public void setId(final int id)
		{
			this.id = id;
		}
		
		
		/**
		 * @return the color
		 */
		public ETeamColor getColor()
		{
			return color;
		}
		
		
		/**
		 * @param color the color to set
		 */
		public void setColor(final ETeamColor color)
		{
			this.color = color;
		}
		
		
		/**
		 * @return the timestamp
		 */
		public long getTimestamp()
		{
			return timestamp;
		}
		
		
		/**
		 * @param timestamp the timestamp to set
		 */
		public void setTimestamp(final long timestamp)
		{
			this.timestamp = timestamp;
		}
		
		
		/**
		 * @return the trajVel
		 */
		public IVector2 getTrajVel()
		{
			return trajVel;
		}
		
		
		/**
		 * @param trajVel the trajVel to set
		 */
		public void setTrajVel(final IVector2 trajVel)
		{
			this.trajVel = trajVel;
		}
		
		
		/**
		 * @return the trajPos
		 */
		public IVector2 getTrajPos()
		{
			return trajPos;
		}
		
		
		/**
		 * @param trajPos the trajPos to set
		 */
		public void setTrajPos(final IVector2 trajPos)
		{
			this.trajPos = trajPos;
		}
		
		
		/**
		 * @return the setVel
		 */
		public IVector3 getSetVel()
		{
			return setVel;
		}
		
		
		/**
		 * @param setVel the setVel to set
		 */
		public void setSetVel(final IVector3 setVel)
		{
			this.setVel = setVel;
		}
		
		
		/**
		 * @return the setPos
		 */
		public IVector3 getSetPos()
		{
			return setPos;
		}
		
		
		/**
		 * @param setPos the setPos to set
		 */
		public void setSetPos(final IVector3 setPos)
		{
			this.setPos = setPos;
		}
		
		
		/**
		 * @return the localVel
		 */
		public IVector3 getLocalVel()
		{
			return localVel;
		}
		
		
		/**
		 * @param localVel the localVel to set
		 */
		public void setLocalVel(final IVector3 localVel)
		{
			this.localVel = localVel;
		}
	}
	
	
	/**
	 * @return the customNumberListable
	 */
	public final Map<String, INumberListable> getCustomNumberListable()
	{
		return customNumberListable;
	}
}
