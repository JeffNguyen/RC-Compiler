//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

//---------------------------------------------------------------------
// This is the top of the Type hierarchy. You most likely will need to
// create sub-classes (since this one is abstract) that handle specific
// types, such as IntType, FloatType, ArrayType, etc.
//---------------------------------------------------------------------

abstract class Type
{
	// Name of the Type (e.g., int, bool, some structdef, etc.)
	private String m_typeName;
	private int m_size;

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public Type(String strName, int size)
	{
		setName(strName);
		setSize(size);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public String getName()
	{
		return m_typeName;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	private void setName(String str)
	{
		m_typeName = str;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public int getSize()
	{
		return m_size;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	protected void setSize(int size)
	{
		m_size = size;
	}

	//----------------------------------------------------------------
	//	It will be helpful to ask a Type what specific Type it is.
	//	The Java operator instanceof will do this, but you may
	//	also want to implement methods like isNumeric(), isInt(),
	//	etc. Below is an example of isInt(). Feel free to
	//	change this around.
	//----------------------------------------------------------------
	public boolean  isError()   { return false; }
	public boolean  isInt()	    { return false; }

	static boolean checkTypeAssignable(Type x, Type y)
	{
		/* Check if type y can be assigned to type x */
		if (x.equals(y) ||
		   (x == FloatType.getBuiltinType() && y == IntType.getBuiltinType()) ||
		   (x instanceof PointerType && y == PointerType.getNullPtrBuiltinType()))
		{
			return true;
		}

		return false;
	}

	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;

		if (!(obj instanceof Type))
			return false;

		Type type = (Type) obj;

		/* XXX, lazy way */
		return this.getName().equals(type.getName());
	}

}
