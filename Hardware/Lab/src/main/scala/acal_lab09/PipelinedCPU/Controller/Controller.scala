package acal_lab09.PiplinedCPU.Controller

import chisel3._
import chisel3.util._

import acal_lab09.PiplinedCPU.opcode_map._
import acal_lab09.PiplinedCPU.condition._
import acal_lab09.PiplinedCPU.inst_type._
import acal_lab09.PiplinedCPU.alu_op_map._
import acal_lab09.PiplinedCPU.pc_sel_map._
import acal_lab09.PiplinedCPU.wb_sel_map._
import acal_lab09.PiplinedCPU.forwarding_sel_map._

class Controller(memAddrWidth: Int) extends Module {
  val io = IO(new Bundle {
    // Memory control signal interface
    val IM_Mem_R = Output(Bool())
    val IM_Mem_W = Output(Bool())
    val IM_Length = Output(UInt(4.W))
    val IM_Valid = Input(Bool())

    val DM_Mem_R = Output(Bool())
    val DM_Mem_W = Output(Bool())
    val DM_Length = Output(UInt(4.W))
    val DM_Valid = Input(Bool())

    // branch Comp.
    val E_BrEq = Input(Bool())
    val E_BrLT = Input(Bool())

    // Branch Prediction
    val E_Branch_taken = Output(Bool())
    val E_En = Output(Bool())

    val ID_pc = Input(UInt(memAddrWidth.W))
    val EXE_target_pc = Input(UInt(memAddrWidth.W))

    // Flush
    val Flush_WB_ID_DH = Output(Bool()) //TBD
    val Flush_BH = Output(Bool()) //TBD

    // Stall
    // To Be Modified
    val Stall_WB_ID_DH = Output(Bool()) //TBD
    val Stall_MA = Output(Bool()) //TBD

    // inst
    val IF_Inst = Input(UInt(32.W))
    val ID_Inst = Input(UInt(32.W))
    val EXE_Inst = Input(UInt(32.W))
    val MEM_Inst = Input(UInt(32.W))
    val WB_Inst = Input(UInt(32.W))

    // sel
    val PCSel = Output(UInt(2.W))
    val D_ImmSel = Output(UInt(3.W))
    val W_RegWEn = Output(Bool())
    val E_BrUn = Output(Bool())
    val E_ASel = Output(UInt(2.W))
    val E_BSel = Output(UInt(1.W))
    /* swli forwarding */
    val E_AForward = Output(UInt(2.W)) // 0: no forward, 1: from mem, 2: from wb
    val E_BForward = Output(UInt(2.W))
    /* swli forwarding */
    val E_ALUSel = Output(UInt(15.W))
    val W_WBSel = Output(UInt(2.W))

    val Hcf = Output(Bool())
  })
  // Inst Decode for each stage
  val IF_opcode = io.IF_Inst(6, 0)
  val ID_opcode = io.ID_Inst(6, 0)

  val EXE_opcode = io.EXE_Inst(6, 0)
  val EXE_funct3 = io.EXE_Inst(14, 12)
  val EXE_funct7 = io.EXE_Inst(31, 25)
  val EXE_rd = io.EXE_Inst(11, 7)
  val EXE_31_20 = io.EXE_Inst(31,20)

  val MEM_opcode = io.MEM_Inst(6, 0)
  val MEM_funct3 = io.MEM_Inst(14, 12)

  val WB_opcode = io.WB_Inst(6, 0)

  // Control signal - Branch/Jump
  val E_En = Wire(Bool())
  E_En := (EXE_opcode===BRANCH || EXE_opcode===JAL || EXE_opcode===JALR)         // To Be Modified
  val E_Branch_taken = Wire(Bool())
  E_Branch_taken := MuxLookup(EXE_opcode, false.B, Seq(
          BRANCH -> MuxLookup(EXE_funct3, false.B, Seq(
            "b000".U(3.W) -> io.E_BrEq.asUInt, // beq
            "b001".U(3.W) -> (~io.E_BrEq).asUInt, // bne
            "b100".U(3.W) -> io.E_BrLT.asUInt, // blt
            "b101".U(3.W) -> (~io.E_BrLT).asUInt, // bge
            "b110".U(3.W) -> io.E_BrLT.asUInt, // bltu
            "b111".U(3.W) -> (~io.E_BrLT).asUInt, // bgeu
          )),
          JAL   -> true.B,
          JALR  -> true.B,
        ))    // To Be Modified

  io.E_En := E_En
  io.E_Branch_taken := E_Branch_taken

  // pc predict miss signal
  val Predict_Miss = Wire(Bool())
  Predict_Miss := (E_En && E_Branch_taken && io.ID_pc=/=io.EXE_target_pc)

  // Control signal - PC
  when(Predict_Miss){
    io.PCSel := EXE_T_PC
  }.otherwise{
    io.PCSel := IF_PC_PLUS_4
  }

  // Control signal - Branch comparator
  io.E_BrUn := (io.EXE_Inst(13) === 1.U)

