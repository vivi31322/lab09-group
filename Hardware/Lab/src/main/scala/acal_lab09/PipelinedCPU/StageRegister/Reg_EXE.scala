package acal_lab09.PiplinedCPU.StageRegister

import chisel3._
import chisel3.util._

class Reg_EXE(addrWidth:Int) extends Module {
    val io = IO(new Bundle{
        val Flush = Input(Bool())
        val Stall = Input(Bool())

        val inst_in = Input(UInt(32.W))
        val pc_in = Input(UInt(addrWidth.W))
        val rs1_rdata_in = Input(UInt(32.W))
        val rs2_rdata_in = Input(UInt(32.W))
        val imm_in = Input(UInt(32.W))
        /* swli forwarding */ 
        val forward_EXE_in = Input(UInt(32.W))
        val forward_MEM_in = Input(UInt(32.W))
        val forward_WB_in = Input(UInt(32.W))
        /* swli forwarding */

        val inst = Output(UInt(32.W))
        val pc = Output(UInt(addrWidth.W))
        val rs1_rdata = Output(UInt(32.W))
        val rs2_rdata = Output(UInt(32.W))
        val imm = Output(UInt(32.W))
        /* swli forwarding */
        val forward_EXE = Output(UInt(32.W))
        val forward_MEM = Output(UInt(32.W))
        val forward_WB = Output(UInt(32.W))
        /* swli forwarding */
    })

    // stage Registers
    val pcReg =  RegInit(0.U(addrWidth.W))
    val InstReg = RegInit(0.U(32.W))
    val immReg = RegInit(0.U(32.W))
    val rs1Reg = RegInit(0.U(32.W))
    val rs2Reg = RegInit(0.U(32.W))
    /* swli forwarding */
    val feReg = RegInit(0.U(32.W))
    val fmReg = RegInit(0.U(32.W))
    val fwReg = RegInit(0.U(32.W))
    /* swli forwarding */

    /*** stage Registers Action ***/
    when(io.Stall){
        immReg := immReg
        InstReg := InstReg
        pcReg := pcReg
        rs1Reg := rs1Reg
        rs2Reg := rs2Reg
        /* swli forwarding */
        feReg := feReg
        fmReg := fmReg
        fwReg := fwReg
        /* swli forwarding */
    }.elsewhen(io.Flush){
        immReg := 0.U(32.W)
        InstReg := 0.U(32.W)
        pcReg := 0.U(addrWidth.W)
        rs1Reg := 0.U(32.W)
        rs2Reg := 0.U(32.W)
        /* swli forwarding */
        feReg := 0.U(32.W)
        fmReg := 0.U(32.W)
        fwReg := 0.U(32.W)
        /* swli forwarding */
    }.otherwise{
        InstReg := io.inst_in
        immReg := io.imm_in
        pcReg := io.pc_in
        rs1Reg := io.rs1_rdata_in
        rs2Reg := io.rs2_rdata_in
        /* swli forwarding */
        feReg := io.forward_EXE_in
        fmReg := io.forward_MEM_in
        fwReg := io.forward_WB_in
        /* swli forwarding */
    }

    io.inst := InstReg
    io.imm := immReg
    io.pc := pcReg
    io.rs1_rdata := rs1Reg
    io.rs2_rdata := rs2Reg
    /* swli forwarding */
    io.forward_EXE := feReg
    io.forward_MEM := fmReg
    io.forward_WB := fwReg
    /* swli forwarding */
}
