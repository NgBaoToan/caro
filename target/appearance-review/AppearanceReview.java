package caroai;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
public class AppearanceReview {
  static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container nested)layout(nested);}
  static void save(JComponent panel,int w,int h,String name)throws Exception{
    panel.setSize(w,h);panel.dispatchEvent(new ComponentEvent(panel,ComponentEvent.COMPONENT_RESIZED));layout(panel);
    BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();
    ImageIO.write(image,"png",new File("target/appearance-review/"+name+".png"));
  }
  public static void main(String[] args)throws Exception{
    SwingUtilities.invokeAndWait(()->{Theme.Mode old=Theme.mode();try{
      for(Theme.Mode mode:Theme.Mode.values()){
        Theme.setMode(mode);
        JPanel settings=AppearanceSettings.createContent((m,s)->{},()->{});
        save(settings,630,740,"settings-"+mode);
        save(new StartMenuPanel(()->{},()->{}),1120,820,"menu-"+mode);
      }
      BufferedImage sheet=new BufferedImage(800,440,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();Theme.antialias(g);
      for(int row=0;row<2;row++){
        boolean dark=row==0;g.setColor(new Color(dark?0x343F4E:0xFFF9E9));g.fillRect(0,row*220,800,220);
        int i=0;for(Theme.StoneStyle style:Theme.StoneStyle.values()){
          g.setColor(dark?Color.WHITE:Color.BLACK);g.setFont(Theme.ui(Font.BOLD,18));g.drawString(style.label(),i*200+32,row*220+40);
          Theme.drawStone(g,true,i*200+60,row*220+115,32,1,style,new Color(dark?0xF1F5FA:0x242A25));
          Theme.drawStone(g,false,i*200+140,row*220+115,32,1,style,new Color(dark?0xF4BB75:0x9A3E32));i++;
        }
      }g.dispose();ImageIO.write(sheet,"png",new File("target/appearance-review/styles.png"));
    }catch(Exception ex){throw new RuntimeException(ex);}finally{Theme.setMode(old);}});
    System.exit(0);
  }
}
