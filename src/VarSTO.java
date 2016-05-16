//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

class VarSTO extends STO
{
	private boolean m_refValue; /* XXX, it might be better to have pass-by-reference information inside Type */

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public VarSTO(String strName, Type typ)
	{
		this(strName, typ, false);
	}

	public VarSTO(String strName, Type typ, boolean refValue)
	{
		super(strName, typ);
		setIsAddressable(true);

		if (!(typ instanceof ArrayType))
			setIsModifiable(true);

		m_refValue = refValue;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public boolean isVar() 
	{
		return true;
	}

	public boolean getRefValue()
	{
		return m_refValue;
	}
}
