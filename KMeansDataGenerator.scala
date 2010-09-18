import scala.util.Random

object KMeansDataGenerator {
  def main(args: Array[String]) {
	val numClusters = args(0).toInt
	val desiredCentroids = Array.fill(numClusters) { Point.random }
	desiredCentroids.foreach(p => printf("#%f\t%f\n", p.x, p.y))
	
	val numPoints = args(1).toInt
	val spread = args(2).toDouble
	val random = new Random()
	for (i <- 1 to numPoints) {
	  val point = desiredCentroids(random.nextInt(desiredCentroids.length)) +
	    new Point(random.nextGaussian() * spread, random.nextGaussian() * spread)
	  printf("%f\t%f\n", point.x, point.y)
	}
  }
}
