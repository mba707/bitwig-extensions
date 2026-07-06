package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.CcConstValues;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlMidiProcessor;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlMk3Extension;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlXlHwElements;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchViewControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.AbsoluteEncoderBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.ControlTargetId;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.DisplayId;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.LightSendValueBindings;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.LightValueBindings;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.ParameterDisplayBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.RelativeEncoderBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.SegmentDisplayBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.SliderBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchAbsoluteEncoder;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchButton;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchRelativeEncoder;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.GradientColor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

@Component(tag = "XLModel")
public class XlMixerLayer extends AbstractMixerLayer {
    
    @Inject
    private XlDawControlLayer dawLayer;
    
    
    private Row1ButtonMode row1Mode = Row1ButtonMode.SOLO;
    private Row2ButtonMode row2Mode = Row2ButtonMode.SELECT;

    // Send pages: page 1 = sends 1-3 on the three knob rows,
    // page 2 = sends 4-5 on rows 1-2 with pan on row 3.
    private final Layer sendPage1Layer;
    private final Layer sendPage2Layer;
    private boolean sendPage2Active = false;
    
    private enum Row1ButtonMode {
        SOLO,
        ARM
    }
    
    private enum Row2ButtonMode {
        SELECT,
        MUTE
    }
    
    public XlMixerLayer(final Layers layers, final LaunchControlXlHwElements hwElements,
        final LaunchViewControl viewControl, final DisplayControl displayControl,
        final LaunchControlMidiProcessor midiProcessor, final ControllerHost host,
        final TransportHandler transportHandler, final ButtonLayers buttonLayers) {
        super(layers, midiProcessor, host, viewControl, hwElements, displayControl, transportHandler, buttonLayers);

        sendPage1Layer = new Layer(layers, "SEND_PAGE_1");
        sendPage2Layer = new Layer(layers, "SEND_PAGE_2");

        final TrackBank trackBank = viewControl.getTrackBank();
        for (int i = 0; i < 8; i++) {
            bindTrack(hwElements, trackBank, i);
        }
        
        bindNavigation(hwElements);
        transportHandler.bindTransport(this);
        final LaunchButton soloArmButton = hwElements.getButtons(CcConstValues.SOLO_ARM_MODE);
        final LaunchButton muteSelectButton = hwElements.getButtons(CcConstValues.MUTE_SELECT_MODE);
        
        final LaunchButton specModeButton = hwElements.getButtons(CcConstValues.DAW_SPEC);
        specModeButton.bindLight(this, () -> RgbState.BLUE);
        //specModeButton.bindPressed(this, () -> LaunchControlXlMk3Extension.println(" > PRESS SPEC >"));
        
        soloArmButton.bindLight(this, () -> row1Mode == Row1ButtonMode.ARM ? RgbState.RED : RgbState.YELLOW);
        muteSelectButton.bindLight(this, () -> row2Mode == Row2ButtonMode.SELECT ? RgbState.WHITE : RgbState.ORANGE);
        soloArmButton.bindPressed(this, this::toggleSoloArmMode);
        muteSelectButton.bindPressed(this, this::toggleSelectMuteMode);
    }
    
    @Activate
    public void init() {
        this.setIsActive(true);
        applyMode();
    }
    
    private void bindNavigation(final LaunchControlXlHwElements hwElements) {
        final LaunchButton pageUpButton = hwElements.getButtons(CcConstValues.PAGE_UP);
        final LaunchButton pageDownButton = hwElements.getButtons(CcConstValues.PAGE_DOWN);
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        transportHandler.bindTrackNavigation(mixerLayer);
        
        mixerLayer.addBinding(
            new SegmentDisplayBinding("Select Track", cursorTrack.name(), displayControl.getFixedDisplay()));

        pageUpButton.bindLight(mixerLayer, () -> sendPage2Active ? RgbState.WHITE : RgbState.OFF);
        pageDownButton.bindLight(mixerLayer, () -> sendPage2Active ? RgbState.OFF : RgbState.WHITE);
        pageUpButton.bindPressed(mixerLayer, () -> selectSendPage(false));
        pageDownButton.bindPressed(mixerLayer, () -> selectSendPage(true));
    }

    private void selectSendPage(final boolean page2) {
        if (this.sendPage2Active == page2) {
            return;
        }
        this.sendPage2Active = page2;
        displayControl.show2LineTemporary("Knob Rows", page2 ? "Send 4/5 | Pan" : "Sends 1 - 3");
        applySendPage();
    }

