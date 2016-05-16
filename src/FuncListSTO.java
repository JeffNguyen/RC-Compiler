//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

import java.util.Vector;

class FuncListSTO extends STO
{
	private Vector<FuncSTO> m_funcs;

	public FuncListSTO(String strName)
	{
		super(strName);

		m_funcs = new Vector<FuncSTO>();
	}

	public boolean isFunc() 
	{ 
		return true;
	}

	private boolean paramsMatch(Vector<VarSTO> a, Vector<VarSTO> b)
	{
		if (a == null && b == null)
			return true;

		if (a == null || b == null)
			return false;

		if (a.size() != b.size())
			return false;

		for (int i = 0; i < a.size(); i++)
		{
			Type aa = a.get(i).getType();
			Type bb = b.get(i).getType();

			if (aa.equals(bb) == false)
				return false;
		}

		return true;
	}

	private boolean paramsArgsMatch(Vector<STO> b, Vector<VarSTO> a)
	{
		if (a == null && b == null)
			return true;

		if (a == null || b == null)
			return false;

		if (a.size() != b.size())
			return false;

		for (int i = 0; i < a.size(); i++)
		{
			VarSTO param   = a.get(i);
			STO arg        = b.get(i);
			Type paramType = param.getType();
			Type argType   = arg.getType();

			if (argType.equals(paramType) == false)
				return false;

			if (param.getRefValue() == true && arg.isModLValue() == false)
				return false;
		}

		return true;
	}

	public boolean addOverload(FuncSTO sto)
	{
		Vector<VarSTO> newParams = sto.getParams();

		for (FuncSTO f : m_funcs)
		{
			Vector<VarSTO> par = f.getParams();

			if (paramsMatch(newParams, par) == true)
				return false;
		}

		m_funcs.add(sto);
		return true;
	}

	public FuncSTO findOverloadOrOnlyOne(Vector<STO> args)
	{
		/* if function has no overloads, return first item */
		if (m_funcs.size() == 1)
			return m_funcs.get(0);

		for (FuncSTO f : m_funcs)
		{
			Vector<VarSTO> par = f.getParams();

			if (paramsArgsMatch(args, par) == true)
				return f;
		}

		return null;
	}
}
