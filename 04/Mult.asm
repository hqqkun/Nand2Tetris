// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Mult.asm

// Multiplies R0 and R1 and stores the result in R2.
// (R0, R1, R2 refer to RAM[0], RAM[1], and RAM[2], respectively.)
//
// This program only needs to handle arguments that satisfy
// R0 >= 0, R1 >= 0, and R0*R1 < 32768.

// Put your code here.

	// init
	@R2
	M = 0
	@sum
	M = 0
	@i 
	M = 0
	@TEST
	0; JMP
	
	// loop begin
	(LOOP)
	@R0
	D = M
	@sum				// sun += R0
	M = M + D
	@i
	M = M + 1
	
	
	(TEST)
	@i
	D = M
	@R1
	D = D - M
	@LOOP
	D; JLT
	
	// RAM[2] <- sum
	@sum
	D = M
	@R2
	M = D
	
	
	(END)
	@END
	0; JMP
