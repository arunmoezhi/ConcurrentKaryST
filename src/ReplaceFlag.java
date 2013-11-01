
public class ReplaceFlag extends UpdateStep
{
	final Node l, p, newChild;
	final int pIndex;
	final long insertKey;
	
	public ReplaceFlag(Node l, Node p, int pIndex, Node newChild, long insertKey)
	{
		this.l = l;
		this.p = p;
		this.pIndex = pIndex;
		this.newChild = newChild;
		this.insertKey = insertKey;
	}
}
