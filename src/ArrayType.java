public class ArrayType extends Type
{
	private Type m_itemType;
	private int m_count;

	public ArrayType(String name, Type type, int size)
	{
		super(
			name,
			type.getSize() * size);

		m_itemType = type;
		m_count    = size;
	}

	public Type getItemType()
	{
		return m_itemType;
	}

	public int getCount()
	{
		return m_count;
	}
}
