package net.machinemuse.general.geometry;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import net.machinemuse.general.gui.MuseGui;
import net.machinemuse.general.gui.MuseIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.RenderEngine;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.ForgeHooksClient;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Contains a bunch of random OpenGL-related functions, accessed statically.
 * 
 * @author MachineMuse
 * 
 */
public abstract class MuseRenderer {

	protected static RenderItem renderItem;

	public static void drawCircleAround(double xoffset, double yoffset,
			double radius) {
		int start = (int) (System.currentTimeMillis() / 4 % 360);
		double startangle = 2.0 * Math.PI * start / 360.0;
		double endangle = startangle + 2.0 * Math.PI;

		DoubleBuffer vertices = getArcPoints(startangle, endangle,
				radius, xoffset, yoffset, 0);
		int numvertices = vertices.limit() / 3;
		DoubleBuffer colours = getColourGradient(
				new Colour(0.0f, 1.0f, 0.0f, 0.0f),
				new Colour(0.8f, 1.0f, 0.8f, 1.0f),
				numvertices);
		arraysOnC();
		texturelessOn();
		smoothingOn();
		on2D();
		GL11.glPushMatrix();

		GL11.glColorPointer(4, 0, colours);
		GL11.glVertexPointer(3, 0, vertices);

		GL11.glDrawArrays(GL11.GL_LINE_STRIP, 0, numvertices);

		GL11.glPopMatrix();
		off2D();
		texturelessOff();
		arraysOff();
	}

	/**
	 * Creates a list of points linearly interpolated between points a and b
	 * noninclusive.
	 * 
	 * @return A list of num points
	 */
	public static List<Point2D> pointsInLine(int num, Point2D a, Point2D b) {
		List<Point2D> points = new ArrayList<Point2D>();
		if (num < 1) {
			return points;
		} else if (num < 2) {
			points.add(b.minus(a).times(0.5F).plus(a));
		} else {
			Point2D step = b.minus(a).times(1.0F / (num + 1));
			for (int i = 1; i < num + 1; i++) {
				points.add(a.plus(step.times(i)));
			}
		}

		return points;
	}

	public static DoubleBuffer getColourGradient(Colour c1, Colour c2,
			int numsegments) {
		DoubleBuffer buffer = BufferUtils.createDoubleBuffer(numsegments * 4);
		for (double i = 0; i < numsegments; i++) {
			Colour c3 = c1.interpolate(c2, i / numsegments);
			buffer.put(c3.r);
			buffer.put(c3.g);
			buffer.put(c3.b);
			buffer.put(c3.a);
		}
		buffer.flip();
		return buffer;
	}

	/**
	 * Efficient algorithm for drawing circles and arcs in pure opengl!
	 * 
	 * @param startangle
	 *            Start angle in radians
	 * @param endangle
	 *            End angle in radians
	 * @param radius
	 *            Radius of the circle (used in calculating number of segments
	 *            to draw as well as size of the arc)
	 * @param xoffset
	 *            Convenience parameter, added to every vertex
	 * @param yoffset
	 *            Convenience parameter, added to every vertex
	 * @param zoffset
	 *            Convenience parameter, added to every vertex
	 * @return
	 */
	public static DoubleBuffer getArcPoints(double startangle,
			double endangle, double radius, double xoffset, double yoffset,
			double zoffset) {
		// roughly 8 vertices per Minecraft 'pixel' - should result in at least
		// 2 vertices per real pixel on the screen.
		int numVertices = (int) Math.ceil(Math.abs((endangle - startangle) * 16
				* Math.PI));
		double theta = (endangle - startangle) / numVertices;
		DoubleBuffer buffer = BufferUtils
				.createDoubleBuffer(numVertices * 3);

		double x = radius * Math.sin(startangle);
		double y = radius * Math.cos(startangle);
		double tf = Math.tan(theta); // precompute tangent factor: how much to
										// move along the tangent line each
										// iteration
		double rf = Math.cos(theta); // precompute radial factor: how much to
										// move back towards the origin each
										// iteration
		double tx;
		double ty;

		for (int i = 0; i < numVertices; i++) {
			buffer.put(x + xoffset);
			buffer.put(y + yoffset);
			buffer.put(zoffset);
			tx = y; // compute tangent lines
			ty = -x;
			x += tx * tf; // add tangent line * tangent factor
			y += ty * tf;
			x *= rf;
			y *= rf;
		}
		buffer.flip();
		return buffer;
	}

