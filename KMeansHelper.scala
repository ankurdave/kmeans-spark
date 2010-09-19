object KMeansHelper {
  /**
   * Finds the closest centroid to the given point.
   */
  def closestCentroid(centroids: Seq[Point], point: Point) = {
    centroids.reduceLeft((a, b) => if ((point distance a) < (point distance b)) a else b)
  }
}
