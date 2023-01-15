package org.jpc.emulator.peripheral;

import java.awt.Dimension;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.jpc.emulator.AbstractHardwareComponent;
import org.jpc.emulator.HardwareComponent;
import org.jpc.emulator.Timer;
import org.jpc.emulator.TimerResponsive;
import org.jpc.emulator.motherboard.IODevice;
import org.jpc.emulator.motherboard.IOPortHandler;
import org.jpc.emulator.motherboard.InterruptController;
import org.jpc.emulator.pci.peripheral.IDisplayAdapter;
import org.jpc.support.Clock;

public class NCRWacom extends AbstractHardwareComponent implements IODevice, IMouseHandler, TimerResponsive {
	private static final int NCR_COMMAND_REG = 0x161;
	private static final int NCR_STATUS_REG  = 0x161;
	private static final int NCR_DATA_REG    = 0x160;

	/* commands */

	private static final int NCR_REQUEST		= 0x40;
	private static final int NCR_RESET			= 0x52;
	private static final int NCR_DIAGNOSTIC		= 0x63;
	private static final int NCR_ECHOBACK		= 0x65;
	private static final int NCR_VERSION		= 0x6A;
	private static final int NCR_SLEEP			= 0x73;
	private static final int NCR_WAKE			= 0x77;
	private static final int NCR_STREAM205		= 0x31;
	private static final int NCR_STREAM103		= 0x32;
	private static final int NCR_STREAM68		= 0x33;
	private static final int NCR_STREAM51		= 0x34;
	private static final int NCR_STREAM40		= 0x35;
	private static final int NCR_STREAM24		= 0x36;
	private static final int NCR_STREAM10		= 0x37;
	private static final int NCR_STREAMSTOP		= 0x38;

	/* NCR_STATUS_REG bits */

	private static final int NCR_STREAM_BIT2	= 0x80;
	private static final int NCR_STREAM_BIT1	= 0x40;
	private static final int NCR_LOW_SCAN		= 0x20;
	private static final int NCR_SLEEP_MODE		= 0x10;
	private static final int NCR_STREAM_BIT0	= 0x08;
	private static final int NCR_COMMAND_READY	= 0x02;
	private static final int NCR_DATA_READY		= 0x01;

	/* consecutive data bytes to be read from NCR_DATA_REG:
	 * byte 0: status byte
	 *      1: x high byte
	 *      2: x low byte
	 *      3: y high byte
	 *      4: y low byte
	 *      5: z and button */

	/* status byte (byte 5) bits
	 * (bit values found out by experimentation) */

	private static final int  NCR_NCRTAB		= 0x80;  /* no idea what this bit is good for ! */
	private static final int  NCR_NCRREADY		= 0x40;  /* 0: no pen around, no position data */
	/* 1: position data available */
	/* z and button (byte 5) bits */

	private static final int  NCR_NCRBUTTON		= 0x02;  /* 0: pen button not pressed */
	/* 1: pen button pressed */

	private static final int NCR_NCRPENZ		= 0x01;  /* 0: pen close to or against digitizer */
	/* 1: pen firmly against digitzer */

	/* general stuff */

	private static final float  NCR_NCRMAXX		= 2010;
	private static final float  NCR_NCRMAXY		= 1510;
	private static final long   SEC2NANO        = 1000 * 1000 * 1000;

	private int irq; /* irq channel */
	private InterruptController irqDevice;
	private IDisplayAdapter     display;
	private Clock               clock;
	private int                 status = NCR_COMMAND_READY;
	int                         absx;
	int                         absy;
	int                         buttons;
	byte[]                      packet = new byte[6];
	int                         packetptr;
	int[]                       echoback = {NCR_ECHOBACK,0x9a,0x55,0xaa,0x33,0xcc};
	int                         echobackptr = -1;
	int                         streammode;
	long                        interval;
	Timer                       timer;

	public NCRWacom() {
		irq = 10;
	}

