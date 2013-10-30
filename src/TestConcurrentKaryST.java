import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.*;
public class TestConcurrentKaryST implements Runnable
{
	public static final int NUM_OF_THREADS=2;
	static Node grandParentHead;
	static Node parentHead;
	static long nodeCount=0;
	int threadId;
	static final AtomicReferenceFieldUpdater<Node, Node> c0Update = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "c0");
	static final AtomicReferenceFieldUpdater<Node, Node> c1Update = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "c1");
	static final AtomicReferenceFieldUpdater<Node, Node> c2Update = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "c2");
	static final AtomicReferenceFieldUpdater<Node, Node> c3Update = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "c3");
	static final AtomicReferenceFieldUpdater<Node, UpdateStep> infoUpdate = AtomicReferenceFieldUpdater.newUpdater(Node.class, UpdateStep.class, "pending");
	public TestConcurrentKaryST(int threadId)
	{
		this.threadId = threadId;
	}

	static long lookup(Node node, long target)
	{
		boolean ltLastKey;
		while(node.c0 !=null) //loop until a leaf or dummy node is reached
		{
			ltLastKey=false;
			for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
			{
				if(target < node.keys[i])
				{
					ltLastKey = true;
					switch(i)
					{
					case 0:
						node = node.c0;
						break;
					case 1:
						node = node.c1;
						break;
					case 2:
						node = node.c2;
						break;	
					}
					break;
				}
			}

			if(!ltLastKey)
			{
				node = node.c3;
			}
		}
		if(node.keys == null) //dummy node is reached
		{
			//key not found
			return(0);
		}
		else //leaf node is reached
		{
			for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
			{
				if(target == node.keys[i])
				{
					//key found
					return(1);
				}
			}
			//key not found
			return(0);
		}
	}

	static void insert(Node root, long insertKey)
	{
		boolean ltLastKey;
		boolean isLeafFull=true;
		int emptySlotId;
		int nthChild;
		Node node=null;
		Node pnode=null;
		Node currentLeaf=null;
		UpdateStep pPending;
		Node replaceNode;
		while(true)
		{
			node = root;
			isLeafFull=true;
			emptySlotId=0;
			nthChild=-1;
			currentLeaf=null;

			while(node.c0 !=null) //loop until a leaf or dummy node is reached
			{
				ltLastKey=false;

				for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
				{
					if(insertKey < node.keys[i])
					{
						ltLastKey = true;
						pnode = node;
						switch(i)
						{
						case 0:	node = node.c0;break;
						case 1: node = node.c1;break;
						case 2: node = node.c2;break;	
						}
						break;
					}
				}
				if(!ltLastKey)
				{
					pnode = node;
					node = node.c3;
				}
			}
			pPending=pnode.pending;	
			//get the child id w.r.t the parent
			if(pnode.c0 == node)
			{
				currentLeaf = pnode.c0;
				nthChild = 0;
			}
			else if(pnode.c1 == node)
			{
				currentLeaf = pnode.c1;
				nthChild = 1;
			}
			else if(pnode.c2 == node)
			{
				currentLeaf = pnode.c2;
				nthChild = 2;
			}
			else if(pnode.c3 == node)
			{
				currentLeaf = pnode.c3;
				nthChild = 3;
			}
			if(node != currentLeaf )
			{
				continue;
			}

			if(pPending.getClass() != Clean.class)
			{
				System.out.println(Thread.currentThread().getId() + "trying to insert " + insertKey + " but info record is not clean and hence calling help");
				help(pPending,insertKey);
			}
			else
			{
				if(node.keys == null) //dummy node is reached
				{
					//This dummy node can be replaced with a new leaf node containing the key
					long[] keys = new long[Node.NUM_OF_KEYS_IN_A_NODE];
					keys[0] = insertKey;
					replaceNode = new Node(keys,"leafNode");
				}
				else //leaf node is reached
				{
					for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
					{
						if(node.keys[i] > 0)
						{
							if(insertKey == node.keys[i])
							{
								//key is already found
								System.out.println(Thread.currentThread().getId() + " " + insertKey + " is already found");
								return;
							}
						}
						else // leaf has a empty slot
						{
							isLeafFull=false;	
							emptySlotId = i;
						}
					}

					if(!isLeafFull)
					{
						replaceNode = new Node(node.keys,"leafNode");
						replaceNode.keys[emptySlotId] = insertKey;
					}
					else
					{
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

						replaceNode = new Node(tempInternalkeys,"internalNode");
						long[] tempLeafKeys = new long[Node.NUM_OF_KEYS_IN_A_NODE];
						tempLeafKeys[1]=0;
						tempLeafKeys[2]=0;
						tempLeafKeys[0] = extrakey;
						replaceNode.c0 = new Node(tempLeafKeys,"leafNode");
						tempLeafKeys[0] = tempInternalkeys[0];
						replaceNode.c1 = new Node(tempLeafKeys,"leafNode");
						tempLeafKeys[0] = tempInternalkeys[1];
						replaceNode.c2 = new Node(tempLeafKeys,"leafNode");
						tempLeafKeys[0] = tempInternalkeys[2];
						replaceNode.c3 = new Node(tempLeafKeys,"leafNode");
					}
				}

				ReplaceFlag op = new ReplaceFlag(node, pnode, nthChild, replaceNode);

				if(infoUpdate.compareAndSet(pnode, pPending, op))
				{
					System.out.println(Thread.currentThread().getId() + "trying to insert " + insertKey + " and successfully updated info record");
					helpReplace(op,insertKey);
					return;
				}
				else
				{
					System.out.println(Thread.currentThread().getId() + "trying to insert " + insertKey + " but failed to update info record. So helping it");
					help(pnode.pending,insertKey);
				}	
			}
		}
	}

	static void help(UpdateStep pending, long insertKey)
	{
		if(pending.getClass() != Clean.class)
		{
			helpReplace((ReplaceFlag) pending, insertKey);
		}
	}

	static void helpReplace(ReplaceFlag pending, long insertKey)
	{
		
		switch (pending.pIndex)
		{                                                  
		case 0: if(c0Update.compareAndSet(pending.p, pending.l, pending.newChild)) System.out.println(Thread.currentThread().getId() + "inserted " + insertKey + " at c0");  else System.out.println("Somebody helped "+ Thread.currentThread().getId() + " to insert " + insertKey + " at c0"); break;
		case 1: if(c1Update.compareAndSet(pending.p, pending.l, pending.newChild)) System.out.println(Thread.currentThread().getId() +  "inserted " + insertKey + " at c1"); else System.out.println("Somebody helped "+ Thread.currentThread().getId() + " to insert " + insertKey + " at c1"); break;
		case 2: if(c2Update.compareAndSet(pending.p, pending.l, pending.newChild)) System.out.println(Thread.currentThread().getId() +  "inserted " + insertKey + " at c2"); else System.out.println("Somebody helped "+ Thread.currentThread().getId() + " to insert " + insertKey + " at c2");break;
		case 3: if(c3Update.compareAndSet(pending.p, pending.l, pending.newChild)) System.out.println(Thread.currentThread().getId() +  "inserted " + insertKey + " at c3"); else System.out.println("Somebody helped "+ Thread.currentThread().getId() + " to insert " + insertKey + " at c3");break;

		default: assert(false); break;
		}

		infoUpdate.compareAndSet(pending.p, pending, new Clean());
	}
	/*
	static void simpleInsert(Node node,Node pnode, int nthChild, Node replaceNode)
	{
		Node oldChild;

		switch (nthChild)
		{                                                  
		case 0: oldChild=pnode.c0;c0Update.compareAndSet(pnode, oldChild, replaceNode); break;
		case 1: oldChild=pnode.c1;c1Update.compareAndSet(pnode, oldChild, replaceNode); break;
		case 2: oldChild=pnode.c2;c2Update.compareAndSet(pnode, oldChild, replaceNode); break;
		case 3: oldChild=pnode.c3;c3Update.compareAndSet(pnode, oldChild, replaceNode); break;

		default: assert(false); break;
		}
		//pnode.childrenArray[nthChild] = replaceNode; //this has to be atomic
	}
	 */
	/*
	static void delete(Node node, Node pnode, Node gpnode, long deleteKey)
	{
		boolean ltLastKey;
		int nthChild=0,nthParent=0;
		int atleast2Keys=0;
		while(node.c0 !=null) //loop until a leaf or dummy node is reached
		{
			ltLastKey=false;

			for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
			{
				if(deleteKey < node.keys[i])
				{
					ltLastKey = true;
					gpnode = pnode;
					pnode = node;
					switch(i)
					{
					case 0:
						node = node.c0;
						break;
					case 1:
						node = node.c1;
						break;
					case 2:
						node = node.c2;
						break;	
					}
					break;
				}
			}
			if(!ltLastKey)
			{
				gpnode = pnode;
				pnode = node;
				node = node.c3;
			}
		}
		//get the child id w.r.t the parent
		if(pnode.c0 == node)
		{
			nthChild = 0;
		}
		else if(pnode.c1 == node)
		{
			nthChild = 1;
		}
		else if(pnode.c2 == node)
		{
			nthChild = 2;
		}
		else if(pnode.c3 == node)
		{
			nthChild = 3;
		}

		//get the parent id w.r.t the grandparent
				if(gpnode.c0 == pnode)
				{
					nthParent = 0;
				}
				else if(gpnode.c1 == pnode)
				{
					nthParent = 1;
				}
				else if(gpnode.c2 == pnode)
				{
					nthParent = 2;
				}
				else if(gpnode.c3 == pnode)
				{
					nthParent = 3;
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
						simpleInsert(node,pnode,nthChild,replaceNode);
						//pnode.childrenArray[nthChild] = replaceNode; //this has to be atomic
						return;
					}
					else
					{
						for(int j=i+1;j<Node.NUM_OF_KEYS_IN_A_NODE;j++)
						{
							if(replaceNode.keys[j] > 0) //simple delete
							{
								replaceNode.keys[i] = 0;
								simpleInsert(node,pnode,nthChild,replaceNode);
								//pnode.childrenArray[nthChild] = replaceNode; //this has to be atomic
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

	 */

	static void printPreorder(Node node)
	{
		if(node == null)
		{
			return;
		}
		if(node.keys != null)
		{
			if(node.c0 == null)
			{
				System.out.print("L" + "\t");
				for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
				{
					System.out.print(node.keys[i] + "\t");
				}
			}
			else
			{
				System.out.print("I" + "\t");
				for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
				{
					System.out.print(node.keys[i] + "\t");
				}
			}
		}
		else
		{
			System.out.print("Dummy Node");
		}
		System.out.println();
		if(node.c0 != null)
		{
			printPreorder(node.c0);
			printPreorder(node.c1);
			printPreorder(node.c2);
			printPreorder(node.c3);
		}
	}

	static void printOnlyKeysPreorder(Node node)
	{
		if(node == null)
		{
			return;
		}
		if(node.c0 == null && node.keys != null)
		{
			for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
			{
				System.out.print(node.keys[i] + "\t");
			}
			System.out.println();
		}

		if(node.c0 != null)
		{
			printOnlyKeysPreorder(node.c0);
			printOnlyKeysPreorder(node.c1);
			printOnlyKeysPreorder(node.c2);
			printOnlyKeysPreorder(node.c3);

		}
	}

	static void nodeCount(Node node)
	{
		if(node == null)
		{
			return;
		}
		if(node.c0 == null && node.keys != null)
		{
			for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
			{
				if(node.keys[i] > 0)
				{
					nodeCount++;
				}
			}
		}

		if(node.c0 != null)
		{
			nodeCount(node.c0);
			nodeCount(node.c1);
			nodeCount(node.c2);
			nodeCount(node.c3);
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
		grandParentHead.c0 = new Node(keys,"internalNode");
		parentHead = grandParentHead.c0;
	}

	static void getUserInput(int fileNumber)
	{
		String in="";
		String operation="";
		StringTokenizer st;
		FileInputStream fs;
		try
		{
			fs = new FileInputStream("in" + fileNumber + ".txt");
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
					//delete(parentHead.c0,parentHead,grandParentHead,Long.parseLong(st.nextToken()));
				}

			}
			ds.close();
		} 
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}

	public void run()
	{
		getUserInput(this.threadId);
	}

	public static void main(String[] args)
	{
		createHeadNodes();

		Thread[] arrayOfThreads = new Thread[NUM_OF_THREADS];

		for(int i=0;i<NUM_OF_THREADS;i++)
		{
			arrayOfThreads[i] = new Thread(  new TestConcurrentKaryST(i));
			arrayOfThreads[i].start();
		}

		try
		{
			for(int i=0;i<NUM_OF_THREADS;i++)
			{
				arrayOfThreads[i].join();
			}
		}
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}

		printPreorder(grandParentHead);
		//printOnlyKeysPreorder(grandParentHead);
		nodeCount(grandParentHead);
		System.out.println(nodeCount);

	}
}