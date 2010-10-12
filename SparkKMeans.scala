import scala.io.Source
import spark.SparkContext

object SparkKMeans {
  def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println("Usage: SparkKMeans <pointsFile> <numClusters> <host> <slices>")
      System.exit(-1)
    }

    val sc = new SparkContext(args(2), "SparkKMeans")

    // Parse the points from a file into an array
    // TODO: Use an HDFS file
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
    val resultCentroids = kmeans(points, centroids, 0.1, sc, args(3).toInt)

    System.err.println("Final centroids: ")
    println(resultCentroids.map(centroid => "%3f\t%3f\n".format(centroid.x, centroid.y)).mkString)
  }

  def kmeans(points: spark.MappedRDD[Point,String], centroids: Seq[Point], epsilon: Double, sc: SparkContext, slices: Int): Seq[Point] = {
    // Partition work
    val partitions = sc.parallelize(points.grouped(points.length / slices).toSeq, slices).cache

    // Assign points to centroids and compute partial sums (on the workers), then merge the partial sums into one sum per centroid
    val sums = points.map(
      pointPartition => pointPartition.groupBy(KMeansHelper.closestCentroid(centroids, _)).mapValues(partialSumOfPoints)
    ).reduce((mapA, mapB) => mergeMaps(List(mapA, mapB)) {
      case ((pointTotal1, numPoints1), (pointTotal2, numPoints2)) =>
        (pointTotal1 + pointTotal2, numPoints1 + numPoints2)
    })

    // Recalculate centroids as the average of the points in their cluster
    // (or leave them alone if they don't have any points in their cluster)
    val newCentroids = centroids.map(oldCentroid => {
      sums.get(oldCentroid) match {
        case Some((pointTotal, numPoints)) => pointTotal / numPoints
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
      kmeans(points, newCentroids, epsilon, sc, slices)
    else
      return newCentroids
  }

  /**
   * Merges a list of Maps into a single Map, using the mergeValues function to resolve key conflicts.
   */
  def mergeMaps[A, B](maps: Seq[Map[A, B]])(mergeValues: (B, B) => B): Map[A, B] = {
    val allKeyValuePairs = for (map <- maps; keyValuePair <- map) yield keyValuePair
    allKeyValuePairs.foldLeft(Map[A, B]()) {
      (soFar, keyValuePair) => keyValuePair match {
        case (key, value) => {
          soFar + (
            if (soFar.contains(key))
              key -> mergeValues(soFar(key), value)
            else
              key -> value
          )
        }
      }
    }
  }

  /**
   * Computes a partial sum of a list of points, in the form of a 2-tuple with the point sum and the number of points summed.
   */
  def partialSumOfPoints(points: Seq[Point]) = (points.reduceLeft(_ + _), points.length)
}
