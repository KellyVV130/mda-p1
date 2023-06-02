package wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class newGenProjectPage extends WizardPage {

	protected newGenProjectPage(String pageName) {
		super(pageName);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createControl(Composite parent) {
		// TODO Auto-generated method stub
		final Composite container = new Composite(parent, parent.getStyle());
        container.setLayout(new GridLayout(1, false));
        setControl(container);
        
        
	}

}
