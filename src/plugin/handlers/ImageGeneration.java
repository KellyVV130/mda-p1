package plugin.handlers;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.ui.dialogs.DiagnosticDialog;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.gmf.runtime.diagram.core.preferences.PreferencesHint;
import org.eclipse.gmf.runtime.diagram.ui.image.ImageFileFormat;
import org.eclipse.gmf.runtime.diagram.ui.render.util.CopyToImageUtil;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.papyrus.infra.core.editor.ModelSetServiceFactory;
import org.eclipse.papyrus.infra.core.resource.EditingDomainServiceFactory;
import org.eclipse.papyrus.infra.core.resource.ModelMultiException;
import org.eclipse.papyrus.infra.core.resource.ModelSet;
import org.eclipse.papyrus.infra.core.resource.ModelsReader;
import org.eclipse.papyrus.infra.core.sasheditor.di.contentprovider.DiSashModelManager;
import org.eclipse.papyrus.infra.core.sashwindows.di.service.IPageManager;
import org.eclipse.papyrus.infra.core.services.ExtensionServicesRegistry;
import org.eclipse.papyrus.infra.core.services.ServiceDescriptor;
import org.eclipse.papyrus.infra.core.services.ServiceDescriptor.ServiceTypeKind;
import org.eclipse.papyrus.infra.core.services.ServiceException;
import org.eclipse.papyrus.infra.core.services.ServiceStartKind;
import org.eclipse.papyrus.infra.core.services.ServicesRegistry;
import org.eclipse.papyrus.infra.core.utils.ServiceUtils;
import org.eclipse.papyrus.infra.emf.utils.ServiceUtilsForResourceSet;
import org.eclipse.papyrus.infra.gmfdiag.css.service.CssMarkerEventManagerService;
import org.eclipse.papyrus.infra.gmfdiag.css.service.MarkerToPseudoSelectorMappingService;
import plugin.Activator;
import org.eclipse.papyrus.infra.gmfdiag.export.DialogDisplayUtils;
import org.eclipse.papyrus.infra.gmfdiag.export.actions.ExportAllDiagramsParameter;
import org.eclipse.papyrus.infra.gmfdiag.export.engine.DiagramNameProvider;
import org.eclipse.papyrus.infra.gmfdiag.export.engine.ExportDiagramLocalPageService;
import org.eclipse.papyrus.infra.gmfdiag.export.messages.Messages;
import org.eclipse.papyrus.infra.services.decoration.DecorationService;
import org.eclipse.papyrus.infra.services.markerlistener.MarkersMonitorService;
import org.eclipse.papyrus.infra.services.semantic.service.SemanticService;
import org.eclipse.papyrus.infra.services.validation.IValidationMarkersService;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;


/**
 * Engine to export all diagrams to images.
 */
public class ImageGeneration {

	/** The workbench window. */
	private IWorkbenchWindow workbenchWindow;

	/** The display renaming information. */
	private boolean displayRenamingInformation;

	/** The use display runnable. */
	private static boolean useDisplayRunnable = true;


	/** The diagnostic. */
	private BasicDiagnostic diagnostic = null;

	/** The has duplicates. */
	private boolean hasDuplicates = false;

	/** The export parameter. */
	private ExportAllDiagramsParameter exportParameter = null;

	private DiagramNameProvider diagramNameProvider;


	/**
	 * Constructor.
	 *
	 */
	public ImageGeneration() {
		try {
			this.workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		} catch (IllegalStateException e) {
			this.workbenchWindow = null;
			// is normal in batch mode
		}
	}


	/**
	 * Initialise.
	 *
	 * @param parameter
	 *            the parameter
	 */
	public void initialise(ExportAllDiagramsParameter parameter) {
		diagnostic = new BasicDiagnostic(Diagnostic.OK, "", 0, Messages.ExportAllDiagrams_18, null);
		displayRenamingInformation = true;
		exportParameter = parameter;


	}

	/**
	 * Run the export of all diagrams of a *.*di file into images in the given
	 * format.
	 * @return 
	 */
	public IStatus exportDiagramsToImages(IProgressMonitor monitor) {


		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		IProgressMonitor newMonitor = monitor;

		return export(newMonitor);

		

	}

