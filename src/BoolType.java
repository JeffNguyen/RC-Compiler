public class BoolType extends Type
{
	private final static BoolType builtin = new BoolType();

	private BoolType()
	{
		super("bool", 4);
	}

	public static Type getBuiltinType()
	{
		return builtin;
	}
}
