package acal_lab09.PiplinedCPU.DatapathModule.DatapathComponent

import chisel3._
import chisel3.util._

import acal_lab09.PiplinedCPU.opcode_map._
import acal_lab09.PiplinedCPU.alu_op_map._

class ALUIO extends Bundle{
  val src1    = Input(UInt(32.W))
  val src2    = Input(UInt(32.W))
  val ALUSel  = Input(UInt(15.W))
  val out  = Output(UInt(32.W))
}

class ALU extends Module{
  val io = IO(new ALUIO)

  // ----------- hw4 ql zero calculation: clz, ctz -----------
  // 計算參考： https://stackoverflow.com/a/2376530/11283742

  // clz, ctz 是算有多少個 0 在前面或後面，所以只要 reverse 一下，邏輯就可通用
  val src1_for_zero_cal = Mux(io.ALUSel === CTZ, Reverse(io.src1), io.src1)

  val val16 = Wire(UInt(16.W))
  val val8 = Wire(UInt(8.W))
  val val4 = Wire(UInt(4.W))

  val zero_cal = Wire(Vec(5, Bool()))
  zero_cal(4) := (src1_for_zero_cal(31, 16) === 0.U)
  val16 := Mux(zero_cal(4), src1_for_zero_cal(15,0), src1_for_zero_cal(31,16))
  zero_cal(3) := (val16(15, 8) === 0.U)
  val8 := Mux(zero_cal(3), val16(7,0), val16(15,8))
  zero_cal(2) := (val8(7, 4) === 0.U)
  val4 := Mux(zero_cal(2), val8(3,0), val8(7,4))
  zero_cal(1) := (val4(3,2) === 0.U)
  zero_cal(0) := Mux(zero_cal(1), ~val4(1), ~val4(3))
  // ----------- End of zero calculation -----------

  val byte_all_zero = WireDefault(0.U(8.W))
  val byte_all_one = WireDefault("b1111_1111".U(8.W))

  io.out := io.src1+io.src2
  switch(io.ALUSel){
    is(ADD ){io.out := io.src1+io.src2}
    is(SLL ){io.out := io.src1 << io.src2(4,0)}
    is(SLT ){io.out := Mux(io.src1.asSInt < io.src2.asSInt,1.U,0.U)}
    is(SLTU){io.out := Mux(io.src1 < io.src2              ,1.U,0.U)}
    is(XOR ){io.out := io.src1^io.src2}
    is(SRL ){io.out := io.src1 >> io.src2(4,0)}
    is(OR  ){io.out := io.src1|io.src2}
    is(AND ){io.out := io.src1&io.src2}
    is(SUB ){io.out := io.src1-io.src2}
    is(SRA ){io.out := (io.src1.asSInt >> io.src2(4,0)).asUInt}
    // ------------------- hw4 ql
    //
    is (CLZ, CTZ) {io.out := Mux(io.src1 === 0.U, 32.U, zero_cal.asUInt)}
    is (CPOP  ) {io.out := PopCount(io.src1)} // 利用 Chisel 提供的 PopCount 來計算 set bit 的數量
    is (SEXT_B) {io.out := Cat(Fill(24, io.src1( 7)), io.src1( 7,0))} // sign extend the value in the 8 LSB bits
    is (SEXT_H) {io.out := Cat(Fill(16, io.src1(15)), io.src1(15,0))} // sign extend the value in the 16 LSB bits
    //
    is (ANDN ) {io.out := io.src1 & ~io.src2}
    is (ORN  ) {io.out := io.src1 | ~io.src2}
    is (XNOR ) {io.out := ~(io.src1 ^ io.src2)}
    //
    is (MIN  ) {io.out := Mux(io.src1.asSInt < io.src2.asSInt, io.src1 , io.src2)} // signed
    is (MAX  ) {io.out := Mux(io.src1.asSInt < io.src2.asSInt, io.src2 , io.src1)} // signed
    is (MINU ) {io.out := Mux(io.src1 < io.src2, io.src1 , io.src2)} // unsigned
    is (MAXU ) {io.out := Mux(io.src1 < io.src2, io.src2 , io.src1)} // unsigned
    //  ql bset(i), bclr(i), binv(i), bext(i) 應該會從外部決定 src2 是 regFile 的值或是 imm 的值，所以這裡應當不用再寫 is (xxxxI) {}
    is (BSET ) {io.out := io.src1 |  (1.U << io.src2(5,0))} // rs1 with a single bit set at the index specified in rs2
    is (BCLR ) {io.out := io.src1 & ~(1.U << io.src2(5,0))} // rs1 with a single bit cleared at the index specified in rs2
    is (BINV ) {io.out := io.src1 ^  (1.U << io.src2(5,0))} // rs1 with a single bit inverted at the index specified in rs2
    is (BEXT ) {io.out := (io.src1 >> io.src2(5,0)) & 1.U}  // a single bit extracted from rs1 at the index specified in rs2
    // 
    is (ROR  ) {io.out := (io.src1 >> io.src2(4,0)) | (io.src1 << (32.U - io.src2(4,0)))} // a rotate right of rs1 by the amount in least-significant log2(XLEN) bits of rs2.
    is (ROL  ) {io.out := (io.src1 << io.src2(4,0)) | (io.src1 >> (32.U - io.src2(4,0)))}
    //
    is (SHA1ADD) {io.out := (io.src2 + (io.src1 << 1))}
    is (SHA2ADD) {io.out := (io.src2 + (io.src1 << 2))}
    is (SHA3ADD) {io.out := (io.src2 + (io.src1 << 3))}
    //
    is (ZEXT_H ) {io.out := Cat(Fill(16, "b0".U), io.src1(15,0))} // {16{0}, io.src1[15:0]}
    is (REV8   ) {io.out := Cat(io.src1(7,0), io.src1(15,8), io.src1(23,16), io.src1(31,24))} // 以 Byte 為單位換順序
    is (ORC_B  ) {io.out := Cat(Mux(io.src1(31, 24) === 0.U, byte_all_zero, byte_all_one), // 單個 byte 中若有 1，則整個 byte 為 0xff，否則整個 byte 為 0x0
                                Mux(io.src1(23, 16) === 0.U, byte_all_zero, byte_all_one),
                                Mux(io.src1(15,  8) === 0.U, byte_all_zero, byte_all_one),
                                Mux(io.src1( 7,  0) === 0.U, byte_all_zero, byte_all_one))}
    // 其他的如 bseti, bclri, binvi, bexti, rori 的 imm 版本 (需要擴展的是 5-bit shamt 欄位)，因為 sign extension 已經在 ImmGen 處理過了，
    // 又因為上述指令在 io.ALUSel 來看是一樣的，所以在這裡與非 imm 的邏輯是一樣的，就不用重複寫了。
    // eg. bset 和 bseti 本質上是做一樣的事情，並且 ALUSel 都是 "b0010100_11111_001"，
    //     不同的是 bseti 已經在 ImmGen 擴展了 5-bit shamt，但是在傳入 alu 時 src2 選了了 擴展後的 5-bit shamt，
    //     所以這裡不需要額外處理，當作 bset 處理就好
    // -------------------
  }
}

