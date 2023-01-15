package org.jpc.emulator;

import java.io.IOException;
import java.util.Calendar;

import org.jpc.emulator.motherboard.DMAController;
import org.jpc.emulator.motherboard.GateA20Handler;
import org.jpc.emulator.motherboard.IOPortHandler;
import org.jpc.emulator.motherboard.InterruptController;
import org.jpc.emulator.motherboard.IntervalTimer;
import org.jpc.emulator.motherboard.RTC;
import org.jpc.emulator.motherboard.SystemBIOS;
import org.jpc.emulator.motherboard.VGABIOS;
import org.jpc.emulator.pci.PCIBus;
import org.jpc.emulator.pci.PCIHostBridge;
import org.jpc.emulator.pci.PCIISABridge;
import org.jpc.emulator.pci.peripheral.DefaultVGACard;
import org.jpc.emulator.pci.peripheral.EthernetCard;
import org.jpc.emulator.pci.peripheral.PIIX3IDEInterface;
import org.jpc.emulator.peripheral.Adlib;
import org.jpc.emulator.peripheral.FloppyController;
import org.jpc.emulator.peripheral.Keyboard;
import org.jpc.emulator.peripheral.MPU401;
import org.jpc.emulator.peripheral.Midi;
import org.jpc.emulator.peripheral.Mixer;
import org.jpc.emulator.peripheral.PCSpeaker;
import org.jpc.emulator.peripheral.SBlaster;
import org.jpc.emulator.peripheral.SerialPort;
import org.jpc.j2se.Option;

public class PCConfig {

	
	protected void configureBIOS(PC pc) throws IOException {
		//BIOSes
        pc.add(new SystemBIOS(Option.bios.value("/resources/bios/bios.bin")));
        pc.add(new VGABIOS("/resources/bios/vgabios.bin"));

        if (Option.sound.value())
        {
            Midi.MIDI_Init();
            Mixer.MIXER_Init();
            String device = Option.sounddevice.value("sb16");
            if (device.equals("sb16"))
            {
                pc.add(new Mixer());
                pc.add(new MPU401());
                pc.add(new SBlaster());
                pc.add(new Adlib());
            }
        }
	}

	protected void configurePCI(PC pc) {
		//PCI Stuff
        pc.add(new PCIHostBridge());
        pc.add(new PCIISABridge());
        pc.add(new PCIBus());

        if (Option.ethernet.isSet())
            pc.add(new EthernetCard());
	}

	protected void configurePeripherals(PC pc) {
		//Peripherals
        pc.add(new PIIX3IDEInterface());
        if (Option.ethernet.isSet())
            pc.add(pc.ethernet = new EthernetCard());
        pc.add(new DefaultVGACard());

        pc.add(new SerialPort(0));
        pc.add(new SerialPort(1));
        pc.add(new SerialPort(2));
        pc.add(new SerialPort(3));
        pc.add(pc.keyboard = new Keyboard());
        pc.add(new FloppyController());
        pc.add(new PCSpeaker());
	}

	protected void configureMotherboard(PC pc, Calendar startTime) {
		//Motherboard

        pc.add(new IOPortHandler());
        pc.add(pc.pic = new InterruptController());

        pc.add(new DMAController(false, true));
        pc.add(new DMAController(false, false));

        pc.add(new RTC(0x70, 8, startTime));
        pc.add(new IntervalTimer(0x40, 0));
        pc.add(new GateA20Handler());
	}
}
