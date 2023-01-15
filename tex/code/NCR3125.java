package org.jpc.emulator;

import java.awt.Toolkit;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;

import org.jpc.debugger.JPC;
import org.jpc.emulator.motherboard.SystemBIOS;
import org.jpc.emulator.motherboard.VGABIOS;
import org.jpc.emulator.pci.peripheral.EthernetCard;
import org.jpc.emulator.pci.peripheral.PIIX3IDEInterface;
import org.jpc.emulator.peripheral.CLGD6410;
import org.jpc.emulator.peripheral.FloppyController;
import org.jpc.emulator.peripheral.Intel82360SL;
import org.jpc.emulator.peripheral.Keyboard;
import org.jpc.emulator.peripheral.NCRWacom;
import org.jpc.emulator.peripheral.PCSpeaker;
import org.jpc.emulator.peripheral.ParallelPort;
import org.jpc.emulator.peripheral.Port80;
import org.jpc.emulator.peripheral.SerialPort;
import org.jpc.j2se.JPCApplication;
import org.jpc.j2se.Option;
import org.jpc.j2se.VirtualClock;
import org.jpc.support.ArgProcessor;
import org.jpc.support.DriveSet;
import org.jpc.support.EthernetHub;
import org.jpc.support.EthernetOutput;

public class NCR3125 extends PCConfig {
	private static final Logger LOGGING = Logger.getLogger(NCR3125.class.getName());

    private static final int MONITOR_WIDTH  = 640;
    private static final int MONITOR_HEIGHT = 480 + 100;

	protected void configureBIOS(PC pc) throws IOException {
		pc.add(new SystemBIOS(Option.bios.value("/resources/bios/bios.bin")));
		pc.add(new VGABIOS("/resources/bios/vgabios.bin"));

		//pc.add(new SystemBIOS(Option.bios.value("/resources/bios/at386sl.rom")));
		//pc.add(new VGABIOS("/resources/bios/vga6410.rom"));
	}

	protected void configurePeripherals(PC pc) {
		pc.add(new Intel82360SL());

		pc.add(new PIIX3IDEInterface());
		pc.add(new CLGD6410());

		pc.add(new Port80(PORT80_CODES));
		pc.add(new SerialPort(0));
		pc.add(new SerialPort(1));
		pc.add(new ParallelPort(0));
		pc.add(new ParallelPort(1));
		NCRWacom wacom = new NCRWacom();
		pc.add(wacom);
		pc.add(pc.keyboard = new Keyboard(wacom));
		pc.add(new FloppyController());
		pc.add(new PCSpeaker());
	}

	public static void main(String[] args) {
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e)
		{
			LOGGING.log(Level.INFO, "System Look-and-Feel not loaded", e);
		}

		try {

			if(true) {
				PC pc = createNCR3125();

		        final JPCApplication app = new JPCApplication(args, pc);

				app.setBounds(100, 100, MONITOR_WIDTH + 20, MONITOR_HEIGHT + 70);
				try
				{
					app.setIconImage(Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("resources/icon.png")));
				} catch (Exception e) {}

				app.validate();
				app.setVisible(true);
				app.start();
			} else {
				Option.debug_blocks.update(args, 0);

				JPC.initialise();

				JPC.instance = new JPC(false) {
					private static final long serialVersionUID = 4100969956065955439L;

					@Override
					public PC createPC(String[] args) throws IOException {
						PC result = createNCR3125();
						loadNewPC(result);
						return result;
					}
				};

				JPC.instance.validate();
				JPC.instance.setVisible(true);

				JPC.instance.createPC(args);
				JPC.instance.initialLayout();

			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	private static PC createNCR3125() throws IOException {
		Calendar startDate = Calendar.getInstance();
		startDate.setTime(new Date(1997, 1, 1));
		PC result = new PC(new VirtualClock(), 
				DriveSet.buildFromArgs(new String[] {
						"-hda", "resources/images/penpoint.img", //"DEFAULT:/Users/sschubiger/git/JPC/src/resources/images/penpoint.img",
						"-boot", "hda"
				}),
				8 * 1024 * 1024, 
				startDate,
				new NCR3125());
		Runtime.getRuntime().addShutdownHook(new Thread(() -> result.destroy()));
		return result;
	}

	private static final String[] PORT80_CODES = {
		"02","CPU flags test",
		"04","CPU register test in progress",
		"06","Initialize system hardware",
		"08","Initialize chip set registers",
		"0A","BIOS checksum test",
		"0C","DMA page registers",
		"0E","8254 timers test",
		"10","Initialize 8254 timers",
		"12","Both 8237 DMA controllers test",
		"14","Initialize 8237 DMA controllers",
		"16","Initialize 8259 / reset coprocessor",
		"18","8259 IC registers test",
		"1A","Verify refresh is counting",
		"1C","Base memory 64 K address",
		"1E","Base memory 64 K RAM check (16 bits)",
		"20","Upper 16 of 32 bit test failed",
		"22","8742 keyboard controller test",
		"24","Verify CMOS / configure CMOS",
		"26","Verify / load NVRAM parameters",
		"28","Protexted mode 1 - autosize RAM",
		"2A","Autosize memory ICs",
		"2C","Activate interleave for memory, if possible",
		"2E","Exit first protected mode test",
		"30","Unexpected shutdown",
		"32","Determine system board memory size",
		"34","Relocate memory option",
		"36","Configure EMS memory option",
		"38","Configure wait state option",
		"3A","Retest 64 K base memory",
		"3C","Determine relative CPU speed",
		"3E","Get switches from 8742, if any",
		"40","Configure CPU speed",
		"42","Initialize interrupt vectors",
		"44","Verify video configuration",
		"46","Initialize video system",
		"48","Test for unexpected interrupt",
		"4A","Start second protected mode test",
		"4C","Perform LDT instruction test (CPU protected m.)",
		"4E","Perform TR instruction test (CPU protected m.)",
		"50","Perform LSL instruction test (CPU protected m.)",
		"52","Perform LAR instruction test (CPU protected m.)",
		"54","Perform VERR instruction test (CPU protected m.)",
		"56","Unexpected exception",
		"58","Perform A20 gate test",
		"5A","Keybaord ready test",
		"5C","Determine if AT or XT keyboard type connected",
		"5E","Enter third protected mode test",
		"60","Base memory test",
		"62","Base memory address test",
		"64","Shadow memory test",
		"66","Extended memory test",
		"68","Extended address test",
		"6A","Determine memory size",
		"6C","Display error message",
		"6E","Configure ROM/RAM BIOS",
		"70","System timer test",
		"72","Real time clock test (RTC)",
		"74","Test for stuck keys",
		"76","Initialize hardware interrupt vectors",
		"78","Detect and test coprocessor",
		"7A","Determine / initialize COM channels",
		"7C","Determine LPT channels",
		"7E","Initialize BIOS data area",
		"80","Detect flex disk controller",
		"82","Test flexible disk drive",
		"84","Fixed disk test",
		"86","Perform external ROM scan",
		"88","Test keylock / keyboard type",
		"8A","Wait for F1 test",
		"8C","Final system initialization",
		"8E","Interrupt 19 boot loader - system starts boot",
		"B0","Unknown interrupt occured",
	};
}
