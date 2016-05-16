public class FloatType extends Type
{
	private final static FloatType builtin = new FloatType();

	private FloatType()
	{
		super("float", 4);
	}

	public static Type getBuiltinType()
	{
		return builtin;
	}
}
