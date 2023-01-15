package org.jpc.emulator.peripheral;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jpc.emulator.AbstractHardwareComponent;
import org.jpc.emulator.HardwareComponent;
import org.jpc.emulator.motherboard.IODevice;
import org.jpc.emulator.motherboard.IOPortHandler;

public class Intel82360SL extends AbstractHardwareComponent implements IODevice {
	private static final int CPUPWRMODE		    = 0x22; //		0		CPU		16	-W  CPU write mode register
	//	Bitfields for Intel 82360SL CPU write mode register:
	//		Bit(s)	Description	(Table P033)
	//		 0	unlock configuration space
	//		 1	enable selected unit
	//		 3-2	unit
	//			00 memory configuration
	//			01 cache
	//			10 internal bus
	//			11 external bus
	private static final int CFGSTAT			= 0x23; //		0		82360SL	8	R-  configuration status register
	// bit 7: 82360 configuration is open
	private static final int CFGINDEX		    = 0x24; //		0		82360SL	16	-W  82360 configuration index
	private static final int CFGDATA			= 0x25; //		xxh		82360SL	16	RW  82360 configuration data
	private static final int EMSCNTLREG		    = 0x28; //		0		CPU		8
	private static final int EMSINDEXREG		= 0x2A; //		0		CPU		16
	private static final int EMSDPREG		    = 0x2C; //		xxh		CPU		16
	private static final int PORT92			    = 0x92; //		0		CPU		8
	private static final int PPCONFIG		    = 0x102; //		0		CPU		8
	private static final int FAIL_SAFE_NMI_CTRL = 0x461; //		0		CPU		8
	//The followed ports visible only when they enabled,
	//Any writes to this ports caused the action it named.
	private static final int FAST_CPU_RESET	    = 0xEF; //		N/A		82360SL	8
	private static final int FAST_A20_GATE	    = 0xEE; //		N/A		82360SL	8
	private static final int SLOW_CPU	        = 0xF4; //		N/A		CPU		8
	private static final int FAST_CPU	        = 0xF5; //		N/A		CPU		8
	private static final int SFS_DISABLE	    = 0xF9; //		N/A		CPU		8
	private static final int SFS_ENABLE	        = 0xFB; //		N/A		CPU		8

	//	format of CPUPWRMODE register (i386SL):
	//		Bits	Name	Description
	//		15	DT	If Unlock Status {  // See bit 0 of this register
	//					if bit=0 then access to 82360SL
	//					if bit=1 then access to CPUPWRMODE register
	//						}
	//				If Lock Staus	{   // i.e.SB=1
	//					(De-Turbo Select Bit) Selected clock speed
	//					If bit=0 then EFI/2
	//					If bit=1 then EFI/4
	//						}
	//		14	0	Reserved
	//		13..11	IMCPC	(Idle MCP Clock)
	//				13.12.11    Description
	//				000	EFI
	//				001	EFI/2
	//				010	EFI/4
	//				011	EFI/8
	//				100	EFI/16
	//				101	Reserved
	//				110	Reserved
	//				111	Stop Clock
	//		10,9	SLC	(Slow CPU clock)
	//				10.9	Description
	//				00	EFI
	//				01	EFI/2
	//				10	EFI/4
	//				11	EFI?8
	//		8	CPUCNFG
	//				If =1 CPU Lock. (Write Protect to CPUPMODE register)
	//		7	FD	(Flash Disk Enable)
	//				If bit=1 then phisical addresses D0000H - DFFFFh
	//				automatically never caching.
	//		6	0	Reserved
	//		5,4	FCC	(Fast CPU clock)
	//				5.4	Description
	//				00	EFI
	//				01	EFI/2
	//				10	EFI/4
	//				11	EFI/8
	//		3,2	US	(Unit Select)
	//				Select Unit of 82360SL which will be accessable through 23h-25h
	//				I/O Ports
	//				3.2	Description
	//				00	On-Board Memory Controller
	//				01	Cache Unit
	//				10	Internal Bus Unit
	//				11	External Bus Unit
	//		1	UE	(Unit Enable)
	//				If =1 Enable to Access Units
	//				else enable to access System bus.
	//		0	SB	(Status Bit)
	//				If =0 Enable access to CPUPWRMODE register
	//				If =1 Disable
	//
	//
	//
	//		Format of EMSCNTLREG:
	//		Bits	Description
	//		7	(Global Enable)
	//			If =1 EMS enable
	//		6	Valid bit
	//		5	EMSDP Status Bit (Read Only)
	//		4..2	Reserved
	//		1..0	Active EMS Set (0-3)
	//
	//
	//		Format of EMSINDEXREG:
	//		Bits	Description
	//		15..10	Reserved
	//		9..8	EMS set (0-3)
	//		7..6	Reserved
	//		5..0	EMS Page Register Index (0-64)
	//
	//		Format of EMSDPREG:
	//		Bits	Description
	//		15	This EMS Page Enable (i.e. page indexed by EMSINDEXREG)
	//		14	EMS Valid bit
	//		13..11	reserved
	//		10..0	Address lines A24..A14 for page selected by EMSINDEXREG
	//
	//		Important Note:
	//		i386SL have SIGNATURE register have index 30Eh in On-Board Memory Controller
	//		Configuration Space. This Register contain  Stepping Info of i386SL.
	//		Stepping	Signature Register	DX register after reset
	//		A0		4300h			4310h
	//		A1		4300h			4310h
	//		A2		4301h			4310h
	//		A3		4302h			4310h
	//		B0		4310h			4311h
	//		B1		4311h			4311h
	//	
	private final int[] regs = new int[256];
	private int         idx;
	private int         cpupwrmode;
	private int         cfgstat;
	private int         unlock;
	private int         ppconfig = 0xFFFF;

