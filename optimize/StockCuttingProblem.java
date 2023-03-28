package greed;

import java.util.ArrayList;

/**
 * Sure, here's a modified version of the code that uses Minkowski difference to
 * solve the no-fit polygon problem in two-dimensional irregular stock cutting:
 */

public class StockCuttingProblem {

	private static class Vector {
		double x;
		double y;

		public Vector(double x, double y) {
			this.x = x;
			this.y = y;
		}

		public Vector add(Vector v) {
			return new Vector(this.x + v.x, this.y + v.y);
		}

		public Vector subtract(Vector v) {
			return new Vector(this.x - v.x, this.y - v.y);
		}

		public Vector multiply(double scalar) {
			return new Vector(this.x * scalar, this.y * scalar);
		}

		public double dotProduct(Vector v) {
			return this.x * v.x + this.y * v.y;
		}

		public double crossProduct(Vector v) {
			return this.x * v.y - this.y * v.x;
		}

		public double magnitude() {
			return Math.sqrt(this.x * this.x + this.y * this.y);
		}

		public Vector normalize() {
			double mag = this.magnitude();
			return new Vector(this.x / mag, this.y / mag);
		}
	}

	private static class Polygon {
		ArrayList<Vector> vertices;

		public Polygon(ArrayList<Vector> vertices) {
			this.vertices = vertices;
		}

		public Polygon translate(Vector v) {
			ArrayList<Vector> newVertices = new ArrayList<>();
			for (Vector vertex : this.vertices) {
				newVertices.add(vertex.add(v));
			}
			return new Polygon(newVertices);
		}

		public Polygon rotate(double angle) {
			ArrayList<Vector> newVertices = new ArrayList<>();
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			for (Vector vertex : this.vertices) {
				double x = vertex.x * cos - vertex.y * sin;
				double y = vertex.x * sin + vertex.y * cos;
				newVertices.add(new Vector(x, y));
			}
			return new Polygon(newVertices);
		}

		public Polygon minkowskiDifference(Polygon other) {
			ArrayList<Vector> vertices = new ArrayList<>();
			for (Vector v1 : this.vertices) {
				for (Vector v2 : other.vertices) {
					Vector v = v1.subtract(v2);
					vertices.add(v);
				}
			}
			return new Polygon(vertices);
		}

		public double calculateArea() {
			double area = 0;
			int n = this.vertices.size();
			for (int i = 0; i < n; i++) {
				Vector v1 = this.vertices.get(i);
				Vector v2 = this.vertices.get((i + 1) % n);
				area += v1.crossProduct(v2);
			}
			return Math.abs(area) / 2.0;
		}
	}

	public static void main(String[] args) {

		// define the polygons for the stock and the objects to cut
		ArrayList<Vector> stockVertices = new ArrayList<>();
		stockVertices.add(new Vector(0, 0));
		stockVertices.add(new Vector(50, 0));
		stockVertices.add(new Vector(50, 30));
		stockVertices.add(new Vector(30, 50));
		stockVertices.add(new Vector(0, 50));
		Polygon stock = new Polygon(stockVertices);

		ArrayList<Vector> objectVertices = new ArrayList<>();
		objectVertices.add(new Vector(0, 0));
		objectVertices.add(new Vector(20, 0));
		objectVertices.add(new Vector(20, 10));
		Vector translationVector = new Vector(10, 10);
		Polygon object = new Polygon(objectVertices).translate(translationVector);

		// calculate the no-fit polygon
		Polygon noFitPolygon = stock.minkowskiDifference(object);
		noFitPolygon = noFitPolygon.translate(translationVector.multiply(-1));
		noFitPolygon = noFitPolygon.rotate(Math.PI / 2.0);
		double noFitPolygonArea = noFitPolygon.calculateArea();

		System.out.println("No-fit polygon area: " + noFitPolygonArea);
	}
}