	/**
	 * Export all diagrams of the IFile.
	 *
	 * @param newMonitor
	 *            the new monitor
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private IStatus export(IProgressMonitor newMonitor) {
		IStatus status = Status.OK_STATUS;

		// Then iterates on all the diagrams and export them one by one
		newMonitor.beginTask(Messages.ExportAllDiagrams_1, 10);
		newMonitor.subTask(Messages.ExportAllDiagrams_2);

		if (exportParameter != null) {
			ModelSet modelSet = exportParameter.getModelSet();
			try {

				// Step 1 : Get a Model set
				if (modelSet == null) {
					modelSet = initialiseModelSet();

					if (diagnostic.getSeverity() != Diagnostic.OK || newMonitor.isCanceled()) {
						return handleDiagnosticStatus();
					}

					// Step 2 : Initialise necessary service registry
					initialiseServiceRegistry(modelSet);
					if (diagnostic.getSeverity() != Diagnostic.OK || newMonitor.isCanceled()) {
						return handleDiagnosticStatus();
					}
				}




				// Get pages manager from service registry
				IPageManager pageManager;
				SemanticService semanticService;
				try {
					pageManager = ServiceUtilsForResourceSet.getInstance().getService(IPageManager.class, modelSet);
					semanticService = ServiceUtilsForResourceSet.getInstance().getService(SemanticService.class, modelSet);
				} catch (ServiceException e) {
					Activator.log(e);
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to obtain required services.", e);
				}

				if (diagnostic.getSeverity() != Diagnostic.OK || newMonitor.isCanceled()) {
					return handleDiagnosticStatus();
				}

				// Step 3 : Get all diagram
				List<Diagram> diagrams = Stream.of(semanticService.getSemanticRoots())
						.map(ExportDiagramLocalPageService::new)
						.flatMap(service -> pageManager.allLocalPages(service).stream())
						.filter(Diagram.class::isInstance)
						.map(Diagram.class::cast)
						.distinct()
						.collect(Collectors.toList());

				if (newMonitor.isCanceled()) {
					return handleDiagnosticStatus();
				}
				newMonitor.worked(1);

				// Step 4 : Export each diagram
				export(new SubProgressMonitor(newMonitor, 9), diagrams);

			} finally {

				if (exportParameter.getModelSet() == null) {
					// Unload the resource set so that we don't leak loads of UML content in the CacheAdapter
					modelSet.unload();
				}

			}
		} else {
			status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.ExportAllDiagrams_17);
		}


		return status;

	}

	/**
	 * Handle diagnostic status.
	 *
	 * @return the corresponding status
	 */
	private IStatus handleDiagnosticStatus() {
		diagnostic.recomputeSeverity();
		return BasicDiagnostic.toIStatus(diagnostic);
	}

	/**
	 * Create Models set from selected file.
	 *
	 * @return the model set
	 */
	private ModelSet initialiseModelSet() {
		ServicesRegistry service = null;

		try {
			service = new ExtensionServicesRegistry();
		} catch (ServiceException e) {
			Activator.log(e);
			service = new ServicesRegistry(); // This won't really work
		}

		// Override service factory for Model Set
		ServiceDescriptor descriptor = new ServiceDescriptor(ModelSet.class, ModelSetServiceFactory.class.getName(), ServiceStartKind.STARTUP, 10);
		descriptor.setServiceTypeKind(ServiceTypeKind.serviceFactory);
		service.add(descriptor);

		// Override factory for editing domain
		descriptor = new ServiceDescriptor(TransactionalEditingDomain.class, EditingDomainServiceFactory.class.getName(), ServiceStartKind.STARTUP, 10, Collections.singletonList(ModelSet.class.getName()));
		descriptor.setServiceTypeKind(ServiceTypeKind.serviceFactory);
		service.add(descriptor);

		try {
			service.startServicesByClassKeys(
					ModelSet.class,
					TransactionalEditingDomain.class);
		} catch (ServiceException e) {
			Activator.log(e);
		}

		ModelSet modelSet = null;
		try {
			modelSet = ServiceUtils.getInstance().getModelSet(service);
		} catch (ServiceException e) {
			// Ignore service exception
		}

		// Instantiate a Model set
		if (modelSet == null) {
			modelSet = new ModelSet();
			try {
				ModelSetServiceFactory.setServiceRegistry(modelSet, service);
			} catch (ServiceException e) {
				// Ignore service exception
			}
		}



		// Read all Model from selected file
		ModelsReader modelsReader = new ModelsReader();
		modelsReader.readModel(modelSet);
		try {
			modelSet.loadModels(exportParameter.getDiFileUri());
		} catch (ModelMultiException e) {
			diagnostic = new BasicDiagnostic(Diagnostic.ERROR, Activator.PLUGIN_ID, 0, Messages.ExportAllDiagrams_3, new Object[] { e });
		}
		
		// Start all remaining services
		try {
			service.startRegistry();
		} catch (ServiceException e) {
			// Ignore errors: in headless mode, UI services may be missing
		}

		// Initialise an editing domain
		modelSet.getTransactionalEditingDomain();

		return modelSet;
	}

