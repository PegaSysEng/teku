package tech.pegasys.artemis.util.backing.view;

import java.util.ArrayList;
import tech.pegasys.artemis.util.backing.ContainerViewRead;
import tech.pegasys.artemis.util.backing.ViewRead;
import tech.pegasys.artemis.util.backing.tree.TreeNode;
import tech.pegasys.artemis.util.backing.type.CompositeViewType;

public class ContainerViewReadImpl extends AbstractCompositeViewRead<ContainerViewReadImpl, ViewRead>
    implements ContainerViewRead {

  public ContainerViewReadImpl(CompositeViewType type, TreeNode backingNode) {
    super(type, backingNode);
  }

  public ContainerViewReadImpl(
      CompositeViewType type, TreeNode backingNode, ArrayList<ViewRead> cache) {
    super(type, backingNode, cache);
  }

  @Override
  protected ViewRead getImpl(int index) {
    CompositeViewType type = getType();
    TreeNode node = getBackingNode().get(type.getGeneralizedIndex(index));
    return type.getChildType(index).createFromBackingNode(node);
  }

  @Override
  public ContainerViewWriteImpl createWritableCopy() {
    return new ContainerViewWriteImpl(this);
  }

  @Override
  protected int sizeImpl() {
    return (int) getType().getMaxLength();
  }

  @Override
  protected void checkIndex(int index) {
    if (index >= size()) {
      throw new IndexOutOfBoundsException(
          "Invalid index " + index + " for container with size " + size());
    }
  }
}
