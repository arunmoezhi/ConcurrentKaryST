
public class Node
{
	public static final int K = 4;
	public static final int NUM_OF_KEYS_IN_A_NODE= K-1;
	public static final int NUM_OF_CHILDREN_FOR_A_NODE= K;
	long keys[];
	volatile Node c0,c1,c2,c3;
	volatile UpdateStep pending = null;
	public Node()
	{
		
	}
	public Node(long[] keys, String nodeType)
	{
		pending = new Clean();
		this.keys = new long[NUM_OF_KEYS_IN_A_NODE];
		for(int i=0;i<NUM_OF_KEYS_IN_A_NODE;i++)
		{
			this.keys[i] = keys[i];
		}
		if(nodeType.equalsIgnoreCase("internalNode"))
		{
			c0 = (Node) new DummyNode();
			c1 = (Node) new DummyNode();
			c2 = (Node) new DummyNode();
			c3 = (Node) new DummyNode();
			
		}
	}
}
