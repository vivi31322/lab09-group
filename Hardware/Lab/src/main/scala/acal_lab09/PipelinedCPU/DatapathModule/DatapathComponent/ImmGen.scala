package acal_lab09.PiplinedCPU.DatapathModule.DatapathComponent

import chisel3._
import chisel3.util._

import acal_lab09.PiplinedCPU.inst_type._
import acal_lab09.PiplinedCPU.opcode_map._
import acal_lab09.PiplinedCPU.alu_op_map._

class ImmGen extends Module{
    val io = IO(new Bundle{
        val inst_31_7 = Input(UInt(25.W))
        val ImmSel = Input(UInt(3.W))
        val imm = Output(UInt(32.W))
    })

    val inst_shift = Wire(UInt(32.W))
    inst_shift := Cat(io.inst_31_7,0.U(7.W))

    // ql hw4 為了 bseti, bclri, binvi, bexti, rori 的 5-bit shamt，這些 instr 需要擴展的不是 [31:20] bits，而是只有 shamt，並且是 unsigned
    // 另外，因為上述指令的 {intr[31,25], intr[24,20], intr[14,12]} 與不是 imm 的版本一樣。eg. bset 和 bseti 的那 15-bit 都是 "b0010100_11111_001"
    val inst_alu_op = Wire(UInt(15.W)) 
    inst_alu_op := Cat(inst_shift(31,25), "b11111".U, inst_shift(14,12)) // ql 因為 bseti, bclri, binvi, bexti, rori 在 RiscvDefs 中的 alu_op_map 中的中間 5 碼都是 11111

    val inst_24_20 = Wire(UInt(5.W)) 
    inst_24_20 := inst_shift(24,20)

    val simm = Wire(SInt(32.W))

    simm := MuxLookup(io.ImmSel,0.S,Seq(
        //R-type
        R_type -> 0.S,

        //I-type
        // ql hw4 因為 bseti, bclri, binvi, bexti, rori 的 imm 在 instr[24,20] 只有 5 位，需要判斷一下
        I_type -> MuxLookup(inst_alu_op, inst_shift(31,20).asSInt, Seq(
            BSET    -> Cat(0.U(27.W), inst_24_20).asSInt,
            BCLR    -> Cat(0.U(27.W), inst_24_20).asSInt,
            BINV    -> Cat(0.U(27.W), inst_24_20).asSInt,
            BEXT    -> Cat(0.U(27.W), inst_24_20).asSInt,
            ROR     -> Cat(0.U(27.W), inst_24_20).asSInt
        )), 

        //B-type
        B_type -> Cat(inst_shift(31),
                 inst_shift(7),
                 inst_shift(30,25),
                 inst_shift(11,8),
                 0.U(1.W)).asSInt,

        //S-type
        S_type -> Cat(inst_shift(31),
                inst_shift(30,25),
                inst_shift(11,8),
                inst_shift(7)).asSInt,

        //U-type
        U_type -> Cat(inst_shift(31,12),0.U(12.W)).asSInt,

        //J-type
        J_type -> Cat(inst_shift(31),
                 inst_shift(19,12),
                 inst_shift(20),
                 inst_shift(30,21),
                 0.U(1.W)).asSInt,

    ))

    io.imm := simm.asUInt
}