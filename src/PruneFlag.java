
public class PruneFlag extends UpdateStep 
{
	
	final Node l, p, gp;
	final UpdateStep ppending;
	final int gpIndex;
	
	public PruneFlag(Node l, Node p, Node gp, int gpIndex, UpdateStep ppending)
	{
		this.l = l;
		this.p = p;
		this.gp = gp;
		this.gpIndex = gpIndex;
		this.ppending = ppending;
	}
}
