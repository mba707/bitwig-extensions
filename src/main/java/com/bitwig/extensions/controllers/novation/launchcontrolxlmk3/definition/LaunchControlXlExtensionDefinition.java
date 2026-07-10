package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.definition;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlMk3Extension;

public class LaunchControlXlExtensionDefinition extends AbstractLaunchControlExtensionDefinition {
    // Distinct id/name from the bundled extension so Bitwig keeps this fork
    // selected across restarts instead of resolving back to its own copy.
    private static final UUID DRIVER_ID = UUID.fromString("6744077d-4819-47b3-b630-a615880d5095");

    public LaunchControlXlExtensionDefinition() {
    }

    @Override
    public boolean isXlVersion() {
        return true;
    }

    @Override
    public String getName() {
        return "Launch Control XL 3 (Colorblind)";
    }

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    @Override
    public String getHardwareModel() {
        return "Launch Control XL 3";
    }

    @Override
    public String getHelpFilePath() {
        return "Controllers/Novation/Launch Control XL 3.pdf";
    }

    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) {
            list.add(new String[] {"MIDIIN2 (LCXL3 1 MIDI)"}, new String[] {"MIDIOUT2 (LCXL3 1 MIDI)"});
        } else if (platformType == PlatformType.MAC) {
            list.add(new String[] {"LCXL3 1 DAW Out"}, new String[] {"LCXL3 1 DAW In"});
        } else if (platformType == PlatformType.LINUX) {
            list.add(new String[] {"LCXL3 1 LCXL3 1 DAW Out"}, new String[] {"LCXL3 1 LCXL3 1 DAW In"});
        }
    }

    @Override
    public LaunchControlMk3Extension createInstance(final ControllerHost host) {
        return new LaunchControlMk3Extension(this, host);
    }
}
