# initialize
addi x16, x0, 0x010
addi x17, x0, 0x009
addi x18, x0, 0x003
addi x19, x0, 0x004
addi x20, x0, 0x005
addi x21, x0, 0x006
addi x22, x0, 0x001
addi x23, x0, 0x008
addi x24, x0, 0x009
addi x25, x0, 0x6ad
addi x26, x0, 0x00b
addi x27, x0, 0x00c
addi x28, x0, 0x00d
addi x29, x0, 0x00e
addi x30, x0, 0x00a
addi x31, x0, 0xfff

ctz x1, x30
#x1=0x01
andn x3, x16, x17
#x3=0x010
max x4, x31, x25 
#x4=0x6ad
minu x2, x19, x31 
#x2=0x004

lui x5, 0x48c01
addi x5, x5, 0x0a4
sext.h x6, x5 
#x6=0x10a4 

bset x7, x29, x19
#x7=0x01e
bclri x8, x31, 7 
#x8=0xf7f
binvi x9, x16, 4 
#x9=0x000
bext x10, x21, x22 
#x10=1
sh3add x11, x16, x30 
#x11=08a
ror x12, x25, x24 
#x12=56800003
sh1add x13, x17, x18 
#x13=015
zext.h x14, x31 
#x14=0000_ffff
orn x15, x25, x31 
#x15=0x6ad

hcf