  // Control signal - Immediate generator
  io.D_ImmSel := MuxLookup(ID_opcode, 0.U, Seq(
    OP_IMM  -> I_type,
    LOAD    -> I_type,
    BRANCH  -> B_type,
    LUI     -> U_type,
    AUIPC   -> U_type,
    JAL     -> J_type,
    JALR    -> J_type,
    STORE   -> S_type,
  )) // To Be Modified

  /* swli forwarding */
  val WB_rd = io.WB_Inst(11, 7)
  val MEM_rd = io.MEM_Inst(11, 7)
  val EXE_rd = io.EXE_Inst(11, 7)
  val ID_rs1 = io.ID_Inst(19, 15)
  val ID_rs2 = io.ID_Inst(24, 20)
  // Use rs in ID stage 
  val is_D_use_rs1 = Wire(Bool()) 
  val is_D_use_rs2 = Wire(Bool())
  is_D_use_rs1 := MuxLookup(ID_opcode,false.B,Seq(
    OP      -> true.B,
    OP_IMM  -> true.B,
    BRANCH  -> true.B,
    JALR    -> true.B,
    LOAD    -> true.B,
    STORE   -> true.B,
  ))   // R, I, B, jalr, load, store
  is_D_use_rs2 := MuxLookup(ID_opcode,false.B,Seq(
    OP      -> true.B,
    BRANCH  -> true.B,
    STORE   -> true.B,
  ))   // R, B, store

  // Use rd in WB stage
  val is_W_use_rd = Wire(Bool())
  is_W_use_rd := MuxLookup(WB_opcode,false.B,Seq(
    OP      -> true.B,
    OP_IMM  -> true.B,
    AUIPC   -> true.B,
    LUI     -> true.B,
    JAL     -> true.B,
    JALR    -> true.B,
    LOAD    -> true.B,
  ))   // R, I, auipc, lui, jal, jalr, load

  // Use rd in MEM stage
  val is_M_use_rd = Wire(Bool())
  is_M_use_rd := MuxLookup(MEM_opcode,false.B,Seq(
    OP      -> true.B,
    OP_IMM  -> true.B,
    AUIPC   -> true.B,
    LUI     -> true.B,
    JAL     -> true.B,
    JALR    -> true.B,
    LOAD    -> true.B,
  ))   // R, I, auipc, lui, jal, jalr, load

  // Use rd in EXE stage
  val is_E_use_rd = Wire(Bool())
  is_E_use_rd := MuxLookup(EXE_opcode,false.B,Seq(
    OP      -> true.B,
    OP_IMM  -> true.B,
    AUIPC   -> true.B,
    LUI     -> true.B,
    JAL     -> true.B,
    JALR    -> true.B,
    LOAD    -> true.B,
  ))   // R, I, auipc, lui, jal, jalr, load

  // Hazard condition (rd, rs overlap)
  val is_D_rs1_W_rd_overlap = Wire(Bool())
  val is_D_rs2_W_rd_overlap = Wire(Bool())

  val is_D_rs1_M_rd_overlap = Wire(Bool())
  val is_D_rs2_M_rd_overlap = Wire(Bool())

  val is_D_rs1_E_rd_overlap = Wire(Bool())
  val is_D_rs2_E_rd_overlap = Wire(Bool())

  is_D_rs1_W_rd_overlap := is_D_use_rs1 && is_W_use_rd && (ID_rs1 === WB_rd) && (WB_rd =/= 0.U(5.W))
  is_D_rs2_W_rd_overlap := is_D_use_rs2 && is_W_use_rd && (ID_rs2 === WB_rd) && (WB_rd =/= 0.U(5.W))

  is_D_rs1_M_rd_overlap := is_D_use_rs1 && is_M_use_rd && (ID_rs1 === MEM_rd) && (MEM_rd =/= 0.U(5.W))
  is_D_rs2_M_rd_overlap := is_D_use_rs2 && is_M_use_rd && (ID_rs2 === MEM_rd) && (MEM_rd =/= 0.U(5.W))

  is_D_rs1_E_rd_overlap := is_D_use_rs1 && is_E_use_rd && (ID_rs1 === EXE_rd) && (EXE_rd =/= 0.U(5.W))
  is_D_rs2_E_rd_overlap := is_D_use_rs2 && is_E_use_rd && (ID_rs2 === EXE_rd) && (EXE_rd =/= 0.U(5.W))
  
  val rs1_MEM_hazard = RegNext(is_D_rs1_W_rd_overlap)
  val rs2_MEM_hazard = RegNext(is_D_rs2_W_rd_overlap)
  val rs1_EXE_hazard = RegNext(is_D_rs1_M_rd_overlap)
  val rs2_EXE_hazard = RegNext(is_D_rs2_M_rd_overlap)
  val rs1_load_use_hazard = RegInit(false.B)
  val rs2_load_use_hazard = RegInit(false.B)

