This project implements a "Concurrent K-ary Search Tree".
Reference - Non-blocking k-ary Search Trees - Trever Brown and Joanna Helga - OPODIS 2011
Source Files:
Node.java - Creates a internal node with k-children and k-1 keys. Or create a leaf node with K-1 keys.
DummyNode.java - Subclass of Node. Have to use dummy nodes instead of NULL pointers to avoid ABA problem
UpdateStep.java - This is an object stored in an internal node which has complete information about the operation being performed on this Node's children.
Clean.java - subclass of UpdateStep. Clean flag on parent node
Mark.java - subclass of UpdateStep. Create a Mark on a node. Used for pruning delete
PruneFlag.java - subclass of UpdateStep. Create a PruneFlag which is used for pruning delete
ReplaceFlag.java - subclass of UpdateStep. Create a ReplaceFlag which is used to help inserts
ConcurrentKaryST.java - Performs insert, delete and lookup
TestConcurrentKaryST.java - Test bench for the ConcurrentKaryST

How to compile:
javac *.java
How to run:
java TestConcurrentKaryST <# of threads>
For example to run with 4 threads do,
java TestConcurrentKaryST 4
This will read 4 input files in0.txt in1.txt in2.txt and in3.txt.
Thread 0 will run independently and once it completes, all other threads will execute in parallel. 
Output:
# of nodes in the tree

Input format:
Insert <key>
Find <key>
Delete <key>
each input file should terminate with a "quit" keyword

Note:
the input files are referenced using relative path ../input/in*.txt. This will work fine in unix.
for Windows change this accordingly.
