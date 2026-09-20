package caroai;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
public class AdvantageReview {
 public static void main(String[] args)throws Exception{
  var engine=new GameEngine(new Player("X","X",false),new Player("O","O",false));
  engine.makeMove(new Coord(2,2),GameEngine.Cell.X);engine.makeMove(new Coord(3,2),GameEngine.Cell.X);
  var panel=new AdvantagePanel();panel.setSize(204,94);panel.updatePosition(engine);
  var image=new BufferedImage(244,134,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(Theme.SURFACE);g.fillRect(0,0,244,134);g.translate(20,20);panel.paint(g);g.dispose();
  ImageIO.write(image,"png",new File("target/appearance-review/advantage.png"));
 }
}
