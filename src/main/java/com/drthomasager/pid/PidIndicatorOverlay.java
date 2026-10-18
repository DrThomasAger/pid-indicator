package com.drthomasager.pid;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A diamond over your head and over your opponent's. Gold with a crown on
 * whoever has PID, grey with a cross on whoever does not or while nobody
 * knows yet, and under each the ticks until PID can be drawn again.
 */
class PidIndicatorOverlay extends Overlay
{
	private static final Color GOLD = new Color(255, 200, 0);
	private static final Color GOLD_EDGE = new Color(255, 225, 90);
	private static final Color GOLD_BACK = new Color(20, 16, 10, 210);
	private static final Color GREY = new Color(115, 115, 115, 145);
	private static final Color GREY_DARK = new Color(55, 55, 55, 135);
	private static final Color GREY_EDGE = new Color(90, 90, 90, 140);
	private static final Color GREY_CROSS = new Color(80, 80, 80, 120);
	private static final Color GREY_TEXT = new Color(200, 200, 200);
	private static final Color UNKNOWN = new Color(150, 150, 150, 180);

	private final Client client;
	private final PidIndicatorPlugin plugin;
	private final PidIndicatorConfig config;

	@Inject
	PidIndicatorOverlay(Client client, PidIndicatorPlugin plugin, PidIndicatorConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Player me = client.getLocalPlayer();
		if (me == null)
		{
			return null;
		}
		Player op = plugin.getOpponent();
		boolean fighting = op != null;
		if (!fighting && !(config.showInPvpPlaces() && plugin.inPvpPlace()))
		{
			return null;
		}
		PidTracker.Holder h = plugin.getHolder();
		boolean unknown = !fighting || h == PidTracker.Holder.UNKNOWN;
		diamond(g, me, fighting && h == PidTracker.Holder.ME, unknown, fighting ? note(h, true) : null);
		if (op != null)
		{
			diamond(g, op, h == PidTracker.Holder.THEM, unknown, note(h, false));
		}
		return null;
	}

	private String note(PidTracker.Holder h, boolean mine)
	{
		if (!config.showSwapTicks())
		{
			return null;
		}
		if (h == PidTracker.Holder.UNKNOWN)
		{
			return mine ? "PID ? hit to learn" : null;
		}
		int[] w = plugin.redrawWindow();
		int toLo = w[0] - plugin.getSinceChange();
		int toHi = w[1] - plugin.getSinceChange();
		if (toLo > 0)
		{
			return "swap " + toLo + "-" + toHi + "t";
		}
		if (toHi > 0)
		{
			return "swap now-" + toHi + "t";
		}
		return "swap any tick";
	}

	private void diamond(Graphics2D g, Player p, boolean hasPid, boolean unknown, String note)
	{
		Point pt = p.getCanvasTextLocation(g, "", p.getLogicalHeight() + 40);
		if (pt == null)
		{
			return;
		}
		drawDiamond(g, pt.getX(), pt.getY(), hasPid, unknown, note);
	}

	/** The diamond itself, centred on x, y. */
	static void drawDiamond(Graphics2D g, int x, int y, boolean hasPid, boolean unknown, String note)
	{
		int r = 9;
		Polygon outer = new Polygon(new int[]{x, x + r, x, x - r}, new int[]{y - r, y, y + r, y}, 4);
		g.setColor(hasPid ? GOLD_BACK : GREY_DARK);
		g.fillPolygon(outer);
		g.setColor(unknown ? UNKNOWN : hasPid ? GOLD : GREY);
		g.fillPolygon(new Polygon(new int[]{x, x + 5, x, x - 5}, new int[]{y - 5, y, y + 5, y}, 4));
		g.setStroke(new BasicStroke(hasPid ? 2.2f : 1.2f));
		g.setColor(hasPid ? GOLD_EDGE : GREY_EDGE);
		g.drawPolygon(outer);
		if (hasPid)
		{
			Polygon crown = new Polygon(
				new int[]{x - 7, x - 5, x - 2, x, x + 2, x + 5, x + 7},
				new int[]{y - 8, y - 14, y - 10, y - 15, y - 10, y - 14, y - 8}, 7);
			g.setColor(GOLD);
			g.fillPolygon(crown);
			g.setColor(Color.BLACK);
			g.setStroke(new BasicStroke(1.0f));
			g.drawPolygon(crown);
		}
		else
		{
			g.setColor(GREY_CROSS);
			g.drawLine(x - 6, y - 6, x + 6, y + 6);
		}
		if (note != null)
		{
			g.setFont(FontManager.getRunescapeSmallFont());
			FontMetrics fm = g.getFontMetrics();
			int tx = x - fm.stringWidth(note) / 2;
			int ty = y + r + 12;
			g.setColor(Color.BLACK);
			g.drawString(note, tx + 1, ty + 1);
			g.setColor(hasPid ? GOLD : GREY_TEXT);
			g.drawString(note, tx, ty);
		}
		g.setStroke(new BasicStroke(1.0f));
	}
}