	/**
	 * Initialise service registry.
	 *
	 * @param modelSet
	 *            the model set
	 */
	private void initialiseServiceRegistry(ModelSet modelSet) {
		ServicesRegistry service = ModelSetServiceFactory.getServiceRegistry(modelSet);

		try {

			if (service == null) {
				service = new ExtensionServicesRegistry();
				startModelSetServices(modelSet, service);
			}

			startCSSServices(service);
			startModelerServices(service);

		} catch (ServiceException e) {
			diagnostic = new BasicDiagnostic(Diagnostic.ERROR, Activator.PLUGIN_ID, 0, "Failed to load models", new Object[] { e });
		}
	}


	/**
	 * Starts the model set services.
	 *
	 * @param modelSet
	 *            the model set
	 * @param service
	 *            the service
	 */
	private void startModelSetServices(ModelSet modelSet, ServicesRegistry service) throws ServiceException {
		service.add(ModelSet.class, 10, modelSet);

		service.startServicesByClassKeys(ModelSet.class);
	}


	/**
	 * Starts the modeler services.
	 *
	 * @param service
	 *            the service
	 */
	private void startModelerServices(ServicesRegistry service) throws ServiceException {
		service.startServicesByClassKeys(
				DiSashModelManager.class,
				IPageManager.class,
				SemanticService.class);
	}


	/**
	 * Starts the css services.
	 *
	 * @param service
	 *            the service
	 */
	private void startCSSServices(ServicesRegistry service) throws ServiceException {
		service.startServicesByClassKeys(
				MarkersMonitorService.class,
				CssMarkerEventManagerService.class,
				MarkerToPseudoSelectorMappingService.class,
				IValidationMarkersService.class,
				DecorationService.class);
	}

	/**
	 * export all the diagrams in image.
	 *
	 * @param newMonitor
	 *            , the monitor
	 * @param diagrams
	 *            , the emf element diagrams
	 */
	public void export(IProgressMonitor newMonitor, List<Diagram> diagrams) {
		int tasksAmount = 9;
		if (exportParameter.getModelSet() == null) {
			tasksAmount++;
		}
		newMonitor.beginTask(Messages.ExportAllDiagrams_4, tasksAmount);

		diagramNameProvider = new DiagramNameProvider(diagrams, exportParameter.isQualifiedName());
		diagramNameProvider.getDiagnostic().forEach(this.diagnostic::addAll);

		// Create file associate to all diagram
		createDiagramFiles(new SubProgressMonitor(newMonitor, 9), diagrams);

		// If the model set is already loaded in Papyrus, unload must be skipped
		if (exportParameter.getModelSet() == null) {
			unloadResources(new SubProgressMonitor(newMonitor, 1), diagrams);
		}

		// Alert the user that file names have been changed to avoid duplicates
		if (displayRenamingInformation && diagrams.stream().anyMatch(diagramNameProvider::hasDuplicates)) {
			final String message = Messages.ExportAllDiagrams_5;
			if (workbenchWindow != null && workbenchWindow.getShell() != null) {

				BasicDiagnostic newDiagnostic = new BasicDiagnostic(Diagnostic.WARNING, "", 0, message, null); //$NON-NLS-1$
				diagnostic.add(newDiagnostic);

			} else {
				Activator.log(IStatus.INFO, message);
			}
		}

		handleExportDiagnostic();

	}


