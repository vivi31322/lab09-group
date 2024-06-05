# initialize
addi x16, x0, 0xfec
addi x17, x0, 0x472
addi x18, x0, 0xab7
addi x19, x0, 0xda2
addi x20, x0, 0x561
addi x21, x0, 0x146
addi x22, x0, 0x531
addi x23, x0, 0x456
addi x24, x0, 0x11
addi x25, x0, 0x98
addi x26, x0, 0xa3
addi x27, x0, 0x10
addi x28, x0, 0x55
addi x29, x0, 0xffe
addi x30, x0, 0x4
addi x31, x0, 0xff1

clz x1, x30
# x1 = 0x01d
cpop x2, x23
# x2 = 0x05
xnor x3, x16, x17
# x3 = 0x0461
min x4, x31, x25
# x4 = 0xfffffff1
maxu x5, x19, x30
# x5 = 0xffff_fda2
sext.b x6, x21 
# x6 = 0x046
bseti x7, x29, 0x0
# x7 = 0xffff_ffff
bclr x8, x31, x30
# x8 = 0xffff_ffe1
binv x9, x16, x24
# x9 = 0xfffd_ffec
bexti x10, x21, 0x2
# x10 = 0x01
rol x11, x16, x30
# x11 = ffff_fecf
rori x12, x23, 0xc
# x12 = 0x4560_0000
sh2add x13, x17, x18
# x13 = 0x0c7f
rev8 x14, x25
# x14 = 0x9800_0000
orc.b x15, x25
# x15 = 0x0ff

addi x0, x0, 0
addi x0, x0, 0
addi x0, x0, 0
addi x0, x0, 0
addi x0, x0, 0

hcf