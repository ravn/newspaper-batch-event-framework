package dk.statsbibliokeket.newspaper.treenode;

/**
 *
 */
public class TreeNodeStateWithChildren extends TreeNodeState {

    @Override
    protected TreeNode createNode(String name, NodeType nodeType, TreeNode previousNode, String location) {
        return new TreeNodeWithChildren(name, nodeType, (TreeNodeWithChildren) previousNode, location);
    }
}