	/**
	 * Handle export diagnostic.
	 */
	private void handleExportDiagnostic() {
		int severity = diagnostic.recomputeSeverity();

		if (severity == Diagnostic.ERROR) {
			BasicDiagnostic oldDiagnostic = diagnostic;
			diagnostic = new BasicDiagnostic(Diagnostic.ERROR, "", 0, Messages.ExportAllDiagrams_22, null); //$NON-NLS-1$
			diagnostic.addAll(oldDiagnostic);
		} else if (severity == Diagnostic.WARNING) {
			BasicDiagnostic oldDiagnostic = diagnostic;
			diagnostic = new BasicDiagnostic(Diagnostic.WARNING, "", 0, Messages.ExportAllDiagrams_24, null); //$NON-NLS-1$
			diagnostic.addAll(oldDiagnostic);
		}

		// Display dialog to validate export
		if (workbenchWindow != null && workbenchWindow.getShell() != null && exportParameter.isDisplayStatus()) {
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {
					if (diagnostic.getSeverity() == Diagnostic.OK) {
						MessageDialog.openInformation(DialogDisplayUtils.getActiveWorkbenchShell(), Messages.ExportAllDiagrams_25, Messages.ExportAllDiagrams_26 + exportParameter.getOutputDirectoryPath());
					} else {
						DiagnosticDialog.open(DialogDisplayUtils.getActiveWorkbenchShell(), Messages.ExportAllDiagrams_27, "", diagnostic); //$NON-NLS-1$
					}
				}
			});
		}

	}

	/**
	 * Browse all the diagrams and export them.
	 *
	 * @param newMonitor
	 *            the new monitor
	 * @param diagrams
	 *            the diagrams
	 * @return true, if there is no duplicates
	 */
	private void createDiagramFiles(final IProgressMonitor newMonitor, List<Diagram> diagrams) {
		try {

			Map<Diagram, String> diagramNames = diagramNameProvider.getDiagramNames();

			try {
				newMonitor.beginTask(Messages.ExportAllDiagrams_7, diagrams.size());

				for (final Diagram diagram : diagrams) {

					// Verify if export was cancelled
					if (newMonitor.isCanceled()) {
						break;
					}

					String uniqueFileName = diagramNames.get(diagram);

					final String finalUniqueFileName = uniqueFileName;
					newMonitor.subTask(uniqueFileName);
					if (useDisplayRunnable) {
						Display.getDefault().syncExec(new Runnable() {

							@Override
							public void run() {
								exportDiagram(finalUniqueFileName, diagram, newMonitor, diagramNames);
							}
						});
					} else {
						exportDiagram(uniqueFileName, diagram, newMonitor, diagramNames);
					}

					newMonitor.worked(1);
				}
			} catch (SWTError e) {
				String message = Messages.ExportAllDiagrams_9;
				Activator.log(message, new Exception(message, e));
			}
		} catch (Exception e) {
			Activator.log(e);
		}
	}

	/**
	 * Export diagram.
	 *
	 * @param uniqueFileName
	 *            the unique file name
	 * @param diagram
	 *            the diagram
	 * @param newMonitor
	 *            the new monitor
	 * @param diagramNames
	 */
	private void exportDiagram(String uniqueFileName, Diagram diagram, IProgressMonitor newMonitor, Map<Diagram, String> diagramNames) {
		CopyToImageUtil copyImageUtil = new CopyToImageUtil();

		// Build path of image
		IPath imagePath = new Path(exportParameter.getOutputDirectoryPath() + File.separator + uniqueFileName);
		imagePath = imagePath.addFileExtension(exportParameter.getExportFormat().getName());

		try {
			copyImageUtil.copyToImage(diagram, imagePath, exportParameter.getExportFormat(), new SubProgressMonitor(newMonitor, 1), PreferencesHint.USE_DEFAULTS);

		} catch (Throwable e) {
			BasicDiagnostic newDiagnostic = new BasicDiagnostic(Diagnostic.ERROR, "", 0, String.format(Messages.ExportAllDiagrams_11, uniqueFileName, diagram.eResource().getURI().toString()), null); //$NON-NLS-1$
			diagnostic.add(newDiagnostic);
			String errorMessage = String.format(Messages.ExportAllDiagrams_11, uniqueFileName, diagram.eResource().getURI().toString());
			Activator.log(errorMessage, e);
		}
	}

	/**
	 * Unload resources.
	 *
	 * @param newMonitor
	 *            the new monitor
	 * @param diagrams
	 *            the diagrams
	 */
	public void unloadResources(IProgressMonitor newMonitor, List<Diagram> diagrams) {
		if (newMonitor == null) {
			newMonitor = new NullProgressMonitor();
		}
		newMonitor.subTask(Messages.ExportAllDiagrams_12);
		if (diagrams != null && !diagrams.isEmpty()) {
			ResourceSet diagramResourceSet = diagrams.get(0).eResource().getResourceSet();

			newMonitor.beginTask(Messages.ExportAllDiagrams_13, diagramResourceSet.getResources().size());
			for (int i = diagramResourceSet.getResources().size() - 1; i >= 0; i--) {
				try {
					Resource resource = diagramResourceSet.getResources().get(i);
					if (resource.isLoaded()) {
						resource.unload();
					}
				} catch (Exception e) {
					// not very clean but it sometimes occurs
				}
				newMonitor.worked(1);
			}
		}

	}

}
