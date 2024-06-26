import kotlin.math.pow
import kotlin.math.sqrt

data class Point(val x: Double, val y: Double)

fun trilateration(P1: Point, P2: Point, P3: Point, R1: Double, R2: Double, R3: Double): Point? {
    val disP1P2 = distance(P1, P2);
    val ex = Point(
        (P2.x - P1.x) / disP1P2,
        (P2.y - P1.y) / disP1P2
    )
    val i = dotProduct(ex, Point(P3.x - P1.x, P3.y - P1.y))
    val ey = Point(
        (P3.x - P1.x - i * ex.x) / distance(Point(P3.x - P1.x - i * ex.x, P3.y - P1.y - i * ex.y), Point(0.0, 0.0)),
        (P3.y - P1.y - i * ex.y) / distance(Point(P3.x - P1.x - i * ex.x, P3.y - P1.y - i * ex.y), Point(0.0, 0.0))
    )
    val d = distance(P1, P2)
    val j = dotProduct(ey, Point(P3.x - P1.x, P3.y - P1.y))

    val x = (R1.pow(2) - R2.pow(2) + d.pow(2)) / (2 * d)
    val y = (R1.pow(2) - R3.pow(2) + i.pow(2) + j.pow(2)) / (2 * j) - (i / j) * x

    val receptorX = P1.x + x * ex.x + y * ey.x
    val receptorY = P1.y + x * ex.y + y * ey.y

    return Point(receptorX, receptorY)
}

fun distance(P1: Point, P2: Point): Double {
    return sqrt((P2.x - P1.x).pow(2) + (P2.y - P1.y).pow(2))
}

fun dotProduct(P1: Point, P2: Point): Double {
    return P1.x * P2.x + P1.y * P2.y
}

// Ejemplo de uso:
fun main() {
    val P1 = Point(800.0, 350.0)
    val P2 = Point(0.0, 700.0)
    val P3 = Point(800.0, 700.0)
    val R1 = 400.0
    val R2 = 500.0
    val R3 = 500.0
//    (Point(x=800.0, y=350.0), 2.6887192376492446) - (Point(x=0.0, y=700.0), 6.019292593000714) - (Point(x=800.0, y=700.0), 6.648555106100166)
    val receptorPosition = trilateration(P1, P2, P3, R1, R2, R3)
    if (receptorPosition != null) {
        println("Receiver located at: (${receptorPosition.x}, ${receptorPosition.y})")
    } else {
        println("Receiver could not be located.")
    }
}