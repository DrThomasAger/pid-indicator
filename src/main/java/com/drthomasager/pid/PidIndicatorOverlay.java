package com.drthomasager.pid;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A small diamond beside each fighter's health bar, cut from the game's own
 * stone texture (the blob behind a selected side panel tab). Gold with a 1 on
 * whoever goes first, grey with a 2 on whoever goes second, grey with a ?
 * while nobody knows yet, and 1? and 2? once the game may hand PID over. It
 * sits to the right of the health bar, where the game draws nothing, so the
 * prayer icon, skull and overheads are never covered or moved.
 */
class PidIndicatorOverlay extends Overlay
{
	/** The selected side panel tab's stone, a 33 by 36 sprite. */
	static final int STONE_SPRITE = 1030;
	private static final int R_FIRST = 10;
	private static final int R_OTHER = 8;
	/** The game's health bar sits this far above the point 15 units over the head. */
	private static final int BAR_Y = -15;
	/** Half the health bar's width, plus a gap. */
	private static final int BAR_RIGHT = 19;

	enum State { FIRST, SECOND, UNKNOWN }

	private final Client client;
	private final PidIndicatorPlugin plugin;
	private final PidIndicatorConfig config;
	private final SpriteManager spriteManager;
	private final Map<State, BufferedImage> textures = new EnumMap<>(State.class);

	@Inject
	PidIndicatorOverlay(Client client, PidIndicatorPlugin plugin, PidIndicatorConfig config, SpriteManager spriteManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.spriteManager = spriteManager;
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

	private BufferedImage texture(State s)
	{
		BufferedImage t = textures.get(s);
		if (t == null)
		{
			BufferedImage stone = spriteManager.getSprite(STONE_SPRITE, 0);
			if (stone == null)
			{
				return null;
			}
			t = tint(stone, s);
			textures.put(s, t);
		}
		return t;
	}

	private void diamond(Graphics2D g, Player p, State s, boolean mayChange)
	{
		Point pt = p.getCanvasTextLocation(g, "", p.getLogicalHeight() + 15);
		if (pt == null)
		{
			return;
		}
		int r = s == State.FIRST ? R_FIRST : R_OTHER;
		drawDiamond(g, texture(s), pt.getX() + BAR_RIGHT + r, pt.getY() + BAR_Y, r, s, mayChange);
		if (config.showAnimationIds() && p.getAnimation() >= 0)
		{
			String id = "animation " + p.getAnimation();
			g.setFont(FontManager.getRunescapeSmallFont());
			int tx = pt.getX() + BAR_RIGHT;
			g.setColor(Color.BLACK);
			g.drawString(id, tx + 1, pt.getY() + BAR_Y + r + 13);
			g.setColor(Color.CYAN);
			g.drawString(id, tx, pt.getY() + BAR_Y + r + 12);
		}
	}

	/**
	 * The game's stone, recoloured: its light and dark kept as they are, its
	 * colour gold for whoever goes first and grey for everyone else.
	 */
	static BufferedImage tint(BufferedImage stone, State s)
	{
		int[] base = s == State.FIRST ? new int[]{255, 196, 60} : new int[]{150, 150, 150};
		double k = s == State.FIRST ? 2.2 : 1.25;
		int w = stone.getWidth();
		int h = stone.getHeight();
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < w; x++)
		{
			for (int y = 0; y < h; y++)
			{
				int p = stone.getRGB(x, y);
				int a = p >>> 24;
				double l = (0.3 * ((p >> 16) & 255) + 0.59 * ((p >> 8) & 255) + 0.11 * (p & 255)) / 128.0;
				int r = (int) Math.min(255, base[0] * l * k);
				int gr = (int) Math.min(255, base[1] * l * k);
				int b = (int) Math.min(255, base[2] * l * k);
				out.setRGB(x, y, (a << 24) | (r << 16) | (gr << 8) | b);
			}
		}
		return out;
	}

	/**
	 * The diamond centred on x, y: the stone texture cut to shape with the
	 * game's black edge and a faint shadow, and its number centred on the
	 * glyph's own outline. Whoever goes second, or a diamond not read yet, is
	 * half see-through so it stays out of the way.
	 */
	static void drawDiamond(Graphics2D g, BufferedImage tex, int x, int y, int r, State s, boolean mayChange)
	{
		Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		Composite comp = g.getComposite();
		Shape clip = g.getClip();
		boolean first = s == State.FIRST;
		if (!first)
		{
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f));
		}
		Polygon face = new Polygon(new int[]{x, x + r, x, x - r}, new int[]{y - r, y, y + r, y}, 4);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g.setColor(new Color(0, 0, 0, 110));
		g.translate(1, 1);
		g.fillPolygon(face);
		g.translate(-1, -1);
		if (tex != null)
		{
			g.clip(face);
			g.drawImage(tex, x - r - 2, y - r - 2, 2 * r + 4, 2 * r + 4, null);
			g.setClip(clip);
		}
		else
		{
			g.setColor(first ? new Color(214, 160, 40) : new Color(95, 95, 95));
			g.fillPolygon(face);
		}
		g.setColor(Color.BLACK);
		g.drawPolygon(face);

		String n = first ? "1" : s == State.SECOND ? "2" : "?";
		if (mayChange && s != State.UNKNOWN)
		{
			n = n + "?";
		}
		Font f = FontManager.getRunescapeSmallFont();
		GlyphVector gv = f.createGlyphVector(g.getFontRenderContext(), n);
		Rectangle2D vb = gv.getVisualBounds();
		int tx = (int) Math.round(x - vb.getX() - vb.getWidth() / 2.0);
		int ty = (int) Math.round(y - vb.getY() - vb.getHeight() / 2.0);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setFont(f);
		g.setColor(Color.BLACK);
		g.drawString(n, tx + 1, ty + 1);
		g.setColor(first ? Color.WHITE : new Color(220, 220, 220));
		g.drawString(n, tx, ty);

		g.setComposite(comp);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa == null ? RenderingHints.VALUE_ANTIALIAS_DEFAULT : aa);
	}
}
