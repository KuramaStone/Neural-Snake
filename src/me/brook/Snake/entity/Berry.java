package me.brook.Snake.entity;

import java.awt.Color;
import java.awt.Point;

public class Berry {
	
	private Point point;
	private Color color;
	
	public Berry(Point point, Color color) {
		this.point = point;
		this.color = color;
	}

	public Point getPoint() {
		return point;
	}

	public Color getColor() {
		return color;
	}

}
