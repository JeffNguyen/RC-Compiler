//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

import java_cup.runtime.*;

import java.math.BigDecimal;
import java.util.Vector;

class MyParser extends parser
{
	private Lexer m_lexer;
	private ErrorPrinter m_errors;
	private boolean m_debugMode;
	private int m_nNumErrors;
	private String m_strLastLexeme;
	private boolean m_bSyntaxError = true;
	private int m_nSavedLineNum;
	AssemblyCodeGenerator m_myAsWriter;
	private int globalCounter;
	private int localCounter;
	private int floatCounter;
	private int assignCounter;
	private int COutCounter;
	private SymbolTable m_symtab;
	final Type int_type   = IntType.getBuiltinType();
	final Type float_type = FloatType.getBuiltinType();
	final Type bool_type = BoolType.getBuiltinType();

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public MyParser(Lexer lexer, ErrorPrinter errors, boolean debugMode)
	{
		m_lexer = lexer;
		m_symtab = new SymbolTable();
		m_errors = errors;
		m_debugMode = debugMode;
		m_nNumErrors = 0;
		m_myAsWriter = new AssemblyCodeGenerator("rc.s");
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public boolean Ok()
	{
		return m_nNumErrors == 0;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public Symbol scan()
	{
		Token t = m_lexer.GetToken();

		//	We'll save the last token read for error messages.
		//	Sometimes, the token is lost reading for the next
		//	token which can be null.
		m_strLastLexeme = t.GetLexeme();

		switch (t.GetCode())
		{
			case sym.T_ID:
			case sym.T_ID_U:
			case sym.T_STR_LITERAL:
			case sym.T_FLOAT_LITERAL:
			case sym.T_INT_LITERAL:
				return new Symbol(t.GetCode(), t.GetLexeme());
			default:
				return new Symbol(t.GetCode());
		}
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void syntax_error(Symbol s)
	{
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void report_fatal_error(Symbol s)
	{
		m_nNumErrors++;
		if (m_bSyntaxError)
		{
			m_nNumErrors++;

			//	It is possible that the error was detected
			//	at the end of a line - in which case, s will
			//	be null.  Instead, we saved the last token
			//	read in to give a more meaningful error 
			//	message.
			m_errors.print(Formatter.toString(ErrorMsg.syntax_error, m_strLastLexeme));
		}
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void unrecovered_syntax_error(Symbol s)
	{
		report_fatal_error(s);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void DisableSyntaxError()
	{
		m_bSyntaxError = false;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void EnableSyntaxError()
	{
		m_bSyntaxError = true;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public String GetFile()
	{
		return m_lexer.getEPFilename();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public int GetLineNum()
	{
		return m_lexer.getLineNumber();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void SaveLineNum()
	{
		m_nSavedLineNum = m_lexer.getLineNumber();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public int GetSavedLineNum()
	{
		return m_nSavedLineNum;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoProgramStart()
	{
		// Opens the global scope.
		m_symtab.openScope(false);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoProgramEnd()
	{
		m_symtab.closeScope();
		m_myAsWriter.dispose();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------

	Type createArrayType(Type basicType, Vector<STO> arrayList)
	{
		final Type int_type = IntType.getBuiltinType();

		Type type = basicType;

		if (arrayList != null)
		{
			/* for checking use the order from left to right */
			for (STO item : arrayList)
			{
				if (item.isError())
				{
					/* Silent below warnings, like in case when we use undeclared symbol int foobar[not_existing_constant] */
					return new ErrorType();
				}

				if (item.getType() != int_type)
				{
					m_nNumErrors++;
					m_errors.print(Formatter.toString(ErrorMsg.error10i_Array, item.getType().getName()));
					return new ErrorType();
				}

				if (item.isConst() == false)
				{
					m_nNumErrors++;
					m_errors.print(ErrorMsg.error10c_Array);
					return new ErrorType();
				}

				int arrayLen = ((ConstSTO) item).getIntValue();

				if (arrayLen <= 0)
				{
					m_nNumErrors++;
					m_errors.print(Formatter.toString(ErrorMsg.error10z_Array, arrayLen));
					return new ErrorType();
				}
			}

			/* when accesing array (in expression), use the order from right to left */
			int idx = arrayList.size();
			String bracketStr = "";

			while (--idx >= 0)
			{
				ConstSTO item = (ConstSTO) arrayList.get(idx);

				bracketStr = "[" + item.getIntValue() + "]" + bracketStr;
				String name = basicType.getName() + bracketStr;
				type = new ArrayType(name, type, item.getIntValue());
			}
		}

		return type;
	}

	STO DoParamDecl(Type basicType, Vector<STO> arrayList, Boolean refval, String id)
	{
		if (m_symtab.accessLocal(id) != null)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
		}

		Type type = createArrayType(basicType, arrayList);

		VarSTO sto = new VarSTO(id, type, refval);
		m_symtab.insert(sto);

		return sto;
	}

	STO DoVarDecl(String initStatic, Type basicType, Vector<STO> arrayList, String id, STO initExpr)
	{
		
		String name = "var " + id + ((initExpr != null) ? (" = " + initExpr.getName()) : "");

		if (m_symtab.accessLocal(id) != null)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
			return new ErrorSTO(name);
		}

		Type type = createArrayType(basicType, arrayList);

		VarSTO sto = new VarSTO(id, type);
		
		if (m_symtab.getLevel() > 1){
			sto.setVisibility("Local");
		}
		else {
			sto.setVisibility("Global");
		}
		m_symtab.insert(sto);

		if (initExpr != null)
		{
			if (initExpr.isError())
			{
				return new ErrorSTO(name);
			}

			if (Type.checkTypeAssignable(type, initExpr.getType()) == false)
			{
				m_nNumErrors++;
				m_errors.print(Formatter.toString(ErrorMsg.error8_Assign, initExpr.getType().getName(), type.getName()));
				return new ErrorSTO(name);
			}
		}
		if (m_symtab.getLevel() > 1){
			localCounter++;
			sto.setBase("%fp");
			sto.setOffset(Integer.toString(localCounter * -4));
		}
		else{
			sto.setBase("%g0");
			sto.setOffset(id);
		}
		
		if (m_symtab.getLevel() == 1)
		{
			m_myAsWriter.doGlobalDecl(initStatic, basicType, id, initExpr);
		}
		else {
			// Pass in localCounter because sto"OFFSET" isn't set yet, but need to do local declaration
			// For doAssign, offset and base are set so no passing in counter or increment, just straight calling
			m_myAsWriter.doLocalDecl(initStatic, basicType, id, initExpr, localCounter);
		}
		return sto;
	}

	void DoVarDecl(String id)
	{
		DoVarDecl(null, null, null, id, null);
	}

	STO DoVarStructDecl(Type structType, Vector<STO> arrayList, String id, Vector<STO> ctorExpr)
	{
		String name = "var " + id + ((ctorExpr != null) ? (" : " + "...") : "");

		if (structType.isError())
		{
			/* Type is Error, for example when user used type which is not a declared struct, don't report more errors */
			return new ErrorSTO(name);
		}

		if (m_symtab.accessLocal(id) != null)
		{
			m_nNumErrors++;

			if (m_symtab.getStructdef() != null)
				m_errors.print(Formatter.toString(ErrorMsg.error13a_Struct, id));
			else
				m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
			return new ErrorSTO(name);
		}

		Type type = createArrayType(structType, arrayList);

		VarSTO sto = new VarSTO(id, type);
		m_symtab.insert(sto);

		return DoFuncCall(((StructType) structType).getCtorList(), ctorExpr);
	}

	void DoFieldVarDecl(Type basicType, Vector<STO> arrayList, String id)
	{
		String name = "var " + id;

		StructdefSTO sto = m_symtab.getStructdef();
		if (sto == null)
		{
			m_nNumErrors++;
			m_errors.print("internal: DoFieldVarDecl says no struct!");
			return;
		}

		StructType structType = (StructType) sto.getType();

		Scope symtab = structType.getScope();
		if (symtab.accessLocal(id) != null)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error13a_Struct, id));
			return;
		}

		Type type = createArrayType(basicType, arrayList);

		VarSTO fieldSto = new VarSTO(id, type);
		structType.addVar(fieldSto);
	}

	void DoForeachCheck(Type type, Boolean refval, String id, STO expr)
	{
		/* String name = "foreach " + id + " : " + expr.getName(); */

		STO varSTO = DoVarDecl(null, type, null, id, null);

		if (varSTO.isError() || expr.isError())
		{
			return;
		}

		if (!(expr.getType() instanceof ArrayType))
		{
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error12a_Foreach);
			return;
		}

		Type elementType = ((ArrayType) expr.getType()).getItemType();

		if (refval == Boolean.FALSE && Type.checkTypeAssignable(type, elementType) == false)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error12v_Foreach, elementType.getName(), id, type.getName()));
		}
		else if (refval == Boolean.TRUE && type.equals(elementType) == false)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error12r_Foreach, elementType.getName(), id, type.getName()));
		}
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoExternDecl(Type basicType, Vector<STO> arrayList, String id)
	{
		if (m_symtab.accessLocal(id) != null)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
		}

		Type type = createArrayType(basicType, arrayList);

		VarSTO sto = new VarSTO(id, type);
		m_symtab.insert(sto);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoConstDecl(Type type, String id, STO initExpr)
	{
		if (m_symtab.accessLocal(id) != null)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
		}

		String name = "const " + id + " = " + initExpr.getName();

		if (initExpr.isError())
		{
			return; /* new ErrorSTO(name); */
		}

		if (initExpr.isConst() == false)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error8_CompileTime, id));
			return; /* new ErrorSTO(name); */
		}