	private static final int IDX_EXT_STS = 0xFB;

	public Intel82360SL() {

	}

	public void ioPortWrite16(int address, int data) {
		switch(address) {
		case CPUPWRMODE:
			if(unlock == 2 && data == 0x080)
				unlock++;
			if(unlock > 2) {
				cpupwrmode = data;
			}
			break;
		default:
			super.ioPortWrite16(address, data);
		}		
	}

	public int ioPortRead16(int address) {
		switch(address) {
		case PPCONFIG:
			return ppconfig;
		}
		return super.ioPortRead16(address);
	}

	@Override
	public void ioPortWrite8(int address, int data) {
		switch(address) {
		case CPUPWRMODE:
			if(unlock == 1 && data == 0x080)
				unlock++;
			else if(unlock > 2)
				cpupwrmode = data;
			break;
		case CFGINDEX:
			if(unlock > 2)
				idx = data;
			break;
		case CFGSTAT:
			if(data == 0)
				unlock++;
			break;
		case CFGDATA:
			if(unlock > 2)
				regs[idx] = data;
			break;
		default:
			int address16 = address & ~1;
			int value = ioPortRead16(address16);
			value &= (address & 1) != 0 ? 0x00FF   : 0xFF00;
			value |= (address & 1) != 0 ? data << 8: data;
			ioPortWrite16(address16, value);
		}
	}

	@Override
	public int ioPortRead8(int address) {
		switch(address) {
		case CFGSTAT:
			return cfgstat;
		case CFGDATA:
			return regs[idx];
		case 0x0FFF9:
		case 0x0FFFB:
		case 0x0FFFD:
		case 0x0FFFF:
				return 0;
		default:
			int result = ioPortRead16(address & ~1);
			return (address & 1) != 0 ? result >> 8 : result & 0xff;
		}
	}

	@Override
	public void saveState(DataOutput output) throws IOException {
	}

	@Override
	public void loadState(DataInput input) throws IOException {
	}

	public int[] ioPortsRequested() {
		return new int[]{
				0x22,0x23,0x24,0x25,

				0x28,

				0x102,0x103,

				0x300,0x301,0x302,0x303,0x304,0x305,0x306,0x307,
				0x308,0x309,0x30a,0x30b,0x30c,0x30d,0x30e,0x30f,
				0x310,0x311,0x312,0x313,0x314,0x315,0x316,0x317,
				0x318,0x319,0x31a,0x31b,0x31c,0x31d,0x31e,0x31f,

				0x700,0x701,0x705,0x706,

				0xb00,

				0xfff8,0xfff9,0xfffa,0xfffb,0xfffc,0xfffd,0xfffe,0xffff,
		};
	}

	private boolean ioportRegistered;

	public void reset() {
		ioportRegistered = false;
	}

	public boolean initialised() {
		return ioportRegistered;
	}

	public boolean updated() {
		return ioportRegistered;
	}

	private void init() {
		regs[IDX_EXT_STS] = 0x0C;
		ioportRegistered = true;
	}
	
	public void updateComponent(HardwareComponent component) {
		if ((component instanceof IOPortHandler) && component.updated()) {
			((IOPortHandler)component).registerIOPortCapable(this);
			init();
		}
	}

	public void acceptComponent(HardwareComponent component) {
		if ((component instanceof IOPortHandler) && component.initialised()) {
			((IOPortHandler)component).registerIOPortCapable(this);
			init();
		}
	}
}