    private void applySendPage() {
        final boolean mixerActive = mode == BaseMode.MIXER;
        sendPage1Layer.setIsActive(mixerActive && !sendPage2Active);
        sendPage2Layer.setIsActive(mixerActive && sendPage2Active);
    }
    
    
    private void bindTrack(final LaunchControlXlHwElements hwElements, final TrackBank trackBank, final int index) {
        final Track track = trackBank.getItemAt(index);
        track.color().addValueObserver((r, g, b) -> changeTrackColor(index, ColorLookup.toColor(r, g, b)));
        track.addIsSelectedInMixerObserver(select -> {
            if (select) {
                this.selectedTrackIndex.set(index);
            }
        });
        track.arm().markInterested();
        track.exists().markInterested();
        track.mute().markInterested();
        track.solo().markInterested();
        
        final LaunchAbsoluteEncoder row1Encoder = hwElements.getAbsoluteEncoder(0, index);
        final LaunchAbsoluteEncoder row2Encoder = hwElements.getAbsoluteEncoder(1, index);
        final LaunchRelativeEncoder row3Encoder = hwElements.getRelativeEncoder(2, index);
        
        // Page 1: knob rows 1-3 = sends 1-3
        bindSendToEncoder(sendPage1Layer, track, 0, row1Encoder);
        bindSendToEncoder(sendPage1Layer, track, 1, row2Encoder);
        final Send send3 = track.sendBank().getItemAt(2);
        sendPage1Layer.addBinding(new ParameterDisplayBinding(
            new DisplayId(row3Encoder.getTargetId(), displayControl), track.name(), send3));
        sendPage1Layer.addBinding(new LightSendValueBindings(send3, row3Encoder.getLight(), track.sendBank(), 2));
        sendPage1Layer.addBinding(new RelativeEncoderBinding(send3, row3Encoder));

        // Page 2: knob rows 1-2 = sends 4-5, row 3 = pan
        bindSendToEncoder(sendPage2Layer, track, 3, row1Encoder);
        bindSendToEncoder(sendPage2Layer, track, 4, row2Encoder);
        // fixedPanLabel
        final ParameterDisplayBinding panDisplayBinding =
            new ParameterDisplayBinding(
                new DisplayId(row3Encoder.getTargetId(), displayControl), track.name(), track.pan());
        sendPage2Layer.addBinding(panDisplayBinding);
        sendPage2Layer.addBinding(new LightValueBindings(track.pan(), row3Encoder.getLight(), GradientColor.PAN));
        sendPage2Layer.addBinding(new RelativeEncoderBinding(track.pan(), row3Encoder));
        
        // fixedVolumeLabel
        final ControlTargetId sliderId = new ControlTargetId(index);
        final ParameterDisplayBinding volumeDisplayBinding =
            new ParameterDisplayBinding(new DisplayId(index + 5, displayControl), track.name(), track.volume());
        mixerLayer.addBinding(volumeDisplayBinding);
        final HardwareSlider slider = hwElements.getSlider(index);
        this.addBinding(new SliderBinding(sliderId, track.volume(), slider));
        
        final LaunchButton row2Button = hwElements.getRowButtons(1, index);
        final LaunchButton row1Button = hwElements.getRowButtons(0, index);
        row1Button.bindLight(buttonLayers.getArmLayer(), () -> armColor(track));
        row1Button.bindIsPressed(buttonLayers.getArmLayer(), pressed -> toggleArm(pressed, track));
        row1Button.bindLight(buttonLayers.getSoloLayer(), () -> soloColor(track));
        row1Button.bindIsPressed(buttonLayers.getSoloLayer(), pressed -> toggleSolo(pressed, track));
        
        row2Button.bindLight(buttonLayers.getSelectLayer(), () -> selectColor(track, index));
        row2Button.bindPressed(buttonLayers.getSelectLayer(), () -> selectTrack(track));
        row2Button.bindLight(buttonLayers.getMuteLayer(), () -> muteColor(track));
        row2Button.bindPressed(buttonLayers.getMuteLayer(), () -> track.mute().toggle());
    }

    private void bindSendToEncoder(final Layer layer, final Track track, final int sendIndex,
        final LaunchAbsoluteEncoder encoder) {
        final Send send = track.sendBank().getItemAt(sendIndex);
        layer.addBinding(new ParameterDisplayBinding(
            new DisplayId(encoder.getTargetId(), displayControl), track.name(), send));
        layer.addBinding(new AbsoluteEncoderBinding(send, encoder));
        layer.addBinding(new LightSendValueBindings(send, encoder.getLight(), track.sendBank(), sendIndex));
    }
    
    private void toggleSoloArmMode() {
        if (this.row1Mode == Row1ButtonMode.ARM) {
            this.row1Mode = Row1ButtonMode.SOLO;
            displayControl.show2LineTemporary("Arm/Solo", "Solo");
        } else {
            this.row1Mode = Row1ButtonMode.ARM;
            displayControl.show2LineTemporary("Arm/Solo", "Arm");
        }
        applyArmSoloMode();
    }
    
    private void toggleSelectMuteMode() {
        if (this.row2Mode == Row2ButtonMode.SELECT) {
            this.row2Mode = Row2ButtonMode.MUTE;
            displayControl.show2LineTemporary("Mute/Select", "Mute");
        } else {
            this.row2Mode = Row2ButtonMode.SELECT;
            displayControl.show2LineTemporary("Mute/Select", "Select");
        }
        applySelectMuteMode();
    }
    
    protected void applyMode() {
        if (mode == BaseMode.MIXER) {
            midiProcessor.setToRelative(0, false);
            midiProcessor.setToRelative(1, false);
            midiProcessor.setToRelative(2, true);
        } else {
            midiProcessor.setToRelative(0, true);
            midiProcessor.setToRelative(1, true);
            midiProcessor.setToRelative(2, true);
        }
        this.mixerLayer.setIsActive(mode == BaseMode.MIXER);
        this.dawLayer.setIsActive(mode == BaseMode.DAW);
        applySendPage();
        
        
        applySelectMuteMode();
        applyArmSoloMode();
        
        this.buttonLayers.getSelectLayer().setIsActive(true);
        
    }
    
    private void applyArmSoloMode() {
        this.buttonLayers.getSoloLayer().setIsActive(row1Mode == Row1ButtonMode.SOLO);
        this.buttonLayers.getArmLayer().setIsActive(row1Mode == Row1ButtonMode.ARM);
    }
    
    private void applySelectMuteMode() {
        this.buttonLayers.getSelectLayer().setIsActive(row2Mode == Row2ButtonMode.SELECT);
        this.buttonLayers.getMuteLayer().setIsActive(row2Mode == Row2ButtonMode.MUTE);
    }
    
}
