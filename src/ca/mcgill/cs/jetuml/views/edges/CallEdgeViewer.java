/*******************************************************************************
 * JetUML - A desktop application for fast UML diagramming.
 *
 * Copyright (C) 2018, 2019 by the contributors of the JetUML project.
 *     
 * See: https://github.com/prmr/JetUML
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package ca.mcgill.cs.jetuml.views.edges;

import java.util.ArrayList;

import ca.mcgill.cs.jetuml.diagram.Edge;
import ca.mcgill.cs.jetuml.diagram.edges.CallEdge;
import ca.mcgill.cs.jetuml.diagram.nodes.CallNode;
import ca.mcgill.cs.jetuml.geom.Conversions;
import ca.mcgill.cs.jetuml.geom.Direction;
import ca.mcgill.cs.jetuml.geom.Line;
import ca.mcgill.cs.jetuml.geom.Point;
import ca.mcgill.cs.jetuml.geom.Rectangle;
import ca.mcgill.cs.jetuml.views.ArrowHead;
import ca.mcgill.cs.jetuml.views.ArrowHeadView;
import ca.mcgill.cs.jetuml.views.LineStyle;
import ca.mcgill.cs.jetuml.views.StringViewer;
import ca.mcgill.cs.jetuml.views.ToolGraphics;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;

/**
 * A viewer to show call edges in a sequence diagrams. These are labeled
 * edges that are either straight or self-edges, in a solid line, and 
 * have either a V or half-V arrow head.
 */
public final class CallEdgeViewer extends AbstractEdgeViewer
{	
	private static final StringViewer CENTERED_STRING_VIEWER = new StringViewer(StringViewer.Align.CENTER, false, false);
	private static final StringViewer LEFT_JUSTIFIED_STRING_VIEWER = new StringViewer(StringViewer.Align.LEFT, false, false);

	private static final int SHIFT = -10;
	
	@Override
	protected Shape getShape(Edge pEdge)
	{
		Point[] points = getPoints(pEdge);
		Path path = new Path();
		Point point = points[points.length - 1];
		MoveTo moveTo = new MoveTo(point.getX(), point.getY());
		path.getElements().add(moveTo);
		for(int i = points.length - 2; i >= 0; i--)
		{
			point = points[i];
			LineTo lineTo = new LineTo(point.getX(), point.getY());
			path.getElements().add(lineTo);
		}
		return path;
	}
	
	@Override
	public Line getConnectionPoints(Edge pEdge)
	{
		Point[] points = getPoints(pEdge);
		assert points.length >= 2;
		return new Line(points[0], points[points.length-1]);
	}
	
	private ArrowHeadView getArrowHeadView(CallEdge pEdge)
	{
		if(pEdge.isSignal())
		{
			return ArrowHead.HALF_V.view();
		}
		else
		{
			return ArrowHead.V.view();
		}
	}
	
	@Override
	public Rectangle getBounds(Edge pEdge)
	{
		Rectangle bounds = super.getBounds(pEdge);
		Line connectionPoints = getConnectionPoints(pEdge);
		bounds = bounds.add(Conversions.toRectangle(getArrowHeadView((CallEdge)pEdge).getPath(connectionPoints.getPoint1(), 
					connectionPoints.getPoint2()).getBoundsInLocal()));
		return bounds;
	}

	@Override
	public void draw(Edge pEdge, GraphicsContext pGraphics)
	{
		ToolGraphics.strokeSharpPath(pGraphics, (Path) getShape(pEdge), LineStyle.SOLID);
		
		Point[] points = getPoints(pEdge); // TODO already called by getShape(), find a way to avoid having to do 2 calls.
		getArrowHeadView((CallEdge)pEdge).draw(pGraphics, points[points.length - 2], points[points.length - 1]);
		String label = ((CallEdge)pEdge).getMiddleLabel();
		if( label.length() > 0 )
		{
			drawLabel(pEdge, pGraphics, label);
		}
	}

	private void drawLabel(Edge pEdge, GraphicsContext pGraphics, String pLabel)
	{
		if( ((CallEdge)pEdge).isSelfEdge() )
		{
			Point[] points = getPoints(pEdge);
			Rectangle bounds = new Rectangle(points[1].getX(), points[1].getY() + SHIFT/2, 0 , 0);
			LEFT_JUSTIFIED_STRING_VIEWER.draw(pLabel, pGraphics, bounds);
		}
		else
		{
			CENTERED_STRING_VIEWER.draw(pLabel, pGraphics, getConnectionPoints(pEdge).spanning().translated(0, SHIFT));
		}
	}
	
	/* Gets the points on a segmented path */ 
	private Point[] getPoints(Edge pEdge)
	{
		ArrayList<Point> points = new ArrayList<>();
		Rectangle start = pEdge.getStart().view().getBounds();
		Rectangle end = pEdge.getEnd().view().getBounds();
      
		if( ((CallEdge)pEdge).isSelfEdge() )
		{
			Point p = new Point(start.getMaxX(), end.getY() - CallNode.CALL_YGAP / 2);
			Point q = new Point(end.getMaxX(), end.getY());
			Point s = new Point(q.getX() + end.getWidth(), q.getY());
			Point r = new Point(s.getX(), p.getY());

			points.add(p);
			points.add(r);
			points.add(s);
			points.add(q);
		}
		else     
		{
			Direction direction = new Direction(start.getX() - end.getX(), 0);
			Point endPoint = pEdge.getEnd().view().getConnectionPoint(direction);
         
			if(start.getCenter().getX() < endPoint.getX())
			{
				points.add(new Point(start.getMaxX(), endPoint.getY()));
			}
			else
			{
				points.add(new Point(start.getX(), endPoint.getY()));
			}
			points.add(endPoint);
		}
		return points.toArray(new Point[points.size()]);
	}
	
	@Override
	public Canvas createIcon(Edge pEdge)
	{
		final float scale = 0.6f;
		final int offset = 15;
		Canvas canvas = new Canvas(BUTTON_SIZE, BUTTON_SIZE);
		GraphicsContext graphics = canvas.getGraphicsContext2D();
		canvas.getGraphicsContext2D().scale(scale, scale);
		Path path = new Path();
		path.getElements().addAll(new MoveTo(1, offset), new LineTo(BUTTON_SIZE*(1/scale)-1, offset));
		ToolGraphics.strokeSharpPath(graphics, path, LineStyle.SOLID);
		ArrowHead.V.view().draw(graphics, new Point(1, offset), new Point((int)(BUTTON_SIZE*(1/scale)-1), offset));
		return canvas;
	}
}