package eu.ondryaso.ssd1306;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.wiringpi.I2C;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.lang.reflect.Field;

public class Display {
    private int width, height, pages;
    private boolean usingI2C;

    private GpioPinDigitalOutput rstPin, dcPin;
    private I2CDevice i2c;
    private SpiDevice spi;

    private int fd;
    protected int vccState;

    private byte[] buffer;
    protected BufferedImage img;
    protected Graphics2D graphics;

    public Display(int width, int height, GpioController gpio, SpiDevice spi, Pin rstPin, Pin dcPin) {
        this(width, height, false, gpio, rstPin);

        this.dcPin = gpio.provisionDigitalOutputPin(dcPin);
        this.spi = spi;
    }

    public Display(int width, int height, GpioController gpio, I2CBus i2c, int address, Pin rstPin) throws IOException {
        this(width, height, true, gpio, rstPin);

        this.i2c = i2c.getDevice(address);

        try {
            Field f = this.i2c.getClass().getDeclaredField("fd");
            f.setAccessible(true);
            this.fd = f.getInt(this.i2c);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private Display(int width, int height, boolean i2c, GpioController gpio, Pin rstPin) {
        this.width = (int) width;
        this.height = (int) height;
        this.pages = (int) (height / 8);
        this.buffer = new byte[width * this.pages];
        this.usingI2C = i2c;

        this.rstPin = gpio.provisionDigitalOutputPin(rstPin);

        this.img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        this.graphics = this.img.createGraphics();
    }

    private void initDisplay() {
        if(this.width == 128 && this.height == 64) {
            this.init(0x3F, 0x12, 0x80);
        } else if(this.width == 128 && this.height == 32) {
            this.init(0x1F, 0x02, 0x80);
        } else if(this.width == 96 && this.height == 16) {
            this.init(0x0F, 0x02, 0x60);
        }

    }

    private void init(int multiplex, int compins, int ratio) {
            this.command(Constants.SSD1306_DISPLAYOFF);
            this.command(Constants.SSD1306_SETDISPLAYCLOCKDIV);
            this.command((short) ratio);
            this.command(Constants.SSD1306_SETMULTIPLEX);
            this.command((short) multiplex);
            this.command(Constants.SSD1306_SETDISPLAYOFFSET);
            this.command((short) 0x0);
            this.command((short) (Constants.SSD1306_SETSTARTLINE | 0x0));
            this.command(Constants.SSD1306_CHARGEPUMP);

            if (this.vccState == Constants.SSD1306_EXTERNALVCC)
                this.command((short) 0x10);
            else
                this.command((short) 0x14);

            this.command(Constants.SSD1306_MEMORYMODE);
            this.command((short) 0x00);
            this.command((short)(Constants.SSD1306_SEGREMAP | 0x1));
            this.command(Constants.SSD1306_COMSCANDEC);
            this.command(Constants.SSD1306_SETCOMPINS);
            this.command((short) compins);
            this.command(Constants.SSD1306_SETCONTRAST);

            if (this.vccState == Constants.SSD1306_EXTERNALVCC)
                this.command((short) 0x9F);
            else
                this.command((short) 0xCF);

            this.command(Constants.SSD1306_SETPRECHARGE);

            if (this.vccState == Constants.SSD1306_EXTERNALVCC)
                this.command((short) 0x22);
            else
                this.command((short) 0xF1);

            this.command(Constants.SSD1306_SETVCOMDETECT);
            this.command((short) 0x40);
            this.command(Constants.SSD1306_DISPLAYALLON_RESUME);
            this.command(Constants.SSD1306_NORMALDISPLAY);
    }

    public void command(int command) {
        if (this.usingI2C) {
            this.i2cWrite(0, command);
        } else {
            this.dcPin.setState(false);
            try {
                this.spi.write((short) command);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void data(int data) {
        if (this.usingI2C) {
            this.i2cWrite(0x40, data);
        } else {
            this.dcPin.setState(true);
            try {
                this.spi.write((short) data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void data(byte[] data) {
        if (this.usingI2C) {
            for(int i = 0; i < data.length; i += 16) {
                this.i2cWrite(0x40, data[i]);
            }
        } else {
            this.dcPin.setState(true);
            try {
                this.spi.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void begin() {
        this.begin(Constants.SSD1306_SWITCHCAPVCC);
    }

    public void begin(int vccState) {
        this.vccState = vccState;
        this.reset();
        this.initDisplay();
        this.command(Constants.SSD1306_DISPLAYON);
        this.clear();
        this.display();
    }

    public void reset() {
        try {
            this.rstPin.setState(true);
            Thread.sleep(1);
            this.rstPin.setState(false);
            Thread.sleep(10);
            this.rstPin.setState(true);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void display() {
        this.command(Constants.SSD1306_COLUMNADDR);
        this.command(0);
        this.command(this.width - 1);
        this.command(Constants.SSD1306_PAGEADDR);
        this.command(0);
        this.command(this.pages - 1);

        this.data(this.buffer);
    }

    public void clear() {
        this.buffer = new byte[this.width * this.pages];
    }

    public void setContrast(byte contrast) {
        this.command(Constants.SSD1306_SETCONTRAST);
        this.command(contrast);
    }

    public void dim(boolean dim) {
        if(dim) {
            this.setContrast((byte) 0);
        } else {
            if(this.vccState == Constants.SSD1306_EXTERNALVCC) {
                this.setContrast((byte) 0x9F);
            } else {
                this.setContrast((byte) 0xCF);
            }
        }
    }

    public void invertDisplay(boolean invert) {
        if(invert) {
            this.command(Constants.SSD1306_INVERTDISPLAY);
        } else {
            this.command(Constants.SSD1306_NORMALDISPLAY);
        }
    }

    public void scrollHorizontally(boolean left, int start, int end) {
        this.command(left ? Constants.SSD1306_LEFT_HORIZONTAL_SCROLL : Constants.SSD1306_RIGHT_HORIZONTAL_SCROLL);
        this.command(0);
        this.command(start);
        this.command(0);
        this.command(end);
        this.command(0);
        this.command(0xFF);
        this.command(Constants.SSD1306_ACTIVATE_SCROLL);
    }

    public void scrollDiagonally(boolean left, int start, int end) {
        this.command(Constants.SSD1306_SET_VERTICAL_SCROLL_AREA);
        this.command(0);
        this.command(this.height);
        this.command(left ? Constants.SSD1306_VERTICAL_AND_LEFT_HORIZONTAL_SCROLL :
                Constants.SSD1306_VERTICAL_AND_RIGHT_HORIZONTAL_SCROLL);
        this.command(0);
        this.command(start);
        this.command(0);
        this.command(end);
        this.command(1);
        this.command(Constants.SSD1306_ACTIVATE_SCROLL);
    }

    public void stopScroll() {
        this.command(Constants.SSD1306_DEACTIVATE_SCROLL);
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public boolean setPixel(int x, int y, boolean white) {
        if (x < 0 || x > this.width || y < 0 || y > this.height) {
            return false;
        }

        if (white) {
            this.buffer[x + (y / 8) * this.width] |= (1 << (y & 7));
        } else {
            this.buffer[x + (y / 8) * this.width] &= ~(1 << (y & 7));
        }

        return true;
    }

    public void displayImage() {
        Raster r = this.img.getRaster();

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                this.setPixel(x, y, (r.getSample(x, y, 0) > 0));
            }
        }

        this.display();
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    public void setBufferByte(int position, byte value) {
        this.buffer[position] = value;
    }

    public void setImage(BufferedImage img, boolean createGraphics) {
        this.img = img;

        if (createGraphics) {
            this.graphics = img.createGraphics();
        }
    }

    public BufferedImage getImage() {
        return this.img;
    }

    public Graphics2D getGraphics() {
        return this.graphics;
    }

    private void i2cWrite(int register, int value) {
        value &= 0xFF;
        I2C.wiringPiI2CWriteReg8(this.fd, register, value);
    }
}