		ConstSTO sto = new ConstSTO(id, type, ((ConstSTO) initExpr).getValue());
		sto.setIsAddressable(true);
		if (m_symtab.getLevel() > 1){
			sto.setVisibility("Local");
		}
		else {
			sto.setVisibility("Global");
		}
		m_symtab.insert(sto);

		if (m_debugMode) { System.err.println("variable: " + sto.getName() + " evaluates to: " + sto.getValue()); }

		if (Type.checkTypeAssignable(type, initExpr.getType()) == false)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error8_Assign, initExpr.getType().getName(), type.getName()));
			return; /* new ErrorSTO(name); */
		}
		if (m_symtab.getLevel() > 1){
			localCounter++;
			sto.setBase("%fp");
			sto.setOffset(Integer.toString(localCounter * -4));
		}
		else{
			sto.setBase("%g0");
			sto.setOffset(id);
		}
		
		if (m_symtab.getLevel() == 1)
		{
			m_myAsWriter.doGlobalConstDecl(type, id, initExpr);
		}
		else {
			// Pass in localCounter because sto"OFFSET" isn't set yet, but need to do local declaration
			// For doAssign, offset and base are set so no passing in counter or increment, just straight calling
			m_myAsWriter.doLocalConstDecl(type, id, initExpr, localCounter);
		}
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoStructdefDecl_1(String id)
	{
		String name = "structdef " + id;

		if (m_symtab.accessLocal(id) != null)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
			return new ErrorSTO(name);
		}
		
		StructdefSTO sto = new StructdefSTO(id);
		m_symtab.insert(sto);

		DoBlockOpen(false);
		m_symtab.setStructdef(sto);

		return sto;
	}

	void DoStructdefDeclDefaultCtor()
	{
		StructdefSTO sto = m_symtab.getStructdef();
		if (sto == null)
		{
			m_nNumErrors++;
			m_errors.print ("internal: DoStructdefDeclDefaultCtor says no struct!");
			return;
		}

		/* XXX, hacky, use symbol 'structname' to get ctor list */
		FuncListSTO ctorList = null;

		STO tmp = ((StructType) sto.getType()).getScope().accessLocal(sto.getName());
		if (tmp != null && tmp.isFunc() == true)
		{
			/* XXX, it should be forbidden to use structure name as other STO than FuncSTO */
			ctorList = ((FuncListSTO) tmp);
		}

		((StructType) sto.getType()).setCtorList(ctorList);
	}

	void DoStructdefDecl_2()
	{
		m_symtab.setStructdef(null);
		DoBlockClose();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoFuncDecl_1(Type type, String id, boolean modLValue)
	{
		FuncListSTO stoList = null;

		StructdefSTO structDef = m_symtab.getStructdef();

		Scope scope = m_symtab.getScope();
		if (structDef != null)
		{
			scope = ((StructType) structDef.getType()).getScope();
		}

		STO tmp = scope.accessLocal(id);
		if (tmp != null)
		{
			if (tmp.isFunc() == false)
			{
				m_nNumErrors++;
				if (structDef != null)
					m_errors.print(Formatter.toString(ErrorMsg.error13a_Struct, id));
				else
					m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
			}
			else
			{
				stoList = ((FuncListSTO) tmp);
			}
		}

		if (modLValue == true && type == VoidType.getBuiltinType())
		{
			/* XXX, uhm, error for such case not defined, do it not fatal warning */
			/* m_nNumErrors++; */
			m_errors.print(Formatter.toString("IEIEIE function %S with void type declared, but return-by-reference set (& ignored)!", id));

			/* in such case ignore modLValue, this will also protect from NullPointerException in DoReturnCheck() when checking for expr.isModLValue()  */
			modLValue = false;
		}

		if (stoList == null)
		{
			stoList = new FuncListSTO(id);
			if (tmp == null) { scope.InsertLocal(stoList); }
		}

		FuncSTO sto = new FuncSTO(id, stoList);
		sto.setReturnType(type, modLValue);
		m_symtab.openScope(false);
		m_symtab.setFunc(sto);
	}

	void DoFuncDecl_1(Type type, String id)
	{
		DoFuncDecl_1(type, id, false);
	}

	void DoCtorDecl_1(String id)
	{
		DoFuncDecl_1(VoidType.getBuiltinType(), id);

		StructdefSTO sto = m_symtab.getStructdef();
		if (sto == null)
		{
			m_nNumErrors++;
			m_errors.print ("internal: DoCtorDecl_1 says no struct!");
			return;
		}

		if (sto.getName().equals(id) == false)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error13b_Ctor, id, sto.getName()));
			return;
		}
	}

	void DoDtorDecl_1(String realId)
	{
		String id = "~" + realId;

		DoFuncDecl_1(VoidType.getBuiltinType(), id);

		StructdefSTO sto = m_symtab.getStructdef();
		if (sto == null)
		{
			m_nNumErrors++;
			m_errors.print ("internal: DoDtorDecl_1 says no struct!");
			return;
		}

		if (sto.getName().equals(realId) == false)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error13b_Dtor, id, sto.getName()));
			return;
		}
	}

	void DoFuncDeclReturnCheck()
	{
		FuncSTO sto = m_symtab.getFunc();

		if (sto == null)
		{
			m_nNumErrors++;
			m_errors.print ("internal: DoFuncDeclReturnCheck says no proc!");
			return;
		}

		/* Check if there was return statement on same level as function one */
		if (sto.getReturnType() != VoidType.getBuiltinType() && sto.returnStatementMinLevel() != m_symtab.getLevel())
		{
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error6c_Return_missing);
		}
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoFuncDecl_2()
	{
		m_symtab.closeScope();
		m_symtab.setFunc(null);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoFormalParams(Vector<VarSTO> params)
	{
		FuncSTO sto = m_symtab.getFunc();

		if (sto == null)
		{
			m_nNumErrors++;
			m_errors.print ("internal: DoFormalParams says no proc!");
			return;
		}

		sto.setParams(params);

		FuncListSTO stoList = sto.getList();

		/* this will fail, if function overload is illegal */
		if (stoList.addOverload(sto) == false)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error9_Decl, sto.getName()));
		}
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoBlockOpen(boolean loopBlock)
	{
		// Open a scope.
		m_symtab.openScope(loopBlock);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoBlockClose()
	{
		m_symtab.closeScope();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoAssignExpr(STO stoDes, STO expr)
	{
		if (stoDes.getVisibility().equals("Local")){
			assignCounter++;
		}
		final Type int_type = IntType.getBuiltinType();
		final Type float_type = FloatType.getBuiltinType();
		final Type bool_type = BoolType.getBuiltinType();
		
		m_myAsWriter.doAssign(stoDes, expr, assignCounter);
		
		String name = stoDes.getName() + " = " + " " + expr.getName();

		/* only report one error per assign statement */
		/* TODO: this is still failing for '(2 = 3) = 5 = 3 = false;' */
		if (stoDes.isError() || expr.isError())
		{
			return new ErrorSTO(name);
		}

		if (!stoDes.isModLValue())
		{
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error3a_Assign);
			return new ErrorSTO(name);
		}

		if (stoDes.getType() == null || expr.getType() == null) { return debugNoTypeSkip(name); }

		if (Type.checkTypeAssignable(stoDes.getType(), expr.getType()) == false)
		{
			m_nNumErrors++;
			/* Value of type <TYPE1> not assignable to variable of type <TYPE2> */
			m_errors.print(Formatter.toString(ErrorMsg.error3b_Assign, expr.getType().getName(), stoDes.getType().getName()));
			return new ErrorSTO(name);
		}

		return new ExprSTO(name, stoDes.getType());
		//return stoDes;
	}

	STO debugNoTypeSkip(String name)
	{
		if (m_debugMode) { System.err.println("No Type information for: " + name); }
		return new ExprSTO(name);
	}

	ConstSTO debugConstantFolding(ConstSTO expr)
	{
		if (m_debugMode) { System.err.println("expression: " + expr.getName() + " evaluates to: " + expr.getValue()); }
		return expr;
	}

	STO DoBinaryNumericExpr(STO expr1, String operator, STO expr2)
	{
		/* The operand types must be numeric (equivalent to either int or float), and the resulting type is int when both operands are int, or float otherwise. */
		final Type int_type   = IntType.getBuiltinType();
		final Type float_type = FloatType.getBuiltinType();

		Type type;
		BigDecimal resultValue = null;

		String name = "(" + expr1.getName() + " " + operator + " " + expr2.getName() + ")";

		if (expr1.isError() || expr2.isError())
		{
			return new ErrorSTO(name);
		}

		if ("%".equals(operator))
		{
			/* The operand types must be equivalent to int, and the resulting type is int. */
			return DoBinaryIntExpr(expr1, operator, expr2);
		}

		if (expr1.getType() == null || expr2.getType() == null) { return debugNoTypeSkip(name); }

		if (expr1.getType() != float_type && expr1.getType() != int_type)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error1n_Expr, expr1.getType().getName(), operator));
			return new ErrorSTO(name);
		}

		if (expr2.getType() != float_type && expr2.getType() != int_type)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error1n_Expr, expr2.getType().getName(), operator));
			return new ErrorSTO(name);
		}

		if ((expr1.getType() == float_type || expr2.getType() == float_type))
		{
			type = float_type;
		}
		else
		{
			type = int_type;
		}

		if (expr1.isConst() && expr2.isConst())
		{
			BigDecimal number1 = ((ConstSTO) expr1).getValue();
			BigDecimal number2 = ((ConstSTO) expr2).getValue();

			if ("+".equals(operator)) {
				resultValue =  number1.add(number2);
				//return new ExprSTO(name, type, expr.getValue());
				return debugConstantFolding(new ConstSTO(name, type, number1.add(number2)));
			}
			if ("-".equals(operator)) {
				resultValue = number1.subtract(number2);
				//return new ExprSTO(name, type, expr.getValue());
				return debugConstantFolding(new ConstSTO(name, type, number1.subtract(number2)));
			}

			if ("*".equals(operator)) {
				resultValue = number1.multiply(number2);
				//return new ExprSTO(name, type, expr.getValue());
				return debugConstantFolding(new ConstSTO(name, type, number1.multiply(number2)));
			}
			if ("/".equals(operator))
			{
				if (BigDecimal.ZERO.compareTo(number2) == 0)
				{
					m_nNumErrors++;
					m_errors.print(ErrorMsg.error8_Arithmetic);
					return new ErrorSTO(name);
				}

				if (type == int_type)
				{
					resultValue = number1.divideToIntegralValue(number2);
					//return new ExprSTO(name, type, expr.getValue());
					return debugConstantFolding(new ConstSTO(name, type, number1.divideToIntegralValue(number2)));
				}
				else
				{
					resultValue = number1.divide(number2, java.math.MathContext.DECIMAL64);
					//return new ExprSTO(name, type, expr.getValue());
					return debugConstantFolding(new ConstSTO(name, type, number1.divide(number2, java.math.MathContext.DECIMAL64)));
				}
			}
			m_errors.print("internal: constant folding in DoBinaryNumericExpr() unknown operator, expression: " + operator);
		}
		/* XXX, here should be some BinaryExprSTO */
		return new ExprSTO(name, type, resultValue);
		
	}

	STO DoUnaryNumericExpr(String operator, STO expr)
	{
		/* The operand types must be numeric (equivalent to either int or float), and the resulting type is int when both operands are int, or float otherwise. */
		final Type int_type   = IntType.getBuiltinType();
		final Type float_type = FloatType.getBuiltinType();

		String name = "(" + operator + " " + expr.getName() + ")";

		if (expr.isError())
		{
			return new ErrorSTO(name);
		}

		if (expr.getType() == null) { return debugNoTypeSkip(name); }

		if (expr.getType() != float_type && expr.getType() != int_type)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error1u_Expr, expr.getType().getName(), operator, "numeric (int/float)"));
			return new ErrorSTO(name);
		}

		if (expr.isConst())
		{
			BigDecimal number = ((ConstSTO) expr).getValue();

			if ("-".equals(operator))  return debugConstantFolding(new ConstSTO(name, expr.getType(), number.negate()));
			if ("+".equals(operator))  return debugConstantFolding(new ConstSTO(name, expr.getType(), number));

			m_errors.print("internal: constant folding in DoUnaryNumericExpr() unknown operator, expression: " + operator);
		}


		/* XXX, here should be some UnaryExprSTO */
		return new ExprSTO(name, expr.getType());
	}

	STO DoBinaryNumericRelationExpr(STO expr1, String operator, STO expr2)
	{
		/* The operand types must be numeric, and the resulting type is bool. */
		final Type int_type   = IntType.getBuiltinType();
		final Type float_type = FloatType.getBuiltinType();
		final Type bool_type  = BoolType.getBuiltinType();

		String name = "(" + expr1.getName() + " " + operator + " " + expr2.getName() + ")";

		if (expr1.isError() || expr2.isError())
		{
			return new ErrorSTO(name);
		}

		if (expr1.getType() == null || expr2.getType() == null) { return debugNoTypeSkip(name); }

		if (expr1.getType() != float_type && expr1.getType() != int_type)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error1n_Expr, expr1.getType().getName(), operator));
			return new ErrorSTO(name);
		}

		if (expr2.getType() != float_type && expr2.getType() != int_type)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error1n_Expr, expr2.getType().getName(), operator));
			return new ErrorSTO(name);
		}

		if (expr1.isConst() && expr2.isConst())
		{
			BigDecimal number1 = ((ConstSTO) expr1).getValue();
			BigDecimal number2 = ((ConstSTO) expr2).getValue();

			if ("<".equals(operator))  return debugConstantFolding(new ConstSTO(name, bool_type, number1.compareTo(number2)  < 0));
			if (">".equals(operator))  return debugConstantFolding(new ConstSTO(name, bool_type, number1.compareTo(number2)  > 0));
			if ("<=".equals(operator)) return debugConstantFolding(new ConstSTO(name, bool_type, number1.compareTo(number2) <= 0));
			if (">=".equals(operator)) return debugConstantFolding(new ConstSTO(name, bool_type, number1.compareTo(number2) >= 0));

			m_errors.print("internal: constant folding in DoBinaryNumericRelationExpr() unknown operator?, expression: " + operator);
		}

		/* XXX, here should be some BinaryExprSTO */
		return new ExprSTO(name, bool_type);
	}

	STO DoBinaryEqExpr(STO expr1, String operator, STO expr2)
	{
		/* The operand types must be either BOTH numeric, or BOTH equivalent to bool, and the resulting type is bool. */
		final Type int_type   = IntType.getBuiltinType();
		final Type float_type = FloatType.getBuiltinType();
		final Type bool_type  = BoolType.getBuiltinType();

		String name = "(" + expr1.getName() + " " + operator + " " + expr2.getName() + ")";

		if (expr1.isError() || expr2.isError())
		{
			return new ErrorSTO(name);
		}

		if (expr1.getType() == null || expr2.getType() == null) { return debugNoTypeSkip(name); }

		if (expr1.getType() instanceof PointerType || expr2.getType() instanceof PointerType)
		{
			if (!(expr1.getType() instanceof PointerType && expr2.getType() instanceof PointerType) ||
			   (expr1.getType().equals(expr2.getType()) == false &&
			    expr1.getType() != PointerType.getNullPtrBuiltinType() &&
			    expr2.getType() != PointerType.getNullPtrBuiltinType()))
			{
				m_nNumErrors++;
				m_errors.print(Formatter.toString(ErrorMsg.error17_Expr, operator, expr1.getType().getName(), expr2.getType().getName()));
				return new ErrorSTO(name);
			}
		}
		else if (expr1.getType() != int_type && expr1.getType() != float_type && expr1.getType() != bool_type)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error1b_Expr, expr1.getType().getName(), operator, expr2.getType().getName()));
			return new ErrorSTO(name);
		}
		else if (expr2.getType() != int_type && expr2.getType() != float_type && expr2.getType() != bool_type)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error1b_Expr, expr1.getType().getName(), operator, expr2.getType().getName()));
			return new ErrorSTO(name);
		}
		else if ((expr1.getType() == bool_type || expr2.getType() == bool_type) && expr1.getType() != expr2.getType())
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error1b_Expr, expr1.getType().getName(), operator, expr2.getType().getName()));
			return new ErrorSTO(name);
		}

		if (expr1.isConst() && expr2.isConst())
		{
			BigDecimal number1 = ((ConstSTO) expr1).getValue();
			BigDecimal number2 = ((ConstSTO) expr2).getValue();

			if ("==".equals(operator)) return debugConstantFolding(new ConstSTO(name, bool_type, number1.compareTo(number2) == 0));
			if ("!=".equals(operator)) return debugConstantFolding(new ConstSTO(name, bool_type, number1.compareTo(number2) != 0));

			m_errors.print("internal: constant folding in DoBinaryEqExpr() unknown operator?, expression: " + operator);
		}

		/* XXX, here should be some BinaryExprSTO */
		return new ExprSTO(name, bool_type);
	}

	STO DoBinaryLogicExpr(STO expr1, String operator, STO expr2)
	{
		/* The operand types must be equivalent to bool, and the resulting type is bool. */
		final Type bool_type = BoolType.getBuiltinType();

		String name = "(" + expr1.getName() + " " + operator + " " + expr2.getName() + ")";

		if (expr1.isError() || expr2.isError())
		{
			return new ErrorSTO(name);
		}

		if (expr1.getType() == null || expr2.getType() == null) { return debugNoTypeSkip(name); }

		if (expr1.getType() != bool_type)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error1w_Expr, expr1.getType().getName(), operator, bool_type.getName()));
			return new ErrorSTO(name);
		}

		if (expr2.getType() != bool_type)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error1w_Expr, expr2.getType().getName(), operator, bool_type.getName()));
			return new ErrorSTO(name);
		}

		if (expr1.isConst() && expr2.isConst())
		{
			boolean value1 = ((ConstSTO) expr1).getBoolValue();
			boolean value2 = ((ConstSTO) expr2).getBoolValue();

			if ("&&".equals(operator)) return debugConstantFolding(new ConstSTO(name, bool_type, value1 && value2));
			if ("||".equals(operator)) return debugConstantFolding(new ConstSTO(name, bool_type, value1 || value2));

			m_errors.print("internal: constant folding in DoBinaryLogicExpr() unknown operator?, expression: " + operator);
		}

		/* XXX, here should be some BinaryExprSTO */
		return new ExprSTO(name, bool_type);
	}

	STO DoUnaryLogicExpr(String operator, STO expr)
	{
		/* The operand types must be equivalent to bool, and the resulting type is bool. */
		final Type bool_type = BoolType.getBuiltinType();

		String name = "(" + operator + " " + expr.getName() + ")";

		if (expr.isError())
		{
			return new ErrorSTO(name);
		}

		if (expr.getType() == null) { return debugNoTypeSkip(name); }

		if (expr.getType() != bool_type)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error1u_Expr, expr.getType().getName(), operator, bool_type.getName()));
			return new ErrorSTO(name);
		}

		if (expr.isConst())
		{
			boolean value = ((ConstSTO) expr).getBoolValue();

			if ("!".equals(operator)) return debugConstantFolding(new ConstSTO(name, bool_type, !value));

			m_errors.print("internal: constant folding in DoUnaryLogicExpr() unknown operator?, expression: " + operator);
		}

		/* XXX, here should be some UnaryExprSTO */
		return new ExprSTO(name, bool_type);
	}

	STO DoBinaryIntExpr(STO expr1, String operator, STO expr2)
	{
		/* The operand types must be equivalent to int, and the resulting type is int. */
		final Type int_type   = IntType.getBuiltinType();

		String name = "(" + expr1.getName() + " " + operator + " " + expr2.getName() + ")";

		if (expr1.isError() || expr2.isError())
		{
			return new ErrorSTO(name);
		}

		if (expr1.getType() == null || expr2.getType() == null) { return debugNoTypeSkip(name); }

		if (expr1.getType() != int_type)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error1w_Expr, expr1.getType().getName(), operator, int_type.getName()));
			return new ErrorSTO(name);
		}

		if (expr2.getType() != int_type)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error1w_Expr, expr2.getType().getName(), operator, int_type.getName()));
			return new ErrorSTO(name);
		}

		if (expr1.isConst() && expr2.isConst())
		{
			BigDecimal number1 = ((ConstSTO) expr1).getValue();
			BigDecimal number2 = ((ConstSTO) expr2).getValue();

			if ("&".equals(operator)) return debugConstantFolding(new ConstSTO(name, int_type, number1.intValue() & number2.intValue()));
			if ("|".equals(operator)) return debugConstantFolding(new ConstSTO(name, int_type, number1.intValue() | number2.intValue()));
			if ("^".equals(operator)) return debugConstantFolding(new ConstSTO(name, int_type, number1.intValue() ^ number2.intValue()));

			if ("%".equals(operator))
			{
				if (number2.intValue() == 0)
				{
					m_nNumErrors++;
					m_errors.print(ErrorMsg.error8_Arithmetic);
					return new ErrorSTO(name);
				}
				return debugConstantFolding(new ConstSTO(name, int_type, number1.intValue() % number2.intValue()));
			}

			m_errors.print("internal: constant folding in DoBinaryIntExpr() unknown operator?, expression: " + operator);
		}

		/* XXX, here should be some BinaryExprSTO */
		return new ExprSTO(name, int_type);
	}

	STO DoIncDecNumericExpr(String operator, STO expr)
	{
		/* The operand types must be equivalent to numeric */
		final Type int_type   = IntType.getBuiltinType();
		final Type float_type = FloatType.getBuiltinType();

		String name = "(" + expr.getName() + operator + ")"; /* XXX, or reverse (operator expression) */

		if (expr.isError())
		{
			return new ErrorSTO(name);
		}

		if (expr.getType() == null) { return debugNoTypeSkip(name); }

		/* XXX, what about nullptr, should it print error about wrong type, or it's enough to show error about not modifiable l-value ? */
		if (expr.getType() != float_type && expr.getType() != int_type && !(expr.getType() instanceof PointerType))
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error2_Type, expr.getType().getName(), operator));
			return new ErrorSTO(name);
		}

		if (expr.isModLValue() == false)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error2_Lval, operator));
			return new ErrorSTO(name);
		}

		/* XXX, here should be some UnaryExprSTO */
		return new ExprSTO(name, expr.getType());
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoFuncCall(STO sto, Vector<STO> args)
	{
		int args_count = (args != null) ? args.size() : 0;

		String fullName = sto.getName() + "(";
		for (int i = 0; i < args_count; i++)
		{
			STO arg = (STO) args.get(i);

			if (i != 0)
				fullName += ", ";

			fullName += arg.getName();
		}
		fullName += ")";

		if (sto.isError())
		{
			return new ErrorSTO(fullName);
		}

		if (!sto.isFunc())
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.not_function, sto.getName()));
			return new ErrorSTO(fullName);
		}

		FuncListSTO funcList = ((FuncListSTO) sto);

		FuncSTO func = funcList.findOverloadOrOnlyOne(args);
		if (func == null)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error9_Illegal, sto.getName()));
			return new ErrorSTO(fullName);
		}

		Vector<VarSTO> params = func.getParams();
		int params_count = (params != null) ? params.size() : 0;

		if (args_count != params_count)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error5n_Call, args_count, params_count));
			return new ErrorSTO(sto.getName());
		}

		ErrorSTO first_error = null;

		for (int i = 0; i < args_count; i++)
		{
			STO arg      = args.get(i);
			VarSTO param = params.get(i);

			ErrorSTO arg_error = null;

			String name = sto.getName() + "( ... " + arg.getName() + " ... )";

			boolean pass_by_ref = param.getRefValue();

			if (arg.getType() == null) { debugNoTypeSkip(arg.getName()); continue; }
			if (param.getType() == null) { debugNoTypeSkip(param.getName()); continue; }

			if (arg.isError())
			{
				arg_error = new ErrorSTO(name);
			}
			else if (pass_by_ref == false && Type.checkTypeAssignable(param.getType(), arg.getType()) == false)
			{
				m_nNumErrors++;
				m_errors.print(Formatter.toString(ErrorMsg.error5a_Call, arg.getType().getName(), param.getName(), param.getType().getName()));
				arg_error = new ErrorSTO(name);
			}
			else if (pass_by_ref == true && param.getType().equals(arg.getType()) == false)
			{
				m_nNumErrors++;
				m_errors.print(Formatter.toString(ErrorMsg.error5r_Call, arg.getType().getName(), param.getName(), param.getType().getName()));
				arg_error = new ErrorSTO(name);
			}

			if (arg_error == null && pass_by_ref == true && (arg.isModLValue() == false && !(arg.getType() instanceof ArrayType)))
			{
				m_nNumErrors++;
				m_errors.print(Formatter.toString(ErrorMsg.error5c_Call, param.getName(), param.getType().getName()));
				arg_error = new ErrorSTO(name);
			}

			if (first_error == null) first_error = arg_error;
		}

		if (first_error != null)
		{
			return first_error;
		}

		STO expr = new ExprSTO(fullName, func.getReturnType());
		if (func.getReturnTypeByReference() == true)
		{
			expr.setIsAddressable(true);
			expr.setIsModifiable(true);
		}

		return expr;
	}

	STO DoDesignator_Ampersand(STO expr)
	{
		String name = "&" + expr.getName();

		if (expr.isError())
		{
			return new ErrorSTO(name);
		}

		if (expr.getIsAddressable() == false)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error18_AddressOf, expr.getType().getName()));
			return new ErrorSTO(name);
		}

		return new ExprSTO(name, new PointerType(expr.getType()));
	}

	STO DoDesignator_Star(STO expr)
	{
		String name = "(*" + expr.getName() + ")";

		if (expr.isError())
		{
			return new ErrorSTO(name);
		}

		if ("nullptr".equals(expr.getName()))
		{
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error15_Nullptr);
			return new ErrorSTO(name);
		}

		if (!(expr.getType() instanceof PointerType))
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error15_Receiver, expr.getType().getName()));
			return new ErrorSTO(name);
		}

		PointerType type = (PointerType) expr.getType();

		STO ret = new ExprSTO(name, type.getDerefType());
		ret.setIsAddressable(true);
		ret.setIsModifiable(true);

		return ret;
	}

	STO DoDesignator_Arrow(STO expr, String strID)
	{
		String name = expr.getName() + "." + strID;

		if (expr.isError())
		{
			return new ErrorSTO(name);
		}

		if ("nullptr".equals(expr.getName()))
		{
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error15_Nullptr);
			return new ErrorSTO(name);
		}

		if (!(expr.getType() instanceof PointerType))
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error15_ReceiverArrow, expr.getType().getName()));
			return new ErrorSTO(name);
		}

		PointerType ptype = (PointerType) expr.getType();

		if (!(ptype.getDerefType() instanceof StructType))
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error15_ReceiverArrow, expr.getType().getName()));
			return new ErrorSTO(name);
		}

		StructType type = (StructType) ptype.getDerefType();

		STO arrowSto = type.getScope().access(strID);
		if (arrowSto == null)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error14f_StructExp, strID, type.getName()));
			return new ErrorSTO(name);
		}

		return arrowSto;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoDesignator2_Dot(STO sto, String strID)
	{
		String name = sto.getName() + "." + strID;

		if (sto.isError())
		{
			return new ErrorSTO(name);
		}

		if (!(sto.getType() instanceof StructType))
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error14t_StructExp, sto.getType().getName()));
			return new ErrorSTO(sto.getName());
		}

		StructType type = (StructType) sto.getType();

		STO fieldSto = type.getScope().access(strID);
		if (fieldSto == null)
		{
			m_nNumErrors++;
			/* XXX, what if some function returns "this", should it also print about current, or only if "this" keyword is explicit used? */
			if ("this".equals(sto.getName()) == true)
				m_errors.print(Formatter.toString(ErrorMsg.error14c_StructExpThis, strID));
			else
				m_errors.print(Formatter.toString(ErrorMsg.error14f_StructExp, strID, type.getName()));
			return new ErrorSTO(sto.getName());
		}

		return fieldSto;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoDesignator2_Array(STO sto, STO indexExpr)
	{
		final Type int_type = IntType.getBuiltinType();

		String name = sto.getName() + "[" + indexExpr.getName() + "]";

		if (sto.isError() || indexExpr.isError())
		{
			/* don't report another error, like when sto is undefined symbol */
			return new ErrorSTO(name);
		}

		if ("nullptr".equals(sto.getName()))
		{
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error15_Nullptr);
			return new ErrorSTO(name);
		}

		Type itemType;
		if (sto.getType() instanceof ArrayType)
		{
			itemType = ((ArrayType) sto.getType()).getItemType();
		}
		else if (sto.getType() instanceof PointerType)
		{
			itemType = ((PointerType) sto.getType()).getDerefType();
		}
		else
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error11t_ArrExp, sto.getType().getName()));
			return new ErrorSTO(name);
		}

		if (indexExpr.getType() != int_type)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error11i_ArrExp, indexExpr.getType().getName()));
			return new ErrorSTO(name);
		}

		if (indexExpr.isConst() && sto.getType() instanceof ArrayType)
		{
			ArrayType arrayType = (ArrayType) sto.getType();
			int index = ((ConstSTO) indexExpr).getIntValue();

			if (index < 0 || arrayType.getCount() <= index)
			{
				m_nNumErrors++;
				m_errors.print(Formatter.toString(ErrorMsg.error11b_ArrExp, index, arrayType.getCount()));
				return new ErrorSTO(name);
			}
		}

		STO expr = new ExprSTO(name, itemType);

		/* XXX, ugly, expr.getType().isModifableType() ? */
		if (!(expr.getType() instanceof ArrayType))
		{
			/* This is now basic type so it is modifiable L-value */
                        expr.setIsAddressable(true);
                        expr.setIsModifiable(true);
		}

		return expr;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoDesignator3_GlobalID(String strID)
	{
		STO sto;

		if ((sto = m_symtab.accessGlobal(strID)) == null)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error0g_Scope, strID));
			sto = new ErrorSTO(strID);
		}

		return sto;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoDesignator3_ID(String strID)
	{
		STO sto;

		if ((sto = m_symtab.access(strID)) == null)
		{
			m_nNumErrors++;
		 	m_errors.print(Formatter.toString(ErrorMsg.undeclared_id, strID));
			sto = new ErrorSTO(strID);
		}

		return sto;
	}

	STO DoDesignator3_this()
	{
		StructdefSTO sto = m_symtab.getStructdef();
		if (sto == null)
		{
			/* The "this" keyword will only be tested inside struct member functions and constructors/destructors. (p.25) */
			m_nNumErrors++;
			m_errors.print ("internal: DoDesignator3_this says no struct!");
			return new ErrorSTO("this");
		}

		return new ExprSTO ("this", sto.getType());
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	Type DoStructType_ID(String strID)
	{
		STO sto;

		if ((sto = m_symtab.access(strID)) == null)
		{
			m_nNumErrors++;
		 	m_errors.print(Formatter.toString(ErrorMsg.undeclared_id, strID));
			return new ErrorType();
		}

		if (!sto.isStructdef())
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.not_type, sto.getName()));
			return new ErrorType();
		}

		return sto.getType();
	}

	void DoReturnCheck(STO expr)
	{
		FuncSTO sto = m_symtab.getFunc();

		if (sto == null)
		{
			m_nNumErrors++;
			m_errors.print ("internal: DoReturnCheck says no proc!");
			return;
		}
		sto.setReturnStatementLevel(m_symtab.getLevel());

		if (expr != null && expr.isError())
		{
			return;
		}

		if (expr == null && sto.getReturnType() != VoidType.getBuiltinType())
		{
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error6a_Return_expr);
			return;
		}

		Type expressionReturnType  = (expr != null) ? expr.getType() : VoidType.getBuiltinType();
		Type functionReturnType    = sto.getReturnType();

		if (sto.getReturnTypeByReference() == false)
		{
			/* return by value */
			if (!Type.checkTypeAssignable(functionReturnType, expressionReturnType))
			{
				m_nNumErrors++;
				m_errors.print(Formatter.toString(ErrorMsg.error6a_Return_type, expressionReturnType.getName(), functionReturnType.getName()));
				return;

			}
		}
		else
		{
			/* return by reference */
			if (functionReturnType.equals(expressionReturnType) == false)
			{
				m_nNumErrors++;
				m_errors.print(Formatter.toString(ErrorMsg.error6b_Return_equiv, expressionReturnType.getName(), functionReturnType.getName()));
				return;
			}

			if (expr.isModLValue() == false)
			{
				m_nNumErrors++;
				m_errors.print(ErrorMsg.error6b_Return_modlval);
				return;
			}
		}
	}

	void DoBoolLogicCheck(STO expr)
	{
		final Type bool_type = BoolType.getBuiltinType();

		if (expr.isError())
		{
			return;
		}

		if (expr.getType() == null) { debugNoTypeSkip(expr.getName()); return; }

		if (expr.getType() != bool_type)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error4_Test, expr.getType().getName()));
		}
	}

	void DoExitCheck(STO expr)
	{
		final Type int_type = IntType.getBuiltinType();

		if (expr.isError())
		{
			return;
		}

		if (expr.getType() == null) { debugNoTypeSkip(expr.getName()); return; }

		if (expr.getType() != int_type)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error7_Exit, expr.getType().getName()));
		}
	}

	void DoCheckIfInsideLoop(String errorMsg)
	{
		if (m_symtab.isInsideLoop() == false)
		{
			m_nNumErrors++;
			m_errors.print(errorMsg);
		}
	}

	void DoNewCheck(STO expr, Vector<STO> ctorExpr)
	{
		if (expr.isError())
		{
			return;
		}

		if (expr.isModLValue() == false)
		{
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error16_New_var);
			return;
		}

		if (expr.getType() == null) { debugNoTypeSkip(expr.getName()); return; }

		if (!(expr.getType() instanceof PointerType))
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error16_New, expr.getType().getName()));
			return;
		}

		PointerType type = (PointerType) expr.getType();
		if (type.getDerefType() instanceof StructType)
		{
			DoFuncCall(((StructType) type.getDerefType()).getCtorList(), ctorExpr);
		}
		else if (ctorExpr != null)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error16b_NonStructCtorCall, expr.getType().getName()));
		}
	}

	void DoDeleteCheck(STO expr)
	{
		if (expr.isError())
		{
			return;
		}

		if (expr.isModLValue() == false)
		{
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error16_Delete_var);
			return;
		}

		if (expr.getType() == null) { debugNoTypeSkip(expr.getName()); return; }

		if (!(expr.getType() instanceof PointerType))
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error16_Delete, expr.getType().getName()));
		}
	}

	STO DoTypeCast(Type type, STO expr)
	{
		final Type float_type = FloatType.getBuiltinType();
		final Type bool_type  = BoolType.getBuiltinType();
		final Type int_type   = IntType.getBuiltinType();
		final Type nullptr    = PointerType.getNullPtrBuiltinType();

		String name = "((" + type.getName() + ") " + expr.getName() + " )";

		if (type.isError() || expr.isError())
		{
			return new ErrorSTO(name);
		}

		boolean exprTypeOK = (expr.getType() != nullptr) &&
			(expr.getType() == float_type ||
			expr.getType() == bool_type ||
			expr.getType() == int_type ||
			expr.getType() instanceof PointerType);

		boolean typeOK = (type != nullptr) &&
			(type == float_type ||
			type == bool_type ||
			type == int_type ||
			type instanceof PointerType);

		if (exprTypeOK == false || typeOK == false || "nullptr".equals(expr.getName()) == true)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error20_Cast, expr.getType().getName(), type.getName()));
			return new ErrorSTO(name);
		}

		if (expr.isConst())
		{
			BigDecimal number = ((ConstSTO) expr).getValue();

			if (type == float_type) return debugConstantFolding(new ConstSTO(name, float_type, number));
			if (type == bool_type)  return debugConstantFolding(new ConstSTO(name, bool_type, BigDecimal.ZERO.compareTo(number) != 0));
			if (type == int_type)   return debugConstantFolding(new ConstSTO(name, int_type, number.intValue()));
		}

		return new ExprSTO(name, type);
	}

	STO DoSizeof(STO expr)
	{
		String name = "sizeof( " + expr.getName() + " )";

		if (expr.getIsAddressable() == false)
		{
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error19_Sizeof);
			return new ErrorSTO(name);
		}

		Type type = expr.getType();

		if (m_debugMode) { System.err.println("sizeof expression: " + name + " returns: " + type.getSize()); }

		return new ConstSTO(name, IntType.getBuiltinType(), type.getSize());
	}

	STO DoSizeof(Type basicType, Vector<STO> arrayList)
	{
		Type type = createArrayType(basicType, arrayList);

		String name = "sizeof( " + type.getName() + " )";

		if (type.isError())
		{
			return new ErrorSTO(name);
		}

		if (m_debugMode) { System.err.println("sizeof expression: " + name + " returns: " + type.getSize()); }

		return new ConstSTO(name, IntType.getBuiltinType(), type.getSize());
	}
	
	void PrintCOutAssembly(Vector<STO> exprList)
	{
		m_myAsWriter.COutAssembly(exprList);
	}
	
}
