// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

// Put your code here.

	@8192	// the size of screen.
	D = A
	@size
	M = D
	@i		// temp var
	M = 0
	
(LISTEN)
	@KBD
	D = M
	@BLACK
	D; JNE
	
// white loop -------------------------------------------------------
(WHITE)
	@i				// i = 0
	M = 0
	@TESTWHITESIZE
	0;JMP
(LOOPWHITE)
	@SCREEN
	D = A
	@i
	A = D + M
	M = 0		// RAM[SCREEN + i] = white
	@i
	M = M + 1
	
(TESTWHITESIZE)
	@i
	D = M
	@size
	D = D - M
	@LOOPWHITE
	D; JNE
	
	@LISTEN
	0; JMP
// white loop done!--------------------------------------------------

// black loop -------------------------------------------------------
(BLACK)
	@i				// i = 0
	M = 0
	@TESTBLACKSIZE
	0;JMP
(LOOPBLACK)
	@SCREEN
	D = A
	@i
	A = D + M
	M = -1		// RAM[SCREEN + i] = black
	@i
	M = M + 1
	
(TESTBLACKSIZE)
	@i
	D = M
	@size
	D = D - M
	@LOOPBLACK
	D; JNE
	
	@LISTEN
	0; JMP
// black loop done! ----------------------------------------------------------