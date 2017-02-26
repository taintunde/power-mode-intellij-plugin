package de.ax.powermode.power.element

import java.awt.image.BufferedImage
import java.awt.{AlphaComposite, Font, Graphics, Graphics2D}

import com.intellij.openapi.editor.Editor
import de.ax.powermode.power.ElementOfPower
import de.ax.powermode.{PowerMode, Util}

import scala.collection.mutable

/**
  * Created by nyxos on 25.02.17.
  */

object PowerIndicator {
  var indicators = mutable.Queue.empty[PowerIndicator]

  def addIndicator(i: PowerIndicator): Unit = {
    this.synchronized {
      PowerIndicator.indicators += i
      PowerIndicator.indicators = PowerIndicator.indicators.filter(i => i.alive)
    }
  }
}

case class PowerIndicator(_x: Float, _y: Float, _width: Float, _height: Float, initLife: Long, editor: Editor) extends ElementOfPower {
  val identifier = System.currentTimeMillis() + (Math.random() * 1000000)
  var diffLife = Option.empty[Long]
  var x: Double = _x
  var y: Double = _y
  var width: Double = 0
  var height: Double = 0
  PowerIndicator.addIndicator(this)
  val life2 = System.currentTimeMillis() + initLife

  override def life = {

    if (isLast) {
      math.max(life2, System.currentTimeMillis() + (initLife / 2))
    } else {
      diffLife = Some(diffLife.getOrElse(System.currentTimeMillis() + (initLife / 2)))
      diffLife.get
    }
  }


  def isLast: Boolean = {
    PowerIndicator.indicators.lastOption.exists(_.identifier == identifier)
  }

  //
  //  override def lifeFactor: Float = {
  //    if(isLast) {
  //      math.max(super.lifeFactor,0.5)
  //    } else{
  //      super.lifeFactor
  //    }
  //
  //  }

  override def update(delta: Float): Boolean = {
    if (alive) {
      x = _x + (0.5 * _width) - (0.5 * _width * (1 - lifeFactor))
      y = _y + (0.5 * _height) - (0.5 * _height * (1 - lifeFactor))
      width = _width * (1 - lifeFactor)
      height = _height * (1 - lifeFactor)
    }
    !alive
  }

  var lastScrollPosition = Option.empty[(Int, Int)]

  override def render(g: Graphics, _dxx: Int, _dyy: Int): Unit = {
    def limit(v: Int, max: Int): Int = {
      //      if (v > max) max else if (v < -max) -max else v
      v
    }

    if (alive) {
      val Some((dxx, dyy)) = lastScrollPosition.map(lp => {
        val (nx, ny) = (editor.getScrollingModel.getHorizontalScrollOffset, editor.getScrollingModel.getVerticalScrollOffset)
        (lp._1 - nx, lp._2 - ny)
      }).orElse(Some(0, 0)).map { case (x, y) => (limit(x, 100), limit(y, 100)) }
      //val Some((dxx, dyy)) =Some((0,0))
      val g2d: Graphics2D = g.create.asInstanceOf[Graphics2D]
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
        Util.alpha(0.9f * lifeFactor)))
//      println(s"${this.identifier} alife $alive last $isLast $x $y $width $height #### ${_width} ${_height}")

      val bufferedImage = new BufferedImage(Util.powerBamImage.getWidth, Util.powerBamImage.getHeight, BufferedImage.TYPE_INT_ARGB)
      val graphics = bufferedImage.getGraphics
      graphics.drawImage(Util.powerBamImage, 0, 0, null)
      graphics.setFont(new Font("Dialog", Font.PLAIN, 100))
      graphics.drawString((PowerMode.getInstance.rawValueFactor * 100).toInt.toString, 200, 100)
      g2d.drawImage(bufferedImage, math.max(x, 0) - dxx toInt, math.max(y, 0) - dyy toInt, width toInt, height toInt, null)
      g2d.setFont(new Font("Dialog", Font.PLAIN, 50))
      g2d.dispose()
      lastScrollPosition = Some((editor.getScrollingModel.getHorizontalScrollOffset, editor.getScrollingModel.getVerticalScrollOffset))
    }
  }
}
