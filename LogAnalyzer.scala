import java.io.File
import scala.io.Source
import scala.util.matching.Regex
import scala.collection.immutable.SortedMap

object LogAnalyzer {
  def main(args: Array[String]) {
    if (args.length < 1) {
      System.err.println("Usage: LogAnalyzer <logDirectory>")
      System.exit(-1)
    }

    val logdir = args(0)

    val LogFileName = """(\d+)-(\d+)\.log""".r
    val TaskTimeEntry = """.*Tasks finished in (\d+.\d+) s""".r

    val times =
      for {
	logfile <- new File(logdir).listFiles
	if !logfile.isDirectory
	LogFileName(manip, trial) = logfile.getName
	src = Source.fromFile(logfile)
	line <- src.getLines.filter(_.matches(TaskTimeEntry.toString)).drop(1).toTraversable
	TaskTimeEntry(timeStr) = line
      } yield (manip.toInt, trial.toInt) -> timeStr.toDouble

    val timesCollected = 
      mergePairsIntoMap((a: List[Double], b: List[Double]) => a ::: b, times.map({case ((manip, trial), time) => manip -> List(time)}))

    for ((manip, times) <- SortedMap[Int, List[Double]]() ++ timesCollected) {
      println("%d, %s".format(manip, times.mkString(", ")))
    }
  }

  /**
   * Merges a list of Maps into a single Map, using the mergeValues function to resolve key conflicts.
   */
  def mergePairsIntoMap[A, B](mergeValues: (B, B) => B, pairs: Seq[Pair[A, B]]): Map[A, B] = {
    pairs.foldLeft(Map[A, B]()) {
      (soFar, pair) => pair match {
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
}
