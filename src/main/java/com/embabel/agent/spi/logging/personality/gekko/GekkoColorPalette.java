package com.embabel.agent.spi.logging.personality.gekko;


import com.embabel.agent.spi.logging.ColorPalette;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("gekko")
public class GekkoColorPalette implements ColorPalette {

    public static final int PROFIT_GREEN = 0x00C853;
    public static final int TERMINAL_GREEN = 0x00FF00;

    @Override
    public int getHighlight() {
        return PROFIT_GREEN;
    }

    @Override
    public int getColor2() {
        return TERMINAL_GREEN;
    }
}