	/**
	 * Draws a swirly green circle at the specified coordinates in the current
	 * reference frame.
	 * 
	 * @param xoffset
	 * @param yoffset
	 * @param radius
	 */
	public static void drawTriangles2D(float[] v, float[] c,
			int[] i) {
		arraysOnC();
		texturelessOn();
		smoothingOn();
		on2D();

		// float subdivisions = 5f;
		// float radius = 0.5f;

		// GL11.glPushMatrix();
		// GL11.glTranslatef(-radius, -radius, 0);
		// for (int i1 = 0; i1 <= subdivisions * 2; i1++) {
		// for (int i2 = 0; i2 <= subdivisions * 2; i2++) {
		FloatBuffer vertices = BufferUtils.createFloatBuffer(v.length);
		vertices.put(v);
		vertices.flip();

		FloatBuffer colours = BufferUtils.createFloatBuffer(c.length);
		colours.put(c);
		colours.flip();

		IntBuffer indices = BufferUtils.createIntBuffer(i.length);
		indices.put(i);
		indices.flip();

		// GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

		GL11.glVertexPointer(3, 0, vertices);
		GL11.glColorPointer(4, 0, colours);

		GL11.glDrawElements(GL11.GL_TRIANGLES, indices);

		// GL11.glTranslatef(0, radius / subdivisions, 0);
		// }
		// GL11.glTranslatef(radius / subdivisions, -radius * 2, 0);
		// }
		// GL11.glPopMatrix();

		off2D();
		texturelessOff();
		arraysOff();

	}

	/**
	 * Draws the given vertex arrays (textureless)
	 * 
	 */
	public static void drawTriangles3DR(float[] v, float[] c,
			int[] i) {
		arraysOnC();
		texturelessOn();
		smoothingOn();

		// float subdivisions = 5f;
		// float radius = 0.5f;

		// GL11.glPushMatrix();
		// GL11.glTranslatef(-radius, -radius, 0);
		// for (int i1 = 0; i1 <= subdivisions * 2; i1++) {
		// for (int i2 = 0; i2 <= subdivisions * 2; i2++) {
		FloatBuffer vertices = BufferUtils.createFloatBuffer(v.length);
		vertices.put(v);
		vertices.flip();

		FloatBuffer colours = BufferUtils.createFloatBuffer(c.length);
		colours.put(c);
		colours.flip();

		IntBuffer indices = BufferUtils.createIntBuffer(i.length);
		indices.put(i);
		indices.flip();

		// GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

		GL11.glVertexPointer(3, 0, vertices);
		GL11.glColorPointer(4, 0, colours);

		GL11.glDrawElements(GL11.GL_TRIANGLES, indices);

		// GL11.glTranslatef(0, radius / subdivisions, 0);
		// }
		// GL11.glTranslatef(radius / subdivisions, -radius * 2, 0);
		// }
		// GL11.glPopMatrix();

		texturelessOff();
		arraysOff();

	}

	/**
	 * 2D rendering mode on/off
	 */

	public static void on2D() {

		GL11.glDisable(GL11.GL_DEPTH_TEST);
		// GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_LIGHTING);

		// attempt at fake antialiasing
		// GL11.glBlendFunc(GL11.GL_SRC_ALPHA_SATURATE, GL11.GL_ONE);
		// GL11.glColorMask(false, false, false, true);
		// GL11.glClearColor(0, 0, 0, 0);
		// GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		// GL11.glColorMask(true, true, true, true);

		// GL11.glHint(GL11.GL_POINT_SMOOTH, GL11.GL_NICEST);
		// GL11.glHint(GL11.GL_LINE_SMOOTH, GL11.GL_NICEST);
		// GL11.glHint(GL11.GL_POLYGON_SMOOTH, GL11.GL_NICEST);
		// GL11.glDepthFunc(GL11.GL_GREATER);
	}

