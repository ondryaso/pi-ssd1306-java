package eu.ondryaso.ssd1306.examples;

import com.pi4j.io.gpio.*;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiFactory;
import eu.ondryaso.ssd1306.Display;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Random;

public class Pong implements Runnable {
    private Display disp;
    private GpioController gpio;

    protected int tps = 20, magicConstant = 1000000000;
    protected double tickTime = 1d / tps;
    protected double tickTimeSec = this.tickTime * this.magicConstant;
    protected long time, lastTime, lastInfo;
    protected int fps, ticks, lastTicks;

    Rectangle p1 = new Rectangle(3, 13);
    Rectangle p2 = new Rectangle(3, 13);

    Rectangle b;
    Rectangle top, bottom;

    float xV = 2f, yV = 1f, p1yV = 1.6f, p2yV = 1.4f, x = 0, y = 0, lastX = 0, lastY = 0;
    float p1y, p1lastY, p2y, p2lastY;
    int centreX, centreY;
    Random rnd = new Random();

    BufferedImage[] numbers = new BufferedImage[10];
    List<Integer> digits = new ArrayList<>();

    int s1, s2;
    GpioPinDigitalInput up, down;
    Pin upp, downp, rst, dc;

    public Pong() { this("GPIO 0", "GPIO 1", "GPIO 3", "GPIO 4", "1.6", "1.4", "2", "1"); }

    public Pong(String pinUp, String pinDown, String pinRst, String pinDc, String p1yV, String p2yV, String bxV, String byV) {
        this.upp = RaspiPin.getPinByName(pinUp);
        this.downp = RaspiPin.getPinByName(pinDown);
        this.rst = RaspiPin.getPinByName(pinRst);
        this.dc = RaspiPin.getPinByName(pinDc);
        this.p1yV = Float.parseFloat(p1yV);
        this.p2yV = Float.parseFloat(p2yV);
        this.xV = Float.parseFloat(bxV);
        this.yV = Float.parseFloat(byV);
    }

