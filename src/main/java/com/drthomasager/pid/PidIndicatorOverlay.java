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
	private static final int R = 14;

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

	/**
	 * The diamond itself, centred on x, y, with a ? after the number when PID
	 * may change. Drawn the way the game draws its own icons: a shaded face, a
	 * light upper edge and a dark lower one, a black outline and a soft shadow.
	 * Whoever goes second, or a diamond nobody can read yet, is smaller, darker
	 * and half see-through, so it stays out of the way.
	 */
	static void drawDiamond(Graphics2D g, int x, int y, State s, boolean mayChange)
	{
		Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		Object textAa = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
		java.awt.Composite comp = g.getComposite();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		boolean first = s == State.FIRST;
		int r = first ? R : R - 2;
		if (!first)
		{
			g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.6f));
		}
		Color top = first ? new Color(255, 236, 140) : new Color(140, 140, 140);
		Color bottom = first ? new Color(196, 128, 0) : new Color(62, 62, 62);
		Color light = first ? new Color(255, 250, 210) : new Color(175, 175, 175);
		Color dark = first ? new Color(120, 70, 0) : new Color(35, 35, 35);

		Polygon face = new Polygon(new int[]{x, x + r, x, x - r}, new int[]{y - r, y, y + r, y}, 4);
		Polygon shadow = new Polygon(new int[]{x + 2, x + r + 2, x + 2, x - r + 2}, new int[]{y - r + 2, y + 2, y + r + 2, y + 2}, 4);
		g.setColor(new Color(0, 0, 0, 140));
		g.fillPolygon(shadow);
		g.setPaint(new java.awt.GradientPaint(x, y - r, top, x, y + r, bottom));
		g.fillPolygon(face);
		// the bevel: light on the upper edges, dark on the lower
		g.setStroke(new BasicStroke(1.5f));
		g.setColor(light);
		g.drawLine(x - r + 2, y, x, y - r + 2);
		g.drawLine(x, y - r + 2, x + r - 2, y);
		g.setColor(dark);
		g.drawLine(x - r + 2, y, x, y + r - 2);
		g.drawLine(x, y + r - 2, x + r - 2, y);
		g.setStroke(new BasicStroke(1f));
		g.setColor(Color.BLACK);
		g.drawPolygon(face);

		if (first)
		{
			drawCrown(g, x, y - r - 1);
		}

		// the number in the game's own bold font, drawn sharp as the game draws it
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setFont(FontManager.getRunescapeBoldFont());
		String n = first ? "1" : s == State.SECOND ? "2" : "?";
		if (mayChange && s != State.UNKNOWN)
		{
			n = n + "?";
		}
		FontMetrics fm = g.getFontMetrics();
		int tx = x - fm.stringWidth(n) / 2 + 1;
		int ty = y + fm.getAscent() / 2 - 1;
		g.setColor(Color.BLACK);
		g.drawString(n, tx + 1, ty + 1);
		g.setColor(first ? Color.WHITE : new Color(200, 200, 200));
		g.drawString(n, tx, ty);

		g.setComposite(comp);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa == null ? RenderingHints.VALUE_ANTIALIAS_DEFAULT : aa);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textAa == null ? RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT : textAa);
	}

	/** A small shaded crown resting on (x, base), three points each with a ball. */
	private static void drawCrown(Graphics2D g, int x, int base)
	{
		int w = 9;
		int h = 8;
		Polygon crown = new Polygon(
			new int[]{x - w, x - w, x - w / 2, x, x + w / 2, x + w, x + w},
			new int[]{base, base - h, base - h / 2, base - h - 2, base - h / 2, base - h, base}, 7);
		Polygon shadow = new Polygon(crown.xpoints.clone(), crown.ypoints.clone(), crown.npoints);
		shadow.translate(1, 1);
		g.setColor(new Color(0, 0, 0, 140));
		g.fillPolygon(shadow);
		g.setPaint(new java.awt.GradientPaint(x, base - h - 2, new Color(255, 240, 150), x, base, new Color(200, 130, 0)));
		g.fillPolygon(crown);
		g.setColor(new Color(120, 70, 0));
		g.drawLine(x - w + 1, base - 1, x + w - 1, base - 1);
		g.setColor(Color.BLACK);
		g.drawPolygon(crown);
		int[][] balls = {{x - w, base - h}, {x, base - h - 2}, {x + w, base - h}};
		for (int[] b : balls)
		{
			g.setColor(new Color(255, 250, 210));
			g.fillOval(b[0] - 2, b[1] - 2, 4, 4);
			g.setColor(Color.BLACK);
			g.drawOval(b[0] - 2, b[1] - 2, 4, 4);
		}
	}
}