	public static void off2D() {
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_LIGHTING);
	}

	/**
	 * Arrays on/off
	 */

	public static void arraysOnC() {
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		// GL11.glEnableClientState(GL11.GL_INDEX_ARRAY);
	}

	public static void arraysOnT() {
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		// GL11.glEnableClientState(GL11.GL_INDEX_ARRAY);
	}

	public static void arraysOff() {
		GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
		GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		// GL11.glDisableClientState(GL11.GL_INDEX_ARRAY);

	}

	/**
	 * Call before doing any pure geometry (ie. with colours rather than
	 * textures).
	 */
	public static void texturelessOn() {
		GL11.glDisable(GL11.GL_TEXTURE_2D);

	}

	/**
	 * Call after doing pure geometry (ie. with colours) to go back to the
	 * texture mode (default).
	 */
	public static void texturelessOff() {
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

	public static void smoothingOn() {
		GL11.glShadeModel(GL11.GL_SMOOTH);
		// GL11.glEnable(GL11.GL_LINE_SMOOTH);
		// GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}

	public static void smoothingOff() {
		GL11.glShadeModel(GL11.GL_FLAT);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
		GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
		GL11.glDisable(GL11.GL_BLEND);
		// GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}

	/**
	 * Draws a rectangle with a vertical gradient between the specified colors.
	 */
	public static void drawGradientRect(float left, float top, float right,
			float bottom, Colour c1, Colour c2, double zLevel)
	{
		texturelessOn();
		smoothingOn();
		on2D();

		Tessellator tessellator = Tessellator.instance;

		tessellator.startDrawingQuads();
		tessellator.setColorRGBA_F((float) c1.r, (float) c1.g, (float) c1.b,
				(float) c1.a);
		tessellator.addVertex(right, top,
				zLevel);
		tessellator
				.addVertex(left, top, zLevel);

		tessellator.setColorRGBA_F((float) c2.r, (float) c2.g, (float) c2.b,
				(float) c2.a);
		tessellator.addVertex(left, bottom,
				zLevel);
		tessellator.addVertex(right, bottom,
				zLevel);
		tessellator.draw();

		off2D();
		texturelessOff();
	}

	/**
	 * Draws a rectangle with a vertical gradient between the specified colors.
	 */
	public static void drawFrameRect(double left, double top, double right,
			double bottom, Colour borderColour, Colour insideColour,
			double zLevel, double cornerradius)
	{
		texturelessOn();
		smoothingOn();
		on2D();

		arraysOnC();

		DoubleBuffer corner = getArcPoints(
				Math.PI, 3.0 * Math.PI / 2.0,
				cornerradius, left + cornerradius, top + cornerradius, zLevel);

		DoubleBuffer allVertices = BufferUtils.createDoubleBuffer(corner
				.limit() * 4);
		allVertices.put(corner);
		corner = getArcPoints(
				3.0 * Math.PI / 2.0, 2.0 * Math.PI,
				cornerradius, left + cornerradius, bottom - cornerradius,
				zLevel);
		allVertices.put(corner);
		corner = getArcPoints(
				0, Math.PI / 2.0,
				cornerradius, right - cornerradius, bottom - cornerradius,
				zLevel);
		allVertices.put(corner);
		corner = getArcPoints(
				Math.PI / 2.0, Math.PI,
				cornerradius, right - cornerradius, top + cornerradius,
				zLevel);
		allVertices.put(corner);
		allVertices.flip();
		DoubleBuffer colours = getColourGradient(
				borderColour, borderColour,
				allVertices.limit() * 4 / 3 + 8);

		GL11.glColorPointer(4, 0, colours);
		GL11.glVertexPointer(3, 0, allVertices);
		GL11.glDrawArrays(GL11.GL_LINE_LOOP, 0, allVertices.limit() / 3);

		DoubleBuffer triFanVertices = BufferUtils
				.createDoubleBuffer(allVertices.limit());

		// allVertices.flip();
		triFanVertices.put(allVertices);
		triFanVertices.flip();

		colours = getColourGradient(
				insideColour, insideColour,
				allVertices.limit() * 4 / 3 + 8);

		GL11.glColorPointer(4, 0, colours);
		GL11.glVertexPointer(3, 0, triFanVertices);
		GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, triFanVertices.limit() / 3);

		arraysOff();
		off2D();
		texturelessOff();
	}

	public static void drawFrameRect(Point2D topleft, Point2D bottomright,
			Colour borderColour, Colour insideColour, double zLevel,
			double cornerRadius) {
		drawFrameRect(topleft.x(), topleft.y(), bottomright.x(),
				bottomright.y(), borderColour,
				insideColour, zLevel, cornerRadius);
	}

	public static void drawGradientRect3D(Vec3 origin, Vec3 size, Colour c1,
			Colour c2)
	{
		texturelessOn();
		GL11.glBegin(GL11.GL_QUADS);
		c1.doGL();
		GL11.glVertex3d(origin.xCoord, origin.yCoord,
				origin.zCoord);
		GL11.glVertex3d(origin.xCoord + size.xCoord, origin.yCoord,
				origin.zCoord);

		c2.doGL();
		GL11.glVertex3d(origin.xCoord + size.xCoord, origin.yCoord
				+ size.yCoord,
				origin.zCoord + size.zCoord);
		GL11.glVertex3d(origin.xCoord, origin.yCoord + size.yCoord,
				origin.zCoord + size.zCoord);

		GL11.glEnd();
		texturelessOff();
	}

	public static void drawItemAt(double x, double y, ItemStack item) {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		// GL11.glDepthFunc(GL11.GL_GREATER);
		GL11.glDisable(GL11.GL_LIGHTING);

		getRenderItem().renderItemAndEffectIntoGUI(
				getFontRenderer(), getRenderEngine(), item, (int) x, (int) y);
		getRenderItem().renderItemOverlayIntoGUI(getFontRenderer(),
				getRenderEngine(), item, (int) x, (int) y);

		// GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_LIGHTING);
	}

	public static void drawIconAt(double x, double y,
			MuseIcon icon, Colour colour) {
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		texturelessOff();
		smoothingOn();

		ForgeHooksClient.bindTexture(icon.getTexturefile(), 0);

		if (colour != null)
		{
			colour.doGL();
		}

		Tessellator tess = Tessellator.instance;
		tess.startDrawingQuads();
		float r = 0.0625f;
		float u = (icon.getIconIndex() % 16) * r;
		float v = (icon.getIconIndex() / 16) * r;
		tess.addVertexWithUV(
				x, y, 0,
				u, v);
		tess.addVertexWithUV(
				x, y + 16, 0,
				u, v + r);
		tess.addVertexWithUV(
				x + 16, y + 16, 0,
				u + r, v + r);
		tess.addVertexWithUV(
				x + 16, y, 0,
				u + r, v);
		tess.draw();

		MuseRenderer.smoothingOff();
		GL11.glEnable(GL11.GL_CULL_FACE);
		// GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glPopMatrix();
	}

	/**
	 * 
	 */
	public static void relativeCoords(MuseGui gui) {
		GL11.glPushMatrix();
		GL11.glTranslatef(gui.width / 2, gui.height / 2, 0);
		GL11.glScalef(gui.getxSize(), gui.getySize(), 0);
	}

	/**
	 * 
	 */
	public static void popMatrix() {
		GL11.glPopMatrix();
	}

	public static void drawString(String s, double x, double y) {
		RenderHelper.disableStandardItemLighting();
		getFontRenderer().drawStringWithShadow(s, (int) x, (int) y,
				new Colour(1, 1, 1, 1).getInt());
	}

	public static void drawCenteredString(String s, double x, double y) {
		double xradius = getFontRenderer().getStringWidth(s) / 2;
		drawString(s, x - xradius, y);
	}

	/**
	 * Draws a rectangular prism (cube or otherwise orthogonal)
	 */
	public static void drawRectPrism(double x, double d, double e,
			double f, double z, double g,
			float texturex, float texturey,
			float texturex2, float texturey2) {
		arraysOnT();
		texturelessOff();
		Vec3[] points = {
				Vec3.createVectorHelper(x, e, z),
				Vec3.createVectorHelper(d, e, z),
				Vec3.createVectorHelper(x, f, z),
				Vec3.createVectorHelper(d, f, z),
				Vec3.createVectorHelper(x, e, g),
				Vec3.createVectorHelper(d, e, g),
				Vec3.createVectorHelper(x, f, g),
				Vec3.createVectorHelper(d, f, g)
		};
		PositionTextureVertex[] va1 = {
				new PositionTextureVertex(points[0], texturex, texturey2),
				new PositionTextureVertex(points[2], texturex2, texturey2),
				new PositionTextureVertex(points[3], texturex2, texturey),
				new PositionTextureVertex(points[1], texturex, texturey)

		};
		new TexturedQuad(va1).draw(Tessellator.instance, 1.0F);
		PositionTextureVertex[] va2 = {
				new PositionTextureVertex(points[2], texturex, texturey2),
				new PositionTextureVertex(points[6], texturex2, texturey2),
				new PositionTextureVertex(points[7], texturex2, texturey),
				new PositionTextureVertex(points[3], texturex, texturey)

		};
		new TexturedQuad(va2).draw(Tessellator.instance, 1.0F);
		PositionTextureVertex[] va3 = {
				new PositionTextureVertex(points[6], texturex, texturey2),
				new PositionTextureVertex(points[4], texturex2, texturey2),
				new PositionTextureVertex(points[5], texturex2, texturey),
				new PositionTextureVertex(points[7], texturex, texturey)

		};
		new TexturedQuad(va3).draw(Tessellator.instance, 1.0F);
		PositionTextureVertex[] va4 = {
				new PositionTextureVertex(points[4], texturex, texturey2),
				new PositionTextureVertex(points[0], texturex2, texturey2),
				new PositionTextureVertex(points[1], texturex2, texturey),
				new PositionTextureVertex(points[5], texturex, texturey)

		};
		new TexturedQuad(va4).draw(Tessellator.instance, 1.0F);
		PositionTextureVertex[] va5 = {
				new PositionTextureVertex(points[1], texturex, texturey2),
				new PositionTextureVertex(points[3], texturex2, texturey2),
				new PositionTextureVertex(points[7], texturex2, texturey),
				new PositionTextureVertex(points[5], texturex, texturey)

		};
		new TexturedQuad(va5).draw(Tessellator.instance, 1.0F);
		PositionTextureVertex[] va6 = {
				new PositionTextureVertex(points[0], texturex, texturey2),
				new PositionTextureVertex(points[4], texturex2, texturey2),
				new PositionTextureVertex(points[6], texturex2, texturey),
				new PositionTextureVertex(points[2], texturex, texturey)

		};
		new TexturedQuad(va6).draw(Tessellator.instance, 1.0F);
		// int[] indices = {
		// 0, 3, 1,
		// 0, 2, 3,
		// 2, 6, 7,
		// 2, 7, 3,
		// 6, 4, 5,
		// 6, 5, 7,
		// 4, 0, 1,
		// 4, 1, 5,
		// 1, 3, 7,
		// 1, 7, 5,
		// 0, 6, 2,
		// 0, 4, 6
		// };
		// drawTriangles3DT(points, textures, indices);
		texturelessOff();
		arraysOff();
	}

	private static void drawTriangles3DT(float[] v, float[] textures2,
			int[] i) {
		arraysOnT();
		texturelessOff();

		// float subdivisions = 5f;
		// float radius = 0.5f;

		// GL11.glPushMatrix();
		// GL11.glTranslatef(-radius, -radius, 0);
		// for (int i1 = 0; i1 <= subdivisions * 2; i1++) {
		// for (int i2 = 0; i2 <= subdivisions * 2; i2++) {
		FloatBuffer vertices = BufferUtils.createFloatBuffer(v.length);
		vertices.put(v);
		vertices.flip();

		FloatBuffer textures = BufferUtils.createFloatBuffer(textures2.length);
		textures.put(textures2);
		textures.flip();

		IntBuffer indices = BufferUtils.createIntBuffer(i.length);
		indices.put(i);
		indices.flip();

		// GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

		GL11.glVertexPointer(3, 0, vertices);
		GL11.glTexCoordPointer(2, 0, textures);

		GL11.glDrawElements(GL11.GL_TRIANGLES, indices);

		// GL11.glTranslatef(0, radius / subdivisions, 0);
		// }
		// GL11.glTranslatef(radius / subdivisions, -radius * 2, 0);
		// }
		// GL11.glPopMatrix();

		texturelessOff();
		arraysOff();

	}

	public static void drawStringsJustified(List<String> words, double x1,
			double x2, double y) {
		int totalwidth = 0;
		for (String word : words) {
			totalwidth += getFontRenderer().getStringWidth(
					word);
		}

		double spacing = (x2 - x1 - totalwidth) / (words.size() - 1);

		double currentwidth = 0;
		for (String word : words) {
			MuseRenderer.drawString(word, x1 + currentwidth, y);
			currentwidth += getFontRenderer().getStringWidth(
					word) + spacing;
		}

	}

	/**
	 * Singleton pattern for FontRenderer
	 */
	public static FontRenderer getFontRenderer() {
		return Minecraft.getMinecraft().fontRenderer;
	}

	/**
	 * Singleton pattern for RenderEngine
	 */
	public static RenderEngine getRenderEngine() {
		return Minecraft.getMinecraft().renderEngine;
	}

	/**
	 * Singleton pattern for the RenderItem
	 * 
	 * @return the static renderItem instance
	 */
	public static RenderItem getRenderItem() {
		if (renderItem == null) {
			renderItem = new RenderItem();
		}
		return renderItem;
	}
}