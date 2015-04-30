package eu.ondryaso.ssd1306;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.wiringpi.I2C;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.lang.reflect.Field;

public class Display {
    protected int vccState;
    protected BufferedImage img;
    protected Graphics2D graphics;
    private int width, height, pages;
    private boolean usingI2C, hasRst;
    private GpioPinDigitalOutput rstPin, dcPin;
    private I2CDevice i2c;
    private SpiDevice spi;
    private int fd;
    private byte[] buffer;

    /**
     * Display object using SPI communication with a reset pin
     *
     * @param width  Display width
     * @param height Display height
     * @param gpio   GPIO object
     * @param spi    SPI device
     * @param rstPin Reset pin
     * @param dcPin  Data/Command pin
     * @see GpioFactory#getInstance() GpioController instance factory
     * @see com.pi4j.io.spi.SpiFactory#getInstance(SpiChannel) SpiDevice factory
     */
    public Display(int width, int height, GpioController gpio, SpiDevice spi, Pin rstPin, Pin dcPin) {
        this(width, height, false, gpio, rstPin);

        this.dcPin = gpio.provisionDigitalOutputPin(dcPin);
        this.spi = spi;
    }

    /**
     * Display object using I2C communication with a reset pin
     * <br/>
     * As I haven't got an I2C display and I don't understand I2C much, I just tried to copy
     * the Adafruit's library and I am using a hack to use WiringPi function similar to one in the original lib directly.
     *
     * @param width   Display width
     * @param height  Display height
     * @param gpio    GPIO object
     * @param i2c     I2C object
     * @param address Display address
     * @param rstPin  Reset pin
     * @see GpioFactory#getInstance() GpioController instance factory
     * @see com.pi4j.io.i2c.I2CFactory#getInstance(int) I2C bus factory
     * @throws ReflectiveOperationException Thrown if I2C handle is not accessible
     * @throws IOException                  Thrown if the bus can't return device for specified address
     */
    public Display(int width, int height, GpioController gpio, I2CBus i2c, int address, Pin rstPin) throws ReflectiveOperationException, IOException {
        this(width, height, true, gpio, rstPin);

        this.i2c = i2c.getDevice(address);

        Field f = this.i2c.getClass().getDeclaredField("fd");
        f.setAccessible(true);
        this.fd = f.getInt(this.i2c);
    }

    /**
     * Display object using SPI communication without a reset pin
     *
     * @param width  Display width
     * @param height Display height
     * @param gpio   GPIO object
     * @param spi    SPI device
     * @param dcPin  Data/Command pin
     * @see Display#Display(int, int, GpioController, SpiDevice, Pin, Pin) Using this constructor with null Pin
     * @see GpioFactory#getInstance() GpioController instance factory
     * @see com.pi4j.io.spi.SpiFactory#getInstance(SpiChannel) SpiDevice factory
     */
    public Display(int width, int height, GpioController gpio, SpiDevice spi, Pin dcPin) {
        this(width, height, gpio, spi, null, dcPin);
    }

    /**
     * Display object using I2C communication without a reset pin
     *
     * @param width   Display width
     * @param height  Display height
     * @param gpio    GPIO object
     * @param i2c     I2C object
     * @param address Display address
     * @see Display#Display(int, int, GpioController, I2CBus, int, Pin) Using this constructor with null Pin
     * @see GpioFactory#getInstance() GpioController instance factory
     * @see com.pi4j.io.i2c.I2CFactory#getInstance(int) I2C bus factory
     */
    public Display(int width, int height, GpioController gpio, I2CBus i2c, int address) throws ReflectiveOperationException, IOException {
        this(width, height, gpio, i2c, address, null);
    }

    private Display(int width, int height, boolean i2c, GpioController gpio, Pin rstPin) {
        this.width = width;
        this.height = height;
        this.pages = (height / 8);
        this.buffer = new byte[width * this.pages];
        this.usingI2C = i2c;

        if (rstPin != null) {
            this.rstPin = gpio.provisionDigitalOutputPin(rstPin);
            this.hasRst = true;
        } else {
            this.hasRst = false;
        }

        this.img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        this.graphics = this.img.createGraphics();
    }

