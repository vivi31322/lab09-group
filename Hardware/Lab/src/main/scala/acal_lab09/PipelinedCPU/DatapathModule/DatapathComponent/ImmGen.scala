package acal_lab09.PiplinedCPU.DatapathModule.DatapathComponent

import chisel3._
import chisel3.util._

import acal_lab09.PiplinedCPU.inst_type._

class ImmGen extends Module{
    val io = IO(new Bundle{
        val inst_31_7 = Input(UInt(25.W))
        val ImmSel = Input(UInt(3.W))
        val imm = Output(UInt(32.W))
    })

    val inst_shift = Wire(UInt(32.W))
    inst_shift := Cat(io.inst_31_7,0.U(7.W))

    val simm = Wire(SInt(32.W))

    simm := MuxLookup(io.ImmSel,0.S,Seq(
        //R-type
        R_type -> 0.S,

        //I-type
        I_type -> inst_shift(31,20).asSInt, // TODO ql hw4 因為 bseti, bclri, binvi, bexti, rori 的 imm 在 instr[25,20] 只有 6 位，這裡可能需要修改？ REV8 需要擴展的是 instr[19,15] 共 5 位？

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