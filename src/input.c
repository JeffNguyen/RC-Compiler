//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------
/*
 * Determine which input function to call based on the type of Designator in
 *
 *   ReadStmt ::=        T_CIN T_ISTREAM Designator T_SEMI
 *
 * inputInt() to read an integer from stdin
 * inputFloat() to read a float from stdin
 */

#include <stdio.h>
#include <stdlib.h>

int
inputInt()
{
    int ret;
    if (scanf("%d", &ret) != 1) {
	if ( !feof(stdin) ) {
	  perror("inputInt: invalid input");
	  exit(1);
	} else {
	  exit(0);
	}
    }
    return ret;		/* Integer returned in %o0 */
}

float
inputFloat()
{
    float ret;
    if (scanf("%f", &ret) != 1) {
	if ( !feof(stdin) ) {
	  perror("inputFloat: invalid input");
	  exit(1);
	} else {
	  exit(0);
	}
    }
    return ret;		/* Float returned in %f0 */
}

