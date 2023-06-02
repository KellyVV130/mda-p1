package wizard;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.papyrus.infra.gmfdiag.export.actions.ExportAllDiagramsParameter;
import org.eclipse.papyrus.infra.gmfdiag.export.actions.ExportComposite;
import org.eclipse.papyrus.infra.gmfdiag.export.engine.ExportAllDiagramsEngine;
import org.eclipse.papyrus.infra.gmfdiag.export.messages.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class ExportDiagramPage extends WizardPage {
	
	private ExportComposite export;

	/** The export all diagrams. */
	private ExportAllDiagramsEngine exportAllDiagrams = null;

	/** The Export parameter. */
	private ExportAllDiagramsParameter parameter = null;
	

	public ExportComposite getExport() {
		return export;
	}

	private final IResource outputDirectory;

	/**
	 * Create the wizard.
	 * @since 2.0
	 */
	public ExportDiagramPage(IResource outputDirectory, ExportAllDiagramsParameter parameter) {
		super(Messages.ExportDiagramsPage_0);
		setTitle("Configure digrams format and output directory.");
		this.exportAllDiagrams = new ExportAllDiagramsEngine();
		this.outputDirectory = outputDirectory;
		this.parameter = parameter;
		setTitle(Messages.ExportDiagramsPage_0);
		setDescription(Messages.ExportDiagramsPage_2);
	}

	/**
	 * Create contents of the wizard.
	 *
	 * @param parent
	 */
	@Override
	public void createControl(Composite parent) {
		export = new ExportComposite(parent, SWT.NONE);
		export.setOutputDirectory(outputDirectory);
		setControl(export);
		validatePage();
	}
	
	public void validatePage() {
		boolean performFinish = this.parameter != null;
		setPageComplete(false);
		if (performFinish) {

			ExportComposite exportPage = this.getExport();
			performFinish = exportPage.getOutputDirectory().isAccessible();
			if (performFinish) {
				this.setErrorMessage(null);

				this.parameter.setOutputDirectory(exportPage.getOutputDirectory());
				this.parameter.setExportFormat(exportPage.getExporter());
				this.parameter.setQualifiedName(exportPage.getQualifiedName());

				exportAllDiagrams.initialise(this.parameter);
				// exportAllDiagrams.exportDiagramsToImages();
				setPageComplete(true);
			} else {
				this.setErrorMessage(Messages.ExportAllWizard_0);
				setPageComplete(false);
			}

		} else {
			setPageComplete(false);
		}
		

	}

}
