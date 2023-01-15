LC_ALL=C \
PATH=/usr/local/sbin:/usr/local/bin:/usr/bin \
HOME=/var/lib/libvirt/qemu/domain--1-PenPointOS \
XDG_DATA_HOME=/var/lib/libvirt/qemu/domain--1-PenPointOS/.local/share \
XDG_CACHE_HOME=/var/lib/libvirt/qemu/domain--1-PenPointOS/.cache \
XDG_CONFIG_HOME=/var/lib/libvirt/qemu/domain--1-PenPointOS/.config \
/usr/bin/qemu-system-x86_64 \
    -name guest=PenPointOS,debug-threads=on \
    -S \
    -object '{"qom-type":"secret","id":"masterKey0","format":"raw","file":"/var/lib/libvirt/qemu/domain--1-PenPointOS/master-key.aes"}' \
    -machine pc-i440fx-7.0,usb=off,vmport=off,dump-guest-core=off,memory-backend=pc.ram \
    -accel kvm \
    -cpu host,migratable=on \
    -m 64 \
    -object '{"qom-type":"memory-backend-ram","id":"pc.ram","size":67108864}' \
    -overcommit mem-lock=off \
    -smp 1,sockets=1,cores=1,threads=1 \
    -uuid ed7859fc-c29c-40f5-8cda-a5a3da5baae1 \
    -no-user-config \
    -nodefaults \
    -chardev socket,id=charmonitor,path=/var/lib/libvirt/qemu/domain--1-PenPointOS/monitor.sock,server=on,wait=off \
    -mon chardev=charmonitor,id=monitor,mode=control \
    -rtc base=utc,driftfix=slew \
    -global kvm-pit.lost_tick_policy=delay \
    -no-hpet \
    -no-shutdown \
    -global PIIX4_PM.disable_s3=1 \
    -global PIIX4_PM.disable_s4=1 \
    -boot menu=on,strict=on \
    -device '{"driver":"ich9-usb-ehci1","id":"usb","bus":"pci.0","addr":"0x5.0x7"}' \
    -device '{"driver":"ich9-usb-uhci1","masterbus":"usb.0","firstport":0,"bus":"pci.0","multifunction":true,"addr":"0x5"}' \
    -device '{"driver":"ich9-usb-uhci2","masterbus":"usb.0","firstport":2,"bus":"pci.0","addr":"0x5.0x1"}' \
    -device '{"driver":"ich9-usb-uhci3","masterbus":"usb.0","firstport":4,"bus":"pci.0","addr":"0x5.0x2"}' \
    -device '{"driver":"virtio-serial-pci","id":"virtio-serial0","bus":"pci.0","addr":"0x6"}' \
    -blockdev '{"driver":"file","filename":"/var/lib/libvirt/images/PenPointOS.qcow2","node-name":"libvirt-3-storage","auto-read-only":true,"discard":"unmap"}' \
    -blockdev '{"node-name":"libvirt-3-format","read-only":false,"discard":"unmap","driver":"qcow2","file":"libvirt-3-storage"}' \
    -device '{"driver":"ide-hd","bus":"ide.0","unit":0,"drive":"libvirt-3-format","id":"ide0-0-0","bootindex":1}' \
    -device '{"driver":"ide-cd","bus":"ide.0","unit":1,"id":"ide0-0-1"}' \
    -blockdev '{"node-name":"libvirt-1-format","read-only":false,"driver":"raw","file":"libvirt-1-storage"}' \
    -device '{"driver":"floppy","unit":0,"drive":"libvirt-1-format","id":"fdc0-0-0"}' \
    -netdev '{"type":"tap","fd":"30","id":"hostnet0"}' \
    -device '{"driver":"e1000","netdev":"hostnet0","id":"net0","mac":"52:54:00:a1:b0:77","bus":"pci.0","addr":"0x3"}' \
    -chardev pty,id=charserial0 \
    -device '{"driver":"isa-serial","chardev":"charserial0","id":"serial0","index":0}' \
    -chardev spicevmc,id=charchannel0,name=vdagent \
    -device '{"driver":"virtserialport","bus":"virtio-serial0.0","nr":1,"chardev":"charchannel0","id":"channel0","name":"com.redhat.spice.0"}' \
    -device '{"driver":"usb-tablet","id":"input0","bus":"usb.0","port":"1"}' \
    -audiodev '{"id":"audio1","driver":"spice"}' \
    -spice port=5901,addr=127.0.0.1,disable-ticketing=on,image-compression=off,seamless-migration=on \
    -device '{"driver":"qxl-vga","id":"video0","max_outputs":1,"ram_size":67108864,"vram_size":67108864,"vram64_size_mb":0,"vgamem_mb":16,"bus":"pci.0","addr":"0x2"}' \
    -device '{"driver":"intel-hda","id":"sound0","bus":"pci.0","addr":"0x4"}' \
    -device '{"driver":"hda-duplex","id":"sound0-codec0","bus":"sound0.0","cad":0,"audiodev":"audio1"}' \
    -chardev spicevmc,id=charredir0,name=usbredir \
    -device '{"driver":"usb-redir","chardev":"charredir0","id":"redir0","bus":"usb.0","port":"2"}' \
    -chardev spicevmc,id=charredir1,name=usbredir \
    -device '{"driver":"usb-redir","chardev":"charredir1","id":"redir1","bus":"usb.0","port":"3"}' \
    -device '{"driver":"virtio-balloon-pci","id":"balloon0","bus":"pci.0","addr":"0x7"}' \
    -sandbox on,obsolete=deny,elevateprivileges=deny,spawn=deny,resourcecontrol=deny \
    -msg timestamp=on