  // load-use hazard, is_D_rs1_E_rd_overlap -> stall -> rs1_EXE_hazard
  when(is_D_rs1_E_rd_overlap && EXE_opcode === LOAD){
    rs1_load_use_hazard := true.B
  }.elsewhen(rs1_EXE_hazard){
    rs1_load_use_hazard := false.B
  }

  when(is_D_rs2_E_rd_overlap && EXE_opcode === LOAD){
    rs2_load_use_hazard := true.B
  }.elsewhen(rs2_EXE_hazard){
    rs2_load_use_hazard := false.B
  }

  when(rs1_load_use_hazard){
    io.E_AForward := 3.U
  }.elsewhen(rs1_EXE_hazard){
    io.E_AForward := 1.U
  }.elsewhen(rs1_MEM_hazard){
    io.E_AForward := 2.U
  }.otherwise{
    io.E_AForward := 0.U
  }

  when(rs2_load_use_hazard){
    io.E_BForward := 3.U
  }.elsewhen(rs2_EXE_hazard){
    io.E_BForward := 1.U
  }.elsewhen(rs2_MEM_hazard){
    io.E_BForward := 2.U
  }.otherwise{
    io.E_BForward := 0.U
  }
  /* swli forwarding */

  // Control signal - Scalar ALU
  io.E_ASel := MuxLookup(EXE_opcode, 0.U, Seq(
    BRANCH  -> 1.U,
    AUIPC   -> 1.U, // pc
    LUI     -> 2.U, // 0.U
    JAL     -> 1.U,
    JALR    -> 0.U, // rs1
    LOAD    -> 0.U,
    STORE   -> 0.U,
  ))    // To Be Modified
  
  io.E_BSel := MuxLookup(EXE_opcode, 0.U, Seq(
    OP      -> 0.U,
    OP_IMM  -> 1.U, // imm
    LUI     -> 1.U,
    AUIPC   -> 1.U, 
    BRANCH  -> 1.U,
    JAL     -> 1.U,
    JALR    -> 1.U,
    LOAD    -> 1.U,
    STORE   -> 1.U,
  ))

  // self: srai should use SRA as alu_op, but will use SRL if this is not added
  val is_srai = WireDefault(false.B)
  is_srai := (EXE_opcode === OP_IMM && 
              EXE_funct7 === "b0100000".U && 
              EXE_funct3 === "b101".U) 
  // io.E_BSel := 1.U // To Be Modified
  val is_EXE_rev8 = Mux(EXE_31_20 === "b011010011000".U && EXE_funct3 === "101".U && EXE_opcode === OP_IMM, true.B, false.B) // ql hw4 判斷是否為 rev8，這條指令的 opcode 是 OP_IMM，但是用到的都是 regfile 中的，不會用到 immediate number
  // io.E_BSel := Mux(EXE_opcode === OP || is_EXE_rev8, 0.U, 1.U) // ql OP(0b0110011) 代表 R-type，或者是 rev8，否則使用 ImmGen // TODO 要多測試一下這裡的判斷

  io.E_ALUSel := MuxLookup(EXE_opcode, (Cat(0.U(7.W), "b11111".U, 0.U(3.W))), Seq( // TODO ql hw4 待確認需不需要修改 imm 的判斷 eg. bseti, binvi, ...
    OP -> (Cat(EXE_funct7, "b11111".U, EXE_funct3)),
    OP_IMM -> Mux(is_srai, Cat(EXE_funct7, "b11111".U, EXE_funct3), (Cat(0.U(7.W), "b11111".U, EXE_funct3))),
  )) // To Be Modified, default: add

  // Control signal - Data Memory
  io.DM_Mem_R := (MEM_opcode===LOAD)
  io.DM_Mem_W := (MEM_opcode===STORE)
  io.DM_Length := Cat(0.U(1.W),MEM_funct3) // length

  // Control signal - Inst Memory
  io.IM_Mem_R := true.B // always true
  io.IM_Mem_W := false.B // always false
  io.IM_Length := "b0010".U // always load a word(inst)

  // Control signal - Scalar Write Back
  io.W_RegWEn := MuxLookup(WB_opcode, false.B, Seq(
    OP      -> true.B,
    OP_IMM  -> true.B,
    LOAD    -> true.B,
    LUI     -> true.B,
    AUIPC   -> true.B,
    JAL     -> true.B,
    JALR    -> true.B,
  ))  // To Be Modified


  io.W_WBSel := MuxLookup(WB_opcode, ALUOUT, Seq(
    LOAD  -> LD_DATA,
    JAL   -> PC_PLUS_4,
    JALR  -> PC_PLUS_4,
  )) // To Be Modified

  // Control signal - Others
  io.Hcf := (IF_opcode === HCF)

  /****************** Data Hazard ******************/
  io.Stall_WB_ID_DH := (is_D_rs1_E_rd_overlap || is_D_rs2_E_rd_overlap)
  io.Stall_MA := false.B
  
  io.Flush_BH := Predict_Miss 
  io.Flush_WB_ID_DH := (is_D_rs1_E_rd_overlap || is_D_rs2_E_rd_overlap)
  /****************** Data Hazard End******************/


}