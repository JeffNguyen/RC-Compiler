public class IntType extends Type
{
	private final static IntType builtin = new IntType();

	private IntType()
	{
		super("int", 4);
	}

	public static Type getBuiltinType()
	{
		return builtin;
	}
}
