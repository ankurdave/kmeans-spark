import spark._
import spark.SparkContext._

object SparkKMeans {
  def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println("Usage: SparkKMeans <pointsFile> <numClusters> <host>")
      System.exit(-1)
    }

    val sc = new SparkContext(args(2), "SparkKMeans")

    // Parse the points from a file into an RDD
    val points = sc.textFile(args(0)).filter(line => !line.matches("^\\s*#.*")).map(
      line => {
        val parts = line.split("\t").map(_.toDouble)
        new Point(parts(0), parts(1))
      }
    ).cache
    System.err.println("Read " + points.count() + " points.")

    // Initialize k random centroids
    val centroids = Array.fill(args(1).toInt) { Point.random }

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
       .reduceByKey({
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
