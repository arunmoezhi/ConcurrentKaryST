
public class ReplaceFlag extends UpdateStep
{
	Node l, p, newChild;
	int pIndex;
	
	public ReplaceFlag(Node l, Node p, int pIndex, Node newChild)
	{
		this.l = l;
		this.p = p;
		this.pIndex = pIndex;
		this.newChild = newChild;
	}
}
