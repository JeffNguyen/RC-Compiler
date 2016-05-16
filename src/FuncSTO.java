//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

import java.util.Vector;

class FuncSTO extends STO
{
	private Type m_returnType;
	private boolean m_returnByReference;
	private int m_returnStatementMinLevel = -1;
	private Vector<VarSTO> m_params;
	private FuncListSTO m_list;

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public FuncSTO(String strName, FuncListSTO list)
	{
		super (strName);
		setReturnType(null, false);
		m_list = list;
		// You may want to change the isModifiable and isAddressable                      
		// fields as necessary
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public boolean isFunc() 
	{ 
		return true;
		// You may want to change the isModifiable and isAddressable                      
		// fields as necessary
	}

	//----------------------------------------------------------------
	// This is the return type of the function. This is different from 
	// the function's type (for function pointers - which we are not 
	// testing in this project).
	//----------------------------------------------------------------
	public void setReturnType(Type typ, boolean returnByReference)
	{
		m_returnType = typ;
		m_returnByReference = returnByReference;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public Type getReturnType ()
	{
		return m_returnType;
	}

	public boolean getReturnTypeByReference()
	{
		return m_returnByReference;
	}

	public void setParams(Vector<VarSTO> params)
	{
		m_params = params;
	}

	public Vector<VarSTO> getParams()
	{
		return m_params;
	}

	public void setReturnStatementLevel(int level)
	{
		if (m_returnStatementMinLevel == -1 || level < m_returnStatementMinLevel)
			m_returnStatementMinLevel = level;
	}

	public int returnStatementMinLevel()
	{
		return m_returnStatementMinLevel;
	}

	public FuncListSTO getList() { return m_list; }
}
