package eu.ondryaso.ssd1306.examples;

import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiFactory;
import eu.ondryaso.ssd1306.Display;

import java.io.IOException;

public class Pixels {
    public static void main(String[] args) throws IOException {
        Display disp = new Display(128, 64, GpioFactory.getInstance(),
                SpiFactory.getInstance(SpiChannel.CS1, 8000000), RaspiPin.GPIO_03, RaspiPin.GPIO_04);

        disp.begin();

        long last, nano = 0;

        for(int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                disp.setPixel(x, y, true);
                last = System.nanoTime();
                disp.display();
                nano += (System.nanoTime() - last);
            }
        }

        System.out.println("Display lasts " + ((nano / 1000000) / (64 * 64)) + " ms");
    }
}
