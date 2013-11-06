import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.concurrent.atomic.*;
public class ConcurrentKaryST
{

	static Node grandParentHead;
	static Node parentHead;
	static ConcurrentKaryST obj;
	static long nodeCount=0;
	static final AtomicReferenceFieldUpdater<Node, Node> c0Update = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "c0");
	static final AtomicReferenceFieldUpdater<Node, Node> c1Update = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "c1");
	static final AtomicReferenceFieldUpdater<Node, Node> c2Update = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "c2");
	static final AtomicReferenceFieldUpdater<Node, Node> c3Update = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "c3");
	static final AtomicReferenceFieldUpdater<Node, UpdateStep> infoUpdate = AtomicReferenceFieldUpdater.newUpdater(Node.class, UpdateStep.class, "pending");
	FileOutputStream outf;
	PrintStream out;
	public ConcurrentKaryST()
	{
		try 
		{
			outf = new FileOutputStream("out.txt");
			out = new PrintStream(outf);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}

	public final long lookup(Node node, long target)
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

	public final void insert(Node root, long insertKey, int threadId)
	{
		boolean ltLastKey;
		boolean isLeafFull;
		int emptySlotId;
		int nthChild;
		Node node;
		Node pnode;
		Node currentLeaf;
		UpdateStep pPending;
		Node replaceNode;
		while(true)
		{
			ltLastKey=false;
			isLeafFull=true;
			emptySlotId=0;
			nthChild=-1;
			node = root;
			pnode = null;
			currentLeaf=null;
			replaceNode=null;

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

			if(pPending != null && pPending.getClass() != Clean.class)
			{
				//out.println(threadId + "trying to insert " + insertKey + " but info record is not clean and hence calling help");
				help(pPending, threadId);
			}
			else
			{
				if(node.keys == null) //dummy node is reached
				{
					//This dummy node can be replaced with a new leaf node containing the key
					final long[] keys = new long[Node.NUM_OF_KEYS_IN_A_NODE];
					keys[0] = insertKey;
					replaceNode = new Node(keys,"leafNode");
					//out.println(threadId  + "Dummy Node - Trying simple insert for " + insertKey);
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
								//out.println(threadId  + " " + insertKey + " is already found");
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
						//out.println(threadId  + "Non Full Leaf Node - Trying simple insert for " + insertKey);
						replaceNode.keys[emptySlotId] = insertKey;
					}
					else
					{
						//here the leaf is full
						//find the minimum key in the leaf and if insert key is greater than min key then do a swap

						//final long[] tempInternalkeys = node.keys;
						final long[] tempInternalkeys = new long[Node.NUM_OF_KEYS_IN_A_NODE];
						tempInternalkeys[0] = node.keys[0];
						tempInternalkeys[1] = node.keys[1];
						tempInternalkeys[2] = node.keys[2];

						//out.println(threadId  + "Trying to insert " + insertKey + " and this node" + node + " is getting replaced" + tempInternalkeys[0] + tempInternalkeys[1] + tempInternalkeys[2]);
						final long extrakey;
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
						//out.println("by" + tempInternalkeys + " " + tempInternalkeys[0] + tempInternalkeys[1] + tempInternalkeys[2]);
						replaceNode = new Node(tempInternalkeys,"internalNode");
						final long[] tempLeafKeys = new long[Node.NUM_OF_KEYS_IN_A_NODE];
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
						//out.println(threadId + "Full leaf node - Trying sprouting insert for " + insertKey);
					}
				}

				ReplaceFlag op = new ReplaceFlag(node, pnode, nthChild, replaceNode, insertKey);

				if(infoUpdate.compareAndSet(pnode, pPending, op))
				{
					//out.println(threadId  + "trying to insert " + insertKey + " and successfully updated info record");
					helpReplace(op,threadId);
					return;
				}
				else
				{
					//out.println(threadId  + "trying to insert " + insertKey + " but failed to update info record. So helping it");
					help(pnode.pending,threadId);
				}	
			}
		}
	}

	public final void help(final UpdateStep pending, int threadId)
	{
		//out.println(threadId + " trying to help " + pending.getClass());
		if(pending.getClass() != Clean.class)
		{
			if(pending.getClass() == ReplaceFlag.class)
			{
				helpReplace1((ReplaceFlag) pending, threadId);
			}
			else if(pending.getClass() == PruneFlag.class)
			{
				helpPrune((PruneFlag) pending, threadId);
			}
			if(pending.getClass() == Mark.class)
			{
				helpMarked(((Mark)pending).pending, threadId);
			}		
		}
		else
		{
			//out.println("In help method - pending became clean again");
		}
	}

	public final void helpReplace1(ReplaceFlag pending, int threadId)
	{

		switch (pending.pIndex)
		{                                                  
		case 0: 
			if(c0Update.compareAndSet(pending.p, pending.l, pending.newChild)) 
				//out.println(threadId  + "successfully helped inserting " + pending.insertKey + " at c0");  
				//else 
				//out.println("Somebody helped "+ threadId  + " which was trying to help insert " + pending.insertKey + " at c0"); 
				break;
		case 1: 
			if(c1Update.compareAndSet(pending.p, pending.l, pending.newChild))
				//out.println(threadId  + "successfully helped inserting " + pending.insertKey + " at c1");
				//else 
				//out.println("Somebody helped "+ threadId  + " which was trying to help insert " + pending.insertKey + " at c1");
				break;
		case 2: 
			if(c2Update.compareAndSet(pending.p, pending.l, pending.newChild))
				//out.println(threadId  + "successfully helped inserting " + pending.insertKey + " at c2"); 
				//else 
				//out.println("Somebody helped "+ threadId  + " which was trying to help insert " + pending.insertKey + " at c2"); 
				break;
		case 3:
			if(c3Update.compareAndSet(pending.p, pending.l, pending.newChild)) 
				//out.println(threadId  + "successfully helped inserting " + pending.insertKey + " at c3"); 
				//else 
				//out.println("Somebody helped "+ threadId  + " which was trying to help insert " + pending.insertKey + " at c3");
				break;

		default: assert(false); break;
		}

		infoUpdate.compareAndSet(pending.p, pending, new Clean());
	}

	public final void helpReplace(ReplaceFlag pending, int threadId)
	{

		switch (pending.pIndex)
		{                                                  
		case 0:
			if(c0Update.compareAndSet(pending.p, pending.l, pending.newChild))
				//out.println(threadId + "inserted " + pending.insertKey + " at c0"); 
				//else 
				//out.println("Somebody helped "+ threadId + " to insert " + pending.insertKey + " at c0");
				break;
		case 1: 
			if(c1Update.compareAndSet(pending.p, pending.l, pending.newChild))
				//out.println(threadId +  "inserted " + pending.insertKey + " at c1"); 
				//else 
				//out.println("Somebody helped "+ threadId + " to insert " + pending.insertKey + " at c1"); 
				break;
		case 2: 
			if(c2Update.compareAndSet(pending.p, pending.l, pending.newChild))
				//out.println(threadId +  "inserted " + pending.insertKey + " at c2"); 
				//else 
				//out.println("Somebody helped "+ threadId + " to insert " + pending.insertKey + " at c2");
				break;
		case 3: 
			if(c3Update.compareAndSet(pending.p, pending.l, pending.newChild))
				//out.println(threadId+  "inserted " + pending.insertKey + " at c3"); 
				//else 
				//out.println("Somebody helped "+ threadId + " to insert " + pending.insertKey + " at c3");
				break;

		default: assert(false); break;
		}

		infoUpdate.compareAndSet(pending.p, pending, new Clean());
	}

	public final boolean helpPrune(PruneFlag pending, int threadId)
	{
		//precondition - this method is called after successfully flagging gp with pruneflag
		final boolean result;
		result = infoUpdate.compareAndSet(pending.p, pending.ppending, new Mark(pending)); //mark parent with mark flag

		final UpdateStep newValue = pending.p.pending;
		if(result || (newValue.getClass() == Mark.class && ((Mark) newValue).pending == pending)) //marking successful- So go ahead and complete the deletion
		{
			helpMarked(pending,threadId); 
			return true;
		}
		else //marking failed
		{
			help(newValue,threadId); //help the operation pending on parent
			infoUpdate.compareAndSet(pending.gp, pending, new Clean()); //unflag the prune flag set in grandparent
			return false;			
		}
	}

	public final void helpMarked(PruneFlag pending, int threadId)
	{
		Node other=null;
		if(pending.p.c0 != pending.l && pending.p.c0.keys != null)
		{
			other = pending.p.c0;	
		}
		else if(pending.p.c1 != pending.l && pending.p.c1.keys != null)
		{
			other = pending.p.c1;
		}
		else if(pending.p.c2 != pending.l && pending.p.c2.keys != null)
		{
			other = pending.p.c2;
		}
		else if(pending.p.c3 != pending.l && pending.p.c3.keys != null)
		{
			other = pending.p.c3;
		}
		else
		{
			other = pending.p.c0;
		}
		switch(pending.gpIndex) // replace the child pointer of gp (if it is still pointing to the parent) to point to other sibling
		{
		case 0: c0Update.compareAndSet(pending.gp, pending.p, other);break;
		case 1: c1Update.compareAndSet(pending.gp, pending.p, other);break;
		case 2: c2Update.compareAndSet(pending.gp, pending.p, other);break;
		case 3: c3Update.compareAndSet(pending.gp, pending.p, other);break;
		}

		infoUpdate.compareAndSet(pending.gp, pending, new Clean()); //unflag the prune flag set in grandparent
	}

	public final void delete(Node proot, Node gproot, long deleteKey, int threadId)
	{
		boolean ltLastKey;
		boolean keyFound;
		int keyIndex;
		int nthChild;
		int nthParent;
		int atleast2Keys;
		Node node;
		Node pnode;
		Node gpnode;
		UpdateStep pPending;
		UpdateStep gpPending;
		Node replaceNode;
		while(true) //loop until a leaf or dummy node is reached
		{
			ltLastKey=false;
			keyFound=false;
			keyIndex=-1;
			nthChild=-1;
			nthParent=-1;
			atleast2Keys=0;
			pnode=proot;
			gpnode=gproot;
			node=proot.c0;
			replaceNode=null;
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
						case 0:node = node.c0;break;
						case 1:node = node.c1;break;
						case 2:node = node.c2;break;	
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
			pPending=pnode.pending;
			gpPending=gpnode.pending;
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

			if(gpPending != null && gpPending.getClass() != Clean.class)
			{
				//out.println(threadId + "trying to delete " + deleteKey + " but gpinfo record is not clean and hence calling help");
				help(gpPending, threadId);
			}
			else if (pPending != null && pPending.getClass() != Clean.class)
			{
				//out.println(threadId + "trying to delete " + deleteKey + " but pinfo record is not clean and hence calling help");
				help(pPending, threadId);
			}
			else if(node.keys != null) //leaf node is reached
			{

				replaceNode = new Node(node.keys,"leafNode");
				for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
				{
					if(replaceNode.keys[i] > 0)
					{
						atleast2Keys++;
					}
					if(deleteKey == replaceNode.keys[i])
					{
						
						keyFound=true;
						keyIndex=i;
					}
				}

				if(keyFound)
				{
					//out.println(threadId + " found key " + deleteKey);
					if(atleast2Keys > 1) //simple delete
					{
						replaceNode.keys[keyIndex] = 0;
						ReplaceFlag op = new ReplaceFlag(node, pnode, nthChild, replaceNode, deleteKey);
						if(infoUpdate.compareAndSet(pnode, pPending, op))
						{
							//out.println(threadId  + "trying to delete " + deleteKey + " and successfully updated info record");
							helpReplace(op,threadId);
							return;
						}
						else
						{
							//out.println(threadId  + "trying to delete " + deleteKey + " but failed to update info record. So helping it");
							help(pnode.pending,threadId);
						}	
					}
					else //only 1 key is present in leaf. Have to check if parent has at least 3 non-dummy children. 
					{
						int nonDummyChildCount=0;
						if(pnode.c0.keys != null)
						{
							nonDummyChildCount++;
						}
						if(pnode.c1.keys != null)
						{
							nonDummyChildCount++;
						}
						if(pnode.c2.keys != null)
						{
							nonDummyChildCount++;
						}
						if(pnode.c3.keys != null)
						{
							nonDummyChildCount++;
						}
						if(nonDummyChildCount != 2) //simple delete. Replace leaf node with a dummy node
						{
							//Note: This can be deleting the first and last key in the tree
							replaceNode = new Node();
							ReplaceFlag op = new ReplaceFlag(node, pnode, nthChild, replaceNode, deleteKey);
							if(infoUpdate.compareAndSet(pnode, pPending, op))
							{
								//out.println(threadId  + "trying to delete " + deleteKey + " and successfully updated info record");
								helpReplace(op,threadId);
								return;
							}
							else
							{
								//out.println(threadId  + "trying to delete " + deleteKey + " but failed to update info record. So helping it");
								help(pnode.pending,threadId);
							}	
						}
						else//pruning delete. Only this node and another sibling exist. Make the gp point to the sibling.
						{
							//out.println(threadId  + " trying a pruning delete for " + deleteKey);
							final PruneFlag op = new PruneFlag(node,pnode,gpnode,nthParent,pPending);
							if(infoUpdate.compareAndSet(gpnode, gpPending, op)) //flag gp with prune flag
							{
								//out.println(threadId  + " trying a pruning delete for " + deleteKey + " and successfully flagged gp" );
								if(helpPrune(op,threadId))
								{
									//out.println(threadId  + " trying a pruning delete for " + deleteKey + " and helpPrune was successful" );
									return;
								}
							}
							else
							{
								//out.println(threadId  + " trying a pruning delete for " + deleteKey + " and failed to flag gp" );
								help(gpnode.pending,threadId);
							}
						}
					}
				}
				else
				{
					//key not found
					//out.println(threadId + "'s search for " + deleteKey + " ended in a leaf node but key not found");
					return;
				}
			}
			else
			{
				//dummy node is reached
				//out.println(threadId + "'s search for " + deleteKey + " ended in a dummy node");
				return;
			}
		}
	}
	//out.println("In leaf node - Delete cannot delete a non-existent key");


	public final void printPreorder(Node node)
	{
		if(node == null)
		{
			return;
		}
		if(node.keys != null)
		{
			if(node.c0 == null)
			{
				out.print("L" + "\t");
				for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
				{
					out.print(node.keys[i] + "\t");
				}
			}
			else
			{
				out.print("I" + "\t");
				for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
				{
					out.print(node.keys[i] + "\t");
				}
			}
		}
		else
		{
			out.print("Dummy Node");
		}
		out.println();
		if(node.c0 != null)
		{
			printPreorder(node.c0);
			printPreorder(node.c1);
			printPreorder(node.c2);
			printPreorder(node.c3);
		}
	}

	public final void printOnlyKeysPreorder(Node node)	
	{
		if(node == null)
		{
			return;
		}
		if(node.c0 == null && node.keys != null)
		{
			for(int i=0;i<Node.NUM_OF_KEYS_IN_A_NODE;i++)
			{
				out.print(node.keys[i] + "\t");
			}
			out.println();
		}

		if(node.c0 != null)
		{
			printOnlyKeysPreorder(node.c0);
			printOnlyKeysPreorder(node.c1);
			printOnlyKeysPreorder(node.c2);
			printOnlyKeysPreorder(node.c3);

		}
	}

	public final void nodeCount(Node node)
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

	public final void createHeadNodes()
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

}