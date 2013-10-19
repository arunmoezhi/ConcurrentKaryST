
public class TestConcurrentKaryST
{
	public static void printPreorder(Node node)
	{
		if(node == null)
		{
			return;
		}
		if(node.keys != null)
		{
			for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
			{
				System.out.print(node.keys[i] + "\t");
			}
		}
		else
		{
			System.out.print("Dummy Node");
		}
		System.out.println();
		if(node.childrenArray != null)
		{
			for(int i=0;i<Node.NUM_OF_CHILDREN_FOR_A_NODE;i++)
			{
				printPreorder(node.childrenArray[i]);
			}
		}
	}

	public static void main(String[] args)
	{
		long[] keys = new long[Node.NUM_OF_KEYS_IN_A_NODE];
		Node grandParentHead;
		Node parentHead;
		Node root;

		for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
		{
			keys[i] = 1L<<62;
		}

		grandParentHead = new Node(keys,"internalNode");
		grandParentHead.childrenArray[0] = new Node(keys,"internalNode");
		parentHead = grandParentHead.childrenArray[0];

		for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
		{
			keys[i] = (i+1)*10;
		}
		parentHead.childrenArray[0] = new Node(keys,"internalNode");
		root = parentHead.childrenArray[0];
		root.childrenArray[1] = new Node(keys,"leafNode");
		printPreorder(grandParentHead);
	}
}








