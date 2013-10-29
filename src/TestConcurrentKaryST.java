import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.StringTokenizer;
public class TestConcurrentKaryST
{
	static Node grandParentHead;
	static Node parentHead;
	static long nodeCount=0;

	static long lookup(Node node, long target)
	{
		boolean ltLastKey;
		while(node.childrenArray !=null) //loop until a leaf or dummy node is reached
		{
			ltLastKey=false;
			for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
			{
				if(target < node.keys[i])
				{
					ltLastKey = true;
					node = node.childrenArray[i];
					break;
				}
			}
			if(!ltLastKey)
			{
				node = node.childrenArray[Node.NUM_OF_KEYS_IN_A_NODE];
			}
		}
		if(node.keys == null) //dummy node is reached
		{
			//System.out.println("key not found");
			return(0);
		}
		else //leaf node is reached
		{
			for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
			{
				if(target == node.keys[i])
				{
					//System.out.println("key found");
					return(1);
				}
			}
			//System.out.println("key not found");
			return(0);
		}
	}

	static void insert(Node node, long insertKey)
	{
		boolean ltLastKey;
		int nthChild=0;
		Node pnode=null;
		while(node.childrenArray !=null) //loop until a leaf or dummy node is reached
		{
			ltLastKey=false;

			for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
			{
				if(insertKey < node.keys[i])
				{
					ltLastKey = true;
					pnode = node;
					node = node.childrenArray[i];
					break;
				}
			}
			if(!ltLastKey)
			{
				pnode = node;
				node = node.childrenArray[Node.NUM_OF_KEYS_IN_A_NODE];
			}
		}
		for(int i =0;i<Node.NUM_OF_CHILDREN_FOR_A_NODE;i++) //get the child id w.r.t the parent
		{
			if(pnode.childrenArray[i] == node)
			{
				nthChild = i;
				break;
			}
		}
		if(node.keys == null) //dummy node is reached
		{
			//System.out.println("This dummy node can be replaced with a new leaf node containing the key");	
			long[] keys = new long[Node.NUM_OF_KEYS_IN_A_NODE];
			keys[0] = insertKey;
			pnode.childrenArray[nthChild] = new Node(keys,"leafNode"); //this has to be atomic
			return;
		}
		else //leaf node is reached
		{
			Node replaceNode = new Node(node.keys,"leafNode");
			for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
			{
				if(replaceNode.keys[i] > 0)
				{
					if(insertKey == replaceNode.keys[i])
					{
						//key is already found
						return;
					}
				}
				else // leaf has a empty slot
				{
					replaceNode.keys[i] = insertKey;
					pnode.childrenArray[nthChild] = replaceNode; //this has to be atomic
					return;
				}

			}
			//here the leaf is full
			//find the minimum key in the leaf and if insert key is greater than min key then do a swap
			long[] tempInternalkeys = node.keys;
			long extrakey;
			long min = tempInternalkeys[0];
			int  minPos = 0;
			for(int i=1;i<tempInternalkeys.length;i++)
			{
				if(tempInternalkeys[i]<min)
				{
					min = tempInternalkeys[i];
					minPos = i;
				}
			}
			if(insertKey > min)
			{
				extrakey = min;
				tempInternalkeys[minPos] = insertKey;
			}
			else
			{
				extrakey = insertKey;
			}
			Arrays.sort(tempInternalkeys);

			Node tempInternalNode = new Node(tempInternalkeys,"internalNode");
			long[] tempLeafKeys = new long[Node.NUM_OF_KEYS_IN_A_NODE];
			tempLeafKeys[0] = extrakey;
			tempInternalNode.childrenArray[0] = new Node(tempLeafKeys,"leafNode");
			for(int i=1;i<Node.NUM_OF_CHILDREN_FOR_A_NODE;i++)
			{
				tempLeafKeys[0] = tempInternalkeys[i-1];
				tempInternalNode.childrenArray[i] = new Node(tempLeafKeys,"leafNode");
			}
			pnode.childrenArray[nthChild] = tempInternalNode; //this has to be atomic
			return;
		}
	}

