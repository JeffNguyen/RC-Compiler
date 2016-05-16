public class VoidType extends Type
{
	private final static VoidType builtin = new VoidType();

	private VoidType()
	{
		super("void", 0); /* XXX, in C sizeof(void) == 1 */
	}

	public static Type getBuiltinType()
	{
		return builtin;
	}
}
