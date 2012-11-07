package jd.ide.eclipse.realignment.menu;

import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;

@SuppressWarnings("restriction")
public class AttachCommandHandler extends AbstractHandler
{
  public Object execute(ExecutionEvent event) throws ExecutionException
  {
    Set<PackageFragmentRoot> roots = SelectUtils.getSelectedRoots();

    for (PackageFragmentRoot root : roots)
    {
      if (!DecompUtils.isDecompilerAttached(root))
      {
        DecompUtils.doDecompilerAttach(root);
      }
    }

    return null;
  }
}
