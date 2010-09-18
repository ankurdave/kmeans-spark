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
