package wizard;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.papyrus.infra.core.resource.IModel;
import org.eclipse.papyrus.infra.core.resource.ModelSet;
import org.eclipse.papyrus.infra.core.resource.sasheditor.DiModel;
import org.eclipse.papyrus.infra.core.services.ServiceException;
import org.eclipse.papyrus.infra.emf.utils.ResourceUtils;
import org.eclipse.papyrus.infra.gmfdiag.export.actions.ExportAllDiagramsParameter;
import org.eclipse.papyrus.infra.gmfdiag.export.actions.ExportComposite;
import org.eclipse.papyrus.infra.gmfdiag.export.engine.ExportAllDiagramsEngine;
import org.eclipse.papyrus.infra.gmfdiag.export.messages.Messages;
import org.eclipse.papyrus.infra.gmfdiag.export.utils.SelectionHelper;
import org.eclipse.papyrus.infra.ui.util.ServiceUtilsForSelection;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;


public class ExportAllWizard extends Wizard implements IExportWizard {
	/** wizard page to export all diagram from a Papyrus model. */
	private ExportDiagramsPage page;

	/** error wizard page shown when the selected file is incorrect. */
	private ExportDiagramsErrorPage pageError;

	/** Selected file. */
	private IFile file;

	/** The export all diagrams. */
	private ExportAllDiagramsEngine exportAllDiagrams = null;

	/** The Export parameter. */
	private ExportAllDiagramsParameter parameter = null;

	/**
	 * Constructor.
	 *
	 */
	public ExportAllWizard() {
		super();
		setWindowTitle(Messages.ExportAllWizard_1);
		exportAllDiagrams = new ExportAllDiagramsEngine();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean performFinish() {
		// TODO Auto-generated method stub
		return false;
	}

}
