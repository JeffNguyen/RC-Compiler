public class PointerType extends Type
{
	private final static Type nullptrType = new PointerType("nullptr", VoidType.getBuiltinType());

	private Type m_derefType;

	public PointerType(Type type)
	{
		super(type.getName() + "*", 4);
		m_derefType = type;
	}

	private PointerType(String name, Type type)
	{
		super(name, 4);
		m_derefType = type;
	}

	public Type getDerefType()
	{
		return m_derefType;
	}

	public static Type getNullPtrBuiltinType()
	{
		return nullptrType;
	}

	public static Type createPointers(Type type, int count)
	{
		while (count > 0)
		{
			type = new PointerType(type);
			count--;
		}

		return type;
	}

}
