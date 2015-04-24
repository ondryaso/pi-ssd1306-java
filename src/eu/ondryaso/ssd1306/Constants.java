package eu.ondryaso.ssd1306;

public class Constants {
    public static final short SSD1306_I2C_ADDRESS = 0x3C;
    public static final short SSD1306_SETCONTRAST = 0x81;
    public static final short SSD1306_DISPLAYALLON_RESUME = 0xA4;
    public static final short SSD1306_DISPLAYALLON = 0xA5;
    public static final short SSD1306_NORMALDISPLAY = 0xA6;
    public static final short SSD1306_INVERTDISPLAY = 0xA7;
    public static final short SSD1306_DISPLAYOFF = 0xAE;
    public static final short SSD1306_DISPLAYON = 0xAF;
    public static final short SSD1306_SETDISPLAYOFFSET = 0xD3;
    public static final short SSD1306_SETCOMPINS = 0xDA;
    public static final short SSD1306_SETVCOMDETECT = 0xDB;
    public static final short SSD1306_SETDISPLAYCLOCKDIV = 0xD5;
    public static final short SSD1306_SETPRECHARGE = 0xD9;
    public static final short SSD1306_SETMULTIPLEX = 0xA8;
    public static final short SSD1306_SETLOWCOLUMN = 0x00;
    public static final short SSD1306_SETHIGHCOLUMN = 0x10;
    public static final short SSD1306_SETSTARTLINE = 0x40;
    public static final short SSD1306_MEMORYMODE = 0x20;
    public static final short SSD1306_COLUMNADDR = 0x21;
    public static final short SSD1306_PAGEADDR = 0x22;
    public static final short SSD1306_COMSCANINC = 0xC0;
    public static final short SSD1306_COMSCANDEC = 0xC8;
    public static final short SSD1306_SEGREMAP = 0xA0;
    public static final short SSD1306_CHARGEPUMP = 0x8D;
    public static final short SSD1306_EXTERNALVCC = 0x1;
    public static final short SSD1306_SWITCHCAPVCC = 0x2;

    public static final short SSD1306_ACTIVATE_SCROLL = 0x2F;
    public static final short SSD1306_DEACTIVATE_SCROLL = 0x2E;
    public static final short SSD1306_SET_VERTICAL_SCROLL_AREA = 0xA3;
    public static final short SSD1306_RIGHT_HORIZONTAL_SCROLL = 0x26;
    public static final short SSD1306_LEFT_HORIZONTAL_SCROLL = 0x27;
    public static final short SSD1306_VERTICAL_AND_RIGHT_HORIZONTAL_SCROLL = 0x29;
    public static final short SSD1306_VERTICAL_AND_LEFT_HORIZONTAL_SCROLL = 0x2A;
}