    @Override
    public void run() {
        try {
            this.gpio = GpioFactory.getInstance();
            this.disp = new Display(128, 64, this.gpio, SpiFactory.getInstance(SpiChannel.CS1, 8000000),
                    this.rst, this.dc);

            this.up = this.gpio.provisionDigitalInputPin(RaspiPin.GPIO_00, PinPullResistance.PULL_UP);
            this.down = this.gpio.provisionDigitalInputPin(RaspiPin.GPIO_01, PinPullResistance.PULL_UP);

            this.disp.begin();
            this.disp.clear();
            this.disp.display();

            this.centreX = this.disp.getWidth() / 2;
            this.centreY = this.disp.getHeight() / 2;

            this.x = this.centreX;
            this.y = this.centreY;

            this.top = new Rectangle(0, 0, this.disp.getWidth(), 1);
            this.bottom = new Rectangle(0, this.disp.getHeight() - 2, this.disp.getWidth(), 1);
            this.b = new Rectangle(this.centreX, this.centreY, 3, 3);

            this.createNumbers();

            this.time = System.nanoTime();
            this.lastTime = this.time;
            this.lastInfo = this.time;

            while(Pong.run) {
                float ptt = (this.time - this.lastTime) / ((float) this.tickTimeSec);

                this.disp.getGraphics().clearRect(0, 0, this.disp.getWidth(), this.disp.getHeight());
                this.render(ptt);
                this.disp.displayImage();
                this.disp.display();

                this.fps++;
                this.time = System.nanoTime();

                while (time - lastTime >= this.tickTimeSec) {
                    this.update();
                    lastTime += this.tickTimeSec;
                }

                if (time - lastInfo >= this.magicConstant) {
                    lastInfo += this.magicConstant;
                    System.out.println("FPS: " + fps);
                    lastTicks = ticks;
                    fps = 0;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public void update() {
        this.lastX = this.x;
        this.lastY = this.y;
        this.p1lastY = this.p1y;
        this.p2lastY = this.p2y;

        if(this.y <= 2 || this.y >= this.disp.getHeight() - 5) {
            this.yV = -this.yV;
        }

        if(this.x >= (this.disp.getWidth() - 4)) {
            if(this.y >= this.p2y && this.y <= (this.p2y + this.p2.height)) {
                this.xV = -this.xV;
            } else {
                this.lastX = this.centreX;
                this.lastY = this.centreY;
                this.x = this.centreX;
                this.y = this.centreY;
                this.s1++;
            }
        }

        if(this.x <= 2) {
            if(this.y >= this.p1y && this.y <= (this.p1y + this.p1.height)) {
                this.xV = -this.xV;
            } else {
                this.lastX = this.centreX;
                this.lastY = this.centreY;
                this.x = this.centreX;
                this.y = this.centreY;
                this.s2++;
            }
        }

        if(this.up.isLow() && this.p1y > 1) {
            this.p1y -= this.p1yV;
        }

        if(this.down.isLow() && (this.p1y + this.p1.height) < (this.disp.getHeight() - 1)) {
            this.p1y += this.p1yV;
        }

        if(this.p2y <= this.y) {
            this.p2y += this.p2yV;
        }

        if(this.p2y > this.y) {
            this.p2y -= this.p2yV;
        }

        if(this.p2y <= 1) {
            this.p2y = 1;
        }

        if((this.p2y + this.p2.height) >= (this.disp.getHeight() - 1)) {
            this.p2y = (this.disp.getHeight() - this.p2.height - 1);
        }

        this.x += this.xV;
        this.y += this.yV;
    }

    public void render(float ptt) {
        this.drawField();
        this.drawScores();

        this.b.setLocation((int) (this.lastX + (this.x - this.lastX) * ptt),
                (int) (this.lastY + (this.y - this.lastY) * ptt));

        this.p1.setLocation(0, (int) (this.p1lastY + (this.p1y - this.p1lastY) * ptt));
        this.p2.setLocation(this.disp.getWidth() - 3,
                (int)(this.p2lastY + (this.p2y - this.p2lastY) * ptt));

        this.disp.getGraphics().fill(this.p1);
        this.disp.getGraphics().fill(this.p2);
        this.disp.getGraphics().fill(this.b);
    }

    private void drawScores() {
        this.getDigits(this.s2, false);
        int x = this.centreX + 2;
        for(int i : this.digits) {
            this.disp.getGraphics().drawImage(this.numbers[i], x, 3, null);
            x += this.numbers[i].getWidth() + 1;
        }

        this.getDigits(this.s1, true);
        x = this.centreX - 3;
        for(int i : this.digits) {
            x -= this.numbers[i].getWidth() - 1;
            this.disp.getGraphics().drawImage(this.numbers[i], x, 3, null);
        }
    }

    private void drawField() {
        this.disp.getGraphics().draw(this.top);
        this.disp.getGraphics().draw(this.bottom);
        this.disp.getGraphics().drawRect(this.centreX - 1, 0, 1, this.disp.getHeight());
    }

    private void createNumbers() {
        for(int i = 0; i < 10; i++) {
            numbers[i] = new BufferedImage(5, 7, BufferedImage.TYPE_BYTE_BINARY);
        }

        this.createNumber(numbers[0], 5, 7, new int[] {
                1, 1, 1, 1, 1,
                1, 0, 0, 0, 1,
                1, 0, 0, 0, 1,
                1, 0, 0, 0, 1,
                1, 0, 0, 0, 1,
                1, 0, 0, 0, 1,
                1, 1, 1, 1, 1
        });

        this.createNumber(numbers[1], 1, 7, new int[] {1, 1, 1, 1, 1, 1, 1});
        this.createNumber(numbers[2], 5, 7, new int[] {
                1, 1, 1, 1, 1,
                1, 0, 0, 0, 1,
                0, 0, 0, 0, 1,
                1, 1, 1, 1, 1,
                1, 0, 0, 0, 0,
                1, 0, 0, 0, 1,
                1, 1, 1, 1, 1
        });

        this.createNumber(numbers[3], 5, 7, new int[] {
                1, 1, 1, 1, 1,
                1, 0, 0, 0, 1,
                0, 0, 0, 0, 1,
                1, 1, 1, 1, 1,
                0, 0, 0, 0, 1,
                1, 0, 0, 0, 1,
                1, 1, 1, 1, 1
        });

        this.createNumber(numbers[4], 4, 7, new int[] {
                1, 1, 0, 0,
                1, 0, 0, 0,
                1, 0, 1, 0,
                1, 1, 1, 1,
                0, 0, 1, 0,
                0, 0, 1, 0,
                0, 1, 1, 1
        });

        this.createNumber(numbers[5], 5, 7, new int[] {
                1, 1, 1, 1, 1,
                1, 0, 0, 0, 1,
                1, 0, 0, 0, 0,
                1, 1, 1, 1, 1,
                0, 0, 0, 0, 1,
                1, 0, 0, 0, 1,
                1, 1, 1, 1, 1
        });

        this.createNumber(numbers[6], 5, 7, new int[] {
                1, 1, 1, 1, 1,
                1, 0, 0, 0, 1,
                1, 0, 0, 0, 0,
                1, 1, 1, 1, 1,
                1, 0, 0, 0, 1,
                1, 0, 0, 0, 1,
                1, 1, 1, 1, 1
        });

        this.createNumber(numbers[7], 5, 7, new int[]{
                1, 1, 1, 1, 0,
                1, 0, 0, 1, 0,
                0, 0, 0, 1, 0,
                0, 0, 1, 1, 1,
                0, 0, 0, 1, 0,
                0, 0, 0, 1, 0,
                0, 0, 1, 1, 1
        });

        this.createNumber(numbers[8], 5, 7, new int[]{
                1, 1, 1, 1, 1,
                1, 0, 0, 0, 1,
                1, 0, 0, 0, 1,
                1, 1, 1, 1, 1,
                1, 0, 0, 0, 1,
                1, 0, 0, 0, 1,
                1, 1, 1, 1, 1
        });

        this.createNumber(numbers[9], 5, 7, new int[]{
                1, 1, 1, 1, 1,
                1, 0, 0, 0, 1,
                1, 0, 0, 0, 1,
                1, 1, 1, 1, 1,
                0, 0, 0, 0, 1,
                1, 0, 0, 0, 1,
                1, 1, 1, 1, 1
        });
    }

    private void createNumber(BufferedImage img, int w, int h, int[] data) {
        WritableRaster r = img.getRaster();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                r.setSample(x, y, 0, data[x + y * w]);
            }
        }
    }

    public void getDigits(int num, boolean reverse) {
        this.digits.clear();

        if(reverse)
            this.collectDigitsReverse(num, this.digits);
        else
            this.collectDigits(num, this.digits);
    }

    private void collectDigitsReverse(int num, List<Integer> digits) {
        if(num == 0) {
            digits.add(0);
            return;
        }

        while(num > 0) {
            digits.add(num % 10);
            num = num / 10;
        }
    }

    private void collectDigits(int num, List<Integer> digits) {
        if (num / 10 > 0) {
            this.collectDigits(num / 10, digits);
        }

        digits.add(num % 10);
    }

    private static boolean run = true;
    public static void main(String[] args) {
        Thread t = new Thread(args.length == 8 ? new Pong(args[0], args[1],
                args[2], args[3], args[4], args[5], args[6], args[7]) : new Pong());
        t.run();
    }
}
