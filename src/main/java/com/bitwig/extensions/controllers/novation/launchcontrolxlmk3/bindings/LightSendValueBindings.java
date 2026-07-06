package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchLight;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.GradientColor;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.RgbColor;
import com.bitwig.extensions.framework.Binding;

public class LightSendValueBindings extends Binding<Parameter, LaunchLight> {

    // Ring color per send bus, chosen by absolute send index (scroll position + row).
    // Reorder or extend to change the palette.
    private static final GradientColor[] SEND_GRADIENTS = {
        GradientColor.WHITE,   // Send 1
        GradientColor.BLUE,    // Send 2
        GradientColor.RED,     // Send 3
        GradientColor.YELLOW,  // Send 4
        GradientColor.PURPLE   // Send 5
    };

    private final int itemIndex;
    private GradientColor gradient;
    protected int value;
    protected boolean exists;

    public LightSendValueBindings(final Send parameter, final LaunchLight target, final SendBank sendBank,
        final int itemIndex) {
        super(parameter, parameter, target);
        this.itemIndex = itemIndex;

        sendBank.scrollPosition().markInterested();
        sendBank.scrollPosition().addValueObserver(this::handleScroll);
        this.gradient = gradientFor(sendBank.scrollPosition().get() + itemIndex);

        parameter.value().addValueObserver(gradient.length(), this::handleValue);
        this.value = (int) (parameter.value().get() * (gradient.length() - 1));
        parameter.exists().addValueObserver(this::handleExists);
    }

    private static GradientColor gradientFor(final int absoluteIndex) {
        final int i = Math.max(0, Math.min(absoluteIndex, SEND_GRADIENTS.length - 1));
        return SEND_GRADIENTS[i];
    }

    private void handleScroll(final int scrollPosition) {
        this.gradient = gradientFor(scrollPosition + itemIndex);
        if (isActive()) {
            sendColor();
        }
    }

    private void handleExists(final boolean exists) {
        this.exists = exists;
        if (isActive()) {
            sendColor();
        }
    }

    private void handleValue(final int value) {
        if (this.value != value) {
            this.value = value;
            if (isActive()) {
                sendColor();
            }
        }
    }

    protected void sendColor() {
        if (!exists) {
            getTarget().sendRgbColor(RgbColor.OFF);
        } else {
            getTarget().sendRgbColor(gradient.getColor(value));
        }
    }

    @Override
    protected void deactivate() {

    }

    @Override
    protected void activate() {
        sendColor();
    }

}
