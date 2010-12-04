import spark._
import spark.SparkContext._

object SparkKMeans {
  def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println("Usage: SparkKMeans <pointsFile> <numClusters> <host> [<startCentroid1X>,<startCentroid1Y> ...]")
      System.exit(-1)
    }

    val pointsFile = args(0)
    val k = args(1).toInt
    val host = args(2)
    val startCentroids =
      (args
       .slice(3, 3 + 2 * k)
       .map(p => {
	 val ps = p.split(',')
	 Point(ps(0).toDouble, ps(1).toDouble)
       }))

    val sc = new SparkContext(host, "SparkKMeans")

    // Parse the points from a file into an RDD
    val points = sc.textFile(pointsFile).filter(line => !line.matches("^\\s*#.*")).map(
      line => {
        val parts = line.split("\t").map(_.toDouble)
        new Point(parts(0), parts(1))
      }
    ).cache
    System.err.println("Read " + points.count() + " points.")

    // Use the given centroids, or initialize k random ones
    val centroids = 
      if (startCentroids.length == k)
	startCentroids
      else
	Array.fill(k) { Point.random }

    // Start the Spark run
    val resultCentroids = kmeans(points, centroids, 0.1, sc)

    System.err.println("Final centroids: ")
    println(resultCentroids.map(centroid => "%3f\t%3f\n".format(centroid.x, centroid.y)).mkString)
  }

  def kmeans(points: spark.RDD[Point], centroids: Seq[Point], epsilon: Double, sc: SparkContext): Seq[Point] = {
    // Assign points into clusters based on their closest centroids,
    // and take the average of all points in each cluster
    val clusters =
      (points
       .map(point => KMeansHelper.closestCentroid(centroids, point) -> (point, 1))
       .reduceByKeyToDriver({
         case ((ptA, numA), (ptB, numB)) => (ptA + ptB, numA + numB)
       })
       .map({
         case (centroid, (ptSum, numPts)) => centroid -> ptSum / numPts
       }))

    // Recalculate centroids based on their clusters
    // (or leave them alone if they don't have any points in their cluster)
    val newCentroids = centroids.map(oldCentroid => {
      clusters.get(oldCentroid) match {
        case Some(newCentroid) => newCentroid
        case None => oldCentroid
      }
    })

    // Calculate the centroid movement for the stopping condition
    val movement = (centroids zip newCentroids).map({ case (a, b) => a distance b })

    System.err.println("Centroids changed by\n" +
            "\t   " + movement.map(d => "%3f".format(d)).mkString("(", ", ", ")") + "\n" +
            "\tto " + newCentroids.mkString("(", ", ", ")"))

    // Iterate if movement exceeds threshold
    if (movement.exists(_ > epsilon))
      kmeans(points, newCentroids, epsilon, sc)
    else
      return newCentroids
  }
}
