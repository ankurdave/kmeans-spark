object LocalKMeans {
  def main(args: Array[String]) {
    val points = Array.fill(1000000) { Point.random }
    val centroids = Array.fill(3) { Point.random }
    val resultCentroids = kmeans(points, centroids, 0.1)
    println(resultCentroids)
  }

  def kmeans(points: Seq[Point], centroids: Seq[Point], epsilon: Double): Seq[Point] = {
	// Group points by the nearest centroids
    val pointGroups = points.groupBy(
	  point => centroids.reduceLeft(
		(a, b) =>
		  if ((point distance a) < (point distance b))
			a
		  else
			b))

	// Calculate new centroids as the average of the points in their cluster
    val newCentroids = pointGroups.map({
      case (centroid, pts) => pts.reduceLeft(_ + _) / pts.length}).toSeq

	// Calculate the centroid movement
    val movement = (pointGroups zip newCentroids).map({
      case ((oldCentroid, pts), newCentroid) => oldCentroid distance newCentroid
    })

    println(newCentroids)

	// Repeat if movement exceeds threshold
    if (movement.exists(_ > epsilon))
      kmeans(points, newCentroids, epsilon)
    else
      return newCentroids
  }

}

class Point(my_x: Double, my_y: Double) {
  val x = my_x
  val y = my_y

  def + (that: Point) = new Point(this.x + that.x, this.y + that.y)
  def - (that: Point) = this + (-that)
  def unary_- () = new Point(-this.x, -this.y)
  def / (d: Double) = new Point(this.x / d, this.y / d)
  def magnitude = math.sqrt(x * x + y * y)
  def distance(that: Point) = (that - this).magnitude
  override def toString = format("(%.2f,%.2f)", x, y)
}

object Point {
  def random() = {
    new Point(
      math.random * 50,
      math.random * 50)
  }
}
