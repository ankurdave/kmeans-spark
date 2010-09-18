import scala.io.Source

object SparkKMeans {
  def main(args: Array[String]) {
    val points = Source.fromFile(args(0)).getLines.toSeq.filter(line => !line.matches("^\\s*#.*")).map(line => {
	  val parts = line.split("\t").map(_.toDouble)
	  new Point(parts(0), parts(1))}).toArray

	println("Got " + points.length + " points.")

    val centroids = Array.fill(args(1).toInt) { Point.random }
    val resultCentroids = kmeans(points, centroids, 0.1, new spark.SparkContext("local", "SparkKMeans"))
    println(resultCentroids)
  }

  def kmeans(points: Seq[Point], centroids: Seq[Point], epsilon: Double, spark: SparkContext): Seq[Point] = {
	val assignedPoints = spark.parallelize(points, 10).map(point => (point, centroids.reduceLeft(
	  (a, b) =>
		if ((point distance a) < (point distance b))
		  a
		else
		  b))).toArray

	val pointGroups = assignedPoints.groupBy({ case (point, closestCentroid) => closestCentroid }).mapValues({ case (point, closestCentroid) => point })

	// Calculate new centroids as the average of the points in their cluster
	// (or leave them alone if they don't have any points in their cluster)
    val newCentroids = centroids.map(oldCentroid => {
	  val closestPoints = pointGroups.getOrElse(oldCentroid, List())
	  if (closestPoints.length > 0)
		closestPoints.reduceLeft(_ + _) / closestPoints.length
	  else
		oldCentroid
	})

	// Calculate the centroid movement
    val movement = (centroids zip newCentroids).map({ case (a, b) => a distance b })

    println(newCentroids + ", delta: " + movement.map(d => "%3f".format(d)).mkString("(", ", ", ")"))

	// Repeat if movement exceeds threshold
    if (movement.exists(_ > epsilon))
      kmeans(points, newCentroids, epsilon)
    else
      return newCentroids
  }

}
