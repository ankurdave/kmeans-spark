import scala.io.Source

object LocalKMeans {
  def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println("Usage: LocalKMeans <pointsFile> <numClusters>")
      System.exit(-1)
    }

    // Parse the points from a file into an array
    val points = Source.fromFile(args(0)).getLines.toSeq.filter(line => !line.matches("^\\s*#.*")).map(
      line => {
        val parts = line.split("\t").map(_.toDouble)
        new Point(parts(0), parts(1))
      }
    ).toArray
    System.err.println("Read " + points.length + " points.")

    // Initialize k random centroids
    val centroids = Array.fill(args(1).toInt) { Point.random }

    // Start the local run
    val resultCentroids = kmeans(points, centroids, 0.1)

    System.err.println("Final centroids: ")
	println(resultCentroids.map(centroid => "%3f\t%3f\n".format(centroid.x, centroid.y)).mkString)
  }

  def kmeans(points: Seq[Point], centroids: Seq[Point], epsilon: Double): Seq[Point] = {
    // Assign points to centroids
    val clusters = points.groupBy(KMeansHelper.closestCentroid(centroids, _))

    // Recalculate centroids as the average of the points in their cluster
    // (or leave them alone if they don't have any points in their cluster)
    val newCentroids = centroids.map(oldCentroid => {
      clusters.get(oldCentroid) match {
        case Some(pointsInCluster) => pointsInCluster.reduceLeft(_ + _) / pointsInCluster.length
        case None => oldCentroid
      }})

    // Calculate the centroid movement for the stopping condition
    val movement = (centroids zip newCentroids).map({ case (a, b) => a distance b })

    System.err.println("Centroids changed by\n" +
            "\t   " + movement.map(d => "%3f".format(d)).mkString("(", ", ", ")") + "\n" +
            "\tto " + newCentroids.mkString("(", ", ", ")"))

    // Iterate if movement exceeds threshold
    if (movement.exists(_ > epsilon))
      kmeans(points, newCentroids, epsilon)
    else
      return newCentroids
  }
}
