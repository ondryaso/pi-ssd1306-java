package eu.ondryaso.ssd1306.examples;

import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiFactory;
import eu.ondryaso.ssd1306.Display;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;

public class BasicGraphics {
    public static void main(String[] args) throws IOException {
        Display disp = new Display(128, 64, GpioFactory.getInstance(),
                SpiFactory.getInstance(SpiChannel.CS1, 8000000), RaspiPin.GPIO_04);
        // Create 128x64 display on CE1 (change to SpiChannel.CS0 for using CE0) with D/C pin on WiringPi pin 04

        disp.begin();
        // Init the display

        Image i = ImageIO.read(BasicGraphics.class.getResourceAsStream("lord.png"));
        disp.getGraphics().setColor(Color.WHITE);
        disp.getGraphics().drawImage(i, 0, 0, null);
        disp.getGraphics().setFont(new Font("Monospaced", Font.PLAIN, 10));
        disp.getGraphics().drawString("Praise him", 64, 60);
        disp.getGraphics().drawRect(0, 0, disp.getWidth() - 1, disp.getHeight() - 1);
        // Deal with the image using AWT

        disp.displayImage();
        // Copy AWT image to an inner buffer and send to the display

        for(int x = 70; x < 90; x += 2) {
            for(int y = 10; y < 30; y += 2) {
                disp.setPixel(x, y, true);
            }
        }
        // Set some pixels in the buffer manually

        disp.display();
        // Send the buffer to the display again, now with the modified pixels
    }
}
