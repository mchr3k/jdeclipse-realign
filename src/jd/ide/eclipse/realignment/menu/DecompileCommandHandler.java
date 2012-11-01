package jd.ide.eclipse.realignment.menu;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class DecompileCommandHandler extends AbstractHandler
{
  public Object execute(ExecutionEvent event) throws ExecutionException
  {
    System.out.println("foo");
    return null;
  }
}
