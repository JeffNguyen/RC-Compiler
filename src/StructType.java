public class StructType extends Type
{
	private FuncListSTO m_ctorList;
	private Scope m_scope;

	public StructType(String name)
	{
		super(name, 0 /* not known yet */);
		m_scope = new Scope(false);
	}

	public void addVar(VarSTO var)
	{
		m_scope.InsertLocal(var);
		setSize(getSize() + var.getType().getSize());
	}

	public void setCtorList(FuncListSTO list)
	{
		if (list == null)
		{
			list = new FuncListSTO(this.getName());

			FuncSTO defaultCtor = new FuncSTO(this.getName(), list);
			defaultCtor.setReturnType(VoidType.getBuiltinType(), false);

			list.addOverload(defaultCtor);
		}

		m_ctorList = list;
	}

	public FuncListSTO getCtorList()
	{
		return m_ctorList;
	}

	public Scope getScope()
	{
		return m_scope;
	}
}
