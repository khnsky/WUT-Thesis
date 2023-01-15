package org.jpc.emulator.peripheral;

public interface IMouseHandler {
	void mouseEvent(int x, int y, int z, int dx, int dy, int dz, int buttons);
}
