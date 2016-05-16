/*
 * Generated Wed May 20 04:17:25 PDT 2015
 */

	! x = true
	set		-4, %o1
	add		%fp, %o1, %o1
	set		1, %o0
	st		%o0, [%o1]

	! cout << false
	set		0, %o0
	call	.$$.printBool
	nop

	! cout << endl
	set		.$$.strEndl, %o0
	call	printf
	nop