	@Override
	public void ioPortWrite8(int address, int data) {
		switch(address) {
		case NCR_DATA_REG:
			break;
		case NCR_COMMAND_REG:
			switch(data) {
			case NCR_STREAMSTOP:
				streammode = 0;
				break;
			case NCR_RESET:
				streammode = 0;
				break;
			case NCR_ECHOBACK:
				echobackptr = 0;
				break;
			case NCR_REQUEST:
				fillPacket();
				packetptr = 0;
				status |= NCR_DATA_READY;
				break;
			case NCR_STREAM205:
			case NCR_STREAM103:
			case NCR_STREAM68:
			case NCR_STREAM51:
			case NCR_STREAM40:
			case NCR_STREAM24:
			case NCR_STREAM10:
				streammode = data;
				switch(streammode) {
				case NCR_STREAM205: interval = SEC2NANO / 205; break;
				case NCR_STREAM103: interval = SEC2NANO / 103; break;
				case NCR_STREAM68:  interval = SEC2NANO / 68;  break;
				case NCR_STREAM51:  interval = SEC2NANO / 51;  break;
				case NCR_STREAM40:  interval = SEC2NANO / 40;  break;
				case NCR_STREAM24:  interval = SEC2NANO / 24;  break;
				case NCR_STREAM10:  interval = SEC2NANO / 10;  break;
				}
				timer.setExpiry(clock.getEmulatedNanos() + interval);
				break;
			default:
				System.out.println("Unknown command:" + Integer.toHexString(data));
			}
			break;
		default:
			super.ioPortWrite8(address, data);
		}
	}

	@Override
	public int ioPortRead8(int address) {
		switch(address) {
		case NCR_DATA_REG:
			int result = 0;
			if(echobackptr >= 0) {
				result = echoback[echobackptr++];
				if(echobackptr >= echoback.length) {
					echobackptr = -1;
				}
			} else {
				result = packet[packetptr++];
				if(packetptr >= packet.length) {
					packetptr = 0;
					status &= ~NCR_DATA_READY;
					irqDevice.setIRQ(irq, 0);
				}
			}
			return result;
		case NCR_STATUS_REG:
			return status;
		}
		return super.ioPortRead8(address);
	}

	public int[] ioPortsRequested() {
		return new int[]{NCR_DATA_REG,NCR_COMMAND_REG};
	}

	@Override
	public void saveState(DataOutput output) throws IOException {
		output.writeInt(status);
		output.writeInt(echobackptr);
		output.writeInt(streammode);
	}

	@Override
	public void loadState(DataInput input) throws IOException {
		status      = input.readInt();
		echobackptr = input.readInt();
		streammode  = input.readInt();
	}

	private boolean ioportRegistered;

	public void reset()
	{
		irqDevice        = null;
		display          = null;
		clock            = null;
		timer            = null;
		ioportRegistered = false;
	}

	public boolean initialised()
	{
		return ioportRegistered && (irqDevice != null);
	}

	public boolean updated() {
		return ioportRegistered && irqDevice.updated();
	}

	public void updateComponent(HardwareComponent component) {
		if ((component instanceof IOPortHandler) && component.updated()) {
			((IOPortHandler)component).registerIOPortCapable(this);
			ioportRegistered = true;
		}
	}

	public void acceptComponent(HardwareComponent component) {
		if ((component instanceof InterruptController) && component.initialised())
			irqDevice = (InterruptController)component;
		if ((component instanceof IDisplayAdapter) && component.initialised())
			display = (IDisplayAdapter)component;
		if ((component instanceof Clock) && component.initialised()) {
			clock = (Clock)component;
			timer = clock.newTimer(this);
		}
		if ((component instanceof IOPortHandler) && component.initialised()) {
			((IOPortHandler)component).registerIOPortCapable(this);
			ioportRegistered = true;
		}
	}

	@Override
	public void mouseEvent(int x, int y, int z, int dx, int dy, int dz, int buttons) {
		Dimension dim = display.getDisplaySize();
		this.absx    = (int) ((0.87f * x / dim.width)  * NCR_NCRMAXX) + 30;
		this.absy    = (int) ((0.94f * y / dim.height) * NCR_NCRMAXY) + 30;
		this.buttons = buttons;	
	}

	@Override
	public void callback() {
		fillPacket();

		if(streammode != 0) {
			status |= NCR_DATA_READY;
			timer.setExpiry(clock.getEmulatedNanos() + interval);
			irqDevice.setIRQ(irq, 1);
		}
	}

	private void fillPacket() {
		packet[0] = (byte)(NCR_NCRTAB | NCR_NCRREADY);
		if     (absx < 0) absx = 0;
		else if(absx > NCR_NCRMAXX) absx = (int) NCR_NCRMAXX;
		if     (absy < 0) absy = 0;
		else if(absy > NCR_NCRMAXY) absx = (int) NCR_NCRMAXY;

		packet[1] = (byte)(absx >> 8);
		packet[2] = (byte)absx;
		packet[3] = (byte)(absy >> 8);
		packet[4] = (byte)absy;
		packet[5] = 0;
		if((buttons & 0x1) != 0)
			packet[5] |= NCR_NCRPENZ;
		if((buttons & 0x6) != 0)
			packet[5] |= NCR_NCRBUTTON;
	}

	@Override
	public int getType() {
		return -1;
	}
}