    private void initDisplay() {
        if (this.width == 128 && this.height == 64) {
            this.init(0x3F, 0x12, 0x80);
        } else if (this.width == 128 && this.height == 32) {
            this.init(0x1F, 0x02, 0x80);
        } else if (this.width == 96 && this.height == 16) {
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
        this.command(Constants.SSD1306_SETSTARTLINE);
        this.command(Constants.SSD1306_CHARGEPUMP);

        if (this.vccState == Constants.SSD1306_EXTERNALVCC)
            this.command((short) 0x10);
        else
            this.command((short) 0x14);

        this.command(Constants.SSD1306_MEMORYMODE);
        this.command((short) 0x00);
        this.command((short) (Constants.SSD1306_SEGREMAP | 0x1));
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

    /**
     * Turns on command mode and sends command
     * @param command Command to send. Should be in short range.
     */
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

    /**
     * Turns on data mode and sends data
     * @param data Data to send. Should be in short range.
     */
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

    /**
     * Turns on data mode and sends data array
     * @param data Data array
     */
    public void data(byte[] data) {
        if (this.usingI2C) {
            for (int i = 0; i < data.length; i += 16) {
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

    /**
     * Begin with SWITCHCAPVCC VCC mode
     * @see Constants#SSD1306_SWITCHCAPVCC
     */
    public void begin() {
        this.begin(Constants.SSD1306_SWITCHCAPVCC);
    }

    /**
     * Begin with specified VCC mode (can be SWITCHCAPVCC or EXTERNALVCC)
     * @param vccState VCC mode
     * @see Constants#SSD1306_SWITCHCAPVCC
     * @see Constants#SSD1306_EXTERNALVCC
     */
    public void begin(int vccState) {
        this.vccState = vccState;
        this.reset();
        this.initDisplay();
        this.command(Constants.SSD1306_DISPLAYON);
        this.clear();
        this.display();
    }

    /**
     * Pulls reset pin high and low and resets the display
     */
    public void reset() {
        if (this.hasRst) {
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
    }

    /**
     * Sends the buffer to the display
     */
    public synchronized void display() {
        this.command(Constants.SSD1306_COLUMNADDR);
        this.command(0);
        this.command(this.width - 1);
        this.command(Constants.SSD1306_PAGEADDR);
        this.command(0);
        this.command(this.pages - 1);

        this.data(this.buffer);
    }

    /**
     * Clears the buffer by creating a new byte array
     */
    public void clear() {
        this.buffer = new byte[this.width * this.pages];
    }

    /**
     * Sets the display contract. Apparently not really working.
     * @param contrast Contrast
     */
    public void setContrast(byte contrast) {
        this.command(Constants.SSD1306_SETCONTRAST);
        this.command(contrast);
    }

    /**
     * Sets if the backlight should be dimmed
     * @param dim Dim state
     */
    public void dim(boolean dim) {
        if (dim) {
            this.setContrast((byte) 0);
        } else {
            if (this.vccState == Constants.SSD1306_EXTERNALVCC) {
                this.setContrast((byte) 0x9F);
            } else {
                this.setContrast((byte) 0xCF);
            }
        }
    }

    /**
     * Sets if the display should be inverted
     * @param invert Invert state
     */
    public void invertDisplay(boolean invert) {
        if (invert) {
            this.command(Constants.SSD1306_INVERTDISPLAY);
        } else {
            this.command(Constants.SSD1306_NORMALDISPLAY);
        }
    }

    /**
     * Probably broken
     */
    public void scrollHorizontally(boolean left, int start, int end) {
        this.command(left ? Constants.SSD1306_LEFT_HORIZONTAL_SCROLL : Constants.SSD1306_RIGHT_HORIZONTAL_SCROLL);
        this.command(0);
        this.command(start);
        this.command(0);
        this.command(end);
        this.command(1);
        this.command(0xFF);
        this.command(Constants.SSD1306_ACTIVATE_SCROLL);
    }

    /**
     * Probably broken
     */
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

    /**
     * Stops scrolling
     */
    public void stopScroll() {
        this.command(Constants.SSD1306_DEACTIVATE_SCROLL);
    }

    /**
     * @return Display width
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * @return Display height
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Sets one pixel in the current buffer
     * @param x X position
     * @param y Y position
     * @param white White or black pixel
     * @return True if the pixel was successfully set
     */
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

    /**
     * Copies AWT image contents to buffer. Calls display()
     * @see Display#display()
     */
    public synchronized void displayImage() {
        Raster r = this.img.getRaster();

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                this.setPixel(x, y, (r.getSample(x, y, 0) > 0));
            }
        }

        this.display();
    }

    /**
     * Sets internal buffer
     * @param buffer New used buffer
     */
    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    /**
     * Sets one byte in the buffer
     * @param position Position to set
     * @param value Value to set
     */
    public void setBufferByte(int position, byte value) {
        this.buffer[position] = value;
    }

    /**
     * Sets internal AWT image to specified one.
     * @param img BufferedImage to set
     * @param createGraphics If true, createGraphics() will be called on the image and the result will be saved
     *                       to the internal Graphics field accessible by getGraphics() method
     */
    public void setImage(BufferedImage img, boolean createGraphics) {
        this.img = img;

        if (createGraphics) {
            this.graphics = img.createGraphics();
        }
    }

    /**
     * Returns internal AWT image
     * @return BufferedImage
     */
    public BufferedImage getImage() {
        return this.img;
    }

    /**
     * Returns Graphics object which is associated to current AWT image,
     * if it wasn't set using setImage() with false createGraphics parameter
     * @return Graphics2D object
     */
    public Graphics2D getGraphics() {
        return this.graphics;
    }

    private void i2cWrite(int register, int value) {
        value &= 0xFF;
        I2C.wiringPiI2CWriteReg8(this.fd, register, value);
    }
}
