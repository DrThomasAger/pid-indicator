package com.drthomasager.pid;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A diamond over your head and over your opponent's. Gold with a crown and a
 * 1 on whoever goes first, grey with a 2 on whoever goes second, grey with a ?
 * while nobody knows yet. Once the game may hand PID over they read 1? and 2?.
 */
class PidIndicatorOverlay extends Overlay
{
	private static final Color GOLD = new Color(255, 200, 0);
	private static final Color GOLD_EDGE = new Color(255, 230, 110);
	private static final Color GOLD_DARK = new Color(110, 70, 0);
	private static final Color GREY = new Color(120, 120, 120, 200);
	private static final Color GREY_EDGE = new Color(60, 60, 60, 220);
	private static final Color GREY_TEXT = new Color(225, 225, 225);
	/** How far above the head the diamond sits, clear of the health bar. */
	private static final int HEIGHT_ABOVE = 80;
	private static final int R = 12;

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
		boolean mayChange = !unknown && config.markWhenMayChange()
			&& plugin.getSinceChange() >= plugin.redrawWindow()[0];
		diamond(g, me, unknown ? State.UNKNOWN : h == PidTracker.Holder.ME ? State.FIRST : State.SECOND, mayChange);
		if (op != null)
		{
			diamond(g, op, unknown ? State.UNKNOWN : h == PidTracker.Holder.THEM ? State.FIRST : State.SECOND, mayChange);
		}
		return null;
	}

	enum State { FIRST, SECOND, UNKNOWN }

	private void diamond(Graphics2D g, Player p, State s, boolean mayChange)
	{
		Point pt = p.getCanvasTextLocation(g, "", p.getLogicalHeight() + HEIGHT_ABOVE);
		if (pt == null)
		{
			return;
		}
		drawDiamond(g, pt.getX(), pt.getY(), s, mayChange);
		if (config.showAnimationIds() && p.getAnimation() >= 0)
		{
			String id = "animation " + p.getAnimation();
			g.setFont(FontManager.getRunescapeSmallFont());
			int tx = pt.getX() - g.getFontMetrics().stringWidth(id) / 2;
			g.setColor(Color.BLACK);
			g.drawString(id, tx + 1, pt.getY() + R + 15);
			g.setColor(Color.CYAN);
			g.drawString(id, tx, pt.getY() + R + 14);
		}
	}

	/** The diamond itself, centred on x, y, with a ? after the number when PID may change. */
	static void drawDiamond(Graphics2D g, int x, int y, State s, boolean mayChange)
	{
		Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		Object textAa = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		boolean first = s == State.FIRST;
		Polygon outer = new Polygon(new int[]{x, x + R, x, x - R}, new int[]{y - R, y, y + R, y}, 4);
		g.setColor(first ? GOLD : GREY);
		g.fillPolygon(outer);
		g.setStroke(new BasicStroke(2f));
		g.setColor(first ? GOLD_EDGE : GREY_EDGE);
		g.drawPolygon(outer);
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke(1f));
		g.drawPolygon(new Polygon(new int[]{x, x + R + 1, x, x - R - 1}, new int[]{y - R - 1, y, y + R + 1, y}, 4));
		if (first)
		{
			Polygon crown = new Polygon(
				new int[]{x - 8, x - 6, x - 3, x, x + 3, x + 6, x + 8},
				new int[]{y - R - 2, y - R - 9, y - R - 5, y - R - 10, y - R - 5, y - R - 9, y - R - 2}, 7);
			g.setColor(GOLD);
			g.fillPolygon(crown);
			g.setColor(Color.BLACK);
			g.drawPolygon(crown);
		}
		// the game's own bold font, drawn sharp as the game draws it
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setFont(FontManager.getRunescapeBoldFont());
		String n = first ? "1" : s == State.SECOND ? "2" : "?";
		if (mayChange && s != State.UNKNOWN)
		{
			n = n + "?";
		}
		FontMetrics fm = g.getFontMetrics();
		int tx = x - fm.stringWidth(n) / 2;
		int ty = y + fm.getAscent() / 2 - 1;
		g.setColor(first ? GOLD_DARK : Color.BLACK);
		g.drawString(n, tx + 1, ty + 1);
		g.setColor(first ? Color.WHITE : GREY_TEXT);
		g.drawString(n, tx, ty);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa == null ? RenderingHints.VALUE_ANTIALIAS_DEFAULT : aa);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textAa == null ? RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT : textAa);
	}
}
