!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
!! CSE 131 Floating-Point Output Routine
!! For displaying float types in RC
!! Author: Garo Bournoutian, Winter 2008
!!
!!  NOTE: Pass float to be printed in %f0
!!
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

	.section 	".rodata"
	.align		4
.$rFmt:
	.asciz		"%.2lf"

	.section	".text"
	.align 		4
	.global		printFloat
printFloat:
	save	%sp, -(92 + 8) & -8, %sp	! save room for our double

	fstod	%f0, %f0		! Promote float to double (even register)
	std		%f0, [%fp-8]	! Store double into 8-aligned memory

	set		.$rFmt, %o0		! Set double format string
	ld		[%fp-8], %o1	! Load double into int registers for printf
	ld		[%fp-4], %o2
	call	printf			! Call printf
	nop						! Nop after function call
	
	ret						! Return, we're done!
	restore					! Restore register window (slide baby!)