	static void delete(Node node, Node pnode, Node gpnode, long deleteKey)
	{
		boolean ltLastKey;
		int nthChild=0,nthParent=0;
		int atleast2Keys=0;
		while(node.childrenArray !=null) //loop until a leaf or dummy node is reached
		{
			ltLastKey=false;

			for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
			{
				if(deleteKey < node.keys[i])
				{
					ltLastKey = true;
					gpnode = pnode;
					pnode = node;
					node = node.childrenArray[i];
					break;
				}
			}
			if(!ltLastKey)
			{
				gpnode = pnode;
				pnode = node;
				node = node.childrenArray[Node.NUM_OF_KEYS_IN_A_NODE];
			}
		}
		for(int i=0;i<Node.NUM_OF_CHILDREN_FOR_A_NODE;i++) //get the child id w.r.t the parent
		{
			if(pnode.childrenArray[i] == node)
			{
				nthChild = i;
				break;
			}
		}
		for(int i=0;i<Node.NUM_OF_CHILDREN_FOR_A_NODE;i++) //get the parent id w.r.t the grandparent
		{
			if(gpnode.childrenArray[i] == pnode)
			{
				nthParent = i;
				break;
			}
		}
		if(node.keys == null) //dummy node is reached
		{
			//In dummy node - Delete cannot delete a non-existent key	
			return;
		}
		else //leaf node is reached
		{
			Node replaceNode = new Node(node.keys,"leafNode");
			for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
			{
				if(replaceNode.keys[i] > 0)
				{
					atleast2Keys++;
				}
				if(deleteKey == replaceNode.keys[i])
				{
					//key is found for delete
					if(atleast2Keys > 1) //simple delete
					{
						replaceNode.keys[i] = 0;
						pnode.childrenArray[nthChild] = replaceNode; //this has to be atomic
						return;
					}
					else
					{
						for(int j=i+1;j<Node.NUM_OF_KEYS_IN_A_NODE;j++)
						{
							if(replaceNode.keys[j] > 0) //simple delete
							{
								replaceNode.keys[i] = 0;
								pnode.childrenArray[nthChild] = replaceNode; //this has to be atomic
								return;
							}
						}
						//might be pruning delete
						//int index=0;
						int nonDummyChildCount=0;
						int[] nonDummyChildIndex = new int[3];
						for(int k=0;k<Node.NUM_OF_CHILDREN_FOR_A_NODE && nonDummyChildCount<3;k++)
						{
							if(pnode.childrenArray[k].keys != null)
							{
								nonDummyChildIndex[nonDummyChildCount++] = k;
							}

						}
						if(nonDummyChildCount > 2)
						{
							//replace this leaf node with a dummy node as it has only 1 key 
							pnode.childrenArray[nthChild] = new Node(); //this has to be atomic
							return;
						}
						else
						{
							//pruning delete
							if(nonDummyChildCount ==2)
							{
								if(pnode.childrenArray[nonDummyChildIndex[0]] == node)
								{
									gpnode.childrenArray[nthParent] = pnode.childrenArray[nonDummyChildIndex[1]]; //this has to be atomic
									return;
								}
								else
								{
									gpnode.childrenArray[nthParent] = pnode.childrenArray[nonDummyChildIndex[0]]; //this has to be atomic
									return;
								}
							}
							else
							{
								//deleting the first and last key in the tree
								pnode.childrenArray[0] = new Node();
							}
						}
					}
				}
			}
			//System.out.println("In leaf node - Delete cannot delete a non-existent key");
		}
		return;
	}

	static void printPreorder(Node node)
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

	static void printOnlyKeysPreorder(Node node)
	{
		if(node == null)
		{
			return;
		}
		if(node.childrenArray == null && node.keys != null)
		{
			for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
			{
				System.out.print(node.keys[i] + "\t");
			}
			System.out.println();
		}

		if(node.childrenArray != null)
		{
			for(int i=0;i<Node.NUM_OF_CHILDREN_FOR_A_NODE;i++)
			{
				printOnlyKeysPreorder(node.childrenArray[i]);
			}
		}
	}

	static void nodeCount(Node node)
	{
		if(node == null)
		{
			return;
		}
		if(node.childrenArray == null && node.keys != null)
		{
			for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
			{
				if(node.keys[i] > 0)
				{
					nodeCount++;
				}
			}
		}

		if(node.childrenArray != null)
		{
			for(int i=0;i<Node.NUM_OF_CHILDREN_FOR_A_NODE;i++)
			{
				nodeCount(node.childrenArray[i]);
			}
		}
	}
	
	static void getUserInput()
	{
		String in="";
		String operation="";
		StringTokenizer st;
		FileInputStream fs;
		try
		{
			fs = new FileInputStream("input.txt");
			DataInputStream ds = new DataInputStream(fs);
			BufferedReader reader = new BufferedReader(new InputStreamReader(ds));
			while(!(in = reader.readLine()).equalsIgnoreCase("quit"))
			{
				st = new StringTokenizer(in);
				operation = st.nextToken();
				if(operation.equalsIgnoreCase("Find"))
				{
					lookup(grandParentHead,Long.parseLong(st.nextToken()));
				}
				else if(operation.equalsIgnoreCase("Insert"))
				{
					insert(grandParentHead,Long.parseLong(st.nextToken()));
				}
				else if(operation.equalsIgnoreCase("Delete"))
				{
					delete(parentHead.childrenArray[0],parentHead,grandParentHead,Long.parseLong(st.nextToken()));
				}
				
			}
			ds.close();
		} 
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}

	static void createHeadNodes()
	{
		long[] keys = new long[Node.NUM_OF_KEYS_IN_A_NODE];

		for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
		{
			keys[i] = Long.MAX_VALUE;
		}

		grandParentHead = new Node(keys,"internalNode");
		grandParentHead.childrenArray[0] = new Node(keys,"internalNode");
		parentHead = grandParentHead.childrenArray[0];
	}

	public static void main(String[] args)
	{
		createHeadNodes();
		
		getUserInput();

		printOnlyKeysPreorder(grandParentHead);
		nodeCount(grandParentHead);
		System.out.println(nodeCount);

	}
}








